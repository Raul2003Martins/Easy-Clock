package com.easyclock.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PerfilFuncionarioActivity extends AppCompatActivity {

    private TextInputEditText etNomePerfil;
    private TextInputEditText etEmailPerfil;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_funcionario);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        inicializarComponentes();
        configurarListeners();
        carregarDadosUsuario();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarPerfil = findViewById(R.id.toolbarPerfil);
        if (toolbarPerfil != null) {
            toolbarPerfil.setNavigationOnClickListener(v -> finish());
        }

        etNomePerfil = findViewById(R.id.etNomePerfil);
        etEmailPerfil = findViewById(R.id.etEmailPerfil);

        MaterialButton btnEspelhoPonto = findViewById(R.id.btnEspelhoPonto);
        if (btnEspelhoPonto != null) {
            btnEspelhoPonto.setOnClickListener(v ->
                    startActivity(new Intent(PerfilFuncionarioActivity.this, EspelhoPontoActivity.class))
            );
        }

        MaterialButton btnVisualizarHolerites = findViewById(R.id.btnVisualizarHolerites);
        if (btnVisualizarHolerites != null) {
            btnVisualizarHolerites.setOnClickListener(v ->
                    startActivity(new Intent(PerfilFuncionarioActivity.this, HoleriteActivity.class))
            );
        }

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                autenticacaoService.logout();
                Intent intent = new Intent(PerfilFuncionarioActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void configurarListeners() {
    }

    private void carregarDadosUsuario() {
        String uid = autenticacaoService.getUidAtual();
        if (uid != null) {
            funcionarioDAO.buscarPorUid(uid)
                    .addOnSuccessListener(func -> {
                        if (func != null) {
                            if (etNomePerfil != null && func.getNome() != null) {
                                etNomePerfil.setText(func.getNome());
                            }
                            if (etEmailPerfil != null && func.getEmail() != null) {
                                etEmailPerfil.setText(func.getEmail());
                            }
                        }
                    });
        }
    }
}
