package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class HistoricoBancoHoras {

    private String id;
    private String funcionarioId;
    private String empresaId;
    private String expedienteId;
    private String dataReferencia;
    private long minutosTrabalhados;
    private long cargaPrevistaMinutos;
    private long minutosSaldo;
    private String tipo; // AUTOMATICO_EXPEDIENTE, AJUSTE_MANUAL_RH
    private String justificativa;
    private String rhUid;

    @ServerTimestamp
    private Timestamp dataLancamento;

    public HistoricoBancoHoras() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getExpedienteId() {
        return expedienteId;
    }

    public void setExpedienteId(String expedienteId) {
        this.expedienteId = expedienteId;
    }

    public String getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(String dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public long getMinutosTrabalhados() {
        return minutosTrabalhados;
    }

    public void setMinutosTrabalhados(long minutosTrabalhados) {
        this.minutosTrabalhados = minutosTrabalhados;
    }

    public long getCargaPrevistaMinutos() {
        return cargaPrevistaMinutos;
    }

    public void setCargaPrevistaMinutos(long cargaPrevistaMinutos) {
        this.cargaPrevistaMinutos = cargaPrevistaMinutos;
    }

    public long getMinutosSaldo() {
        return minutosSaldo;
    }

    public void setMinutosSaldo(long minutosSaldo) {
        this.minutosSaldo = minutosSaldo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getRhUid() {
        return rhUid;
    }

    public void setRhUid(String rhUid) {
        this.rhUid = rhUid;
    }

    public Timestamp getDataLancamento() {
        return dataLancamento;
    }

    public void setDataLancamento(Timestamp dataLancamento) {
        this.dataLancamento = dataLancamento;
    }
}
