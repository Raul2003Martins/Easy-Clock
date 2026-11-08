package com.easyclock.app.dao;

import com.easyclock.app.model.HistoricoBancoHoras;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricoBancoHorasDAO {

    private static final String COLECAO = "historicoBancoHoras";
    private static final String COLECAO_SUMARIO = "bancoHoras";
    private final FirebaseFirestore db;

    public HistoricoBancoHorasDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<List<HistoricoBancoHoras>> listarPorFuncionario(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID do funcionário inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("funcionarioId", uid.trim())
                .orderBy("dataLancamento", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<HistoricoBancoHoras> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        HistoricoBancoHoras h = doc.toObject(HistoricoBancoHoras.class);
                        if (h != null) {
                            h.setId(doc.getId());
                            lista.add(h);
                        }
                    }
                    return lista;
                });
    }

    public Task<List<HistoricoBancoHoras>> listarPorEmpresaEFuncionario(String empresaId, String funcionarioUid) {
        if (empresaId == null || funcionarioUid == null) {
            return Tasks.forException(new IllegalArgumentException("empresaId ou funcionarioUid inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId.trim())
                .whereEqualTo("funcionarioId", funcionarioUid.trim())
                .orderBy("dataLancamento", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<HistoricoBancoHoras> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        HistoricoBancoHoras h = doc.toObject(HistoricoBancoHoras.class);
                        if (h != null) {
                            h.setId(doc.getId());
                            lista.add(h);
                        }
                    }
                    return lista;
                });
    }

    public Task<Void> registrarAjusteManualRh(String funcionarioId, String empresaId, String rhUid, long minutosAjuste, String justificativa) {
        if (funcionarioId == null || empresaId == null || rhUid == null || justificativa == null || justificativa.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Dados de ajuste manual de banco de horas inválidos."));
        }

        String uid = funcionarioId.trim();
        String empId = empresaId.trim();
        String motivo = justificativa.trim();

        DocumentReference docRefHistorico = db.collection(COLECAO).document();

        HistoricoBancoHoras h = new HistoricoBancoHoras();
        h.setId(docRefHistorico.getId());
        h.setFuncionarioId(uid);
        h.setEmpresaId(empId);
        h.setExpedienteId("");
        h.setDataReferencia("");
        h.setMinutosTrabalhados(0);
        h.setCargaPrevistaMinutos(0);
        h.setMinutosSaldo(minutosAjuste);
        h.setTipo("AJUSTE_MANUAL_RH");
        h.setJustificativa(motivo);
        h.setRhUid(rhUid.trim());

        WriteBatch batch = db.batch();
        batch.set(docRefHistorico, h);

        Map<String, Object> sumarioUpdate = new HashMap<>();
        sumarioUpdate.put("funcionarioId", uid);
        sumarioUpdate.put("empresaId", empId);
        sumarioUpdate.put("saldoTotalMinutos", FieldValue.increment(minutosAjuste));
        if (minutosAjuste > 0) {
            sumarioUpdate.put("totalPositivasMinutos", FieldValue.increment(minutosAjuste));
        } else if (minutosAjuste < 0) {
            sumarioUpdate.put("totalNegativasMinutos", FieldValue.increment(Math.abs(minutosAjuste)));
        }
        sumarioUpdate.put("atualizadoEm", FieldValue.serverTimestamp());

        batch.set(db.collection(COLECAO_SUMARIO).document(uid), sumarioUpdate, com.google.firebase.firestore.SetOptions.merge());

        return batch.commit();
    }
}
