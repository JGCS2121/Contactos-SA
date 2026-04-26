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

    private final ContactDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ContactRepository(Context context) {
        dao = AppDatabase.getInstance(context).contactDao();
    }

    public void insert(CapturedContact c) {
        executor.execute(() -> dao.insert(c));
    }

    public void update(CapturedContact c) {
        executor.execute(() -> dao.update(c));
    }

    public void delete(CapturedContact c) {
        executor.execute(() -> dao.delete(c));
    }

    public LiveData<List<CapturedContact>> getAllContacts() {
        return dao.getAllContacts();
    }

    public List<CapturedContact> getAllContactsSync() {
        return dao.getAllContactsSync();
    }

    public CapturedContact getByPhone(String phone) {
        return dao.getByPhone(phone);
    }

    public int countByGroup(String group) {
        return dao.countByGroup(group);
    }
}
