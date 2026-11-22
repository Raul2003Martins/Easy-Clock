package com.easyclock.app.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Locale;

public class ReconhecimentoFacialService {

    private static final String TAG = "EasyClockFaceID";
    public static final float LIMIAR_SIMILARIDADE_MINIMA = 0.75f; // 75% de correspondência mínima
    public static final int EMBEDDING_DIMENSION = 128;

    public interface OnAnaliseFacialListener {
        void onSuccess(Bitmap faceCrop, float[] embedding);
        void onFailure(String mensagemErro);
    }

    public static void analisarEExtrairBiometria(Bitmap bitmap, OnAnaliseFacialListener listener) {
        if (bitmap == null || listener == null) {
            if (listener != null) listener.onFailure("Imagem da câmera não disponível.");
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        Log.d(TAG, String.format(Locale.US, "Detecção falhou: 0 rostos em img %dx%d", bitmap.getWidth(), bitmap.getHeight()));
                        listener.onFailure("Nenhum rosto identificado. Posicione seu rosto dentro da câmera.");
                        return;
                    }

                    if (faces.size() > 1) {
                        Log.d(TAG, String.format(Locale.US, "Detecção recusada: %d rostos encontrados", faces.size()));
                        listener.onFailure("Mais de um rosto detectado. Por favor, certifique-se de estar sozinho na câmera.");
                        return;
                    }

                    Face face = faces.get(0);
                    Rect bounds = face.getBoundingBox();

                    // Validação de Posição da Cabeça (Euler Angles)
                    float rotX = face.getHeadEulerAngleX(); // Inclinação vertical (olhar cima/baixo)
                    float rotY = face.getHeadEulerAngleY(); // Guinada horizontal (virar esquerda/direita)
                    float rotZ = face.getHeadEulerAngleZ(); // Inclinação lateral (tombar cabeça)

                    Log.d(TAG, String.format(Locale.US,
                            "Face Detectada: imgW=%d, imgH=%d, rotY=%.1f, rotZ=%.1f, rotX=%.1f, bounds=[%d,%d,%d,%d], size=%dx%d",
                            bitmap.getWidth(), bitmap.getHeight(), rotY, rotZ, rotX,
                            bounds.left, bounds.top, bounds.right, bounds.bottom, bounds.width(), bounds.height()));

                    if (Math.abs(rotY) > 30.0f) {
                        listener.onFailure("Olhe diretamente para a câmera sem virar o rosto para os lados.");
                        return;
                    }

                    if (Math.abs(rotZ) > 30.0f) {
                        listener.onFailure("Mantenha a cabeça reta, sem inclinar para o ombro.");
                        return;
                    }

                    if (Math.abs(rotX) > 30.0f) {
                        listener.onFailure("Mantenha a cabeça e o olhar nivelados com a câmera.");
                        return;
                    }

                    // Validação do tamanho e margens da face
                    int faceWidth = bounds.width();
                    int faceHeight = bounds.height();
                    int imgWidth = bitmap.getWidth();
                    int imgHeight = bitmap.getHeight();

                    if (faceWidth < (imgWidth * 0.15f) || faceHeight < (imgHeight * 0.15f)) {
                        listener.onFailure("Aproxime um pouco mais o rosto da câmera.");
                        return;
                    }

                    // Permite pequena margem de margem sem rejeitar se estiver centralizado
                    int maxBoundaryExceed = 30;
                    if (bounds.left < -maxBoundaryExceed || bounds.top < -maxBoundaryExceed ||
                            bounds.right > imgWidth + maxBoundaryExceed || bounds.bottom > imgHeight + maxBoundaryExceed) {
                        listener.onFailure("Centralize seu rosto dentro da área indicada.");
                        return;
                    }

                    // Recorte do Rosto
                    Bitmap faceCrop = cropBitmapSafely(bitmap, bounds);
                    if (faceCrop == null) {
                        listener.onFailure("Erro ao recortar imagem do rosto. Tente novamente.");
                        return;
                    }

                    // Extração de Vetor de Características Biométricas (128-d)
                    float[] embedding = extrairEmbeddingFacial(face, faceCrop);
                    listener.onSuccess(faceCrop, embedding);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro na detecção facial ML Kit", e);
                    listener.onFailure("Falha ao analisar imagem da câmera: " + e.getLocalizedMessage());
                });
    }

    private static Bitmap cropBitmapSafely(Bitmap src, Rect bounds) {
        try {
            int x = Math.max(0, bounds.left);
            int y = Math.max(0, bounds.top);
            int w = Math.min(src.getWidth() - x, bounds.width());
            int h = Math.min(src.getHeight() - y, bounds.height());
            if (w <= 0 || h <= 0) return null;
            return Bitmap.createBitmap(src, x, y, w, h);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao recortar imagem facial", e);
            return null;
        }
    }

    public static float[] extrairEmbeddingFacial(Face face, Bitmap faceCrop) {
        float[] vector = new float[EMBEDDING_DIMENSION];

        // 1. Extração baseada em pontos anatômicos ML Kit
        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
        FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);

        int idx = 0;
        if (leftEye != null) {
            vector[idx++] = leftEye.getPosition().x / (float) faceCrop.getWidth();
            vector[idx++] = leftEye.getPosition().y / (float) faceCrop.getHeight();
        }
        if (rightEye != null) {
            vector[idx++] = rightEye.getPosition().x / (float) faceCrop.getWidth();
            vector[idx++] = rightEye.getPosition().y / (float) faceCrop.getHeight();
        }
        if (nose != null) {
            vector[idx++] = nose.getPosition().x / (float) faceCrop.getWidth();
            vector[idx++] = nose.getPosition().y / (float) faceCrop.getHeight();
        }
        if (mouthLeft != null) {
            vector[idx++] = mouthLeft.getPosition().x / (float) faceCrop.getWidth();
            vector[idx++] = mouthLeft.getPosition().y / (float) faceCrop.getHeight();
        }
        if (mouthRight != null) {
            vector[idx++] = mouthRight.getPosition().x / (float) faceCrop.getWidth();
            vector[idx++] = mouthRight.getPosition().y / (float) faceCrop.getHeight();
        }

        // 2. Extração espacial de textura e intensidades em grade 16x16
        Bitmap scaled = Bitmap.createScaledBitmap(faceCrop, 16, 16, false);
        for (int y = 0; y < 16 && idx < EMBEDDING_DIMENSION; y++) {
            for (int x = 0; x < 16 && idx < EMBEDDING_DIMENSION; x++) {
                int pixel = scaled.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                float gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
                vector[idx++] = gray;
            }
        }

        normalizarVetor(vector);
        return vector;
    }

    private static void normalizarVetor(float[] vector) {
        float sumSquares = 0.0f;
        for (float val : vector) {
            sumSquares += val * val;
        }
        float norm = (float) Math.sqrt(sumSquares);
        if (norm > 1e-6f) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    public static float calcularSimilaridadeCosseno(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0f;

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }

        if (normA == 0.0f || normB == 0.0f) return 0.0f;

        float sim = dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
        return Math.max(0.0f, Math.min(1.0f, sim));
    }

    public static boolean salvarTemplateBiometrico(Context context, String uid, float[] embedding, Bitmap faceCrop) {
        if (context == null || uid == null || uid.trim().isEmpty() || embedding == null) return false;

        try {
            File dirBio = new File(context.getFilesDir(), "biometrics");
            if (!dirBio.exists()) dirBio.mkdirs();

            File fileBio = new File(dirBio, uid.trim() + ".dat");
            FileOutputStream fos = new FileOutputStream(fileBio);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(embedding);
            oos.close();
            fos.close();

            if (faceCrop != null) {
                File dirImg = new File(context.getFilesDir(), "profile");
                if (!dirImg.exists()) dirImg.mkdirs();

                File fileImg = new File(dirImg, uid.trim() + ".jpg");
                FileOutputStream fosImg = new FileOutputStream(fileImg);
                faceCrop.compress(Bitmap.CompressFormat.JPEG, 90, fosImg);
                fosImg.close();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar template biométrico local", e);
            return false;
        }
    }

    public static float[] carregarTemplateBiometrico(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return null;

        try {
            File fileBio = new File(new File(context.getFilesDir(), "biometrics"), uid.trim() + ".dat");
            if (!fileBio.exists()) return null;

            FileInputStream fis = new FileInputStream(fileBio);
            ObjectInputStream ois = new ObjectInputStream(fis);
            float[] embedding = (float[]) ois.readObject();
            ois.close();
            fis.close();

            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar template biométrico local", e);
            return null;
        }
    }

    public static File obterArquivoFotoPerfil(Context context, String uid) {
        if (context == null || uid == null || uid.trim().isEmpty()) return null;
        File fileImg = new File(new File(context.getFilesDir(), "profile"), uid.trim() + ".jpg");
        return fileImg.exists() ? fileImg : null;
    }
}
