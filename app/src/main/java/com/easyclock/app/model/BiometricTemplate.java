package com.easyclock.app.model;

import java.io.Serializable;

public class BiometricTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int VERSAO_ATUAL = 7;

    private int versao = VERSAO_ATUAL;
    private float[] embedding;
    private String uid;
    private long criadoEm;

    public BiometricTemplate() {
    }

    public BiometricTemplate(int versao, float[] embedding, String uid, long criadoEm) {
        this.versao = versao;
        this.embedding = embedding;
        this.uid = uid;
        this.criadoEm = criadoEm;
    }

    public int getVersao() {
        return versao;
    }

    public void setVersao(int versao) {
        this.versao = versao;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(long criadoEm) {
        this.criadoEm = criadoEm;
    }
}
