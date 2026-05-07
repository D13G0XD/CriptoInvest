package com.criptoinvest.model;

public class Transacao {

    private int idTransacao;
    private String tipo;
    private Criptoativo criptoativo;
    private double quantidade;
    private double precoUnitario;
    private double taxa;
    private String dataOperacao;

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, String dataOperacao) {

        this.idTransacao = id;
        this.criptoativo = criptoativo;
        this.quantidade = quantidade;
        this.precoUnitario = criptoativo.getPrecoAtual();
        this.dataOperacao = dataOperacao;

        if (tipo.equals("COMPRA") || tipo.equals("VENDA") || tipo.equals("CONVERSAO")) {
            this.tipo = tipo;

        } else {
            System.out.println("Aviso: tipo invalido, definido como COMPRA.");
            this.tipo = "COMPRA";
        }

        this.taxa = calcularValorBruto() * 0.001;
    }

    public int getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(int idTransacao) {
        this.idTransacao = idTransacao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Criptoativo getCriptoativo() {
        return criptoativo;
    }

    public void setCriptoativo(Criptoativo criptoativo) {
        this.criptoativo = criptoativo;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(double precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public double getTaxa() {
        return taxa;
    }

    public void setTaxa(double taxa) {
        this.taxa = taxa;
    }

    public String getDataOperacao() {
        return dataOperacao;
    }

    public void setDataOperacao(String dataOperacao) {
        this.dataOperacao = dataOperacao;
    }

    public double calcularValorBruto() {
        return quantidade * precoUnitario;
    }

    public double calcularValorComTaxa() {
        if (tipo.equals("COMPRA")) {
            return calcularValorBruto() + taxa;
        }

        return calcularValorBruto() - taxa;
    }

    public double calcularValorAtual() {
        return quantidade * criptoativo.getPrecoAtual();
    }

    public double calcularLucro() {
        if (tipo.equals("COMPRA")) {
            return calcularValorAtual() - calcularValorComTaxa();
        }

        return 0;
    }

    public void exibirDados() {
        System.out.println("=== Transacao ===");
        System.out.println("Tipo: " + tipo);
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Quantidade: " + quantidade);
        System.out.println("Preco Unitario: R$ " + precoUnitario);
        System.out.println("Valor Bruto: R$ " + String.format("%.2f", calcularValorBruto()));
        System.out.println("Taxa (0.1%): R$ " + String.format("%.2f", taxa));
        System.out.println("Valor Liquido: R$ " + String.format("%.2f", calcularValorComTaxa()));
        System.out.println("Data: " + dataOperacao);

        if (tipo.equals("COMPRA")) {
            System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorAtual()));
            System.out.println("Lucro: R$ " + String.format("%.2f", calcularLucro()));
        }
    }
}