package com.criptoinvest.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pessoa Fisica que opera no sistema.
 * 1:1 obrigatorio com CarteiraPF; 1:N com Empresa (usuario possui empresas).
 */
public class Usuario {

    private int id;                  // PK
    private String nome;
    private CarteiraPF carteira;     // FK 1:1 obrigatoria -> carteira_pf
    private String email;
    private String senha;
    private String cpf;
    private boolean autenticacaoDoisFatores;

    private List<Empresa> empresas;

    public Usuario(int id, String nome, String email, String senha, String cpf,
                   BigDecimal saldoInicial, BigDecimal limiteDiarioSaque) {
        this.id = id;
        this.nome = nome;
        this.carteira = new CarteiraPF("Carteira PF de " + nome, saldoInicial, limiteDiarioSaque);
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.autenticacaoDoisFatores = false;
        this.empresas = new ArrayList<>();
    }

    /** Sobrecargas de conveniencia para os literais da demonstracao. */
    public Usuario(int id, String nome, String email, String senha, String cpf,
                   double saldoInicial, double limiteDiarioSaque) {
        this(id, nome, email, senha, cpf, Valores.de(saldoInicial), Valores.de(limiteDiarioSaque));
    }

    public Usuario(int id, String nome, String email, String senha, String cpf) {
        this(id, nome, email, senha, cpf, Valores.ZERO_DINHEIRO, Valores.de(5000));
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public CarteiraPF getCarteira() { return carteira; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public boolean isAutenticacaoDoisFatores() { return autenticacaoDoisFatores; }
    public void setAutenticacaoDoisFatores(boolean v) { this.autenticacaoDoisFatores = v; }
    public List<Empresa> getEmpresas() { return empresas; }
    public int getTotalEmpresas() { return empresas.size(); }

    public void ativar2FA() {
        this.autenticacaoDoisFatores = true;
        System.out.println("2FA ativado para " + nome);
    }

    public void adicionarEmpresa(Empresa empresa) {
        if (empresa == null || empresas.contains(empresa)) return;
        empresas.add(empresa);
    }

    public void exibirDados() {
        System.out.println("=== Usuario (PF) ===");
        System.out.println("Nome: " + nome);
        System.out.println("Email: " + email);
        System.out.println("CPF: " + cpf);
        System.out.println("2FA: " + autenticacaoDoisFatores);
        System.out.println("Saldo em Reais (carteira PF): R$ " + Valores.formatar(carteira.getSaldoReais()));
        System.out.println("Empresas: " + empresas.size());
    }
}
