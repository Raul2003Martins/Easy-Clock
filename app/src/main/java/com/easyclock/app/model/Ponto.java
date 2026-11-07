package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.HashMap;
import java.util.Map;

public class Ponto {

    private String id;
    private String empresaId;
    private String funcionarioId;
    private String jornadaId;
    private String dataReferencia;
    private String tipoPonto;
    @ServerTimestamp
    private Timestamp dataHora;
    private String status;
    private Map<String, Object> jornadaSnapshot;
    private String validacaoFacial;
    private Double latitude;
    private Double longitude;
    private String metodoAutenticacao;

    public Ponto() {
        this.status = "VALIDO";
        this.validacaoFacial = "NAO_IMPLEMENTADA";
        this.metodoAutenticacao = "MANUAL_SPARK";
        this.jornadaSnapshot = new HashMap<>();
    }

    public Ponto(
            String id,
            String empresaId,
            String funcionarioId,
            String jornadaId,
            String dataReferencia,
            String tipoPonto,
            Timestamp dataHora,
            String status,
            Map<String, Object> jornadaSnapshot,
            String validacaoFacial,
            Double latitude,
            Double longitude,
            String metodoAutenticacao) {
        this.id = id;
        this.empresaId = empresaId;
        this.funcionarioId = funcionarioId;
        this.jornadaId = jornadaId;
        this.dataReferencia = dataReferencia;
        this.tipoPonto = tipoPonto;
        this.dataHora = dataHora;
        this.status = status != null ? status : "VALIDO";
        this.jornadaSnapshot = jornadaSnapshot != null ? jornadaSnapshot : new HashMap<>();
        this.validacaoFacial = validacaoFacial != null ? validacaoFacial : "NAO_IMPLEMENTADA";
        this.latitude = latitude;
        this.longitude = longitude;
        this.metodoAutenticacao = metodoAutenticacao != null ? metodoAutenticacao : "MANUAL_SPARK";
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(String funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public String getJornadaId() {
        return jornadaId;
    }

    public void setJornadaId(String jornadaId) {
        this.jornadaId = jornadaId;
    }

    public String getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(String dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public String getTipoPonto() {
        return tipoPonto;
    }

    public void setTipoPonto(String tipoPonto) {
        this.tipoPonto = tipoPonto;
    }

    public Timestamp getDataHora() {
        return dataHora;
    }

    public void setDataHora(Timestamp dataHora) {
        this.dataHora = dataHora;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getJornadaSnapshot() {
        return jornadaSnapshot;
    }

    public void setJornadaSnapshot(Map<String, Object> jornadaSnapshot) {
        this.jornadaSnapshot = jornadaSnapshot;
    }

    public String getValidacaoFacial() {
        return validacaoFacial;
    }

    public void setValidacaoFacial(String validacaoFacial) {
        this.validacaoFacial = validacaoFacial;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getMetodoAutenticacao() {
        return metodoAutenticacao;
    }

    public void setMetodoAutenticacao(String metodoAutenticacao) {
        this.metodoAutenticacao = metodoAutenticacao;
    }
}
