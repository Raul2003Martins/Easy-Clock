package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class EspelhoPontoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_espelho_ponto);

        MaterialToolbar toolbarEspelhoPonto = findViewById(R.id.toolbarEspelhoPonto);
        if (toolbarEspelhoPonto != null) {
            toolbarEspelhoPonto.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnSolicitarAjuste = findViewById(R.id.btnSolicitarAjuste);
        if (btnSolicitarAjuste != null) {
            btnSolicitarAjuste.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoActivity.this, AjustePontoActivity.class))
            );
        }

        MaterialButton btnEnviarJustificativaFalta = findViewById(R.id.btnEnviarJustificativaFalta);
        if (btnEnviarJustificativaFalta != null) {
            btnEnviarJustificativaFalta.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoActivity.this, JustificativasActivity.class))
            );
        }

        MaterialButton btnSolicitarFolga = findViewById(R.id.btnSolicitarFolga);
        if (btnSolicitarFolga != null) {
            btnSolicitarFolga.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoActivity.this, FolgaFeriasActivity.class))
            );
        }
    }
}
