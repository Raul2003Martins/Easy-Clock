package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class EspelhoPontoFuncionarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_espelho_ponto_funcionario);

        MaterialToolbar toolbarEspelhoPontoRh = findViewById(R.id.toolbarEspelhoPontoRh);
        if (toolbarEspelhoPontoRh != null) {
            toolbarEspelhoPontoRh.setNavigationOnClickListener(v -> finish());

            if (getIntent() != null && getIntent().hasExtra("funcionarioNome")) {
                String nome = getIntent().getStringExtra("funcionarioNome");
                if (nome != null && !nome.trim().isEmpty()) {
                    toolbarEspelhoPontoRh.setTitle("Espelho de Ponto - " + nome);
                }
            }
        }

        MaterialButton btnVerSolicitacoes = findViewById(R.id.btnVerSolicitacoes);
        if (btnVerSolicitacoes != null) {
            btnVerSolicitacoes.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoFuncionarioActivity.this, SolicitacoesFuncionarioActivity.class))
            );
        }

        MaterialButton btnVisualizarJustificativas = findViewById(R.id.btnVisualizarJustificativas);
        if (btnVisualizarJustificativas != null) {
            btnVisualizarJustificativas.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoFuncionarioActivity.this, JustificativasFuncionarioActivity.class))
            );
        }

        MaterialButton btnEditarBancoHoras = findViewById(R.id.btnEditarBancoHoras);
        if (btnEditarBancoHoras != null) {
            btnEditarBancoHoras.setOnClickListener(v ->
                    Toast.makeText(this, "Edição do Banco de Horas em desenvolvimento", Toast.LENGTH_SHORT).show()
            );
        }

        MaterialButton btnGerarHolerite = findViewById(R.id.btnGerarHolerite);
        if (btnGerarHolerite != null) {
            btnGerarHolerite.setOnClickListener(v ->
                    startActivity(new Intent(EspelhoPontoFuncionarioActivity.this, HoleriteRhActivity.class))
            );
        }
    }
}
