package com.styleaeternum.crm.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.styleaeternum.crm.data.AppDatabase;
import com.styleaeternum.crm.data.CapturedContact;
import com.styleaeternum.crm.databinding.ActivitySmartCaptureBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartCaptureActivity extends AppCompatActivity {

    private ActivitySmartCaptureBinding binding;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySmartCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setTitle("Captura Inteligente");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        executor = Executors.newSingleThreadExecutor();

        binding.btnDetect.setOnClickListener(v -> parseData());
        binding.btnSave.setOnClickListener(v -> saveContact());
    }

    private void parseData() {
        String text = binding.etRawMessage.getText() != null ? binding.etRawMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Pega un mensaje primero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiar campos primero
        binding.etName.setText("");
        binding.etPhone.setText("");
        binding.etAddress.setText("");
        binding.etNeighborhood.setText("");
        binding.etFloorApt.setText("");
        binding.etProduct.setText("");
        binding.etPrice.setText("");

        String[] lines = text.split("\\r?\\n");
        
        // Expresiones regulares para extracción
        Pattern phonePattern = Pattern.compile("(?i)(?:tel(?:éfono)?|cel(?:ular)?)\\s*[:\\-]?\\s*(\\+?\\d[\\d\\s\\-]{8,14})");
        Pattern addressPattern = Pattern.compile("(?i)(?:direcci[oó]n|dir|ubicaci[oó]n)\\s*[:\\-]?\\s*(.+)");
        Pattern neighborhoodPattern = Pattern.compile("(?i)(?:barrio|sector|conjunto|urbanizaci[oó]n|urb)\\s*[:\\-]?\\s*(.+)");
        Pattern floorPattern = Pattern.compile("(?i)(?:piso|apto|apartamento|torre|unidad|interior|int)\\s*[:\\-]?\\s*(.+)");
        Pattern pricePattern = Pattern.compile("(?i)(?:precio|valor|total)\\s*[:\\-]?\\s*\\$?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})?)");
        Pattern namePattern = Pattern.compile("(?i)(?:nombre|cliente)\\s*[:\\-]?\\s*(.+)");

        boolean isStructured = text.toLowerCase().contains("nombre:") || text.toLowerCase().contains("tel:");

        if (isStructured) {
            // Modo estructurado
            for (String line : lines) {
                Matcher m;
                if ((m = namePattern.matcher(line)).find() && TextUtils.isEmpty(binding.etName.getText())) binding.etName.setText(m.group(1).trim());
                else if ((m = phonePattern.matcher(line)).find() && TextUtils.isEmpty(binding.etPhone.getText())) binding.etPhone.setText(m.group(1).replaceAll("[^0-9+]", ""));
                else if ((m = addressPattern.matcher(line)).find() && TextUtils.isEmpty(binding.etAddress.getText())) binding.etAddress.setText(m.group(1).trim());
                else if ((m = neighborhoodPattern.matcher(line)).find() && TextUtils.isEmpty(binding.etNeighborhood.getText())) binding.etNeighborhood.setText(m.group(1).trim());
                else if ((m = floorPattern.matcher(line)).find() && TextUtils.isEmpty(binding.etFloorApt.getText())) binding.etFloorApt.setText(m.group(1).trim());
                else if ((m = pricePattern.matcher(line)).find() && TextUtils.isEmpty(binding.etPrice.getText())) binding.etPrice.setText(m.group(1).trim());
            }
        } else {
            // Modo libre inferido por patrón y posición
            Pattern rawPhone = Pattern.compile("(\\b3\\d{9}\\b|\\b\\d{10}\\b)");
            Pattern rawPrice = Pattern.compile("\\b(\\d{1,3}(?:[.,]\\d{3})+)\\b");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                Matcher mPhone = rawPhone.matcher(line);
                Matcher mPrice = rawPrice.matcher(line);

                if (i == 0 && TextUtils.isEmpty(binding.etName.getText())) {
                    binding.etName.setText(line);
                } else if (mPhone.find() && TextUtils.isEmpty(binding.etPhone.getText())) {
                    binding.etPhone.setText(mPhone.group(1));
                } else if (mPrice.find() && TextUtils.isEmpty(binding.etPrice.getText())) {
                    binding.etPrice.setText(mPrice.group(1));
                } else if (line.matches("(?i).*(carrera|cra|kra|calle|cll|av|avenida|diag|transv|#).*")) {
                    binding.etAddress.setText(line);
                } else if (line.matches("(?i).*(piso|apto|apartamento|torre|unidad|interior).*")) {
                    binding.etFloorApt.setText(line);
                } else if (TextUtils.isEmpty(binding.etNeighborhood.getText()) && !TextUtils.isEmpty(binding.etAddress.getText()) && TextUtils.isEmpty(binding.etProduct.getText())) {
                    // Si ya se llenó la dirección pero no el producto, asumimos que esto puede ser el barrio
                    binding.etNeighborhood.setText(line);
                } else if (TextUtils.isEmpty(binding.etProduct.getText())) {
                    // Si sobra algo, puede ser el producto
                    binding.etProduct.setText(line);
                }
            }
        }
        
        Toast.makeText(this, "Datos detectados. Verifica y ajusta si es necesario.", Toast.LENGTH_SHORT).show();
    }

    private void saveContact() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Nombre y teléfono son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            CapturedContact contact = db.contactDao().getByPhone(phone);
            if (contact == null) {
                contact = new CapturedContact();
                contact.id = java.util.UUID.randomUUID().toString();
                contact.capturedAt = System.currentTimeMillis();
                contact.etiqueta = "Nuevo";
                contact.origen = "Captura Inteligente";
            }
            
            contact.name = name;
            contact.phone = phone;

            StringBuilder notesBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(binding.etAddress.getText())) notesBuilder.append("Dirección: ").append(binding.etAddress.getText().toString().trim()).append("\n");
            if (!TextUtils.isEmpty(binding.etNeighborhood.getText())) notesBuilder.append("Barrio: ").append(binding.etNeighborhood.getText().toString().trim()).append("\n");
            if (!TextUtils.isEmpty(binding.etFloorApt.getText())) notesBuilder.append("Piso/Apto: ").append(binding.etFloorApt.getText().toString().trim()).append("\n");
            if (!TextUtils.isEmpty(binding.etProduct.getText())) notesBuilder.append("Producto: ").append(binding.etProduct.getText().toString().trim()).append("\n");
            if (!TextUtils.isEmpty(binding.etPrice.getText())) notesBuilder.append("Precio: ").append(binding.etPrice.getText().toString().trim()).append("\n");

            String existingNotes = contact.notes;
            if (existingNotes == null) existingNotes = "";
            
            contact.notes = notesBuilder.toString() + "\n" + existingNotes;

            if (db.contactDao().getByPhone(phone) == null) {
                db.contactDao().insert(contact);
            } else {
                db.contactDao().update(contact);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
