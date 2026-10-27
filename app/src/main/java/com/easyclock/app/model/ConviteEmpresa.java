package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class ConviteEmpresa {

    @Exclude
    private String conviteId;

    private String empresaId;
    private String codigoHash;
    private boolean ativo;
    private Timestamp criadoEm;
    private Timestamp expiraEm;
    private Long limiteUsos;
    private long quantidadeUsos;

    public ConviteEmpresa() {
        // Construtor vazio necessário para o Firestore
    }

    public ConviteEmpresa(
            String conviteId,
            String empresaId,
            String codigoHash,
            boolean ativo,
            Timestamp criadoEm,
            Timestamp expiraEm,
            Long limiteUsos,
            long quantidadeUsos) {

        this.conviteId = conviteId;
        this.empresaId = empresaId;
        this.codigoHash = codigoHash;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
        this.limiteUsos = limiteUsos;
        this.quantidadeUsos = quantidadeUsos;
    }

    @Exclude
    public String getConviteId() {
        return conviteId;
    }

    public void setConviteId(String conviteId) {
        this.conviteId = conviteId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    public void setCodigoHash(String codigoHash) {
        this.codigoHash = codigoHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Timestamp getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Timestamp criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Timestamp getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(Timestamp expiraEm) {
        this.expiraEm = expiraEm;
    }

    public Long getLimiteUsos() {
        return limiteUsos;
    }

    public void setLimiteUsos(Long limiteUsos) {
        this.limiteUsos = limiteUsos;
    }

    public long getQuantidadeUsos() {
        return quantidadeUsos;
    }

    public void setQuantidadeUsos(long quantidadeUsos) {
        this.quantidadeUsos = quantidadeUsos;
    }
}
