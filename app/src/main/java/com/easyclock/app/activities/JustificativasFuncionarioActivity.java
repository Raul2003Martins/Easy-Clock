package com.easyclock.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class JustificativasFuncionarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_justificativas_funcionario);

        MaterialToolbar toolbarJustificativasRh = findViewById(R.id.toolbarJustificativasRh);
        if (toolbarJustificativasRh != null) {
            toolbarJustificativasRh.setNavigationOnClickListener(v -> finish());

            if (getIntent() != null && getIntent().hasExtra("funcionarioNome")) {
                String nome = getIntent().getStringExtra("funcionarioNome");
                if (nome != null && !nome.trim().isEmpty()) {
                    toolbarJustificativasRh.setTitle("Justificativas - " + nome);
                }
            }
        }

        MaterialButton btnInvalidarFalta = findViewById(R.id.btnInvalidarFalta);
        if (btnInvalidarFalta != null) {
            btnInvalidarFalta.setOnClickListener(v ->
                    Toast.makeText(this, "Invalidar Falta em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnNegarJustificativa = findViewById(R.id.btnNegarJustificativa);
        if (btnNegarJustificativa != null) {
            btnNegarJustificativa.setOnClickListener(v ->
                    Toast.makeText(this, "Negar Justificativa em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
