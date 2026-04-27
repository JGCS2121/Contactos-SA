package com.styleaeternum.crm.sync;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sincroniza contactos con Google Contacts usando Google People API v1.
 */
public class GoogleContactsSync {

    private static final String TAG = "GoogleContactsSync";
    private static final String SCOPE = "https://www.googleapis.com/auth/contacts";

    public static int syncAll(Context context, GoogleSignInAccount account, ContactRepository repository) {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singletonList(SCOPE));
            credential.setSelectedAccount(account.getAccount());

            PeopleService people = new PeopleService.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Style Aeternum CRM")
                    .build();

            // 1. Obtener contactos locales y normalizar sus números
            List<CapturedContact> localContacts = repository.getAllContactsSync();
            Map<String, CapturedContact> localMap = new HashMap<>();
            for (CapturedContact c : localContacts) {
                localMap.put(normalize(c.phone), c);
            }

            // 2. Obtener contactos de Google (Connections)
            ListConnectionsResponse response = people.people().connections().list("people/me")
                    .setPersonFields("names,phoneNumbers,memberships")
                    .setPageSize(1000)
                    .execute();
            List<Person> googleConnections = response.getConnections();
            Set<String> googlePhones = new HashSet<>();

            int downloadedCount = 0;
            if (googleConnections != null) {
                for (Person p : googleConnections) {
                    if (p.getPhoneNumbers() == null || p.getPhoneNumbers().isEmpty()) continue;

                    for (PhoneNumber pn : p.getPhoneNumbers()) {
                        String norm = normalize(pn.getValue());
                        if (norm.isEmpty()) continue;
                        googlePhones.add(norm);

                        // Si no existe en local, lo bajamos del servidor
                        if (!localMap.containsKey(norm)) {
                            String name = "Google Contact";
                            if (p.getNames() != null && !p.getNames().isEmpty()) {
                                name = p.getNames().get(0).getDisplayName();
                            }
                            
                            CapturedContact nc = new CapturedContact();
                            nc.phone = norm;
                            nc.name = name;
                            nc.id = "google_" + System.currentTimeMillis() + "_" + norm;
                            nc.groupMembership = "Google Sync";
                            nc.capturedAt = System.currentTimeMillis();
                            nc.notes = "Descargado de Google Contacts";
                            
                            repository.insertSync(nc);
                            downloadedCount++;
                        }
                    }
                }
            }

            // 3. Subir contactos locales que no están en Google
            int uploadedCount = 0;
            Map<String, String> groupCache = new HashMap<>();

            for (CapturedContact lc : localContacts) {
                String norm = normalize(lc.phone);
                if (!googlePhones.contains(norm)) {
                    // Determinar ResourceName del grupo (ej: "abril2026")
                    String groupResName = getOrCreateGroup(people, lc.groupMembership, groupCache);

                    Person newPerson = new Person()
                            .setNames(Collections.singletonList(new Name().setGivenName(lc.name)))
                            .setPhoneNumbers(Collections.singletonList(new PhoneNumber().setValue(lc.phone).setType("mobile")))
                            .setMemberships(Collections.singletonList(new Membership()
                                    .setContactGroupMembership(new ContactGroupMembership().setContactGroupResourceName(groupResName))));

                    people.people().createContact(newPerson).execute();
                    uploadedCount++;
                    Log.d(TAG, "Subido a Google: " + lc.name);
                }
            }

            Log.i(TAG, "Sincronización terminada. Subidos: " + uploadedCount + ", Bajados: " + downloadedCount);
            return uploadedCount + downloadedCount;

        } catch (Exception e) {
            Log.e(TAG, "Error crítico en sincronización: " + e.getMessage(), e);
            return -1;
        }
    }

    private static String getOrCreateGroup(PeopleService people, String groupName, Map<String, String> cache) throws Exception {
        if (groupName == null || groupName.trim().isEmpty()) return "contactGroups/myContacts";
        if (cache.containsKey(groupName)) return cache.get(groupName);

        // Listar grupos existentes
        ListContactGroupsResponse res = people.contactGroups().list().execute();
        if (res.getContactGroups() != null) {
            for (ContactGroup g : res.getContactGroups()) {
                if (g.getName().equalsIgnoreCase(groupName)) {
                    cache.put(groupName, g.getResourceName());
                    return g.getResourceName();
                }
            }
        }

        // Crear nuevo grupo
        ContactGroup created = people.contactGroups().create(new CreateContactGroupRequest()
                .setContactGroup(new ContactGroup().setName(groupName))).execute();
        
        cache.put(groupName, created.getResourceName());
        return created.getResourceName();
    }

    private static String normalize(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9+]", "");
    }
}
