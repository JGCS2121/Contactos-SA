package com.styleaeternum.crm.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;
import androidx.annotation.NonNull;

@Database(entities = {CapturedContact.class, ContactLabel.class, Agenda.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    public abstract ContactDao contactDao();
    public abstract LabelDao labelDao();
    public abstract AgendaDao agendaDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN etiqueta TEXT DEFAULT ''");
            database.execSQL("CREATE TABLE IF NOT EXISTS `labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `colorHex` TEXT, `prefix` TEXT)");
            
            // Poblar datos por defecto
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Compró', '#4CAF50', 'Clienta_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('No compró', '#F44336', 'NoCompro_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Interesada', '#FF9800', 'Interesada_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Lista negra', '#424242', 'Bloqueada_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Cliente frecuente', '#2196F3', 'VIP_')");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `agenda` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactoId` TEXT, `nombreCliente` TEXT, `telefono` TEXT, `descripcion` TEXT, `fechaHora` INTEGER NOT NULL, `recordatorio1` INTEGER NOT NULL, `recordatorio2` INTEGER NOT NULL, `estado` TEXT, `notas` TEXT, `fechaCreacion` INTEGER NOT NULL, FOREIGN KEY(`contactoId`) REFERENCES `contacts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        }
    };
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN etiqueta TEXT DEFAULT ''");
            database.execSQL("CREATE TABLE IF NOT EXISTS `labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `colorHex` TEXT, `prefix` TEXT)");
            
            // Poblar datos por defecto
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Compró', '#4CAF50', 'Clienta_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('No compró', '#F44336', 'NoCompro_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Interesada', '#FF9800', 'Interesada_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Lista negra', '#424242', 'Bloqueada_')");
            database.execSQL("INSERT INTO labels (name, colorHex, prefix) VALUES ('Cliente frecuente', '#2196F3', 'VIP_')");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "style_aeternum_crm.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
