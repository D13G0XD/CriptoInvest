package com.criptoinvest.model;

import java.math.BigDecimal;

/**
 * Transacao funciona tambem como entidade associativa em nivel de evento
 * entre Carteira e Criptoativo (registro historico de cada operacao).
 * A associativa Posicao agrega o saldo atual; Transacao guarda o evento.
 */
public class Transacao {

    /** Taxa de corretagem de 0,1% sobre o valor bruto. */
    private static final BigDecimal PERCENTUAL_TAXA = new BigDecimal("0.001");

    private int idTransacao;          // PK
    private Carteira carteira;        // FK -> Carteira (idCarteira) - atribuida via Carteira.registrarTransacao
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria
    private String tipo;              // COMPRA, VENDA
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal taxa;          // derivada de quantidade * precoUnitario * 0.1%
    private String dataOperacao;
    private String observacao;

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     BigDecimal quantidade, String dataOperacao) {
        // precoAtualDe valida o criptoativo antes de este construtor delegar,
        // evitando um NullPointerException cru na leitura do preco.
        this(id, tipo, criptoativo, quantidade, precoAtualDe(criptoativo), dataOperacao);
    }

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     BigDecimal quantidade, BigDecimal precoUnitario, String dataOperacao) {

        if (criptoativo == null) {
            throw new IllegalArgumentException("Transacao exige um Criptoativo.");
        }

        this.idTransacao = id;
        this.criptoativo = criptoativo;

        if (Valores.naoPositivo(quantidade)) {
            System.out.println("Aviso: quantidade deve ser positiva, ajustada para 1.");
            this.quantidade = Valores.cripto(BigDecimal.ONE);
        } else {
            this.quantidade = Valores.cripto(quantidade);
        }

        if (Valores.negativo(precoUnitario)) {
            System.out.println("Aviso: precoUnitario negativo, usando precoAtual do criptoativo.");
            this.precoUnitario = criptoativo.getPrecoAtual();
        } else {
            this.precoUnitario = Valores.cripto(precoUnitario);
        }

        this.dataOperacao = dataOperacao;

        if ("COMPRA".equals(tipo) || "VENDA".equals(tipo)) {
            this.tipo = tipo;
        } else {
            System.out.println("Aviso: tipo invalido, definido como COMPRA.");
            this.tipo = "COMPRA";
        }

        recalcularTaxa();
    }

    /** Sobrecargas de conveniencia para os literais da demonstracao. */
    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, String dataOperacao) {
        this(id, tipo, criptoativo, Valores.de(quantidade), dataOperacao);
    }

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, double precoUnitario, String dataOperacao) {
        this(id, tipo, criptoativo, Valores.de(quantidade), Valores.de(precoUnitario), dataOperacao);
    }

    /** Le o preco atual do criptoativo, recusando a transacao sem criptoativo. */
    private static BigDecimal precoAtualDe(Criptoativo criptoativo) {
        if (criptoativo == null) {
            throw new IllegalArgumentException("Transacao exige um Criptoativo.");
        }
        return criptoativo.getPrecoAtual();
    }

    private void recalcularTaxa() {
        this.taxa = Valores.dinheiro(calcularValorBruto().multiply(PERCENTUAL_TAXA));
    }

    public int getIdTransacao() { return idTransacao; }
    public void setIdTransacao(int idTransacao) { this.idTransacao = idTransacao; }

    public Carteira getCarteira() { return carteira; }
    public void setCarteira(Carteira carteira) { this.carteira = carteira; }

    public String getTipo() { return tipo; }

    public Criptoativo getCriptoativo() { return criptoativo; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) {
        if (Valores.naoPositivo(quantidade)) {
            System.out.println("Erro: quantidade deve ser positiva.");
            return;
        }
        this.quantidade = Valores.cripto(quantidade);
        recalcularTaxa();
    }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) {
        if (Valores.negativo(precoUnitario)) {
            System.out.println("Erro: precoUnitario nao pode ser negativo.");
            return;
        }
        this.precoUnitario = Valores.cripto(precoUnitario);
        recalcularTaxa();
    }

    public BigDecimal getTaxa() { return taxa; }

    public String getDataOperacao() { return dataOperacao; }
    public void setDataOperacao(String dataOperacao) { this.dataOperacao = dataOperacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public BigDecimal calcularValorBruto() {
        return Valores.dinheiro(quantidade.multiply(precoUnitario));
    }

    /** Compra custa bruto + taxa; venda rende bruto - taxa. */
    public BigDecimal calcularValorComTaxa() {
        BigDecimal bruto = calcularValorBruto();
        return "COMPRA".equals(tipo) ? bruto.add(taxa) : bruto.subtract(taxa);
    }

    public BigDecimal calcularValorAtual() {
        return Valores.dinheiro(quantidade.multiply(criptoativo.getPrecoAtual()));
    }

    public void exibirDados() {
        System.out.println("=== Transacao ===");
        System.out.println("Tipo: " + tipo);
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Quantidade: " + quantidade.stripTrailingZeros().toPlainString());
        System.out.println("Preco Unitario: R$ " + Valores.formatar(precoUnitario));
        System.out.println("Valor Bruto: R$ " + Valores.formatar(calcularValorBruto()));
        System.out.println("Taxa (0.1%): R$ " + Valores.formatar(taxa));
        System.out.println("Valor Liquido: R$ " + Valores.formatar(calcularValorComTaxa()));
        System.out.println("Data: " + dataOperacao);
        if (observacao != null) {
            System.out.println("Observacao: " + observacao);
        }
    }
}
