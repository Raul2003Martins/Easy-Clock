package com.easyclock.app.model;

public class LocalizacaoRegistro {
    private double latitude;
    private double longitude;
    private float precisaoMetros;
    private float distanciaEmpresaMetros;

    public LocalizacaoRegistro() {
    }

    public LocalizacaoRegistro(double latitude, double longitude, float precisaoMetros, float distanciaEmpresaMetros) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.precisaoMetros = precisaoMetros;
        this.distanciaEmpresaMetros = distanciaEmpresaMetros;
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

    public float getPrecisaoMetros() {
        return precisaoMetros;
    }

    public void setPrecisaoMetros(float precisaoMetros) {
        this.precisaoMetros = precisaoMetros;
    }

    public float getDistanciaEmpresaMetros() {
        return distanciaEmpresaMetros;
    }

    public void setDistanciaEmpresaMetros(float distanciaEmpresaMetros) {
        this.distanciaEmpresaMetros = distanciaEmpresaMetros;
    }
}
