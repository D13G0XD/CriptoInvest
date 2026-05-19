package com.criptoinvest.model;

/**
 * Classe abstrata pai da heranca Joined: CarteiraPF e CarteiraPJ.
 * Concentra o que e comum a todas as carteiras (descricao, saldo em reais,
 * historico de transacoes e calculos derivados).
 */
public abstract class Carteira {

    protected int idCarteira;          // PK
    protected String descricao;
    protected double saldoReais;       // saldo em moeda corrente
    // Relacionamento 1:N com Transacao
    protected Transacao[] transacoes;
    protected int totalTransacoes;

    public Carteira(int id, String descricao, double saldoInicial) {
        this.idCarteira = id;
        this.descricao = descricao;
        this.saldoReais = saldoInicial < 0 ? 0 : saldoInicial;
        this.transacoes = new Transacao[100];
        this.totalTransacoes = 0;
    }

    public int getIdCarteira() { return idCarteira; }
    public void setIdCarteira(int idCarteira) { this.idCarteira = idCarteira; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getSaldoReais() { return saldoReais; }

    public Transacao[] getTransacoes() { return transacoes; }
    public int getTotalTransacoes() { return totalTransacoes; }

    /** Discriminador da heranca (carteira.tipo IN ('PF','PJ')). */
    public abstract String getTipo();

    public void depositar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais += valor;
        System.out.println("Deposito de R$ " + valor + " em " + descricao + ". Saldo: R$ " + saldoReais);
    }

    public void depositar(double valor, String descricaoOperacao) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais += valor;
        System.out.println("Deposito de R$ " + valor + " (" + descricaoOperacao + ") em " + descricao + ". Saldo: R$ " + saldoReais);
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
        System.out.println("Saque de R$ " + valor + " em " + descricao + ". Saldo: R$ " + saldoReais);
    }

    public void registrarTransacao(Transacao transacao) {
        if (totalTransacoes < 100) {
            transacao.setCarteira(this);
            transacoes[totalTransacoes] = transacao;
            totalTransacoes++;
        } else {
            System.out.println("Erro: limite de transacoes atingido.");
        }
    }

    public void registrarTransacao(Transacao transacao, String observacao) {
        if (totalTransacoes < 100) {
            transacao.setCarteira(this);
            transacoes[totalTransacoes] = transacao;
            totalTransacoes++;
            System.out.println("Observacao: " + observacao);
        } else {
            System.out.println("Erro: limite de transacoes atingido.");
        }
    }

    public double calcularSaldoCripto(String sigla) {
        double saldo = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].getCriptoativo().getSigla().equals(sigla)) {
                if (transacoes[i].getTipo().equals("COMPRA")) {
                    saldo += transacoes[i].getQuantidade();
                } else if (transacoes[i].getTipo().equals("VENDA")) {
                    saldo -= transacoes[i].getQuantidade();
                }
            }
        }
        return saldo;
    }

    public double calcularValorTotal() {
        double total = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].getTipo().equals("COMPRA")) {
                String sigla = transacoes[i].getCriptoativo().getSigla();
                double saldoCripto = calcularSaldoCripto(sigla);
                if (saldoCripto > 0) {
                    total += saldoCripto * transacoes[i].getCriptoativo().getPrecoAtual();
                }
            }
        }
        return total;
    }

    public double calcularTotalInvestido() {
        double investido = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].getTipo().equals("COMPRA")) {
                investido += transacoes[i].calcularValorComTaxa();
            }
        }
        return investido;
    }

    public double calcularTotalTaxas() {
        double taxas = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            taxas += transacoes[i].getTaxa();
        }
        return taxas;
    }

    public double calcularRentabilidade() {
        double investido = calcularTotalInvestido();
        if (investido == 0) return 0;
        double atual = calcularValorTotal();
        return ((atual - investido) / investido) * 100;
    }

    public void exibirResumo() {
        System.out.println("=== Carteira " + getTipo() + ": " + descricao + " ===");
        System.out.println("Saldo em Reais: R$ " + String.format("%.2f", saldoReais));
        System.out.println("Total de Transacoes: " + totalTransacoes);
        System.out.println("Total Investido: R$ " + String.format("%.2f", calcularTotalInvestido()));
        System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorTotal()));
        System.out.println("Taxas Pagas: R$ " + String.format("%.2f", calcularTotalTaxas()));
        System.out.println("Rentabilidade: " + String.format("%.2f", calcularRentabilidade()) + "%");
    }
}
