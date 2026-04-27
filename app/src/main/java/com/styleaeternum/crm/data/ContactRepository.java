package com.styleaeternum.crm.data;

import android.content.Context;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositorio único que abstrae el acceso a Room DB.
 */
public class ContactRepository {

    private final ContactDao contactDao;
    private final LabelDao labelDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ContactRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        contactDao = db.contactDao();
        labelDao = db.labelDao();
    }

    public void insert(CapturedContact c) {
        executor.execute(() -> contactDao.insert(c));
    }

    public void update(CapturedContact c) {
        executor.execute(() -> contactDao.update(c));
    }

    public void delete(CapturedContact c) {
        executor.execute(() -> contactDao.delete(c));
    }

    public LiveData<List<CapturedContact>> getAllContacts() {
        return contactDao.getAllContacts();
    }

    public List<CapturedContact> getAllContactsSync() {
        return contactDao.getAllContactsSync();
    }

    public CapturedContact getByPhone(String phone) {
        return contactDao.getByPhone(phone);
    }

    public int countByGroup(String group) {
        return contactDao.countByGroup(group);
    }
    
    // Métodos para Etiquetas
    public LiveData<List<ContactLabel>> getAllLabels() {
        return labelDao.getAllLabels();
    }
    
    public void insertLabel(ContactLabel l) {
        executor.execute(() -> labelDao.insert(l));
    }
    
    public void deleteLabel(ContactLabel l) {
        executor.execute(() -> labelDao.delete(l));
    }
}
