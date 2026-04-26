package com.styleaeternum.crm.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CapturedContact contact);

    @Update
    void update(CapturedContact contact);

    @Delete
    void delete(CapturedContact contact);

    @Query("SELECT * FROM contacts ORDER BY capturedAt DESC")
    LiveData<List<CapturedContact>> getAllContacts();

    @Query("SELECT * FROM contacts WHERE groupMembership = :group ORDER BY id ASC")
    LiveData<List<CapturedContact>> getContactsByGroup(String group);

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    CapturedContact getById(String id);

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    CapturedContact getByPhone(String phone);

    @Query("SELECT COUNT(*) FROM contacts WHERE groupMembership = :group")
    int countByGroup(String group);

    @Query("SELECT * FROM contacts ORDER BY capturedAt DESC")
    List<CapturedContact> getAllContactsSync();
}
