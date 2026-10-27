package com.easyclock.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Empresa {

    @Exclude
    private String empresaId;

    private String nome;
    private String cnpj;
    private String endereco;
    private double latitude;
    private double longitude;
    private double raioPermitido;
    private String responsavelId;
    private String status;
    private Timestamp criadoEm;

    public Empresa() {
        // Construtor vazio necessário para o Firestore
    }

    public Empresa(
            String empresaId,
            String nome,
            String cnpj,
            String endereco,
            double latitude,
            double longitude,
            double raioPermitido,
            String responsavelId,
            String status,
            Timestamp criadoEm) {

        this.empresaId = empresaId;
        this.nome = nome;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.raioPermitido = raioPermitido;
        this.responsavelId = responsavelId;
        this.status = status;
        this.criadoEm = criadoEm;
    }

    @Exclude
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRaioPermitido() {
        return raioPermitido;
    }

    public void setRaioPermitido(double raioPermitido) {
        this.raioPermitido = raioPermitido;
    }

    public String getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(String responsavelId) {
        this.responsavelId = responsavelId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Timestamp criadoEm) {
        this.criadoEm = criadoEm;
    }
}
