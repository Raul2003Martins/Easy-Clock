package com.easyclock.app.dao;

import com.easyclock.app.model.BancoHoras;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BancoHorasDAO {

    private static final String COLECAO = "bancoHoras";
    private final FirebaseFirestore db;

    public BancoHorasDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<BancoHoras> buscarPorFuncionario(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID do funcionário inválido."));
        }

        return db.collection(COLECAO).document(uid.trim()).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) {
                        BancoHoras vazio = new BancoHoras();
                        vazio.setFuncionarioId(uid.trim());
                        vazio.setSaldoTotalMinutos(0);
                        vazio.setTotalPositivasMinutos(0);
                        vazio.setTotalNegativasMinutos(0);
                        return vazio;
                    }
                    BancoHoras bh = doc.toObject(BancoHoras.class);
                    if (bh != null) {
                        bh.setFuncionarioId(doc.getId());
                    }
                    return bh;
                });
    }

    public Task<List<BancoHoras>> listarPorEmpresa(String empresaId) {
        if (empresaId == null || empresaId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("empresaId inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId.trim())
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<BancoHoras> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        BancoHoras bh = doc.toObject(BancoHoras.class);
                        if (bh != null) {
                            bh.setFuncionarioId(doc.getId());
                            lista.add(bh);
                        }
                    }
                    return lista;
                });
    }
}
