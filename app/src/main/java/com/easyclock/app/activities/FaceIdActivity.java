package com.easyclock.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.easyclock.app.R;
import com.easyclock.app.dao.FuncionarioDAO;
import com.easyclock.app.service.AutenticacaoService;
import com.easyclock.app.service.ReconhecimentoFacialService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FaceIdActivity extends AppCompatActivity {

    private static final String TAG = "FaceIdActivity";

    private PreviewView previewViewFaceId;
    private TextView tvInstrucaoFaceId;
    private TextView tvStatusBiometriaDetalhado;
    private MaterialButton btnCapturarFacial;
    private MaterialButton btnBaterPontoFaceId;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    private AutenticacaoService autenticacaoService;
    private FuncionarioDAO funcionarioDAO;

    private String uidFuncionario;
    private boolean isModoCadastro = false;
    private boolean biometriaValidadaSucesso = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarCameraX();
                } else {
                    exibirAlertaPermissaoCameranegada();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id);

        autenticacaoService = new AutenticacaoService();
        funcionarioDAO = new FuncionarioDAO();

        uidFuncionario = getIntent().getStringExtra("uidFuncionario");
        if (uidFuncionario == null || uidFuncionario.trim().isEmpty()) {
            uidFuncionario = autenticacaoService.getUidAtual();
        }

        isModoCadastro = getIntent().getBooleanExtra("modoCadastro", false);

        inicializarComponentes();
        verificarECorrigirModo();
    }

    private void inicializarComponentes() {
        MaterialToolbar toolbarFaceId = findViewById(R.id.toolbarFaceId);
        if (toolbarFaceId != null) {
            toolbarFaceId.setNavigationOnClickListener(v -> finish());
        }

        previewViewFaceId = findViewById(R.id.previewViewFaceId);
        tvInstrucaoFaceId = findViewById(R.id.tvInstrucaoFaceId);
        tvStatusBiometriaDetalhado = findViewById(R.id.tvStatusBiometriaDetalhado);
        btnCapturarFacial = findViewById(R.id.btnCapturarFacial);
        btnBaterPontoFaceId = findViewById(R.id.btnBaterPontoFaceId);

        if (btnCapturarFacial != null) {
            btnCapturarFacial.setOnClickListener(v -> capturarEProcessarRosto());
        }

        if (btnBaterPontoFaceId != null) {
            btnBaterPontoFaceId.setOnClickListener(v -> {
                if (biometriaValidadaSucesso) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "A validação facial é obrigatória para prosseguir.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void verificarECorrigirModo() {
        if (isModoCadastro) {
            if (tvInstrucaoFaceId != null) {
                tvInstrucaoFaceId.setText("PRIMEIRO ACESSO: Posicione seu rosto na área indicada para cadastrar sua biometria oficial.");
            }
            if (tvStatusBiometriaDetalhado != null) {
                tvStatusBiometriaDetalhado.setText("Modo: Cadastro Facial Inicial");
            }
        } else {
            if (tvInstrucaoFaceId != null) {
                tvInstrucaoFaceId.setText("RECONHECIMENTO FACIAL: Olhe para a câmera para confirmar sua identidade.");
            }
            if (tvStatusBiometriaDetalhado != null) {
                tvStatusBiometriaDetalhado.setText("Modo: Reconhecimento Facial Obrigatório (Mínimo: 80,0%)");
            }
        }

        verificarPermissaoEIniciarCamera();
    }

    private void verificarPermissaoEIniciarCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCameraX();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void exibirAlertaPermissaoCameranegada() {
        if (tvStatusBiometriaDetalhado != null) {
            tvStatusBiometriaDetalhado.setText("Permissão de câmera negada. O reconhecimento facial está bloqueado.");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Permissão de Câmera Necessária")
                .setMessage("É necessário permitir o acesso à câmera para realizar o cadastro ou reconhecimento facial e registrar o ponto.")
                .setCancelable(false)
                .setNegativeButton("FECHAR", (dialog, which) -> finish());

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            builder.setPositiveButton("TENTAR NOVAMENTE", (dialog, which) -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            });
        } else {
            builder.setMessage("É necessário permitir o acesso à câmera para registrar o ponto.\n\nAcesse: Configurações → Aplicativos → Easy Clock → Permissões → Câmera.");
            builder.setPositiveButton("CONFIGURAÇÕES", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            });
        }

        builder.show();
    }

    private void iniciarCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                if (previewViewFaceId != null) {
                    preview.setSurfaceProvider(previewViewFaceId.getSurfaceProvider());
                }

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                if (tvStatusBiometriaDetalhado != null) {
                    tvStatusBiometriaDetalhado.setText("Câmera pronta. Clique em CAPTURAR E VALIDAR ROSTO.");
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Erro ao iniciar CameraX", e);
                if (tvStatusBiometriaDetalhado != null) {
                    tvStatusBiometriaDetalhado.setText("Erro ao iniciar câmera frontal: " + e.getLocalizedMessage());
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturarEProcessarRosto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Câmera ainda não inicializada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Invalida imediatamente qualquer aprovação anterior ao iniciar nova captura
        biometriaValidadaSucesso = false;
        if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(false);

        if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(false);
        if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Capturando imagem facial...");

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                if (bitmap != null) {
                    processarBiometriaFacial(bitmap);
                } else {
                    if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
                    if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Falha ao converter captura da câmera.");
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Erro ao capturar foto", exception);
                if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
                if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Erro de captura: " + exception.getLocalizedMessage());
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            int rotationDegrees = image.getImageInfo().getRotationDegrees();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (rawBitmap == null) return null;

            Matrix matrix = new Matrix();
            if (rotationDegrees != 0) {
                matrix.postRotate(rotationDegrees);
            }
            // Espelhamento horizontal para visão de câmera frontal
            matrix.postScale(-1f, 1f, rawBitmap.getWidth() / 2f, rawBitmap.getHeight() / 2f);

            return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter ImageProxy para Bitmap rotacionado", e);
            return null;
        }
    }

    private void processarBiometriaFacial(Bitmap bitmap) {
        if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Analisando características faciais...");

        ReconhecimentoFacialService.analisarEExtrairBiometria(FaceIdActivity.this, bitmap, new ReconhecimentoFacialService.OnAnaliseFacialListener() {
            @Override
            public void onSuccess(Bitmap faceCrop, float[] novoEmbedding) {
                if (isModoCadastro) {
                    executarCadastroFacialInicial(faceCrop, novoEmbedding);
                } else {
                    executarVerificacaoFacialExistente(novoEmbedding);
                }
            }

            @Override
            public void onFailure(String mensagemErro) {
                if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
                biometriaValidadaSucesso = false;
                if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(false);

                if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText(mensagemErro);

                new AlertDialog.Builder(FaceIdActivity.this)
                        .setTitle("Validação Facial")
                        .setMessage(mensagemErro)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void executarCadastroFacialInicial(Bitmap faceCrop, float[] embedding) {
        if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Salvando biometria e foto de perfil...");

        boolean ok = ReconhecimentoFacialService.salvarTemplateBiometrico(this, uidFuncionario, embedding, faceCrop);
        if (!ok) {
            if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
            if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Falha ao salvar template biométrico local.");
            return;
        }

        File fotoPerfilFile = ReconhecimentoFacialService.obterArquivoFotoPerfil(this, uidFuncionario);
        String fotoPath = fotoPerfilFile != null ? fotoPerfilFile.getAbsolutePath() : "";

        Map<String, Object> updateFunc = new HashMap<>();
        updateFunc.put("reconhecimentoFacialCadastrado", true);
        updateFunc.put("solicitacaoRecadastroAutorizada", false);
        if (!fotoPath.isEmpty()) {
            updateFunc.put("fotoPerfilPath", fotoPath);
        }

        funcionarioDAO.atualizarCampos(uidFuncionario, updateFunc)
                .addOnSuccessListener(unused -> {
                    if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
                    biometriaValidadaSucesso = true;
                    if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(true);

                    if (tvStatusBiometriaDetalhado != null) {
                        tvStatusBiometriaDetalhado.setText("Cadastro facial realizado com sucesso!");
                    }

                    Toast.makeText(this, "Cadastro facial concluído com sucesso!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
                    Log.e(TAG, "Erro ao atualizar estado no Firestore", e);
                    if (tvStatusBiometriaDetalhado != null) {
                        tvStatusBiometriaDetalhado.setText("Erro ao salvar cadastro no servidor: " + e.getLocalizedMessage());
                    }
                });
    }

    private void executarVerificacaoFacialExistente(float[] novoEmbedding) {
        if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText("Comparando biometria com cadastro oficial...");

        float[] storedEmbedding = ReconhecimentoFacialService.carregarTemplateBiometrico(this, uidFuncionario);
        if (storedEmbedding == null) {
            if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);
            biometriaValidadaSucesso = false;
            if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(false);

            String erro = "Referência biométrica local indisponível ou em versão antiga. Solicite autorização de recadastro ao RH.";
            if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText(erro);

            new AlertDialog.Builder(this)
                    .setTitle("Recadastro Necessário")
                    .setMessage(erro)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        float sim = ReconhecimentoFacialService.calcularSimilaridadeCosseno(storedEmbedding, novoEmbedding);
        boolean aprovado = ReconhecimentoFacialService.validarAprovacaoSimilaridade(sim);

        if (btnCapturarFacial != null) btnCapturarFacial.setEnabled(true);

        if (aprovado) {
            biometriaValidadaSucesso = true;
            if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(true);

            String msgStatus = String.format(Locale.getDefault(), "Reconhecimento facial aprovado com sucesso! (Similaridade: %.1f%%)", sim * 100.0f);
            if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText(msgStatus);

            Toast.makeText(this, "Reconhecimento facial realizado com sucesso.", Toast.LENGTH_SHORT).show();
        } else {
            biometriaValidadaSucesso = false;
            if (btnBaterPontoFaceId != null) btnBaterPontoFaceId.setEnabled(false);

            String erroReprovado = String.format(Locale.getDefault(),
                    "Reconhecimento facial não aprovado. A similaridade com o rosto cadastrado foi de %.1f%%, inferior ao mínimo exigido de %.0f%%.",
                    sim * 100.0f, ReconhecimentoFacialService.LIMIAR_SIMILARIDADE_MINIMA * 100.0f);

            if (tvStatusBiometriaDetalhado != null) tvStatusBiometriaDetalhado.setText(erroReprovado);

            new AlertDialog.Builder(this)
                    .setTitle("Identidade Não Confirmada")
                    .setMessage(erroReprovado)
                    .setPositiveButton("TENTAR NOVAMENTE", null)
                    .show();
        }
    }
}
