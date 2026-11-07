package com.easyclock.app.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class JornadaTrabalho {

    private String id;
    private String empresaId;
    private String nome;
    private String entrada;
    private String saidaAlmoco;
    private String retornoAlmoco;
    private String saida;
    private List<Long> diasSemana;
    private long cargaHorariaMinutos;
    private long toleranciaMinutos;
    private boolean ativo;

    public JornadaTrabalho() {
        this.diasSemana = new ArrayList<>();
        this.toleranciaMinutos = 5;
        this.ativo = true;
    }

    public JornadaTrabalho(
            String id,
            String empresaId,
            String nome,
            String entrada,
            String saidaAlmoco,
            String retornoAlmoco,
            String saida,
            List<Long> diasSemana,
            long cargaHorariaMinutos,
            long toleranciaMinutos,
            boolean ativo) {
        this.id = id;
        this.empresaId = empresaId;
        this.nome = nome;
        this.entrada = entrada;
        this.saidaAlmoco = saidaAlmoco;
        this.retornoAlmoco = retornoAlmoco;
        this.saida = saida;
        this.diasSemana = diasSemana != null ? diasSemana : new ArrayList<>();
        this.cargaHorariaMinutos = cargaHorariaMinutos;
        this.toleranciaMinutos = toleranciaMinutos;
        this.ativo = ativo;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEntrada() {
        return entrada;
    }

    public void setEntrada(String entrada) {
        this.entrada = entrada;
    }

    public String getSaidaAlmoco() {
        return saidaAlmoco;
    }

    public void setSaidaAlmoco(String saidaAlmoco) {
        this.saidaAlmoco = saidaAlmoco;
    }

    public String getRetornoAlmoco() {
        return retornoAlmoco;
    }

    public void setRetornoAlmoco(String retornoAlmoco) {
        this.retornoAlmoco = retornoAlmoco;
    }

    public String getSaida() {
        return saida;
    }

    public void setSaida(String saida) {
        this.saida = saida;
    }

    public List<Long> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(List<Long> diasSemana) {
        this.diasSemana = diasSemana;
    }

    public long getCargaHorariaMinutos() {
        return cargaHorariaMinutos;
    }

    public void setCargaHorariaMinutos(long cargaHorariaMinutos) {
        this.cargaHorariaMinutos = cargaHorariaMinutos;
    }

    public long getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(long toleranciaMinutos) {
        this.toleranciaMinutos = toleranciaMinutos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
