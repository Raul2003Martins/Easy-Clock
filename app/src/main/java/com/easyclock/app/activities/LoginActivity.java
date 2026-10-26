package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView tvCadastrarLogin = findViewById(R.id.tvCadastrarLogin);
        TextView tvCadastrarEmpresaLogin = findViewById(R.id.tvCadastrarEmpresaLogin);

        if (tvCadastrarLogin != null) {
            tvCadastrarLogin.setOnClickListener(this::abrirCadastroConta);
        }

        if (tvCadastrarEmpresaLogin != null) {
            tvCadastrarEmpresaLogin.setOnClickListener(this::abrirCadastroEmpresa);
        }
    }

    public void abrirCadastroConta(View view) {
        Intent intent = new Intent(this, CadastroActivity.class);
        startActivity(intent);
    }

    public void abrirCadastroEmpresa(View view) {
        Intent intent = new Intent(this, CadastroEmpresaActivity.class);
        startActivity(intent);
    }
}
