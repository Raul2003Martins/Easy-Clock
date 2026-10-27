package com.easyclock.app.dao;

import com.easyclock.app.model.Funcionario;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Map;

public class FuncionarioDAO {

    private static final String COLECAO = "funcionarios";

    private final FirebaseFirestore db;

    public FuncionarioDAO() {
        db = FirebaseFirestore.getInstance();
    }

    // Salva um funcionário usando o UID
    // do Firebase Authentication.
    //
    // A definição de perfil, empresaId e status
    // deve ser autorizada pelo backend.
    public Task<Void> salvarFuncionario(
            Funcionario funcionario) {

        if (funcionario == null ||
                funcionario.getUid() == null ||
                funcionario.getUid().trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Funcionário ou UID inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(funcionario.getUid())
                .set(funcionario);
    }

    // Busca um funcionário pelo UID.
    public Task<Funcionario> buscarPorUid(
            String uid) {

        if (uid == null || uid.trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "UID inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(uid)
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    if (!task.getResult().exists()) {
                        return null;
                    }

                    Funcionario funcionario =
                            task.getResult()
                                    .toObject(Funcionario.class);

                    if (funcionario != null) {
                        funcionario.setUid(
                                task.getResult().getId()
                        );
                    }

                    return funcionario;
                });
    }

    // Consulta funcionários de uma empresa.
    // O Firestore deve impedir consultas
    // a empresas não autorizadas.
    public Task<List<Funcionario>>
    listarPorEmpresa(String empresaId) {

        if (empresaId == null ||
                empresaId.trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "empresaId inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId)
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return task.getResult()
                            .toObjects(Funcionario.class);
                });
    }

    // Consulta funcionários pendentes
    // de aprovação pelo RH.
    public Task<List<Funcionario>>
    listarPendentes(String empresaId) {

        if (empresaId == null ||
                empresaId.trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "empresaId inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .whereEqualTo("empresaId", empresaId)
                .whereEqualTo(
                        "status",
                        "PENDENTE_APROVACAO"
                )
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return task.getResult()
                            .toObjects(Funcionario.class);
                });
    }

    // Atualiza campos específicos.
    // Campos administrativos devem ser
    // protegidos pelas regras do Firestore.
    public Task<Void> atualizarCampos(
            String uid,
            Map<String, Object> campos) {

        if (uid == null ||
                uid.trim().isEmpty() ||
                campos == null ||
                campos.isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Dados de atualização inválidos."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(uid)
                .update(campos);
    }

    // Monta uma consulta por empresa.
    // A execução continua sujeita às regras
    // de segurança do Firestore.
    public Query consultaPorEmpresa(
            String empresaId) {

        return db.collection(COLECAO)
                .whereEqualTo(
                        "empresaId",
                        empresaId
                );
    }
}
