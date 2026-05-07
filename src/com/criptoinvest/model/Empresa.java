package com.criptoinvest.model;

public class Empresa {

    private int idEmpresa;
    private String razaoSocial;
    private String cnpj;
    private Carteira carteira;

    public Empresa(int id, String razaoSocial, String cnpj) {
        this.idEmpresa = id;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.carteira = new Carteira(id, "Carteira - " + razaoSocial);
    }

    public int getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(int idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public void setCarteira(Carteira carteira) {
        this.carteira = carteira;
    }

    public void exibirDados() {
        System.out.println("=== Empresa ===");
        System.out.println("Razao Social: " + razaoSocial);
        System.out.println("CNPJ: " + cnpj);
        System.out.println("--- Carteira da Empresa ---");
        carteira.exibirResumo();
    }
}