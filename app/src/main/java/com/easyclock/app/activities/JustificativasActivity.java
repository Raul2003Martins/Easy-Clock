package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class JustificativasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificativas);

        MaterialToolbar toolbarJustificativas = findViewById(R.id.toolbarJustificativas);
        if (toolbarJustificativas != null) {
            toolbarJustificativas.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnEnviarJustificativaFalta = findViewById(R.id.btnEnviarJustificativaFalta);
        if (btnEnviarJustificativaFalta != null) {
            btnEnviarJustificativaFalta.setOnClickListener(v ->
                    Toast.makeText(this, "Envio de Justificativa de Falta em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
