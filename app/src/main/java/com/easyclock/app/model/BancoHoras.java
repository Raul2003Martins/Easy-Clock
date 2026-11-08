package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class BancoHoras {

    private String funcionarioId;
    private String empresaId;
    private long saldoTotalMinutos;
    private long totalPositivasMinutos;
    private long totalNegativasMinutos;

    @ServerTimestamp
    private Timestamp atualizadoEm;

    public BancoHoras() {}

    public String getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(String funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public long getSaldoTotalMinutos() {
        return saldoTotalMinutos;
    }

    public void setSaldoTotalMinutos(long saldoTotalMinutos) {
        this.saldoTotalMinutos = saldoTotalMinutos;
    }

    public long getTotalPositivasMinutos() {
        return totalPositivasMinutos;
    }

    public void setTotalPositivasMinutos(long totalPositivasMinutos) {
        this.totalPositivasMinutos = totalPositivasMinutos;
    }

    public long getTotalNegativasMinutos() {
        return totalNegativasMinutos;
    }

    public void setTotalNegativasMinutos(long totalNegativasMinutos) {
        this.totalNegativasMinutos = totalNegativasMinutos;
    }

    public Timestamp getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Timestamp atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
