package com.criptoinvest.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstrata pai da heranca Joined: CarteiraPF e CarteiraPJ.
 * Concentra o que e comum a todas as carteiras (descricao, saldo em reais,
 * historico de transacoes, posicoes agregadas por criptoativo e calculos derivados).
 *
 * Posicao e fonte de verdade do saldo cripto. As Transacoes alimentam as Posicoes
 * automaticamente via registrarTransacao().
 */
public abstract class Carteira {

    protected int idCarteira;          // PK
    protected String descricao;
    protected double saldoReais;
    protected List<Transacao> transacoes;
    protected List<Posicao> posicoes;
    protected int proximoIdPosicao;

    public Carteira(int id, String descricao, double saldoInicial) {
        this.idCarteira = id;
        this.descricao = descricao;
        this.saldoReais = saldoInicial < 0 ? 0 : saldoInicial;
        this.transacoes = new ArrayList<>();
        this.posicoes = new ArrayList<>();
        this.proximoIdPosicao = 1;
    }

    public int getIdCarteira() { return idCarteira; }
    public void setIdCarteira(int idCarteira) { this.idCarteira = idCarteira; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getSaldoReais() { return saldoReais; }

    public List<Transacao> getTransacoes() { return transacoes; }
    public int getTotalTransacoes() { return transacoes.size(); }

    public List<Posicao> getPosicoes() { return posicoes; }

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

    public Posicao buscarPosicao(int idCripto) {
        for (Posicao p : posicoes) {
            if (p.getCriptoativo().getIdCripto() == idCripto) return p;
        }
        return null;
    }

    public Posicao buscarPosicao(String sigla) {
        for (Posicao p : posicoes) {
            if (p.getCriptoativo().getSigla().equals(sigla)) return p;
        }
        return null;
    }

    public void registrarTransacao(Transacao transacao) {
        if (!aplicarNaPosicao(transacao)) return;
        transacao.setCarteira(this);
        transacoes.add(transacao);
    }

    public void registrarTransacao(Transacao transacao, String observacao) {
        transacao.setObservacao(observacao);
        registrarTransacao(transacao);
    }

    private boolean aplicarNaPosicao(Transacao t) {
        Criptoativo c = t.getCriptoativo();
        Posicao p = buscarPosicao(c.getIdCripto());

        if ("COMPRA".equals(t.getTipo())) {
            if (p == null) {
                p = new Posicao(proximoIdPosicao++, this, c,
                        t.getQuantidade(), t.getPrecoUnitario(), t.getDataOperacao());
                posicoes.add(p);
            } else {
                p.aplicarCompra(t.getQuantidade(), t.getPrecoUnitario(), t.getDataOperacao());
            }
            return true;
        }
        if ("VENDA".equals(t.getTipo())) {
            if (p == null || p.getQuantidadeAtual() < t.getQuantidade()) {
                System.out.println("Erro: venda de " + t.getQuantidade() + " "
                        + c.getSigla() + " sem posicao suficiente.");
                return false;
            }
            p.aplicarVenda(t.getQuantidade(), t.getDataOperacao());
            return true;
        }
        return false;
    }

    public double calcularSaldoCripto(String sigla) {
        Posicao p = buscarPosicao(sigla);
        return p == null ? 0 : p.getQuantidadeAtual();
    }

    public double calcularValorTotal() {
        double total = 0;
        for (Posicao p : posicoes) total += p.calcularValorAtual();
        return total;
    }

    public double calcularTotalInvestido() {
        double total = 0;
        for (Transacao t : transacoes) {
            if ("COMPRA".equals(t.getTipo())) total += t.calcularValorComTaxa();
        }
        return total;
    }

    public double calcularTotalVendido() {
        double total = 0;
        for (Transacao t : transacoes) {
            if ("VENDA".equals(t.getTipo())) total += t.calcularValorComTaxa();
        }
        return total;
    }

    public double calcularTotalTaxas() {
        double taxas = 0;
        for (Transacao t : transacoes) taxas += t.getTaxa();
        return taxas;
    }

    public double calcularLucroTotal() {
        return calcularValorTotal() + calcularTotalVendido() - calcularTotalInvestido();
    }

    public double calcularRentabilidade() {
        double investido = calcularTotalInvestido();
        if (investido == 0) return 0;
        return (calcularLucroTotal() / investido) * 100;
    }

    public void exibirResumo() {
        System.out.println("=== Carteira " + getTipo() + ": " + descricao + " ===");
        System.out.println("Saldo em Reais: R$ " + String.format("%.2f", saldoReais));
        System.out.println("Total de Transacoes: " + transacoes.size());
        System.out.println("Posicoes ativas: " + posicoes.size());
        System.out.println("Total Investido: R$ " + String.format("%.2f", calcularTotalInvestido()));
        System.out.println("Total Vendido: R$ " + String.format("%.2f", calcularTotalVendido()));
        System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorTotal()));
        System.out.println("Taxas Pagas: R$ " + String.format("%.2f", calcularTotalTaxas()));
        System.out.println("Lucro Total: R$ " + String.format("%.2f", calcularLucroTotal()));
        System.out.println("Rentabilidade: " + String.format("%.2f", calcularRentabilidade()) + "%");
    }
}
