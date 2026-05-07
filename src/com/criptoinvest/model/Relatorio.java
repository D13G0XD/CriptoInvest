package com.criptoinvest.model;

public class Relatorio {

    private int idRelatorio;
    private Carteira carteira;
    private String dataGeracao;
    private double valorTotalCarteira;
    private double totalInvestido;
    private double totalTaxas;
    private double rentabilidadePercentual;

    public Relatorio(int id, Carteira carteira, String dataGeracao) {
        this.idRelatorio = id;
        this.carteira = carteira;
        this.dataGeracao = dataGeracao;
        this.valorTotalCarteira = carteira.calcularValorTotal();
        this.totalInvestido = carteira.calcularTotalInvestido();
        this.totalTaxas = carteira.calcularTotalTaxas();
        this.rentabilidadePercentual = carteira.calcularRentabilidade();
    }

    public int getIdRelatorio() {
        return idRelatorio;
    }

    public void setIdRelatorio(int idRelatorio) {
        this.idRelatorio = idRelatorio;
    }

    public Carteira getCarteira() {
        return carteira;
    }

    public void setCarteira(Carteira carteira) {
        this.carteira = carteira;
    }

    public String getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(String dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public double getValorTotalCarteira() {
        return valorTotalCarteira;
    }

    public void setValorTotalCarteira(double valorTotalCarteira) {
        this.valorTotalCarteira = valorTotalCarteira;
    }

    public double getTotalInvestido() {
        return totalInvestido;
    }

    public void setTotalInvestido(double totalInvestido) {
        this.totalInvestido = totalInvestido;
    }

    public double getTotalTaxas() {
        return totalTaxas;
    }

    public void setTotalTaxas(double totalTaxas) {
        this.totalTaxas = totalTaxas;
    }

    public double getRentabilidadePercentual() {
        return rentabilidadePercentual;
    }

    public void setRentabilidadePercentual(double rentabilidadePercentual) {
        this.rentabilidadePercentual = rentabilidadePercentual;
    }

    public double calcularLucroLiquido() {
        return valorTotalCarteira - totalInvestido;
    }

    public String gerarResumo() {
        return "=== Relatorio de Desempenho ===\n" +
                "Data: " + dataGeracao + "\n" +
                "Carteira: " + carteira.getDescricao() + "\n" +
                "Total Investido: R$ " + String.format("%.2f", totalInvestido) + "\n" +
                "Valor Atual: R$ " + String.format("%.2f", valorTotalCarteira) + "\n" +
                "Taxas Pagas: R$ " + String.format("%.2f", totalTaxas) + "\n" +
                "Lucro Liquido: R$ " + String.format("%.2f", calcularLucroLiquido()) + "\n" +
                "Rentabilidade: " + String.format("%.2f", rentabilidadePercentual) + "%\n";
    }

    public void exibirRelatorio() {
        System.out.println(gerarResumo());
    }
}