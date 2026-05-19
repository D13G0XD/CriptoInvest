package com.criptoinvest.model;

/**
 * Pessoa Juridica que opera no sistema.
 * 1:1 obrigatorio com CarteiraPJ; N:1 com Usuario (empresa pertence a um dono).
 */
public class Empresa {

    private int id;                  // PK
    private String nome;             // razao social
    private String cnpj;
    private Usuario dono;            // FK obrigatoria (1:N do lado da empresa)
    private CarteiraPJ carteira;     // FK 1:1 obrigatoria -> carteira_pj

    public Empresa(int id, String razaoSocial, String cnpj) {
        this.id = id;
        this.nome = razaoSocial;
        this.cnpj = cnpj;
        this.carteira = new CarteiraPJ(id, "Carteira PJ - " + razaoSocial);
    }

    public Empresa(int id, String razaoSocial, String cnpj, String regimeTributario) {
        this.id = id;
        this.nome = razaoSocial;
        this.cnpj = cnpj;
        this.carteira = new CarteiraPJ(id, "Carteira PJ - " + razaoSocial, 0, regimeTributario);
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) { this.dono = dono; }
    public CarteiraPJ getCarteira() { return carteira; }

    public void exibirDados() {
        System.out.println("=== Empresa (PJ) ===");
        System.out.println("Razao Social: " + nome);
        System.out.println("CNPJ: " + cnpj);
        System.out.println("Regime Tributario: " + carteira.getRegimeTributario());
        if (dono != null) {
            System.out.println("Dono: " + dono.getNome());
        }
        System.out.println("--- Carteira da Empresa ---");
        carteira.exibirResumo();
    }
}
