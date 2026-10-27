package com.easyclock.app.dao;

import com.easyclock.app.model.ConviteEmpresa;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ConviteEmpresaDAO {

    private static final String COLECAO =
            "convitesEmpresa";

    private final FirebaseFirestore db;

    public ConviteEmpresaDAO() {
        db = FirebaseFirestore.getInstance();
    }

    // Reserva o ID de um novo convite.
    public String gerarConviteId() {

        return db.collection(COLECAO)
                .document()
                .getId();
    }

    // Persiste um convite previamente
    // preparado por uma operação confiável.
    //
    // Não gera nem calcula o codigoHash.
    // A gravação deve ser autorizada
    // pelas regras de segurança.
    public Task<Void> salvarConvite(
            ConviteEmpresa convite) {

        if (convite == null ||
                convite.getConviteId() == null ||
                convite.getConviteId().trim().isEmpty() ||
                convite.getEmpresaId() == null ||
                convite.getEmpresaId().trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Dados do convite inválidos."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(convite.getConviteId())
                .set(convite);
    }

    // Consulta administrativa por ID.
    // Não utilizar na tela pública
    // de cadastro de funcionário.
    public Task<ConviteEmpresa> buscarPorId(
            String conviteId) {

        if (conviteId == null ||
                conviteId.trim().isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "conviteId inválido."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(conviteId)
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    if (!task.getResult().exists()) {
                        return null;
                    }

                    ConviteEmpresa convite =
                            task.getResult()
                                    .toObject(
                                            ConviteEmpresa.class
                                    );

                    if (convite != null) {
                        convite.setConviteId(
                                task.getResult().getId()
                        );
                    }

                    return convite;
                });
    }

    // Atualização administrativa.
    // Não deve ser utilizada por usuários
    // comuns para ativar convites ou
    // modificar limites de utilização.
    public Task<Void> atualizarCampos(
            String conviteId,
            Map<String, Object> campos) {

        if (conviteId == null ||
                conviteId.trim().isEmpty() ||
                campos == null ||
                campos.isEmpty()) {

            return Tasks.forException(
                    new IllegalArgumentException(
                            "Dados de atualização inválidos."
                    )
            );
        }

        return db.collection(COLECAO)
                .document(conviteId)
                .update(campos);
    }
}
