package com.criptoinvest.model;

/**
 * Transacao funciona tambem como entidade associativa em nivel de evento
 * entre Carteira e Criptoativo (registro historico de cada operacao).
 * A associativa Posicao agrega o saldo atual; Transacao guarda o evento.
 */
public class Transacao {

    private int idTransacao;          // PK
    private Carteira carteira;        // FK -> Carteira (idCarteira) - atribuida via Carteira.registrarTransacao
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria
    private String tipo;              // COMPRA, VENDA
    private double quantidade;
    private double precoUnitario;
    private double taxa;              // derivada de quantidade * precoUnitario * 0.1%
    private String dataOperacao;
    private String observacao;

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, String dataOperacao) {
        this(id, tipo, criptoativo, quantidade, criptoativo.getPrecoAtual(), dataOperacao);
    }

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, double precoUnitario, String dataOperacao) {

        this.idTransacao = id;
        this.criptoativo = criptoativo;

        if (quantidade <= 0) {
            throw new IllegalArgumentException("Aviso: quantidade deve ser positiva");
        } else {
            this.quantidade = quantidade;
        }

        if (precoUnitario < 0) {
            System.out.println("Aviso: precoUnitario negativo, usando precoAtual do criptoativo.");
            this.precoUnitario = criptoativo.getPrecoAtual();
        } else {
            this.precoUnitario = precoUnitario;
        }

        this.dataOperacao = dataOperacao;

        if ("COMPRA".equals(tipo) || "VENDA".equals(tipo)) {
            this.tipo = tipo;
        } else {
            throw new IllegalArgumentException("Aviso: tipo invalido!");
        }

        recalcularTaxa();
    }

    private void recalcularTaxa() {
        this.taxa = calcularValorBruto() * 0.001;
    }

    public int getIdTransacao() { return idTransacao; }
    public void setIdTransacao(int idTransacao) { this.idTransacao = idTransacao; }

    public Carteira getCarteira() { return carteira; }
    public void setCarteira(Carteira carteira) { this.carteira = carteira; }

    public String getTipo() { return tipo; }

    public Criptoativo getCriptoativo() { return criptoativo; }

    public double getQuantidade() { return quantidade; }
    public void setQuantidade(double quantidade) {
        if (quantidade <= 0) {
            System.out.println("Erro: quantidade deve ser positiva.");
            return;
        }
        this.quantidade = quantidade;
        recalcularTaxa();
    }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) {
        if (precoUnitario < 0) {
            System.out.println("Erro: precoUnitario nao pode ser negativo.");
            return;
        }
        this.precoUnitario = precoUnitario;
        recalcularTaxa();
    }

    public double getTaxa() { return taxa; }

    public String getDataOperacao() { return dataOperacao; }
    public void setDataOperacao(String dataOperacao) { this.dataOperacao = dataOperacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public double calcularValorBruto() {
        return quantidade * precoUnitario;
    }

    public double calcularValorComTaxa() {
        if ("COMPRA".equals(tipo)) {
            return calcularValorBruto() + taxa;
        }
        return calcularValorBruto() - taxa;
    }

    public double calcularValorAtual() {
        return quantidade * criptoativo.getPrecoAtual();
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
        if (observacao != null) {
            System.out.println("Observacao: " + observacao);
        }
    }
}
