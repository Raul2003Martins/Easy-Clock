package com.easyclock.app.dao;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class SolicitacaoRecadastroDAO {

    private static final String COLECAO = "solicitacoesRecadastro";
    private final FirebaseFirestore db;

    public SolicitacaoRecadastroDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> criarSolicitacao(String uid, String empresaId, String nomeFuncionario, String emailFuncionario) {
        if (uid == null || uid.trim().isEmpty() || empresaId == null || empresaId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Dados de solicitação inválidos."));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("solicitacaoId", uid.trim());
        data.put("funcionarioId", uid.trim());
        data.put("empresaId", empresaId.trim());
        data.put("nomeFuncionario", nomeFuncionario != null ? nomeFuncionario : "");
        data.put("emailFuncionario", emailFuncionario != null ? emailFuncionario : "");
        data.put("tipo", "RECADASTRO_FACIAL_V2");
        data.put("motivo", "Atualização da biometria facial para a versão v2");
        data.put("status", "PENDENTE");
        data.put("criadoEm", FieldValue.serverTimestamp());
        data.put("rhUid", "");
        data.put("dataResposta", null);

        WriteBatch batch = db.batch();
        batch.set(db.collection(COLECAO).document(uid.trim()), data);

        Map<String, Object> funcUpdate = new HashMap<>();
        funcUpdate.put("solicitacaoRecadastroPendente", true);
        batch.update(db.collection("funcionario").document(uid.trim()), funcUpdate);

        return batch.commit();
    }

    public Task<DocumentSnapshot> buscarPorUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID inválido."));
        }
        return db.collection(COLECAO).document(uid.trim()).get();
    }

    public Task<Void> aprovarSolicitacao(String uid, String rhUid) {
        if (uid == null || uid.trim().isEmpty() || rhUid == null || rhUid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID ou rhUid inválido."));
        }

        WriteBatch batch = db.batch();

        Map<String, Object> solUpdate = new HashMap<>();
        solUpdate.put("status", "APROVADO");
        solUpdate.put("rhUid", rhUid.trim());
        solUpdate.put("dataResposta", FieldValue.serverTimestamp());
        batch.update(db.collection(COLECAO).document(uid.trim()), solUpdate);

        Map<String, Object> funcUpdate = new HashMap<>();
        funcUpdate.put("solicitacaoRecadastroAutorizada", true);
        funcUpdate.put("solicitacaoRecadastroPendente", false);
        batch.update(db.collection("funcionario").document(uid.trim()), funcUpdate);

        return batch.commit();
    }

    public Task<Void> recusarSolicitacao(String uid, String rhUid) {
        if (uid == null || uid.trim().isEmpty() || rhUid == null || rhUid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID ou rhUid inválido."));
        }

        WriteBatch batch = db.batch();

        Map<String, Object> solUpdate = new HashMap<>();
        solUpdate.put("status", "RECUSADO");
        solUpdate.put("rhUid", rhUid.trim());
        solUpdate.put("dataResposta", FieldValue.serverTimestamp());
        batch.update(db.collection(COLECAO).document(uid.trim()), solUpdate);

        Map<String, Object> funcUpdate = new HashMap<>();
        funcUpdate.put("solicitacaoRecadastroAutorizada", false);
        funcUpdate.put("solicitacaoRecadastroPendente", false);
        batch.update(db.collection("funcionario").document(uid.trim()), funcUpdate);

        return batch.commit();
    }
}
