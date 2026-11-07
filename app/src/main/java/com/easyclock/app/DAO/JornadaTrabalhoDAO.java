package com.easyclock.app.dao;

import com.easyclock.app.model.JornadaTrabalho;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class JornadaTrabalhoDAO {

    private static final String COLECAO = "jornadasTrabalho";
    private final FirebaseFirestore db;

    public JornadaTrabalhoDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<String> salvarJornada(JornadaTrabalho jornada) {
        if (jornada == null || jornada.getEmpresaId() == null || jornada.getEmpresaId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Jornada ou empresaId inválido."));
        }

        if (jornada.getId() != null && !jornada.getId().trim().isEmpty()) {
            String docId = jornada.getId().trim();
            return db.collection(COLECAO)
                    .document(docId)
                    .set(jornada)
                    .continueWith(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return docId;
                    });
        } else {
            return db.collection(COLECAO)
                    .add(jornada)
                    .continueWith(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return task.getResult().getId();
                    });
        }
    }

    public Task<JornadaTrabalho> buscarPorId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID de jornada inválido."));
        }

        return db.collection(COLECAO)
                .document(id)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    if (!task.getResult().exists()) return null;

                    JornadaTrabalho jornada = task.getResult().toObject(JornadaTrabalho.class);
                    if (jornada != null) {
                        jornada.setId(task.getResult().getId());
                    }
                    return jornada;
                });
    }

    public Task<List<JornadaTrabalho>> listarPorEmpresa(String empresaId) {
        if (empresaId == null || empresaId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("empresaId inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<JornadaTrabalho> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        JornadaTrabalho jornada = doc.toObject(JornadaTrabalho.class);
                        if (jornada != null) {
                            jornada.setId(doc.getId());
                            lista.add(jornada);
                        }
                    }
                    return lista;
                });
    }

    public Task<Void> alternarStatusJornada(String jornadaId, boolean ativo) {
        if (jornadaId == null || jornadaId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("jornadaId inválido."));
        }

        return db.collection(COLECAO)
                .document(jornadaId)
                .update("ativo", ativo);
    }
}
