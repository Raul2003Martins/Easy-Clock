package com.easyclock.app.model;

import com.google.firebase.Timestamp;

public class Funcionario extends Usuario {

    private String empresaId;
    private String perfil;
    private String cargo;
    private String setorId;
    private String matricula;
    private Timestamp dataAdmissao;
    private String faceId;
    private Timestamp criadoEm;

    public Funcionario() {
        super();
    }

    public Funcionario(
            String uid,
            String nome,
            String email,
            String status,
            String empresaId,
            String perfil,
            String cargo,
            String setorId,
            String matricula,
            Timestamp dataAdmissao,
            String faceId,
            Timestamp criadoEm) {

        super(uid, nome, email, status);

        this.empresaId = empresaId;
        this.perfil = perfil;
        this.cargo = cargo;
        this.setorId = setorId;
        this.matricula = matricula;
        this.dataAdmissao = dataAdmissao;
        this.faceId = faceId;
        this.criadoEm = criadoEm;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getSetorId() {
        return setorId;
    }

    public void setSetorId(String setorId) {
        this.setorId = setorId;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Timestamp getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(Timestamp dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public String getFaceId() {
        return faceId;
    }

    public void setFaceId(String faceId) {
        this.faceId = faceId;
    }

    public Timestamp getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Timestamp criadoEm) {
        this.criadoEm = criadoEm;
    }
}
