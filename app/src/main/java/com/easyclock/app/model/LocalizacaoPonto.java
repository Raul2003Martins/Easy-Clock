package com.easyclock.app.model;

import com.google.firebase.Timestamp;

public class LocalizacaoPonto {
    private double latitude;
    private double longitude;
    private double raioMetros = 100.0;
    private String rhUid;
    private Timestamp atualizadoEm;

    public LocalizacaoPonto() {
    }

    public LocalizacaoPonto(double latitude, double longitude, double raioMetros, String rhUid, Timestamp atualizadoEm) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.raioMetros = raioMetros;
        this.rhUid = rhUid;
        this.atualizadoEm = atualizadoEm;
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

    public double getRaioMetros() {
        return raioMetros;
    }

    public void setRaioMetros(double raioMetros) {
        this.raioMetros = raioMetros;
    }

    public String getRhUid() {
        return rhUid;
    }

    public void setRhUid(String rhUid) {
        this.rhUid = rhUid;
    }

    public Timestamp getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setCriadoEm(Timestamp atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public void setAtualizadoEm(Timestamp atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
