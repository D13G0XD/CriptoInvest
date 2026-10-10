package com.criptoinvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Snapshot de desempenho de uma carteira, fechado na data de geracao.
 *
 * Os numeros sao lidos da carteira uma unica vez, no construtor, e a partir dai
 * nao mudam mais: sao final e nao tem setter. Um relatorio com setters poderia
 * ficar internamente incoerente - alterar o total investido sem mexer no lucro,
 * ou trocar a carteira e manter os valores da anterior - e um snapshot que se
 * contradiz nao serve nem para conferencia nem para historico. Para numeros
 * atualizados, gere um novo Relatorio.
 *
 * Um relatorio lido do banco usa o construtor de reconstrucao, que recebe os
 * numeros gravados em vez de recalcula-los: o snapshot vale pela data em que
 * foi gerado, ainda que a carteira tenha mudado depois.
 */
public class Relatorio {

    private int idRelatorio;     // PK - atribuida pelo banco (seq_relatorio), entao tem setter; 0 = nao persistido
    private final Carteira carteira;   // FK -> Carteira (idCarteira) - obrigatoria - relacionamento N:1
    private final LocalDate dataGeracao;   // relatorio.data_geracao DATE NOT NULL
    private final BigDecimal valorTotalCarteira;
    private final BigDecimal totalInvestido;
    private final BigDecimal totalVendido;
    private final BigDecimal totalTaxas;
    private final BigDecimal lucroTotal;
    private final BigDecimal rentabilidadePercentual;

    /**
     * Gera o snapshot lendo os numeros da carteira neste momento.
     *
     * A data e so LocalDate, sem a sobrecarga em texto de Transacao/Alerta:
     * (int, Carteira, String) tornaria ambigua a chamada com null na data.
     * Para um literal, use LocalDate.parse("yyyy-MM-dd") ou Datas.de.
     *
     * @throws IllegalArgumentException quando a carteira ou a data sao nulas
     */
    public Relatorio(int id, Carteira carteira, LocalDate dataGeracao) {
        if (carteira == null) {
            throw new IllegalArgumentException("Relatorio exige uma Carteira.");
        }
        if (dataGeracao == null) {
            throw new IllegalArgumentException("Relatorio exige a data de geracao.");
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

    /**
     * Reconstrucao de um relatorio ja gravado no banco (uso da camada de persistencia).
     *
     * Recebe os numeros gravados e NAO recalcula nada a partir da carteira; apenas
     * normaliza as escalas (dinheiro com 2 casas, rentabilidade com 4). As colunas
     * de totais aceitam NULL no banco, e null chega aqui como zero (Valores.ouZero).
     *
     * @throws IllegalArgumentException quando a carteira ou a data sao nulas
     */
    public Relatorio(int id, Carteira carteira, LocalDate dataGeracao, BigDecimal valorTotalCarteira,
                     BigDecimal totalInvestido, BigDecimal totalVendido, BigDecimal totalTaxas,
                     BigDecimal lucroTotal, BigDecimal rentabilidadePercentual) {
        if (carteira == null) {
            throw new IllegalArgumentException("Relatorio exige uma Carteira.");
        }
        if (dataGeracao == null) {
            throw new IllegalArgumentException("Relatorio exige a data de geracao.");
        }
        this.idRelatorio = id;
        this.carteira = carteira;
        this.dataGeracao = dataGeracao;
        this.valorTotalCarteira = Valores.dinheiro(valorTotalCarteira);
        this.totalInvestido = Valores.dinheiro(totalInvestido);
        this.totalVendido = Valores.dinheiro(totalVendido);
        this.totalTaxas = Valores.dinheiro(totalTaxas);
        this.lucroTotal = Valores.dinheiro(lucroTotal);
        this.rentabilidadePercentual = Valores.percentual(rentabilidadePercentual);
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

    public LocalDate getDataGeracao() {
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
