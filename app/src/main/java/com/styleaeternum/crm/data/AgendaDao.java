package com.styleaeternum.crm.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AgendaDao {

    @Query("SELECT * FROM agenda ORDER BY fechaHora ASC")
    LiveData<List<Agenda>> getAll();

    @Query("SELECT * FROM agenda WHERE estado = 'pendiente' AND fechaHora >= :ahora ORDER BY fechaHora ASC")
    LiveData<List<Agenda>> getPendientes(long ahora);

    @Query("SELECT * FROM agenda WHERE estado = 'pendiente' AND fechaHora < :ahora ORDER BY fechaHora DESC")
    LiveData<List<Agenda>> getVencidos(long ahora);

    @Query("SELECT * FROM agenda WHERE contactoId = :contactoId ORDER BY fechaHora DESC")
    LiveData<List<Agenda>> getByContactoId(String contactoId);
    
    @Query("SELECT * FROM agenda WHERE id = :id LIMIT 1")
    Agenda getById(int id);

    @Insert
    long insert(Agenda agenda);

    @Update
    void update(Agenda agenda);

    @Delete
    void delete(Agenda agenda);
}
