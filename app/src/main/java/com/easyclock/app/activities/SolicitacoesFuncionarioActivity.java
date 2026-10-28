package com.easyclock.app.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.google.android.material.appbar.MaterialToolbar;

public class SolicitacoesFuncionarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitacoes_funcionario);

        MaterialToolbar toolbarSolicitacoes = findViewById(R.id.toolbarSolicitacoes);
        if (toolbarSolicitacoes != null) {
            toolbarSolicitacoes.setNavigationOnClickListener(v -> finish());

            if (getIntent() != null && getIntent().hasExtra("funcionarioNome")) {
                String nome = getIntent().getStringExtra("funcionarioNome");
                if (nome != null && !nome.trim().isEmpty()) {
                    toolbarSolicitacoes.setTitle("Solicitações - " + nome);
                }
            }
        }
    }
}
