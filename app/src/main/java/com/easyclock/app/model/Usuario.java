package com.easyclock.app.model;

public class Usuario {

    private String uid;
    private String nome;
    private String email;
    private String status;

    public Usuario() {
        // Construtor vazio necessário para o Firestore
    }

    public Usuario(String uid, String nome,
                   String email, String status) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.status = status;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
