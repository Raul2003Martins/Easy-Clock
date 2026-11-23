package com.easyclock.app.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.easyclock.app.model.BiometricTemplate;
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
import java.util.Locale;

public class ReconhecimentoFacialService {

    private static final String TAG = "EasyClockFaceID";
    public static final float LIMIAR_SIMILARIDADE_MINIMA = 0.95f; // 95% de similaridade mínima obrigatória com fundo neutralizado
    public static final int EMBEDDING_DIMENSION = 128;
    public static final int VERSAO_EMBEDDING_ATUAL = 6; // Biometria v6 (Isolamento de Rosto & Neutralização de Fundo)

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
                    float rotX = face.getHeadEulerAngleX(); // Inclinação vertical
                    float rotY = face.getHeadEulerAngleY(); // Guinada horizontal
                    float rotZ = face.getHeadEulerAngleZ(); // Inclinação lateral

                    Log.d(TAG, String.format(Locale.US,
                            "Face Detectada v6: imgW=%d, imgH=%d, rotY=%.1f, rotZ=%.1f, rotX=%.1f, bounds=[%d,%d,%d,%d], size=%dx%d",
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

                    int faceWidth = bounds.width();
                    int faceHeight = bounds.height();
                    int imgWidth = bitmap.getWidth();
                    int imgHeight = bitmap.getHeight();

                    if (faceWidth < (imgWidth * 0.15f) || faceHeight < (imgHeight * 0.15f)) {
                        listener.onFailure("Aproxime um pouco mais o rosto da câmera.");
                        return;
                    }

                    int maxBoundaryExceed = 30;
                    if (bounds.left < -maxBoundaryExceed || bounds.top < -maxBoundaryExceed ||
                            bounds.right > imgWidth + maxBoundaryExceed || bounds.bottom > imgHeight + maxBoundaryExceed) {
                        listener.onFailure("Centralize seu rosto dentro da área indicada.");
                        return;
                    }

                    // Isolamento de Rosto & Neutralização Total do Fundo (Máscara Elíptica Anatómicamente Alinhada)
                    Bitmap faceMasked = isolarEProcessarApenasRosto(bitmap, face);
                    if (faceMasked == null) {
                        listener.onFailure("Erro ao isolar a anatomia do rosto. Tente novamente.");
                        return;
                    }

                    // Extração de Vetor de Características Biométricas da Face Isolada (128-d)
                    float[] embedding = extrairEmbeddingFacial(face, faceMasked);
                    if (embedding == null || contemValoresInvalidos(embedding)) {
                        listener.onFailure("Falha ao gerar o vetor biométrico. Tente novamente com boa iluminação.");
                        return;
                    }

                    listener.onSuccess(faceMasked, embedding);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro na detecção facial ML Kit", e);
                    listener.onFailure("Falha ao analisar imagem da câmera: " + e.getLocalizedMessage());
                });
    }

    private static Bitmap isolarEProcessarApenasRosto(Bitmap src, Face face) {
        if (src == null || face == null) return null;

        Rect bounds = face.getBoundingBox();
        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);

        // 1. Alinhamento Ocular (Correção de Rotação/Inclinação)
        Bitmap srcRotated = src;
        if (leftEye != null && rightEye != null) {
            float dx = rightEye.getPosition().x - leftEye.getPosition().x;
            float dy = rightEye.getPosition().y - leftEye.getPosition().y;
            double angleDeg = Math.toDegrees(Math.atan2(dy, dx));

            if (Math.abs(angleDeg) > 1.0) {
                Matrix matrix = new Matrix();
                float eyeMidX = (leftEye.getPosition().x + rightEye.getPosition().x) / 2.0f;
                float eyeMidY = (leftEye.getPosition().y + rightEye.getPosition().y) / 2.0f;
                matrix.postRotate((float) angleDeg, eyeMidX, eyeMidY);

                try {
                    srcRotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao rotacionar imagem pelos olhos", e);
                }
            }
        }

        // 2. Recorte Justo do Rosto
        int x1 = Math.max(0, bounds.left);
        int y1 = Math.max(0, bounds.top);
        int w = Math.min(srcRotated.getWidth() - x1, bounds.width());
        int h = Math.min(srcRotated.getHeight() - y1, bounds.height());

        if (w <= 20 || h <= 20) return null;

        Bitmap croppedFace;
        try {
            croppedFace = Bitmap.createBitmap(srcRotated, x1, y1, w, h);
        } catch (Exception e) {
            return null;
        }

        // 3. Aplicação de Máscara Elíptica de Remoção de Fundo
        // Cria um bitmap do mesmo tamanho preenchendo o fundo com PRETO NEUTRO (RGB 0,0,0)
        Bitmap maskedFace = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(maskedFace);
        canvas.drawColor(Color.BLACK); // Preenche 100% do fundo com preto neutro

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path path = new Path();

        // Delimita a elipse contendo APENAS a anatomia do rosto
        RectF ovalBounds = new RectF(
                w * 0.05f,
                h * 0.02f,
                w * 0.95f,
                h * 0.98f
        );
        path.addOval(ovalBounds, Path.Direction.CW);

        canvas.clipPath(path);
        canvas.drawBitmap(croppedFace, 0, 0, paint);

        return maskedFace;
    }

    public static float[] extrairEmbeddingFacial(Face face, Bitmap faceCrop) {
        if (faceCrop == null) return null;

        float[] vector = new float[EMBEDDING_DIMENSION];

        // 1. Extração baseada em pontos anatômicos ML Kit
        FaceLandmark leftEye = face != null ? face.getLandmark(FaceLandmark.LEFT_EYE) : null;
        FaceLandmark rightEye = face != null ? face.getLandmark(FaceLandmark.RIGHT_EYE) : null;
        FaceLandmark nose = face != null ? face.getLandmark(FaceLandmark.NOSE_BASE) : null;
        FaceLandmark mouthLeft = face != null ? face.getLandmark(FaceLandmark.MOUTH_LEFT) : null;
        FaceLandmark mouthRight = face != null ? face.getLandmark(FaceLandmark.MOUTH_RIGHT) : null;

        float interEyeDist = 1.0f;
        if (leftEye != null && rightEye != null) {
            float dx = rightEye.getPosition().x - leftEye.getPosition().x;
            float dy = rightEye.getPosition().y - leftEye.getPosition().y;
            interEyeDist = (float) Math.sqrt(dx * dx + dy * dy);
            if (interEyeDist < 1.0f) interEyeDist = 1.0f;
        }

        int idx = 0;
        if (leftEye != null && rightEye != null) {
            vector[idx++] = (rightEye.getPosition().x - leftEye.getPosition().x) / interEyeDist;
            vector[idx++] = (rightEye.getPosition().y - leftEye.getPosition().y) / interEyeDist;
        } else {
            vector[idx++] = 0.0f;
            vector[idx++] = 0.0f;
        }

        if (nose != null && leftEye != null) {
            vector[idx++] = (nose.getPosition().x - leftEye.getPosition().x) / interEyeDist;
            vector[idx++] = (nose.getPosition().y - leftEye.getPosition().y) / interEyeDist;
        } else {
            vector[idx++] = 0.0f;
            vector[idx++] = 0.0f;
        }

        if (mouthLeft != null && mouthRight != null) {
            vector[idx++] = (mouthRight.getPosition().x - mouthLeft.getPosition().x) / interEyeDist;
            vector[idx++] = (mouthRight.getPosition().y - mouthLeft.getPosition().y) / interEyeDist;
        } else {
            vector[idx++] = 0.0f;
            vector[idx++] = 0.0f;
        }

        // 2. Extração espacial de textura e intensidades da Face Isolada (Grade 16x16)
        Bitmap scaled = Bitmap.createScaledBitmap(faceCrop, 16, 16, false);
        int w = scaled.getWidth();
        int h = scaled.getHeight();
        float sumGray = 0.0f;
        float[] rawGrays = new float[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = scaled.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                float gray = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
                rawGrays[y * w + x] = gray;
                sumGray += gray;
            }
        }

        // Normalização de Contraste na Face Isolada
        float meanGray = sumGray / (w * h);

        for (int y = 0; y < 16 && idx < EMBEDDING_DIMENSION; y++) {
            for (int x = 0; x < 16 && idx < EMBEDDING_DIMENSION; x++) {
                float val = rawGrays[y * w + x] - meanGray;
                vector[idx++] = val;
            }
        }

        while (idx < EMBEDDING_DIMENSION) {
            vector[idx] = 0.0f;
            idx++;
        }

        normalizarVetor(vector);
        return vector;
    }

    private static void normalizarVetor(float[] vector) {
        if (vector == null) return;
        float sumSquares = 0.0f;
        for (float val : vector) {
            if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                sumSquares += val * val;
            }
        }
        float norm = (float) Math.sqrt(sumSquares);
        if (norm > 1e-6f) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    public static boolean contemValoresInvalidos(float[] vector) {
        if (vector == null || vector.length != EMBEDDING_DIMENSION) return true;
        for (float val : vector) {
            if (Float.isNaN(val) || Float.isInfinite(val)) {
                return true;
            }
        }
        return false;
    }

    public static float calcularSimilaridadeCosseno(float[] v1, float[] v2) {
        if (v1 == null || v2 == null) return 0.0f;
        if (v1.length != EMBEDDING_DIMENSION || v2.length != EMBEDDING_DIMENSION) return 0.0f;
        if (contemValoresInvalidos(v1) || contemValoresInvalidos(v2)) return 0.0f;

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }

        if (normA <= 1e-6f || normB <= 1e-6f) return 0.0f;

        float sim = dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
        if (Float.isNaN(sim) || Float.isInfinite(sim)) return 0.0f;

        return Math.max(0.0f, Math.min(1.0f, sim));
    }

    public static boolean salvarTemplateBiometrico(Context context, String uid, float[] embedding, Bitmap faceCrop) {
        if (context == null || uid == null || uid.trim().isEmpty() || embedding == null || contemValoresInvalidos(embedding)) return false;

        try {
            File dirBio = new File(context.getFilesDir(), "biometrics");
            if (!dirBio.exists()) dirBio.mkdirs();

            File fileBio = new File(dirBio, uid.trim() + ".dat");
            FileOutputStream fos = new FileOutputStream(fileBio);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            BiometricTemplate templateObj = new BiometricTemplate(VERSAO_EMBEDDING_ATUAL, embedding, uid.trim(), System.currentTimeMillis());
            oos.writeObject(templateObj);

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
            Log.e(TAG, "Erro ao salvar template biométrico local v6", e);
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
            Object obj = ois.readObject();
            ois.close();
            fis.close();

            if (obj instanceof BiometricTemplate) {
                BiometricTemplate tpl = (BiometricTemplate) obj;
                if (tpl.getVersao() != VERSAO_EMBEDDING_ATUAL) {
                    Log.w(TAG, "Versão de biometria incompatível em disco (" + tpl.getVersao() + " != " + VERSAO_EMBEDDING_ATUAL + "). Recadastro v6 exigido.");
                    return null;
                }
                float[] emb = tpl.getEmbedding();
                return contemValoresInvalidos(emb) ? null : emb;
            } else if (obj instanceof float[]) {
                Log.w(TAG, "Versão legada de biometria detectada em disco. Recadastro v6 exigido.");
                return null;
            }

            return null;
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
