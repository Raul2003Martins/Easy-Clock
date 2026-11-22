package com.easyclock.app.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.easyclock.app.BuildConfig;
import com.easyclock.app.R;
import com.easyclock.app.dao.EmpresaDAO;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.model.LocalizacaoPonto;
import com.easyclock.app.service.AutenticacaoService;
import com.easyclock.app.service.LocalizacaoService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.Locale;

public class PerfilRhActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_RH = 200;

    private TextInputEditText etNomePerfilRh;
    private TextInputEditText etEmailPerfilRh;
    private MaterialButton btnEspelhoPontoRh;
    private MaterialButton btnVisualizarHoleritesRh;
    private MaterialButton btnVerTodosColaboradores;
    private MaterialButton btnBancoHorasRh;
    private MaterialButton btnGerarConviteRh;
    private MaterialButton btnGerenciarJornadas;
    private MaterialButton btnConsultarPontosRh;
    private MaterialButton btnConfigurarLocalizacaoEmpresa;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;
    private EmpresaDAO empresaDAO;

    private String empresaIdRh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_rh);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();
        empresaDAO = new EmpresaDAO();

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
        btnBancoHorasRh = findViewById(R.id.btnBancoHorasRh);
        btnGerarConviteRh = findViewById(R.id.btnGerarConviteRh);
        btnGerenciarJornadas = findViewById(R.id.btnGerenciarJornadas);
        btnConsultarPontosRh = findViewById(R.id.btnConsultarPontosRh);
        btnConfigurarLocalizacaoEmpresa = findViewById(R.id.btnConfigurarLocalizacaoEmpresa);

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

        if (btnBancoHorasRh != null) {
            btnBancoHorasRh.setOnClickListener(v -> {
                Intent intent = new Intent(PerfilRhActivity.this, BancoHorasRhActivity.class);
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

        if (btnGerenciarJornadas != null) {
            btnGerenciarJornadas.setOnClickListener(v -> {
                Intent intent = new Intent(PerfilRhActivity.this, JornadaTrabalhoActivity.class);
                startActivity(intent);
            });
        }

        if (btnConsultarPontosRh != null) {
            btnConsultarPontosRh.setOnClickListener(v -> {
                Intent intent = new Intent(PerfilRhActivity.this, ConsultaPontosRhActivity.class);
                startActivity(intent);
            });
        }

        if (btnConfigurarLocalizacaoEmpresa != null) {
            btnConfigurarLocalizacaoEmpresa.setOnClickListener(v -> solicitarCapturaLocalizacaoEmpresa());
        }
    }

    private void carregarDadosUsuario() {
        String uid = autenticacaoService.getUidAtual();
        if (uid != null) {
            android.widget.ImageView ivAvatarRh = findViewById(R.id.ivAvatarRh);
            java.io.File fotoFile = com.easyclock.app.service.ReconhecimentoFacialService.obterArquivoFotoPerfil(this, uid);
            if (fotoFile != null && ivAvatarRh != null) {
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(fotoFile.getAbsolutePath());
                if (bmp != null) {
                    ivAvatarRh.setImageBitmap(bmp);
                    ivAvatarRh.setImageTintList(null);
                }
            }

            funcionarioDAO.buscarPorUid(uid)
                    .addOnSuccessListener(func -> {
                        if (func != null) {
                            empresaIdRh = func.getEmpresaId();
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

    private void solicitarCapturaLocalizacaoEmpresa() {
        if (empresaIdRh == null || empresaIdRh.trim().isEmpty()) {
            Toast.makeText(this, "Identificador da empresa não encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!LocalizacaoService.temPermissaoLocalizacao(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION_RH
            );
            return;
        }

        capturarESalvarLocalizacaoRh();
    }

    private void capturarESalvarLocalizacaoRh() {
        if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(false);
        Toast.makeText(this, "Obtendo localização do dispositivo...", Toast.LENGTH_SHORT).show();

        LocalizacaoService.capturarLocalizacaoAtual(this, new LocalizacaoService.OnLocalizacaoCapturadaListener() {
            @Override
            public void onSuccess(Location location, float precisaoMetros, boolean isMock) {
                if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(true);
                exibirDialogoConfirmacaoLocalizacaoEmpresa(location, precisaoMetros);
            }

            @Override
            public void onFailure(String mensagemErro, boolean gpsDesativado, boolean permissaoNegada) {
                if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(true);
                new AlertDialog.Builder(PerfilRhActivity.this)
                        .setTitle("Localização Indisponível")
                        .setMessage(mensagemErro)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void exibirDialogoConfirmacaoLocalizacaoEmpresa(Location location, float precisaoMetros) {
        String msg = String.format(Locale.getDefault(),
                "Localização obtida com sucesso:\n\n" +
                        "• Latitude: %.6f\n" +
                        "• Longitude: %.6f\n" +
                        "• Precisão estimada: %.1fm\n" +
                        "• Raio permitido: 100m (Fixo)\n\n" +
                        "Deseja confirmar o cadastro desta localização para o registro de ponto dos colaboradores?",
                location.getLatitude(), location.getLongitude(), precisaoMetros);

        new AlertDialog.Builder(this)
                .setTitle("Configurar Localização da Empresa")
                .setMessage(msg)
                .setPositiveButton("SALVAR LOCALIZAÇÃO", (dialog, which) -> salvarLocalizacaoNoFirestore(location))
                .setNegativeButton("CANCELAR", null)
                .setNeutralButton("TENTAR NOVAMENTE", (dialog, which) -> capturarESalvarLocalizacaoRh())
                .show();
    }

    private void salvarLocalizacaoNoFirestore(Location location) {
        String rhUid = autenticacaoService.getUidAtual();
        LocalizacaoPonto locPonto = new LocalizacaoPonto(
                location.getLatitude(),
                location.getLongitude(),
                100.0,
                rhUid != null ? rhUid : "",
                Timestamp.now()
        );

        if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(false);

        empresaDAO.atualizarLocalizacaoPonto(empresaIdRh, locPonto)
                .addOnSuccessListener(unused -> {
                    if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(true);
                    Toast.makeText(this, "Localização da empresa cadastrada com sucesso (100m)!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    if (btnConfigurarLocalizacaoEmpresa != null) btnConfigurarLocalizacaoEmpresa.setEnabled(true);
                    String msg = e != null && e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Erro desconhecido";
                    Toast.makeText(this, "Erro ao salvar localização da empresa: " + msg, Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_RH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturarESalvarLocalizacaoRh();
            } else {
                Toast.makeText(this, "Permissão de localização negada pelo RH.", Toast.LENGTH_SHORT).show();
            }
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
