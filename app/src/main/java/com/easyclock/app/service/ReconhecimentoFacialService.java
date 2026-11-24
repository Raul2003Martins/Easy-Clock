package com.easyclock.app.service;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.easyclock.app.model.BiometricTemplate;
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
import java.util.HashMap;
import java.util.Map;

public class ReconhecimentoFacialService {

    private static final String TAG = "EasyClockFaceID";
    private static final String MODEL_FILE_NAME = "mobilefacenet.tflite";

    public static final int EMBEDDING_DIMENSION = 128;
    public static final int VERSAO_EMBEDDING_QUALCOMM = 7;

    // Novo Limite Neural de Aceitação: Cosseno mínimo de 0.80 (80.0%)
    public static final float LIMIAR_SIMILARIDADE_MINIMA = 0.80f;

    private static Interpreter tfliteInterpreter;
    private static boolean isModelLoaded = false;
    private static String lastModelError = null;

    public interface OnAnaliseFacialListener {
        void onSuccess(Bitmap faceCrop, float[] embedding);
        void onFailure(String mensagemErro);
    }

    public static boolean validarAprovacaoSimilaridade(float sim) {
        return Float.isFinite(sim) && sim >= LIMIAR_SIMILARIDADE_MINIMA;
    }

    private static synchronized void garantirModeloCarregado(Context context) {
        if (isModelLoaded && tfliteInterpreter != null) return;

        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE_NAME);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tfliteInterpreter = new Interpreter(mappedByteBuffer, options);

            isModelLoaded = true;
            lastModelError = null;
            Log.d(TAG, "Modelo Qualcomm MobileFaceNet NCHW TFLite carregado com sucesso na aplicação principal.");
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico ao carregar modelo TFLite na aplicação principal", e);
            isModelLoaded = false;
            tfliteInterpreter = null;
            lastModelError = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public static void analisarEExtrairBiometria(Context context, Bitmap bitmap, OnAnaliseFacialListener listener) {
        if (bitmap == null || listener == null) {
            if (listener != null) listener.onFailure("Imagem da câmera não disponível.");
            return;
        }

        garantirModeloCarregado(context);
        if (!isModelLoaded || tfliteInterpreter == null) {
            listener.onFailure("Erro ao inicializar o motor de reconhecimento neural TFLite: " +
                    (lastModelError != null ? lastModelError : "Modelo indisponível"));
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        listener.onFailure("Nenhum rosto identificado. Posicione seu rosto dentro da câmera.");
                        return;
                    }

                    if (faces.size() > 1) {
                        listener.onFailure("Mais de um rosto detectado. Por favor, certifique-se de estar sozinho na câmera.");
                        return;
                    }

                    Face face = faces.get(0);

                    // Validação de Pose da Cabeça
                    float rotX = face.getHeadEulerAngleX();
                    float rotY = face.getHeadEulerAngleY();
                    float rotZ = face.getHeadEulerAngleZ();

                    if (Math.abs(rotY) > 20.0f || Math.abs(rotZ) > 20.0f || Math.abs(rotX) > 20.0f) {
                        listener.onFailure("Olhe diretamente para a câmera sem virar ou inclinar a cabeça.");
                        return;
                    }

                    // Alinhamento Canônico de 5 Landmarks Faciais
                    Bitmap faceCrop = cropFace5LandmarksAligned(bitmap, face);
                    if (faceCrop == null) {
                        listener.onFailure("Não foi possível identificar todos os 5 pontos faciais. Centralize o rosto e tente novamente.");
                        return;
                    }

                    // Extração de Embedding Neural NCHW Float32 [0.0, 1.0]
                    float[] embedding = extrairEmbeddingMobileFaceNetNCHW(faceCrop);
                    if (embedding == null || contemValoresInvalidos(embedding)) {
                        listener.onFailure("Falha ao gerar o vetor biométrico neural. Tente novamente com boa iluminação.");
                        return;
                    }

                    listener.onSuccess(faceCrop, embedding);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro na detecção facial ML Kit", e);
                    listener.onFailure("Falha ao analisar imagem da câmera: " + e.getLocalizedMessage());
                });
    }

    private static Bitmap cropFace5LandmarksAligned(Bitmap src, Face face) {
        if (src == null || face == null) return null;

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
        FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);

        if (leftEye == null || rightEye == null || noseBase == null || mouthLeft == null || mouthRight == null) {
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
            { 38.2943f, 51.6963f }, // Olho Esquerdo
            { 73.5318f, 51.6963f }, // Olho Direito
            { 56.0252f, 71.7366f }, // Base do Nariz
            { 41.5493f, 92.3655f }, // Canto Esquerdo da Boca
            { 70.7299f, 92.3655f }  // Canto Direito da Boca
        };

        float[] transformParams = computeLeastSquaresSimilarityMatrix(srcPts, dstPts);
        if (transformParams == null) return null;

        float a = transformParams[0];
        float b = transformParams[1];
        float tx = transformParams[2];
        float ty = transformParams[3];

        float scaleSq = a * a + b * b;
        if (scaleSq <= 1e-8f || Float.isNaN(scaleSq)) return null;

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
            Log.e(TAG, "Erro ao alinhar face por 5 landmarks", e);
            return null;
        }
    }

    private static float[] computeLeastSquaresSimilarityMatrix(float[][] srcPts, float[][] dstPts) {
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

    private static float[] extrairEmbeddingMobileFaceNetNCHW(Bitmap faceCrop) {
        if (faceCrop == null || tfliteInterpreter == null) return null;

        try {
            Bitmap scaled = Bitmap.createScaledBitmap(faceCrop, 112, 112, true);
            int width = scaled.getWidth();
            int height = scaled.getHeight();

            int planeSize = width * height;
            int singleImageSize = 3 * planeSize * 4;

            ByteBuffer inputBuffer1 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer1.order(ByteOrder.nativeOrder());

            ByteBuffer inputBuffer2 = ByteBuffer.allocateDirect(singleImageSize);
            inputBuffer2.order(ByteOrder.nativeOrder());

            int[] pixels = new int[planeSize];
            scaled.getPixels(pixels, 0, width, 0, 0, width, height);

            // Layout Planar NCHW [1, 3, 112, 112], RGB em [0.0, 1.0]
            float[] chw = new float[3 * planeSize];

            for (int i = 0; i < planeSize; i++) {
                int pixel = pixels[i];
                float rVal = ((pixel >> 16) & 0xFF) / 255.0f;
                float gVal = ((pixel >> 8) & 0xFF) / 255.0f;
                float bVal = (pixel & 0xFF) / 255.0f;

                chw[i] = rVal;
                chw[planeSize + i] = gVal;
                chw[2 * planeSize + i] = bVal;
            }

            inputBuffer1.asFloatBuffer().put(chw);
            inputBuffer2.asFloatBuffer().put(chw);
            inputBuffer1.rewind();
            inputBuffer2.rewind();

            Object[] inputs = new Object[] { inputBuffer1, inputBuffer2 };

            float[][] outputBuffer = new float[2][EMBEDDING_DIMENSION];
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, outputBuffer);

            synchronized (ReconhecimentoFacialService.class) {
                tfliteInterpreter.runForMultipleInputsOutputs(inputs, outputs);
            }

            return outputBuffer[0].clone();
        } catch (Exception e) {
            Log.e(TAG, "Erro crítico na inferência neural TFLite MobileFaceNet NCHW", e);
            return null;
        }
    }

    public static boolean contemValoresInvalidos(float[] vector) {
        if (vector == null || vector.length != EMBEDDING_DIMENSION) return true;
        for (float val : vector) {
            if (Float.isNaN(val) || Float.isInfinite(val)) return true;
        }
        return false;
    }

    public static float calcularSimilaridadeCosseno(float[] v1, float[] v2) {
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

    public static boolean salvarTemplateBiometrico(Context context, String uid, float[] embedding, Bitmap faceCrop) {
        if (context == null || uid == null || uid.trim().isEmpty() || embedding == null) return false;

        try {
            File dir = new File(context.getFilesDir(), "biometrics");
            if (!dir.exists()) dir.mkdirs();

            BiometricTemplate template = new BiometricTemplate(VERSAO_EMBEDDING_QUALCOMM, embedding, uid, System.currentTimeMillis());

            File datFile = new File(dir, uid + ".dat");
            FileOutputStream fos = new FileOutputStream(datFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(template);
            oos.close();
            fos.close();

            if (faceCrop != null) {
                File profileFile = new File(dir, uid + "_profile.png");
                FileOutputStream imgFos = new FileOutputStream(profileFile);
                faceCrop.compress(Bitmap.CompressFormat.PNG, 100, imgFos);
                imgFos.close();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar template biométrico local do usuário " + uid, e);
            return false;
        }
    }

    public static float[] carregarTemplateBiometrico(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return null;

        try {
            File datFile = new File(new File(context.getFilesDir(), "biometrics"), uid + ".dat");
            if (!datFile.exists()) return null;

            FileInputStream fis = new FileInputStream(datFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            BiometricTemplate template = (BiometricTemplate) ois.readObject();
            ois.close();
            fis.close();

            if (template != null && template.getVersao() == VERSAO_EMBEDDING_QUALCOMM) {
                return template.getEmbedding();
            }

            Log.w(TAG, "Template do usuário " + uid + " em versão incompatível (" +
                    (template != null ? template.getVersao() : "null") + " != " + VERSAO_EMBEDDING_QUALCOMM + "). Recadastro necessário.");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar template biométrico do usuário " + uid, e);
            return null;
        }
    }

    public static File obterArquivoFotoPerfil(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return null;
        File profileFile = new File(new File(context.getFilesDir(), "biometrics"), uid + "_profile.png");
        return profileFile.exists() ? profileFile : null;
    }
}
