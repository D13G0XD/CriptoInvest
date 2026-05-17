package com.criptoinvest.model;

public class Usuario extends Titular {

    // PK: id herdada de Titular
    // FK -> Carteira (idCarteira) herdada de Titular - relacionamento 1:1 obrigatorio
    private String email;
    private String senha;
    private String cpf;
    private boolean autenticacaoDoisFatores;
    private double saldoReais;
    // Relacionamento N:N com Empresa: navegacao legada (substituida formalmente por Participacao)
    private Empresa[] empresas;
    private int totalEmpresas;

    public Usuario(int id, String nome, String email, String senha, String cpf) {
        super(id, nome, new Carteira(id, "Carteira de " + nome));
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.autenticacaoDoisFatores = false;
        this.saldoReais = 0;
        this.empresas = new Empresa[10];
        this.totalEmpresas = 0;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public boolean isAutenticacaoDoisFatores() { return autenticacaoDoisFatores; }
    public void setAutenticacaoDoisFatores(boolean v) { this.autenticacaoDoisFatores = v; }
    public double getSaldoReais() { return saldoReais; }
    public void setSaldoReais(double saldoReais) { this.saldoReais = saldoReais; }
    public Empresa[] getEmpresas() { return empresas; }
    public int getTotalEmpresas() { return totalEmpresas; }

    public void ativar2FA() {
        this.autenticacaoDoisFatores = true;
        System.out.println("2FA ativado para " + getNome());
    }

    public void depositar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais += valor;
        System.out.println("Deposito de R$ " + valor + " realizado. Saldo: R$ " + saldoReais);
    }

    public void depositar(double valor, String descricao) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais += valor;
        System.out.println("Deposito de R$ " + valor + " (" + descricao + ") realizado. Saldo: R$ " + saldoReais);
    }

    public void sacar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        if (valor > saldoReais) {
            System.out.println("Erro: saldo insuficiente.");
            return;
        }
        this.saldoReais -= valor;
        System.out.println("Saque de R$ " + valor + " realizado. Saldo: R$ " + saldoReais);
    }

    public void adicionarEmpresa(Empresa empresa) {
        if (totalEmpresas < 10) {
            empresa.setUsuario(this);
            empresas[totalEmpresas] = empresa;
            totalEmpresas++;
        }
    }

    @Override
    public void exibirDados() {
        System.out.println("=== Usuario ===");
        System.out.println("Nome: " + getNome());
        System.out.println("Email: " + email);
        System.out.println("CPF: " + cpf);
        System.out.println("2FA: " + autenticacaoDoisFatores);
        System.out.println("Saldo em Reais: R$ " + saldoReais);
        System.out.println("Empresas: " + totalEmpresas);
    }
}
