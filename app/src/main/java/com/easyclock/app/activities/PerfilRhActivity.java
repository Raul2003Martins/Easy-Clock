package com.easyclock.app.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.easyclock.app.BuildConfig;
import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class PerfilRhActivity extends AppCompatActivity {

    private TextInputEditText etNomePerfilRh;
    private TextInputEditText etEmailPerfilRh;
    private MaterialButton btnEspelhoPontoRh;
    private MaterialButton btnVisualizarHoleritesRh;
    private MaterialButton btnVerTodosColaboradores;
    private MaterialButton btnGerarConviteRh;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_rh);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        inicializarComponentes();
        configurarListeners();
        carregarDadosUsuario();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarPerfilRh = findViewById(R.id.toolbarPerfilRh);
        if (toolbarPerfilRh != null) {
            toolbarPerfilRh.setNavigationOnClickListener(v -> finish());
        }

        etNomePerfilRh = findViewById(R.id.etNomePerfilRh);
        etEmailPerfilRh = findViewById(R.id.etEmailPerfilRh);
        btnEspelhoPontoRh = findViewById(R.id.btnEspelhoPontoRh);
        btnVisualizarHoleritesRh = findViewById(R.id.btnVisualizarHoleritesRh);
        btnVerTodosColaboradores = findViewById(R.id.btnVerTodosColaboradores);
        btnGerarConviteRh = findViewById(R.id.btnGerarConviteRh);

        MaterialButton btnLogoutRh = findViewById(R.id.btnLogoutRh);
        if (btnLogoutRh != null) {
            btnLogoutRh.setOnClickListener(v -> {
                autenticacaoService.logout();
                Intent intent = new Intent(PerfilRhActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void configurarListeners() {
        if (btnVerTodosColaboradores != null) {
            btnVerTodosColaboradores.setOnClickListener(v -> {
                if (BuildConfig.DEBUG) {
                    Log.d("PerfilRhActivity", "Clicou no botão COLABORADORES. Abrindo ColaboradoresActivity...");
                }
                Intent intent = new Intent(PerfilRhActivity.this, ColaboradoresActivity.class);
                startActivity(intent);
            });
        }

        if (btnEspelhoPontoRh != null) {
            btnEspelhoPontoRh.setOnClickListener(v -> {
                Intent intent = new Intent(PerfilRhActivity.this, EspelhoPontoActivity.class);
                startActivity(intent);
            });
        }

        if (btnVisualizarHoleritesRh != null) {
            btnVisualizarHoleritesRh.setOnClickListener(v -> {
                Intent intent = new Intent(PerfilRhActivity.this, HoleriteRhActivity.class);
                startActivity(intent);
            });
        }

        if (btnGerarConviteRh != null) {
            btnGerarConviteRh.setOnClickListener(v -> solicitarGeracaoConvite());
        }
    }

    private void carregarDadosUsuario() {
        String uid = autenticacaoService.getUidAtual();
        if (uid != null) {
            funcionarioDAO.buscarPorUid(uid)
                    .addOnSuccessListener(func -> {
                        if (func != null) {
                            if (etNomePerfilRh != null && func.getNome() != null) {
                                etNomePerfilRh.setText(func.getNome());
                            }
                            if (etEmailPerfilRh != null && func.getEmail() != null) {
                                etEmailPerfilRh.setText(func.getEmail());
                            }
                        }
                    });
        }
    }

    private void solicitarGeracaoConvite() {
        if (!autenticacaoService.estaAutenticado()) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnGerarConviteRh != null) {
            btnGerarConviteRh.setEnabled(false);
        }

        autenticacaoService.gerarConviteEmpresa()
                .addOnSuccessListener(result -> {
                    if (btnGerarConviteRh != null) {
                        btnGerarConviteRh.setEnabled(true);
                    }

                    String codigo = "";
                    if (result != null && result.get("codigoConvite") != null) {
                        codigo = result.get("codigoConvite").toString();
                    }

                    exibirDialogoConviteGerado(codigo);
                })
                .addOnFailureListener(e -> {
                    if (btnGerarConviteRh != null) {
                        btnGerarConviteRh.setEnabled(true);
                    }

                    String msg = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro ao gerar convite.";
                    new AlertDialog.Builder(this)
                            .setTitle("Falha ao Gerar Convite")
                            .setMessage(msg)
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    private void exibirDialogoConviteGerado(String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Convite gerado com sucesso")
                .setMessage("Código de convite para o funcionário:\n\n" + codigo + "\n\nCompartilhe este código com o funcionário para realizar o cadastro.")
                .setPositiveButton("COPIAR CÓDIGO", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Código de Convite", codigo);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Código copiado para a área de transferência", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("FECHAR", null)
                .show();
    }
}
