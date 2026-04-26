package com.styleaeternum.crm.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.data.ContactRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactsViewModel extends AndroidViewModel {

    private final ContactRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ContactsViewModel(@NonNull Application application) {
        super(application);
        repository = new ContactRepository(application);
    }

    public LiveData<List<CapturedContact>> getAllContacts() {
        return repository.getAllContacts();
    }

    public List<CapturedContact> getAllContactsSync() {
        return repository.getAllContactsSync();
    }

    public LiveData<CapturedContact> getContactById(String id) {
        MutableLiveData<CapturedContact> result = new MutableLiveData<>();
        executor.execute(() -> result.postValue(repository.getByPhone(id)));
        // Usar Room directamente con un wrapper LiveData por ID
        return Transformations.map(getAllContacts(), list -> {
            if (list == null) return null;
            for (CapturedContact c : list) {
                if (c.id.equals(id)) return c;
            }
            return null;
        });
    }

    public void insert(CapturedContact c)  { repository.insert(c);  }
    public void update(CapturedContact c)  { repository.update(c);  }
    public void delete(CapturedContact c)  { repository.delete(c);  }
}
