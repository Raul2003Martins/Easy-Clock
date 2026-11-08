package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class AjustePonto {

    private String id;
    private String funcionarioId;
    private String empresaId;
    private String nomeFuncionario;
    private String dataReferencia; // YYYY-MM-DD
    private String dataExibicao;   // dd/MM/yyyy
    private String tipoPonto;      // ENTRADA, SAIDA_ALMOCO, RETORNO_ALMOCO, SAIDA
    private String horarioAjustado; // HH:mm
    private String motivo;
    private String status;         // PENDENTE, APROVADO, RECUSADO

    @ServerTimestamp
    private Timestamp dataSolicitacao;
    private Timestamp dataResposta;
    private String rhUid;
    private String respostaRh;

    public AjustePonto() {}

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

    public String getNomeFuncionario() {
        return nomeFuncionario;
    }

    public void setNomeFuncionario(String nomeFuncionario) {
        this.nomeFuncionario = nomeFuncionario;
    }

    public String getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(String dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public String getDataExibicao() {
        return dataExibicao;
    }

    public void setDataExibicao(String dataExibicao) {
        this.dataExibicao = dataExibicao;
    }

    public String getTipoPonto() {
        return tipoPonto;
    }

    public void setTipoPonto(String tipoPonto) {
        this.tipoPonto = tipoPonto;
    }

    public String getHorarioAjustado() {
        return horarioAjustado;
    }

    public void setHorarioAjustado(String horarioAjustado) {
        this.horarioAjustado = horarioAjustado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(Timestamp dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public Timestamp getDataResposta() {
        return dataResposta;
    }

    public void setDataResposta(Timestamp dataResposta) {
        this.dataResposta = dataResposta;
    }

    public String getRhUid() {
        return rhUid;
    }

    public void setRhUid(String rhUid) {
        this.rhUid = rhUid;
    }

    public String getRespostaRh() {
        return respostaRh;
    }

    public void setRespostaRh(String respostaRh) {
        this.respostaRh = respostaRh;
    }
}
