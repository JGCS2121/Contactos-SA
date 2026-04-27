package com.styleaeternum.crm.sync;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Membership;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;

import com.styleaeternum.crm.data.CapturedContact;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sincroniza contactos con Google Contacts usando Google People API v1.
 * Requiere scope: https://www.googleapis.com/auth/contacts
 */
public class GoogleContactsSync {

    private static final String TAG = "GoogleContactsSync";
    private static final String SCOPE = "https://www.googleapis.com/auth/contacts";

    /**
     * Sube todos los contactos a Google Contacts.
     * @return número de contactos sincronizados con éxito.
     */
    public static int syncAll(Context context, GoogleSignInAccount account,
                              List<CapturedContact> contacts) {
        if (contacts == null || contacts.isEmpty()) return 0;

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singletonList(SCOPE));
        credential.setSelectedAccount(account.getAccount());

        PeopleService people = new PeopleService.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Style Aeternum CRM")
                .build();

        int synced = 0;
        for (CapturedContact c : contacts) {
            try {
                Person person = new Person()
                        .setNames(Collections.singletonList(
                                new Name().setGivenName(c.name)))
                        .setPhoneNumbers(Collections.singletonList(
                                new PhoneNumber()
                                        .setValue(c.phone)
                                        .setType(c.phoneType)))
                        .setMemberships(Collections.singletonList(
                                new Membership()
                                        .setContactGroupMembership(
                                                new com.google.api.services.people.v1.model
                                                        .ContactGroupMembership()
                                                        .setContactGroupResourceName(
                                                                "contactGroups/myContacts"))));

                people.people().createContact(person).execute();
                synced++;
                Log.i(TAG, "Sincronizado: " + c.id);
            } catch (Exception e) {
                Log.e(TAG, "Error sincronizando " + c.id + ": " + e.getMessage());
            }
        }
        return synced;
    }
}
