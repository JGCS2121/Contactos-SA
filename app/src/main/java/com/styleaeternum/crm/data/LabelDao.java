package com.styleaeternum.crm.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LabelDao {

    @Query("SELECT * FROM labels ORDER BY name ASC")
    LiveData<List<ContactLabel>> getAllLabels();

    @Query("SELECT * FROM labels ORDER BY name ASC")
    LiveData<List<ContactLabel>> getAll();

    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    ContactLabel getByName(String name);

    @Insert
    void insert(ContactLabel label);

    @Update
    void update(ContactLabel label);

    @Delete
    void delete(ContactLabel label);
    
    @Query("SELECT COUNT(*) FROM labels")
    int countLabels();
}
