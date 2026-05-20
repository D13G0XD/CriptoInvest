package com.criptoinvest.model;

/**
 * Pessoa Juridica que opera no sistema.
 * 1:1 obrigatorio com CarteiraPJ; N:1 obrigatorio com Usuario (empresa pertence a um dono).
 */
public class Empresa {

    private int id;                  // PK
    private String nome;             // razao social
    private String cnpj;
    private Usuario dono;            // FK obrigatoria
    private CarteiraPJ carteira;     // FK 1:1 obrigatoria -> carteira_pj

    public Empresa(int id, String razaoSocial, String cnpj, Usuario dono) {
        this(id, razaoSocial, cnpj, "SIMPLES", dono);
    }

    public Empresa(int id, String razaoSocial, String cnpj, String regimeTributario, Usuario dono) {
        if (dono == null) {
            throw new IllegalArgumentException("Empresa exige um Usuario dono.");
        }
        this.id = id;
        this.nome = razaoSocial;
        this.cnpj = cnpj;
        this.dono = dono;
        this.carteira = new CarteiraPJ(id, "Carteira PJ - " + razaoSocial, 0, regimeTributario);
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) {
        if (dono == null) {
            System.out.println("Erro: dono nao pode ser nulo.");
            return;
        }
        this.dono = dono;
    }
    public CarteiraPJ getCarteira() { return carteira; }

    public void exibirDados() {
        System.out.println("=== Empresa (PJ) ===");
        System.out.println("Razao Social: " + nome);
        System.out.println("CNPJ: " + cnpj);
        System.out.println("Regime Tributario: " + carteira.getRegimeTributario());
        System.out.println("Dono: " + dono.getNome());
        System.out.println("--- Carteira da Empresa ---");
        carteira.exibirResumo();
    }
}
