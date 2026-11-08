package com.easyclock.app.dao;

import com.easyclock.app.model.HistoricoBancoHoras;
import com.easyclock.app.model.Ponto;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PontoDAO {

    private static final String COLECAO = "ponto";
    private final FirebaseFirestore db;

    public PontoDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> registrarPontoComExpediente(Ponto ponto, Map<String, Object> jornadaSnapshot, String proximaMarcacao) {
        return registrarPontoComExpedienteEBancoHoras(ponto, jornadaSnapshot, proximaMarcacao, 0, 0, 0);
    }

    public Task<Void> registrarPontoComExpedienteEBancoHoras(Ponto ponto, Map<String, Object> jornadaSnapshot, String proximaMarcacao,
                                                            long minutosTrabalhados, long cargaPrevistaMinutos, long minutosSaldo) {
        if (ponto == null ||
                ponto.getFuncionarioId() == null || ponto.getFuncionarioId().trim().isEmpty() ||
                ponto.getDataReferencia() == null || ponto.getDataReferencia().trim().isEmpty() ||
                ponto.getTipoPonto() == null || ponto.getTipoPonto().trim().isEmpty()) {

            return Tasks.forException(new IllegalArgumentException("Dados de ponto inválidos."));
        }

        String uid = ponto.getFuncionarioId().trim();
        String dataRef = ponto.getDataReferencia().trim();
        String tipoPonto = ponto.getTipoPonto().trim();

        String expedienteDocId = uid + "_" + dataRef;
        String pontoDocId = uid + "_" + dataRef + "_" + tipoPonto;

        ponto.setId(pontoDocId);

        WriteBatch batch = db.batch();

        Map<String, Object> controleData = new HashMap<>();
        controleData.put("funcionarioId", uid);
        controleData.put("empresaId", ponto.getEmpresaId());
        controleData.put("expedienteId", expedienteDocId);
        controleData.put("dataReferencia", dataRef);

        if ("ENTRADA".equalsIgnoreCase(tipoPonto)) {
            Map<String, Object> expedienteData = new HashMap<>();
            expedienteData.put("empresaId", ponto.getEmpresaId());
            expedienteData.put("funcionarioId", uid);
            expedienteData.put("jornadaId", ponto.getJornadaId());
            expedienteData.put("dataReferencia", dataRef);
            expedienteData.put("jornadaSnapshot", jornadaSnapshot);
            expedienteData.put("proximaMarcacao", "SAIDA_ALMOCO");
            expedienteData.put("concluido", false);
            expedienteData.put("criadoEm", FieldValue.serverTimestamp());
            expedienteData.put("atualizadoEm", FieldValue.serverTimestamp());

            batch.set(db.collection("expedientes").document(expedienteDocId), expedienteData);

            controleData.put("estado", "ABERTO");
            controleData.put("atualizadoEm", FieldValue.serverTimestamp());
            batch.set(db.collection("controleExpedienteAtivo").document(uid), controleData);
        } else {
            Map<String, Object> updateExp = new HashMap<>();
            updateExp.put("proximaMarcacao", proximaMarcacao);
            if ("SAIDA".equalsIgnoreCase(tipoPonto)) {
                updateExp.put("concluido", true);
                controleData.put("estado", "CONCLUIDO");
                controleData.put("atualizadoEm", FieldValue.serverTimestamp());
                batch.set(db.collection("controleExpedienteAtivo").document(uid), controleData);

                // Lançamento no Banco de Horas
                DocumentReference docHist = db.collection("historicoBancoHoras").document(expedienteDocId + "_BANCO");
                HistoricoBancoHoras h = new HistoricoBancoHoras();
                h.setId(docHist.getId());
                h.setFuncionarioId(uid);
                h.setEmpresaId(ponto.getEmpresaId());
                h.setExpedienteId(expedienteDocId);
                h.setDataReferencia(dataRef);
                h.setMinutosTrabalhados(minutosTrabalhados);
                h.setCargaPrevistaMinutos(cargaPrevistaMinutos);
                h.setMinutosSaldo(minutosSaldo);
                h.setTipo("AUTOMATICO_EXPEDIENTE");
                h.setJustificativa("Fechamento automático do expediente " + dataRef);

                batch.set(docHist, h);

                Map<String, Object> sumarioUpdate = new HashMap<>();
                sumarioUpdate.put("funcionarioId", uid);
                sumarioUpdate.put("empresaId", ponto.getEmpresaId());
                sumarioUpdate.put("expedienteId", expedienteDocId);
                sumarioUpdate.put("dataReferencia", dataRef);
                sumarioUpdate.put("saldoTotalMinutos", FieldValue.increment(minutosSaldo));
                if (minutosSaldo > 0) {
                    sumarioUpdate.put("totalPositivasMinutos", FieldValue.increment(minutosSaldo));
                } else if (minutosSaldo < 0) {
                    sumarioUpdate.put("totalNegativasMinutos", FieldValue.increment(Math.abs(minutosSaldo)));
                }
                sumarioUpdate.put("atualizadoEm", FieldValue.serverTimestamp());

                batch.set(db.collection("bancoHoras").document(uid), sumarioUpdate, SetOptions.merge());
            } else {
                updateExp.put("concluido", false);
                controleData.put("estado", "ABERTO");
                controleData.put("atualizadoEm", FieldValue.serverTimestamp());
                batch.set(db.collection("controleExpedienteAtivo").document(uid), controleData);
            }
            updateExp.put("atualizadoEm", FieldValue.serverTimestamp());

            batch.update(db.collection("expedientes").document(expedienteDocId), updateExp);
        }

        batch.set(db.collection(COLECAO).document(pontoDocId), ponto);

        return batch.commit();
    }

    public Task<DocumentSnapshot> buscarControleExpedienteAtivo(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID inválido."));
        }
        return db.collection("controleExpedienteAtivo").document(uid.trim()).get();
    }

    public Task<DocumentSnapshot> buscarExpedientePorId(String expedienteId) {
        if (expedienteId == null || expedienteId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("expedienteId inválido."));
        }
        return db.collection("expedientes").document(expedienteId.trim()).get();
    }

    public Task<DocumentSnapshot> buscarExpedienteDoDia(String uid, String dataReferencia) {
        if (uid == null || dataReferencia == null) {
            return Tasks.forException(new IllegalArgumentException("UID ou dataReferencia inválidos."));
        }

        String expedienteDocId = uid.trim() + "_" + dataReferencia.trim();
        return db.collection("expedientes").document(expedienteDocId).get();
    }

    public Task<List<Ponto>> listarPontosDoDia(String uid, String dataReferencia) {
        if (uid == null || dataReferencia == null) {
            return Tasks.forException(new IllegalArgumentException("UID ou dataReferencia inválidos."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("funcionarioId", uid.trim())
                .whereEqualTo("dataReferencia", dataReferencia.trim())
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<Ponto> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Ponto p = doc.toObject(Ponto.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            lista.add(p);
                        }
                    }
                    return lista;
                });
    }

    public Task<List<Ponto>> listarPontosPorFuncionario(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("UID do funcionário inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("funcionarioId", uid.trim())
                .orderBy("dataHora", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<Ponto> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Ponto p = doc.toObject(Ponto.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            lista.add(p);
                        }
                    }
                    return lista;
                });
    }

    public Task<List<Ponto>> listarPontosPorEmpresaEFuncionario(String empresaId, String funcionarioUid) {
        if (empresaId == null || funcionarioUid == null) {
            return Tasks.forException(new IllegalArgumentException("empresaId ou funcionarioUid inválido."));
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId.trim())
                .whereEqualTo("funcionarioId", funcionarioUid.trim())
                .orderBy("dataHora", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<Ponto> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Ponto p = doc.toObject(Ponto.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            lista.add(p);
                        }
                    }
                    return lista;
                });
    }
}
