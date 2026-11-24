package com.easyclock.testprototype;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFaceActivity extends AppCompatActivity {

    private static final String TAG = "TestFaceActivity";
    private static final String MODEL_FILE_NAME = "mobilefacenet.tflite";
    private static final String REGISTERED_IMAGE_NAME = "qualcomm_registered_face.png";
    private static final String TEMPLATE_FILE_NAME = "qualcomm_template.dat";
    private static final String GEOMETRIC_TEMPLATE_FILE_NAME = "qualcomm_geometric_template.dat";
    private static final int EMBEDDING_DIMENSION = 128;
    private static final int GEOMETRIC_VECTOR_DIMENSION = 10;

    private enum ModoCaptura {
        NEURAL,
        GEOMETRICO,
        VALIDACAO
    }

    private PreviewView previewViewTest;
    private ImageView ivRegisteredFacePreview;
    private ImageView ivLastValidatedFacePreview;
    private ImageView ivContourOverlayPreview;
    private TextView tvStatusTest;
    private Button btnCadastrarRostoTest;
    private Button btnCadastrarGeometriaTest;
    private Button btnValidarRostoTest;
    private Button btnLimparCadastroTest;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Interpreter tfliteInterpreter;
    private boolean isModelLoaded = false;
    private String lastModelError = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarCamera();
                } else {
                    if (tvStatusTest != null) tvStatusTest.setText("Permissão de câmera negada.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_face);

        inicializarComponentes();
        carregarModeloTFLite();
        carregarFotoCadastradaFixa();
        verificarPermissaoECamera();
    }

    private void inicializarComponentes() {
        previewViewTest = findViewById(R.id.previewViewTest);
        ivRegisteredFacePreview = findViewById(R.id.ivRegisteredFacePreview);
        ivLastValidatedFacePreview = findViewById(R.id.ivLastValidatedFacePreview);
        ivContourOverlayPreview = findViewById(R.id.ivContourOverlayPreview);
        tvStatusTest = findViewById(R.id.tvStatusTest);
        btnCadastrarRostoTest = findViewById(R.id.btnCadastrarRostoTest);
        btnCadastrarGeometriaTest = findViewById(R.id.btnCadastrarGeometriaTest);
        btnValidarRostoTest = findViewById(R.id.btnValidarRostoTest);
        btnLimparCadastroTest = findViewById(R.id.btnLimparCadastroTest);

        if (btnCadastrarRostoTest != null) {
            btnCadastrarRostoTest.setOnClickListener(v -> capturarEProcessar(ModoCaptura.NEURAL));
        }

        if (btnCadastrarGeometriaTest != null) {
            btnCadastrarGeometriaTest.setOnClickListener(v -> capturarEProcessar(ModoCaptura.GEOMETRICO));
        }

        if (btnValidarRostoTest != null) {
            btnValidarRostoTest.setOnClickListener(v -> capturarEProcessar(ModoCaptura.VALIDACAO));
        }

        if (btnLimparCadastroTest != null) {
            btnLimparCadastroTest.setOnClickListener(v -> limparCadastroDeTeste());
        }
    }

    private void carregarFotoCadastradaFixa() {
        try {
            File imgFile = new File(new File(getFilesDir(), "test_biometrics"), REGISTERED_IMAGE_NAME);
            File datFile = new File(new File(getFilesDir(), "test_biometrics"), TEMPLATE_FILE_NAME);
            File geomFile = new File(new File(getFilesDir(), "test_biometrics"), GEOMETRIC_TEMPLATE_FILE_NAME);

            if (imgFile.exists() && datFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null && ivRegisteredFacePreview != null) {
                    ivRegisteredFacePreview.setImageBitmap(bitmap);
                }
                String hashStr = calcularSHA256Arquivo(datFile);
                boolean hasGeom = geomFile.exists();
                if (tvStatusTest != null) {
                    tvStatusTest.setText(String.format("Cadastro Limpo Fixo Carregado (SHA-256: %s... | Geometria 133p: %s). Pronta para validação:",
                            hashStr.substring(0, 10), hasGeom ? "OK" : "Ausente"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar foto cadastrada fixa", e);
        }
    }

    private void carregarModeloTFLite() {
        try {
            MappedByteBuffer mappedByteBuffer = loadModelFile(this, MODEL_FILE_NAME);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tfliteInterpreter = new Interpreter(mappedByteBuffer, options);

            int inCount = tfliteInterpreter.getInputTensorCount();
            int outCount = tfliteInterpreter.getOutputTensorCount();

            String in1Name = tfliteInterpreter.getInputTensor(0).name();
            int[] in1Shape = tfliteInterpreter.getInputTensor(0).shape();
            String in2Name = inCount > 1 ? tfliteInterpreter.getInputTensor(1).name() : "N/A";
            int[] in2Shape = inCount > 1 ? tfliteInterpreter.getInputTensor(1).shape() : new int[0];

            String outName = tfliteInterpreter.getOutputTensor(0).name();
            int[] outShape = tfliteInterpreter.getOutputTensor(0).shape();

            isModelLoaded = true;
            lastModelError = null;
            Log.d(TAG, String.format(Locale.getDefault(),
                    "Modelo Qualcomm TFLite Multi-Input Auditado -> Inputs: %d [Input 0: %s %s, Input 1: %s %s] | Outputs: %d [Output 0: %s %s]",
                    inCount, in1Name, Arrays.toString(in1Shape), in2Name, Arrays.toString(in2Shape),
                    outCount, outName, Arrays.toString(outShape)));
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico ao carregar modelo TFLite", e);
            isModelLoaded = false;
            tfliteInterpreter = null;
            lastModelError = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private MappedByteBuffer loadModelFile(AppCompatActivity activity, String modelFilename) throws Exception {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void verificarPermissaoECamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                if (previewViewTest != null) {
                    preview.setSurfaceProvider(previewViewTest.getSurfaceProvider());
                }

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                if (tvStatusTest != null) {
                    if (isModelLoaded) {
                        tvStatusTest.setText("Câmera Pronta (Neural + Geometria 133 Pontos Separados). Selecione uma operação:");
                    } else {
                        String errStr = lastModelError != null ? lastModelError : "Modelo Não Encontrado";
                        tvStatusTest.setText("Erro TFLite: " + errStr);
                    }
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Erro ao iniciar CameraX", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturarEProcessar(ModoCaptura modo) {
        if (imageCapture == null) {
            Toast.makeText(this, "Câmera não pronta.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tvStatusTest != null) tvStatusTest.setText("Capturando imagem...");

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                if (bitmap != null) {
                    processarBiometriaEContornos(bitmap, modo);
                } else {
                    if (tvStatusTest != null) tvStatusTest.setText("Erro ao converter captura.");
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Erro ao capturar foto", exception);
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
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees);
            matrix.postScale(-1f, 1f, rawBitmap.getWidth() / 2f, rawBitmap.getHeight() / 2f);

            return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro na conversão da imagem", e);
            return null;
        }
    }

    private void processarBiometriaEContornos(Bitmap bitmap, ModoCaptura modo) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        if (tvStatusTest != null) tvStatusTest.setText("Nenhum rosto detectado na câmera.");
                        return;
                    }

                    if (faces.size() > 1) {
                        if (tvStatusTest != null) tvStatusTest.setText("Mais de um rosto detectado. Mantenha apenas uma pessoa na tela.");
                        return;
                    }

                    Face face = faces.get(0);

                    // 1. Alinhamento Canônico Neural LIMPO (SEMPRE 100% LIMPO SEM PONTOS CIANOS)
                    Bitmap faceCrop = cropFace5LandmarksAligned(bitmap, face);
                    if (faceCrop == null) {
                        if (tvStatusTest != null) {
                            tvStatusTest.setText("Não foi possível identificar todos os 5 pontos faciais. Centralize o rosto e tente novamente.");
                        }
                        Toast.makeText(this, "Não foi possível identificar todos os 5 pontos faciais. Centralize o rosto e tente novamente.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 2. Extração Geométrica e Overlay de Diagnóstico Separado
                    float[] geometricVector = extrairVetorGeometrico133Pontos(face);
                    int totalContourPoints = contarPontosContorno(face);

                    // Desenha overlay ciano unicamente para ivContourOverlayPreview
                    Bitmap overlayBitmap = desenharOverlayContornos133(bitmap, face);

                    if (ivContourOverlayPreview != null && overlayBitmap != null) {
                        ivContourOverlayPreview.setImageBitmap(overlayBitmap);
                    }

                    // Exibe a imagem LIMPA de 112x112 em ivRegisteredFacePreview e ivLastValidatedFacePreview
                    if (modo == ModoCaptura.NEURAL || modo == ModoCaptura.GEOMETRICO) {
                        if (ivRegisteredFacePreview != null) {
                            ivRegisteredFacePreview.setImageBitmap(faceCrop);
                        }
                    } else {
                        if (ivLastValidatedFacePreview != null) {
                            ivLastValidatedFacePreview.setImageBitmap(faceCrop);
                        }
                    }

                    if (modo == ModoCaptura.GEOMETRICO) {
                        if (geometricVector != null) {
                            salvarTemplateGeometrico(geometricVector);
                        } else {
                            if (tvStatusTest != null) tvStatusTest.setText("Incapaz de extrair contornos geométricos completos.");
                        }
                        return;
                    }

                    executorService.execute(() -> {
                        float[] rawEmbedding = extrairEmbeddingMobileFaceNetBruto(faceCrop);
                        runOnUiThread(() -> {
                            if (rawEmbedding == null) {
                                if (tvStatusTest != null) {
                                    String msg = lastModelError != null ?
                                            "Erro TFLite: " + lastModelError :
                                            "Falha na inferência neural TFLite. Modelo indisponível.";
                                    tvStatusTest.setText(msg);
                                }
                                return;
                            }

                            if (modo == ModoCaptura.NEURAL) {
                                salvarTemplateDeTeste(rawEmbedding, faceCrop);
                            } else if (modo == ModoCaptura.VALIDACAO) {
                                validarTemplateEGeometria(rawEmbedding, geometricVector, totalContourPoints, face);
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    if (tvStatusTest != null) tvStatusTest.setText("Erro ML Kit: " + e.getLocalizedMessage());
                });
    }

    private int contarPontosContorno(Face face) {
        if (face == null) return 0;
        List<FaceContour> contours = face.getAllContours();
        if (contours == null) return 0;
        int count = 0;
        for (FaceContour contour : contours) {
            if (contour != null && contour.getPoints() != null) {
                count += contour.getPoints().size();
            }
        }
        return count;
    }

    private Bitmap desenharOverlayContornos133(Bitmap src, Face face) {
        if (src == null || face == null) return null;

        try {
            Bitmap copy = src.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(copy);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.CYAN);
            paint.setStyle(Paint.Style.FILL);

            List<FaceContour> contours = face.getAllContours();
            if (contours != null) {
                for (FaceContour contour : contours) {
                    if (contour != null && contour.getPoints() != null) {
                        for (PointF pt : contour.getPoints()) {
                            canvas.drawCircle(pt.x, pt.y, 4.0f, paint);
                        }
                    }
                }
            }

            Rect bounds = face.getBoundingBox();
            int x1 = Math.max(0, bounds.left);
            int y1 = Math.max(0, bounds.top);
            int w = Math.min(src.getWidth() - x1, bounds.width());
            int h = Math.min(src.getHeight() - y1, bounds.height());

            if (w > 10 && h > 10) {
                Bitmap croppedOverlay = Bitmap.createBitmap(copy, x1, y1, w, h);
                return Bitmap.createScaledBitmap(croppedOverlay, 112, 112, true);
            }
            return Bitmap.createScaledBitmap(copy, 112, 112, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao desenhar overlay de contornos", e);
            return null;
        }
    }

    private float[] extrairVetorGeometrico133Pontos(Face face) {
        if (face == null) return null;

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
        FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);

        if (leftEye == null || rightEye == null || noseBase == null || mouthLeft == null || mouthRight == null) {
            return null;
        }

        float lex = leftEye.getPosition().x;
        float ley = leftEye.getPosition().y;
        float rex = rightEye.getPosition().x;
        float rey = rightEye.getPosition().y;

        float eyeDx = rex - lex;
        float eyeDy = rey - ley;
        float dEyes = (float) Math.sqrt(eyeDx * eyeDx + eyeDy * eyeDy);
        if (dEyes <= 1e-4f) return null;

        Rect bounds = face.getBoundingBox();
        float wFace = bounds.width();
        float hFace = bounds.height();

        float noseX = noseBase.getPosition().x;
        float noseY = noseBase.getPosition().y;

        float mlX = mouthLeft.getPosition().x;
        float mlY = mouthLeft.getPosition().y;
        float mrX = mouthRight.getPosition().x;
        float mrY = mouthRight.getPosition().y;

        float mouthWidth = (float) Math.hypot(mrX - mlX, mrY - mlY);
        float eyeCenterMidX = (lex + rex) / 2.0f;
        float eyeCenterMidY = (ley + rey) / 2.0f;
        float dEyeToNose = (float) Math.hypot(noseX - eyeCenterMidX, noseY - eyeCenterMidY);

        float mouthMidX = (mlX + mrX) / 2.0f;
        float mouthMidY = (mlY + mrY) / 2.0f;
        float dNoseToMouth = (float) Math.hypot(mouthMidX - noseX, mouthMidY - noseY);
        float dEyeToMouth = (float) Math.hypot(mouthMidX - eyeCenterMidX, mouthMidY - eyeCenterMidY);

        float[] g = new float[GEOMETRIC_VECTOR_DIMENSION];
        g[0] = dEyes / Math.max(1.0f, wFace);               // Inter-ocular / Largura facial
        g[1] = dEyeToNose / dEyes;                         // Olho-ao-Nariz / Distância Inter-ocular
        g[2] = dNoseToMouth / dEyes;                        // Nariz-à-Boca / Distância Inter-ocular
        g[3] = mouthWidth / dEyes;                         // Largura da Boca / Distância Inter-ocular
        g[4] = dEyeToMouth / dEyes;                        // Olho-à-Boca / Distância Inter-ocular
        g[5] = wFace / Math.max(1.0f, hFace);              // Razão de Aspecto do Rosto
        g[6] = Math.abs(noseX - eyeCenterMidX) / dEyes;    // Desvio de Simetria do Nariz
        g[7] = Math.abs(mouthMidX - eyeCenterMidX) / dEyes;// Desvio de Simetria da Boca
        g[8] = hFace / dEyes;                              // Altura Facial / Distância Inter-ocular
        g[9] = (dEyeToNose + dNoseToMouth) / dEyes;        // Proporção Vertical Total

        return g;
    }

    private Bitmap cropFace5LandmarksAligned(Bitmap src, Face face) {
        if (src == null || face == null) return null;

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
        FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);

        if (leftEye == null || rightEye == null || noseBase == null || mouthLeft == null || mouthRight == null) {
            Log.w(TAG, "Captura rejeitada: nem todos os 5 landmarks faciais foram detectados.");
            return null;
        }

        float p1_x = leftEye.getPosition().x;
        float p1_y = leftEye.getPosition().y;
        float p2_x = rightEye.getPosition().x;
        float p2_y = rightEye.getPosition().y;

        float pLeftX = Math.min(p1_x, p2_x);
        float pLeftY = (pLeftX == p1_x) ? p1_y : p2_y;
        float pRightX = Math.max(p1_x, p2_x);
        float pRightY = (pRightX == p2_x) ? p2_y : p1_y;

        float m1_x = mouthLeft.getPosition().x;
        float m1_y = mouthLeft.getPosition().y;
        float m2_x = mouthRight.getPosition().x;
        float m2_y = mouthRight.getPosition().y;

        float pMouthLeftX = Math.min(m1_x, m2_x);
        float pMouthLeftY = (pMouthLeftX == m1_x) ? m1_y : m2_y;
        float pMouthRightX = Math.max(m1_x, m2_x);
        float pMouthRightY = (pMouthRightX == m2_x) ? m2_y : m1_y;

        float[][] srcPts = new float[][] {
            { pLeftX, pLeftY },
            { pRightX, pRightY },
            { noseBase.getPosition().x, noseBase.getPosition().y },
            { pMouthLeftX, pMouthLeftY },
            { pMouthRightX, pMouthRightY }
        };

        float[][] dstPts = new float[][] {
            { 38.2943f, 51.6963f },
            { 73.5318f, 51.6963f },
            { 56.0252f, 71.7366f },
            { 41.5493f, 92.3655f },
            { 70.7299f, 92.3655f }
        };

        float[] transformParams = computeLeastSquaresSimilarityMatrix(srcPts, dstPts);
        if (transformParams == null) {
            Log.w(TAG, "Captura rejeitada: matriz de transformação de similaridade singular ou inválida.");
            return null;
        }

        float a = transformParams[0];
        float b = transformParams[1];
        float tx = transformParams[2];
        float ty = transformParams[3];

        float scaleSq = a * a + b * b;
        if (scaleSq <= 1e-8f || Float.isNaN(scaleSq)) {
            Log.w(TAG, "Captura rejeitada: escala de transformação nula ou inválida.");
            return null;
        }

        Matrix transform = new Matrix();
        transform.setValues(new float[] {
            a, -b, tx,
            b,  a, ty,
            0,  0,  1
        });

        try {
            Bitmap aligned112 = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(aligned112);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(src, transform, paint);
            return aligned112;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao renderizar recorte alinhado por 5 pontos", e);
            return null;
        }
    }

    private float[] computeLeastSquaresSimilarityMatrix(float[][] srcPts, float[][] dstPts) {
        try {
            float[][] ATA = new float[4][4];
            float[] ATb = new float[4];

            for (int i = 0; i < 5; i++) {
                float x = srcPts[i][0];
                float y = srcPts[i][1];
                float u = dstPts[i][0];
                float v = dstPts[i][1];

                float[] row1 = new float[] { x, -y, 1f, 0f };
                float[] row2 = new float[] { y,  x, 0f, 1f };

                for (int r = 0; r < 4; r++) {
                    ATb[r] += row1[r] * u + row2[r] * v;
                    for (int c = 0; c < 4; c++) {
                        ATA[r][c] += row1[r] * row1[c] + row2[r] * row2[c];
                    }
                }
            }

            float[][] M = new float[4][5];
            for (int i = 0; i < 4; i++) {
                System.arraycopy(ATA[i], 0, M[i], 0, 4);
                M[i][4] = ATb[i];
            }

            for (int i = 0; i < 4; i++) {
                float pivot = M[i][i];
                if (Math.abs(pivot) < 1e-6f) return null;
                for (int j = i; j <= 4; j++) M[i][j] /= pivot;
                for (int k = 0; k < 4; k++) {
                    if (k != i) {
                        float factor = M[k][i];
                        for (int j = i; j <= 4; j++) M[k][j] -= factor * M[i][j];
                    }
                }
            }

            return new float[] { M[0][4], M[1][4], M[2][4], M[3][4] };
        } catch (Exception e) {
            Log.e(TAG, "Erro na matriz de mínimos quadrados 5 pontos", e);
            return null;
        }
    }

    private float[] extrairEmbeddingMobileFaceNetBruto(Bitmap faceCrop) {
        if (faceCrop == null || tfliteInterpreter == null) {
            Log.e(TAG, "Inferência abortada: tfliteInterpreter é nulo ou faceCrop indisponível.");
            return null;
        }

        try {
            Bitmap scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true);
            int width = scaled.getWidth();
            int height = scaled.getHeight();

            int singleImageSize = 1 * 112 * 112 * 3 * 4;

            ByteBuffer inputBuffer1 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer1.order(ByteOrder.nativeOrder());
            inputBuffer1.rewind();

            ByteBuffer inputBuffer2 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer2.order(ByteOrder.nativeOrder());
            inputBuffer2.rewind();

            int[] pixels = new int[width * height];
            scaled.getPixels(pixels, 0, width, 0, 0, width, height);

            float minVal1 = Float.MAX_VALUE, maxVal1 = -Float.MAX_VALUE, sumVal1 = 0f;

            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                float rVal = r / 255.0f;
                float gVal = g / 255.0f;
                float bVal = blue / 255.0f;

                minVal1 = Math.min(minVal1, Math.min(rVal, Math.min(gVal, bVal)));
                maxVal1 = Math.max(maxVal1, Math.max(rVal, Math.max(gVal, bVal)));
                sumVal1 += (rVal + gVal + bVal);

                inputBuffer1.putFloat(rVal);
                inputBuffer1.putFloat(gVal);
                inputBuffer1.putFloat(bVal);

                inputBuffer2.putFloat(rVal);
                inputBuffer2.putFloat(gVal);
                inputBuffer2.putFloat(bVal);
            }

            Log.d(TAG, String.format(Locale.getDefault(),
                    "Telemetria Tensor Input Qualcomm [0,1] -> Min: %.3f, Max: %.3f, Média Pixel: %.3f",
                    minVal1, maxVal1, sumVal1 / (width * height * 3)));

            Object[] inputs = new Object[] { inputBuffer1, inputBuffer2 };

            float[][] outputBuffer = new float[2][EMBEDDING_DIMENSION];
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputBuffer);

            synchronized (TestFaceActivity.class) {
                tfliteInterpreter.runForMultipleInputsOutputs(inputs, outputs);
            }

            return outputBuffer[0].clone();
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico na inferência neural TFLite MobileFaceNet", e);
            lastModelError = e.getClass().getSimpleName() + ": " + e.getMessage();
            return null;
        }
    }

    private String calcularSHA256Arquivo(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase(Locale.US);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao calcular SHA-256 do arquivo " + file.getName(), e);
            return "ERRO_HASH";
        }
    }

    private float calcularNormaL2Bruta(float[] vector) {
        if (vector == null) return 0.0f;
        float sum = 0.0f;
        for (float val : vector) {
            if (!Float.isNaN(val) && !Float.isInfinite(val)) sum += val * val;
        }
        return (float) Math.sqrt(sum);
    }

    private float calcularMediaAritmeticaBruta(float[] vector) {
        if (vector == null || vector.length == 0) return 0.0f;
        float sum = 0.0f;
        int count = 0;
        for (float val : vector) {
            if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                sum += val;
                count++;
            }
        }
        return count > 0 ? (sum / count) : 0.0f;
    }

    private void normalizarVetorL2Padrão(float[] vector) {
        if (vector == null) return;
        float norm = calcularNormaL2Bruta(vector);
        if (norm > 1e-6f) {
            for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        }
    }

    private float calcularSimilaridadeCosseno(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0f;

        float dot = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }

        if (normA <= 1e-6f || normB <= 1e-6f) return 0.0f;
        float sim = dot / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
        return Math.max(-1.0f, Math.min(1.0f, sim));
    }

    private void salvarTemplateDeTeste(float[] rawEmbedding, Bitmap faceCrop) {
        try {
            File dir = new File(getFilesDir(), "test_biometrics");
            if (!dir.exists()) dir.mkdirs();

            File templateFile = new File(dir, TEMPLATE_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(templateFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(rawEmbedding);
            oos.close();
            fos.close();

            // Salva EXCLUSIVAMENTE o bitmap LIMPO de 112x112 em disco
            File imgFile = new File(dir, REGISTERED_IMAGE_NAME);
            FileOutputStream imgFos = new FileOutputStream(imgFile);
            faceCrop.compress(Bitmap.CompressFormat.PNG, 100, imgFos);
            imgFos.close();

            String templateHash = calcularSHA256Arquivo(templateFile);
            float normRaw = calcularNormaL2Bruta(rawEmbedding);

            if (tvStatusTest != null) {
                tvStatusTest.setText(String.format(Locale.getDefault(),
                        "CADASTRO NEURAL SALVO!\nTemplate SHA-256: %s... (Imutável)\nNorma L2 Bruta: %.4f. Pronto para validação.",
                        templateHash.substring(0, 12), normRaw));
            }
            Toast.makeText(this, "Rosto cadastrado no modelo neural MobileFaceNet!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar template de teste", e);
        }
    }

    private void salvarTemplateGeometrico(float[] geometricVector) {
        try {
            File dir = new File(getFilesDir(), "test_biometrics");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, GEOMETRIC_TEMPLATE_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(geometricVector);
            oos.close();
            fos.close();

            if (tvStatusTest != null) {
                tvStatusTest.setText("CADASTRO GEOMÉTRICO (133 PONTOS) SALVO!\nRecursos 10-d gravados em arquivo separado.");
            }
            Toast.makeText(this, "Geometria facial de 133 pontos cadastrada com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar template geométrico de teste", e);
        }
    }

    private void validarTemplateEGeometria(float[] novoRawEmbedding, float[] novoGeometricVector, int contourPointsCount, Face face) {
        try {
            File dir = new File(getFilesDir(), "test_biometrics");
            File templateFile = new File(dir, TEMPLATE_FILE_NAME);
            File geomFile = new File(dir, GEOMETRIC_TEMPLATE_FILE_NAME);

            if (!templateFile.exists()) {
                if (tvStatusTest != null) tvStatusTest.setText("Nenhum cadastro neural encontrado. Clique em 'CADASTRAR NEURAL'.");
                return;
            }

            // Auditoria de Integridade HASH SHA-256
            String hashAntes = calcularSHA256Arquivo(templateFile);

            FileInputStream fis = new FileInputStream(templateFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            float[] storedRawEmbedding = (float[]) ois.readObject();
            ois.close();
            fis.close();

            String hashDepois = calcularSHA256Arquivo(templateFile);
            boolean isIntegrityVerified = hashAntes.equals(hashDepois);

            float normStored = calcularNormaL2Bruta(storedRawEmbedding);
            float normNovo = calcularNormaL2Bruta(novoRawEmbedding);

            float[] vStoredStandard = storedRawEmbedding.clone();
            float[] vNovoStandard = novoRawEmbedding.clone();
            normalizarVetorL2Padrão(vStoredStandard);
            normalizarVetorL2Padrão(vNovoStandard);
            float simStandard = calcularSimilaridadeCosseno(vStoredStandard, vNovoStandard);

            // Análise da Telemetria Geométrica e Ângulos de Cabeça
            float eulerX = face != null ? face.getHeadEulerAngleX() : 0.0f; // Pitch (Incli. Vertical)
            float eulerY = face != null ? face.getHeadEulerAngleY() : 0.0f; // Yaw (Incli. Lateral)
            float eulerZ = face != null ? face.getHeadEulerAngleZ() : 0.0f; // Roll (Incli. Rotação)

            String geomReportStr = "Geometria Não Cadastrada";
            float maxGeomDiff = 0.0f;
            int maxGeomFeatureIdx = -1;

            if (geomFile.exists() && novoGeometricVector != null) {
                try {
                    FileInputStream gFis = new FileInputStream(geomFile);
                    ObjectInputStream gOis = new ObjectInputStream(gFis);
                    float[] storedGeomVector = (float[]) gOis.readObject();
                    gOis.close();
                    gFis.close();

                    if (storedGeomVector.length == novoGeometricVector.length) {
                        float distGeomSq = 0.0f;
                        for (int i = 0; i < storedGeomVector.length; i++) {
                            float diff = Math.abs(storedGeomVector[i] - novoGeometricVector[i]);
                            if (diff > maxGeomDiff) {
                                maxGeomDiff = diff;
                                maxGeomFeatureIdx = i;
                            }
                            distGeomSq += diff * diff;
                        }
                        float distGeom = (float) Math.sqrt(distGeomSq);
                        geomReportStr = String.format(Locale.getDefault(),
                                "Distância L2 Geométrica: %.4f\n• Maior Δ Recurso [g%d]: %.4f",
                                distGeom, maxGeomFeatureIdx, maxGeomDiff);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao carregar template geométrico", e);
                }
            }

            String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            String resultStr = String.format(Locale.getDefault(),
                    "--- ANÁLISE NEURAL — MOBILEFACENET [%s] ---\n" +
                    "• SHA-256 Template: %s... (%s)\n" +
                    "• L2 Padrão Cosseno: %.6f (%.2f%%)\n\n" +
                    "--- ANÁLISE GEOMÉTRICA & POSTURA (133 PONTOS) ---\n" +
                    "• Ângulos Pose: Pitch=%.1f°, Yaw=%.1f°, Roll=%.1f°\n" +
                    "• Contornos: %d pontos (%s)\n" +
                    "• %s",
                    timeStr,
                    hashAntes.substring(0, 10),
                    isIntegrityVerified ? "Imutável" : "ALERTA",
                    simStandard, simStandard * 100.0f,
                    eulerX, eulerY, eulerZ,
                    contourPointsCount, contourPointsCount >= 100 ? "COMPLETA" : "INCOMPLETA",
                    geomReportStr);

            if (tvStatusTest != null) tvStatusTest.setText(resultStr);

            new AlertDialog.Builder(this)
                    .setTitle("Painel de Diagnóstico Independente")
                    .setMessage(resultStr + "\n\nPré-visualizações neurais 112x112 limpas e overlay de 133 pontos exibido separadamente.")
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao validar template e geometria", e);
        }
    }

    private void limparCadastroDeTeste() {
        try {
            File dir = new File(getFilesDir(), "test_biometrics");
            File datFile = new File(dir, TEMPLATE_FILE_NAME);
            File geomFile = new File(dir, GEOMETRIC_TEMPLATE_FILE_NAME);
            File imgFile = new File(dir, REGISTERED_IMAGE_NAME);

            if (datFile.exists()) datFile.delete();
            if (geomFile.exists()) geomFile.delete();
            if (imgFile.exists()) imgFile.delete();

            if (ivRegisteredFacePreview != null) ivRegisteredFacePreview.setImageBitmap(null);
            if (ivLastValidatedFacePreview != null) ivLastValidatedFacePreview.setImageBitmap(null);
            if (ivContourOverlayPreview != null) ivContourOverlayPreview.setImageBitmap(null);

            if (tvStatusTest != null) tvStatusTest.setText("Cadastros de teste neural e geométrico removidos.");
            Toast.makeText(this, "Cadastros de teste neural e geométrico removidos com sucesso.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao limpar cadastros de teste", e);
        }
    }
}
