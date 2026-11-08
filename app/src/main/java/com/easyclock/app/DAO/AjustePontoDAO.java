package com.easyclock.app.dao;

import com.easyclock.app.model.AjustePonto;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AjustePontoDAO {

    private static final String COLECAO = "solicitacoesAjuste";
    private final FirebaseFirestore db;

    public AjustePontoDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<String> criarSolicitacao(AjustePonto ajuste) {
        if (ajuste == null ||
                ajuste.getFuncionarioId() == null || ajuste.getFuncionarioId().trim().isEmpty() ||
                ajuste.getEmpresaId() == null || ajuste.getEmpresaId().trim().isEmpty() ||
                ajuste.getDataReferencia() == null || ajuste.getDataReferencia().trim().isEmpty() ||
                ajuste.getTipoPonto() == null || ajuste.getTipoPonto().trim().isEmpty() ||
                ajuste.getHorarioAjustado() == null || ajuste.getHorarioAjustado().trim().isEmpty() ||
                ajuste.getMotivo() == null || ajuste.getMotivo().trim().isEmpty()) {

            return Tasks.forException(new IllegalArgumentException("Dados de solicitação de ajuste inválidos."));
        }

        DocumentReference docRef = db.collection(COLECAO).document();
        ajuste.setId(docRef.getId());
        ajuste.setStatus("PENDENTE");

        return docRef.set(ajuste).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return docRef.getId();
        });
    }

    public Task<Boolean> verificarExisteSolicitacaoPendente(String funcionarioId, String dataReferencia, String tipoPonto) {
        if (funcionarioId == null || dataReferencia == null || tipoPonto == null) {
            return Tasks.forException(new IllegalArgumentException("Parâmetros de verificação inválidos."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("funcionarioId", funcionarioId.trim())
                .whereEqualTo("dataReferencia", dataReferencia.trim())
                .whereEqualTo("tipoPonto", tipoPonto.trim())
                .whereEqualTo("status", "PENDENTE")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return !task.getResult().isEmpty();
                });
    }

    public Task<List<AjustePonto>> listarPorFuncionario(String funcionarioId) {
        if (funcionarioId == null || funcionarioId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("funcionarioId inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("funcionarioId", funcionarioId.trim())
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<AjustePonto> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        AjustePonto a = doc.toObject(AjustePonto.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            lista.add(a);
                        }
                    }
                    return lista;
                });
    }

    public Task<List<AjustePonto>> listarPorEmpresa(String empresaId) {
        if (empresaId == null || empresaId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("empresaId inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId.trim())
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<AjustePonto> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        AjustePonto a = doc.toObject(AjustePonto.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            lista.add(a);
                        }
                    }
                    return lista;
                });
    }

    public Task<AjustePonto> buscarPorId(String solicitacaoId) {
        if (solicitacaoId == null || solicitacaoId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("solicitacaoId inválido."));
        }

        return db.collection(COLECAO).document(solicitacaoId.trim()).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) return null;
                    AjustePonto a = doc.toObject(AjustePonto.class);
                    if (a != null) a.setId(doc.getId());
                    return a;
                });
    }

    public Task<Void> responderSolicitacao(String solicitacaoId, String rhUid, String novoStatus, String respostaRh) {
        if (solicitacaoId == null || rhUid == null || novoStatus == null) {
            return Tasks.forException(new IllegalArgumentException("Parâmetros inválidos."));
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", novoStatus.trim());
        updates.put("rhUid", rhUid.trim());
        updates.put("respostaRh", respostaRh != null ? respostaRh.trim() : "");
        updates.put("dataResposta", FieldValue.serverTimestamp());

        return db.collection(COLECAO).document(solicitacaoId.trim()).update(updates);
    }
}
