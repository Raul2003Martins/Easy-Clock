package com.easyclock.app.dao;

import com.easyclock.app.model.Empresa;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class EmpresaDAO {

    private static final String COLECAO = "empresa";

    private final FirebaseFirestore db;

    public EmpresaDAO() {
        db = FirebaseFirestore.getInstance();
    }

    // Reserva um ID para uma nova empresa.
    // Não grava o documento automaticamente.
    public String gerarEmpresaId() {
        return db.collection(COLECAO)
                .document()
                .getId();
    }

    // Salva os dados de uma empresa.
    // O backend deve autorizar esta operação.
    public Task<Void> salvarEmpresa(Empresa empresa) {

        if (empresa == null ||
                empresa.getEmpresaId() == null ||
                empresa.getEmpresaId().trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Empresa ou empresaId inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(empresa.getEmpresaId())
                .set(empresa);
    }

    // Busca uma empresa pelo ID do documento.
    public Task<Empresa> buscarEmpresaPorId(
            String empresaId) {

        if (empresaId == null ||
                empresaId.trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "empresaId inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(empresaId)
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    if (!task.getResult().exists()) {
                        return null;
                    }

                    Empresa empresa = task.getResult()
                            .toObject(Empresa.class);

                    if (empresa != null) {
                        empresa.setEmpresaId(
                                task.getResult().getId()
                        );
                    }

                    return empresa;
                });
    }

    // Atualiza somente os campos informados.
    // Não utilizar para alterar responsavelId
    // ou outros campos protegidos sem autorização.
    public Task<Void> atualizarCampos(
            String empresaId,
            Map<String, Object> campos) {

        if (empresaId == null ||
                empresaId.trim().isEmpty() ||
                campos == null ||
                campos.isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Dados de atualização inválidos."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(empresaId)
                .update(campos);
    }

    // Retorna a referência de um documento.
    public DocumentReference obterReferencia(
            String empresaId) {

        return db.collection(COLECAO)
                .document(empresaId);
    }
}
