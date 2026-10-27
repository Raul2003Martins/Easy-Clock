package com.easyclock.app.model;

import com.google.firebase.Timestamp;

public class RH extends Funcionario {

    public RH() {
        super();
    }

    public RH(
            String uid,
            String nome,
            String email,
            String status,
            String empresaId,
            String cargo,
            String setorId,
            String matricula,
            Timestamp dataAdmissao,
            String faceId,
            Timestamp criadoEm) {

        super(
                uid,
                nome,
                email,
                status,
                empresaId,
                "RH",
                cargo,
                setorId,
                matricula,
                dataAdmissao,
                faceId,
                criadoEm
        );
    }
}
