package com.easyclock.testprototype;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestFaceActivity extends AppCompatActivity {

    private static final String TAG = "TestFaceActivity";
    private static final String MODEL_FILE_NAME = "mobilefacenet.tflite";
    private static final int EMBEDDING_DIMENSION = 128;
    private static final int BATCH_SIZE = 2;

    private PreviewView previewViewTest;
    private ImageView ivProcessedFacePreview;
    private TextView tvStatusTest;
    private Button btnCadastrarRostoTest;
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
        verificarPermissaoECamera();
    }

    private void inicializarComponentes() {
        previewViewTest = findViewById(R.id.previewViewTest);
        ivProcessedFacePreview = findViewById(R.id.ivProcessedFacePreview);
        tvStatusTest = findViewById(R.id.tvStatusTest);
        btnCadastrarRostoTest = findViewById(R.id.btnCadastrarRostoTest);
        btnValidarRostoTest = findViewById(R.id.btnValidarRostoTest);
        btnLimparCadastroTest = findViewById(R.id.btnLimparCadastroTest);

        if (btnCadastrarRostoTest != null) {
            btnCadastrarRostoTest.setOnClickListener(v -> capturarEProcessar(true));
        }

        if (btnValidarRostoTest != null) {
            btnValidarRostoTest.setOnClickListener(v -> capturarEProcessar(false));
        }

        if (btnLimparCadastroTest != null) {
            btnLimparCadastroTest.setOnClickListener(v -> limparCadastroDeTeste());
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
                        tvStatusTest.setText("Câmera Pronta (Qualcomm Multi-Input [img1,img2] -> [2,128] Pronto). Selecione uma operação:");
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

    private void capturarEProcessar(boolean isModoCadastro) {
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
                    processarBiometria(bitmap, isModoCadastro);
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

    private void processarBiometria(Bitmap bitmap, boolean isModoCadastro) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        if (tvStatusTest != null) tvStatusTest.setText("Nenhum rosto detectado.");
                        return;
                    }

                    if (faces.size() > 1) {
                        if (tvStatusTest != null) tvStatusTest.setText("Mais de um rosto detectado.");
                        return;
                    }

                    Face face = faces.get(0);
                    Bitmap faceCrop = cropFaceEyeAligned(bitmap, face);
                    if (faceCrop == null) {
                        if (tvStatusTest != null) tvStatusTest.setText("Erro ao recortar e alinhar rosto.");
                        return;
                    }

                    // Exibe na tela a imagem exata recortada e alinhada enviada ao TFLite
                    if (ivProcessedFacePreview != null) {
                        ivProcessedFacePreview.setImageBitmap(faceCrop);
                    }

                    executorService.execute(() -> {
                        float[] embedding = extrairEmbeddingMobileFaceNet(faceCrop);
                        runOnUiThread(() -> {
                            if (embedding == null) {
                                if (tvStatusTest != null) {
                                    String msg = lastModelError != null ?
                                            "Erro TFLite: " + lastModelError :
                                            "Falha na inferência neural TFLite. Modelo indisponível.";
                                    tvStatusTest.setText(msg);
                                }
                                return;
                            }

                            if (isModoCadastro) {
                                salvarTemplateDeTeste(embedding);
                            } else {
                                validarTemplateDeTeste(embedding);
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    if (tvStatusTest != null) tvStatusTest.setText("Erro ML Kit: " + e.getLocalizedMessage());
                });
    }

    private Bitmap cropFaceEyeAligned(Bitmap src, Face face) {
        if (src == null || face == null) return null;

        Rect bounds = face.getBoundingBox();
        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);

        // Se ambos os olhos estiverem disponíveis, alinha e escala usando a transformação canônica de 2 pontos (38.29, 51.70) e (73.53, 51.70)
        if (leftEye != null && rightEye != null) {
            float p1_x = leftEye.getPosition().x;
            float p1_y = leftEye.getPosition().y;
            float p2_x = rightEye.getPosition().x;
            float p2_y = rightEye.getPosition().y;

            // Ordena os pontos de olho na tela: pLeftX < pRightX
            float pLeftX = Math.min(p1_x, p2_x);
            float pLeftY = (pLeftX == p1_x) ? p1_y : p2_y;
            float pRightX = Math.max(p1_x, p2_x);
            float pRightY = (pRightX == p2_x) ? p2_y : p1_y;

            float[] srcPoints = new float[] {
                pLeftX, pLeftY,
                pRightX, pRightY
            };

            // Posições canônicas ideais dos olhos em um recorte padrão 112x112 ArcFace/MobileFaceNet
            float[] dstPoints = new float[] {
                38.2943f, 51.6963f,  // Olho esquerdo no bitmap 112x112
                73.5318f, 51.6963f   // Olho direito no bitmap 112x112
            };

            Matrix transform = new Matrix();
            transform.setPolyToPoly(srcPoints, 0, dstPoints, 0, 2);

            Bitmap aligned112 = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(aligned112);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(src, transform, paint);

            return aligned112;
        }

        // Fallback para BoundingBox em caso de ausência de landmarks dos olhos
        int x1 = Math.max(0, bounds.left);
        int y1 = Math.max(0, bounds.top);
        int w = Math.min(src.getWidth() - x1, bounds.width());
        int h = Math.min(src.getHeight() - y1, bounds.height());

        if (w <= 10 || h <= 10) return null;

        try {
            Bitmap cropped = Bitmap.createBitmap(src, x1, y1, w, h);
            return Bitmap.createScaledBitmap(cropped, 112, 112, true);
        } catch (Exception e) {
            Log.e(TAG, "Erro no fallback de recorte BoundingBox", e);
            return null;
        }
    }

    private float[] extrairEmbeddingMobileFaceNet(Bitmap faceCrop) {
        if (faceCrop == null || tfliteInterpreter == null) {
            Log.e(TAG, "Inferência abortada: tfliteInterpreter é nulo ou faceCrop indisponível.");
            return null;
        }

        try {
            Bitmap scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true);
            int width = scaled.getWidth();
            int height = scaled.getHeight();

            // O modelo Qualcomm MobileFaceNet exige 2 tensores de entrada independentes: 'img1' [1, 112, 112, 3] e 'img2' [1, 112, 112, 3] (150.528 bytes cada)
            int singleImageSize = 1 * 112 * 112 * 3 * 4;

            ByteBuffer inputBuffer1 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer1.order(ByteOrder.nativeOrder());
            inputBuffer1.rewind();

            ByteBuffer inputBuffer2 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer2.order(ByteOrder.nativeOrder());
            inputBuffer2.rewind();

            int[] pixels = new int[width * height];
            scaled.getPixels(pixels, 0, width, 0, 0, width, height);

            // Preenche cada buffer de entrada independente com os pixels Float32 RGB no intervalo [0.0, 1.0]
            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                float rVal = r / 255.0f;
                float gVal = g / 255.0f;
                float bVal = blue / 255.0f;

                inputBuffer1.putFloat(rVal);
                inputBuffer1.putFloat(gVal);
                inputBuffer1.putFloat(bVal);

                inputBuffer2.putFloat(rVal);
                inputBuffer2.putFloat(gVal);
                inputBuffer2.putFloat(bVal);
            }

            Object[] inputs = new Object[] { inputBuffer1, inputBuffer2 };

            // Declara array Java de saída compatível com o Tensor 'embeddings' de shape [2, 128]
            float[][] outputBuffer = new float[2][EMBEDDING_DIMENSION];
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputBuffer);

            synchronized (TestFaceActivity.class) {
                tfliteInterpreter.runForMultipleInputsOutputs(inputs, outputs);
            }

            // Extrai o vetor biométrico individual de 128 dimensões da posição 0 ('img1')
            float[] vector = outputBuffer[0];
            normalizarVetorL2(vector);
            return vector;
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico na inferência neural TFLite MobileFaceNet", e);
            lastModelError = e.getClass().getSimpleName() + ": " + e.getMessage();
            return null;
        }
    }

    private void normalizarVetorL2(float[] vector) {
        if (vector == null) return;
        float sum = 0.0f;
        for (float val : vector) {
            if (!Float.isNaN(val) && !Float.isInfinite(val)) sum += val * val;
        }
        float norm = (float) Math.sqrt(sum);
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
        return Math.max(0.0f, Math.min(1.0f, sim));
    }

    private void salvarTemplateDeTeste(float[] embedding) {
        try {
            File dir = new File(getFilesDir(), "test_biometrics");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "qualcomm_template.dat");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(embedding);
            oos.close();
            fos.close();

            if (tvStatusTest != null) {
                tvStatusTest.setText("Cadastro Qualcomm Multi-Input (128-d) salvo! Agora teste 'VALIDAR ROSTO'.");
            }
            Toast.makeText(this, "Rosto de teste cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar template de teste", e);
        }
    }

    private void validarTemplateDeTeste(float[] novoEmbedding) {
        try {
            File file = new File(new File(getFilesDir(), "test_biometrics"), "qualcomm_template.dat");
            if (!file.exists()) {
                if (tvStatusTest != null) tvStatusTest.setText("Nenhum cadastro de teste encontrado. Clique em 'CADASTRAR ROSTO'.");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            float[] storedEmbedding = (float[]) ois.readObject();
            ois.close();
            fis.close();

            if (storedEmbedding.length != novoEmbedding.length) {
                if (tvStatusTest != null) tvStatusTest.setText("Dimensão biométrica incompatível (" + storedEmbedding.length + " != " + novoEmbedding.length + "). Clique em LIMPAR CADASTRO.");
                return;
            }

            float sim = calcularSimilaridadeCosseno(storedEmbedding, novoEmbedding);

            String resultStr = String.format(Locale.getDefault(), "Qualcomm TFLite Multi-Input\nSimilaridade: %.1f%%\nCosseno Bruto: %.4f", sim * 100.0f, sim);
            if (tvStatusTest != null) tvStatusTest.setText(resultStr);

            new AlertDialog.Builder(this)
                    .setTitle("Inferência Qualcomm Multi-Input (Real)")
                    .setMessage(resultStr + "\n\nSua similaridade de cosseno bruta é exibida sem travas de limiar.")
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao validar template de teste", e);
        }
    }

    private void limparCadastroDeTeste() {
        try {
            File file = new File(new File(getFilesDir(), "test_biometrics"), "qualcomm_template.dat");
            if (file.exists()) file.delete();

            if (tvStatusTest != null) tvStatusTest.setText("Cadastro de teste removido.");
            Toast.makeText(this, "Cadastro de teste removido com sucesso.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao limpar cadastro de teste", e);
        }
    }
}
