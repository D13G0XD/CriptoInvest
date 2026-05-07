package com.criptoinvest.model;

public class Empresa extends Titular {

    private String cnpj;
    private Usuario usuario;

    public Empresa(int id, String razaoSocial, String cnpj) {
        super(id, razaoSocial, new Carteira(id, "Carteira - " + razaoSocial));
        this.cnpj = cnpj;
    }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    @Override
    public void exibirDados() {
        System.out.println("=== Empresa ===");
        System.out.println("Razao Social: " + getNome());
        System.out.println("CNPJ: " + cnpj);
        if (usuario != null) {
            System.out.println("Proprietario: " + usuario.getNome());
        }
        System.out.println("--- Carteira da Empresa ---");
        getCarteira().exibirResumo();
    }
}
