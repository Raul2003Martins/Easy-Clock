package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class FolgaFeriasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folga_ferias);

        MaterialToolbar toolbarFolgaFerias = findViewById(R.id.toolbarFolgaFerias);
        if (toolbarFolgaFerias != null) {
            toolbarFolgaFerias.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnSolicitarFolgaFerias = findViewById(R.id.btnSolicitarFolgaFerias);
        if (btnSolicitarFolgaFerias != null) {
            btnSolicitarFolgaFerias.setOnClickListener(v ->
                    Toast.makeText(this, "Solicitação de Folga/Férias em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnSolicitarVendaFerias = findViewById(R.id.btnSolicitarVendaFerias);
        if (btnSolicitarVendaFerias != null) {
            btnSolicitarVendaFerias.setOnClickListener(v ->
                    Toast.makeText(this, "Solicitação de Venda de Férias em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
