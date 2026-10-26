package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;

public class CadastroEmpresaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_empresa);

        TextView tvJaTemEmpresa = findViewById(R.id.tvJaTemEmpresa);
        if (tvJaTemEmpresa != null) {
            tvJaTemEmpresa.setOnClickListener(this::irParaLogin);
        }
    }

    public void irParaLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
