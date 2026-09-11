package com.criptoinvest.model;

import java.math.BigDecimal;

/**
 * Snapshot de desempenho de uma carteira, fechado na data de geracao.
 *
 * Os numeros sao lidos da carteira uma unica vez, no construtor, e a partir dai
 * nao mudam mais: sao final e nao tem setter. Um relatorio com setters poderia
 * ficar internamente incoerente - alterar o total investido sem mexer no lucro,
 * ou trocar a carteira e manter os valores da anterior - e um snapshot que se
 * contradiz nao serve nem para conferencia nem para historico. Para numeros
 * atualizados, gere um novo Relatorio.
 */
public class Relatorio {

    private int idRelatorio;     // PK - atribuida pelo banco, entao tem setter
    private final Carteira carteira;   // FK -> Carteira (idCarteira) - obrigatoria - relacionamento N:1
    private final String dataGeracao;
    private final BigDecimal valorTotalCarteira;
    private final BigDecimal totalInvestido;
    private final BigDecimal totalVendido;
    private final BigDecimal totalTaxas;
    private final BigDecimal lucroTotal;
    private final BigDecimal rentabilidadePercentual;

    public Relatorio(int id, Carteira carteira, String dataGeracao) {
        if (carteira == null) {
            throw new IllegalArgumentException("Relatorio exige uma Carteira.");
        }
        this.idRelatorio = id;
        this.carteira = carteira;
        this.dataGeracao = dataGeracao;
        this.valorTotalCarteira = carteira.calcularValorTotal();
        this.totalInvestido = carteira.calcularTotalInvestido();
        this.totalVendido = carteira.calcularTotalVendido();
        this.totalTaxas = carteira.calcularTotalTaxas();
        this.lucroTotal = carteira.calcularLucroTotal();
        this.rentabilidadePercentual = carteira.calcularRentabilidade();
    }

    public int getIdRelatorio() {
        return idRelatorio;
    }

    /** Unico dado mutavel: a PK, que so e conhecida depois do INSERT. */
    public void setIdRelatorio(int idRelatorio) {
        this.idRelatorio = idRelatorio;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public String getDataGeracao() {
        return dataGeracao;
    }

    public BigDecimal getValorTotalCarteira() {
        return valorTotalCarteira;
    }

    public BigDecimal getTotalInvestido() {
        return totalInvestido;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public BigDecimal getTotalTaxas() {
        return totalTaxas;
    }

    public BigDecimal getLucroTotal() {
        return lucroTotal;
    }

    public BigDecimal getRentabilidadePercentual() {
        return rentabilidadePercentual;
    }

    /**
     * Lucro antes das taxas de corretagem.
     * As taxas ja estao embutidas no lucro total (a compra entra por bruto + taxa
     * e a venda por bruto - taxa), entao aqui elas sao somadas de volta.
     */
    public BigDecimal calcularLucroBruto() {
        return lucroTotal.add(totalTaxas);
    }

    /** Lucro depois das taxas - ou seja, o proprio lucro total. */
    public BigDecimal calcularLucroLiquido() {
        return lucroTotal;
    }

    public String gerarResumo() {
        return "=== Relatorio de Desempenho ===\n" +
                "Data: " + dataGeracao + "\n" +
                "Carteira: " + carteira.getDescricao() + "\n" +
                "Total Investido: R$ " + Valores.formatar(totalInvestido) + "\n" +
                "Total Vendido: R$ " + Valores.formatar(totalVendido) + "\n" +
                "Valor Atual: R$ " + Valores.formatar(valorTotalCarteira) + "\n" +
                "Taxas Pagas: R$ " + Valores.formatar(totalTaxas) + "\n" +
                "Lucro Bruto (antes das taxas): R$ " + Valores.formatar(calcularLucroBruto()) + "\n" +
                "Lucro Liquido: R$ " + Valores.formatar(lucroTotal) + "\n" +
                "Rentabilidade: " + String.format("%.2f", rentabilidadePercentual) + "%\n";
    }

    public void exibirRelatorio() {
        System.out.println(gerarResumo());
    }
}
