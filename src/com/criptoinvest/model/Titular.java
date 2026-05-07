package com.criptoinvest.model;

public abstract class Titular {

    private int id;
    private String nome;
    private Carteira carteira;

    public Titular(int id, String nome, Carteira carteira) {
        this.id = id;
        this.nome = nome;
        this.carteira = carteira;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Carteira getCarteira() { return carteira; }

    public abstract void exibirDados();
}