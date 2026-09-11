package com.criptoinvest.model;

import java.math.BigDecimal;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Carteira e Criptoativo.
 * Uma Carteira pode possuir varios Criptoativos, e um Criptoativo pode estar em varias Carteiras.
 * Posicao representa o saldo agregado (snapshot) de um criptoativo dentro de uma carteira.
 *
 * Cardinalidade: Carteira (1) ----- (N) Posicao (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Carteira POSSUI Criptoativo" (com quantidade e preco medio)
 * Obrigatoriedade: Posicao exige Carteira e Criptoativo (ambas FKs obrigatorias).
 */
public class Posicao {

    /**
     * Gerador dos ids das posicoes, espelhando a sequence seq_posicao do banco.
     * O contador e unico para todo o sistema: um contador por carteira faria
     * duas carteiras diferentes gerarem a mesma PK.
     */
    private static int sequencia = 0;

    private int idPosicao;            // PK
    private Carteira carteira;        // FK -> Carteira (idCarteira) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private BigDecimal quantidadeAtual;
    private BigDecimal precoMedioCompra;
    private String dataPrimeiraAquisicao;
    private String dataUltimaAtualizacao;

    public Posicao(Carteira carteira, Criptoativo criptoativo,
                   BigDecimal quantidadeInicial, BigDecimal precoInicial, String dataAquisicao) {
        this.idPosicao = ++sequencia;
        this.carteira = carteira;
        this.criptoativo = criptoativo;
        this.quantidadeAtual = Valores.cripto(quantidadeInicial);
        this.precoMedioCompra = Valores.cripto(precoInicial);
        this.dataPrimeiraAquisicao = dataAquisicao;
        this.dataUltimaAtualizacao = dataAquisicao;
    }

    public int getIdPosicao() { return idPosicao; }
    public void setIdPosicao(int idPosicao) { this.idPosicao = idPosicao; }
    public Carteira getCarteira() { return carteira; }
    public Criptoativo getCriptoativo() { return criptoativo; }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public BigDecimal getPrecoMedioCompra() { return precoMedioCompra; }
    public String getDataPrimeiraAquisicao() { return dataPrimeiraAquisicao; }
    public String getDataUltimaAtualizacao() { return dataUltimaAtualizacao; }

    public void setQuantidadeAtual(BigDecimal quantidadeAtual) {
        // ck_posicao_qtde CHECK (quantidade_atual >= 0)
        if (Valores.negativo(quantidadeAtual)) {
            System.out.println("Erro: quantidade_atual nao pode ser negativa.");
            return;
        }
        this.quantidadeAtual = Valores.cripto(quantidadeAtual);
    }

    public void setPrecoMedioCompra(BigDecimal precoMedioCompra) {
        // ck_posicao_preco CHECK (preco_medio_compra >= 0)
        if (Valores.negativo(precoMedioCompra)) {
            System.out.println("Erro: preco_medio_compra nao pode ser negativo.");
            return;
        }
        this.precoMedioCompra = Valores.cripto(precoMedioCompra);
    }

    public void setDataUltimaAtualizacao(String data) { this.dataUltimaAtualizacao = data; }

    /** Soma a quantidade comprada e recalcula o preco medio ponderado. */
    public void aplicarCompra(BigDecimal quantidade, BigDecimal precoUnitario, String data) {
        BigDecimal valorAtual = quantidadeAtual.multiply(precoMedioCompra);
        BigDecimal valorCompra = Valores.ouZero(quantidade).multiply(Valores.ouZero(precoUnitario));

        this.quantidadeAtual = Valores.cripto(quantidadeAtual.add(quantidade));
        this.precoMedioCompra = valorAtual.add(valorCompra)
                .divide(this.quantidadeAtual, Valores.ESCALA_CRIPTO, Valores.ARREDONDAMENTO);
        this.dataUltimaAtualizacao = data;
    }

    public void aplicarVenda(BigDecimal quantidade, String data) {
        if (Valores.maior(quantidade, quantidadeAtual)) {
            System.out.println("Erro: quantidade superior ao saldo da posicao.");
            return;
        }
        this.quantidadeAtual = Valores.cripto(quantidadeAtual.subtract(quantidade));
        this.dataUltimaAtualizacao = data;
    }

    public BigDecimal calcularValorAtual() {
        return Valores.dinheiro(quantidadeAtual.multiply(criptoativo.getPrecoAtual()));
    }

    public BigDecimal calcularLucroNaoRealizado() {
        return calcularValorAtual().subtract(Valores.dinheiro(quantidadeAtual.multiply(precoMedioCompra)));
    }

    public void exibirDados() {
        System.out.println("=== Posicao ===");
        System.out.println("Carteira: " + carteira.getDescricao());
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Quantidade: " + quantidadeAtual.stripTrailingZeros().toPlainString());
        System.out.println("Preco Medio: R$ " + Valores.formatar(precoMedioCompra));
        System.out.println("Valor Atual: R$ " + Valores.formatar(calcularValorAtual()));
        System.out.println("Lucro nao realizado: R$ " + Valores.formatar(calcularLucroNaoRealizado()));
        System.out.println("Primeira Aquisicao: " + dataPrimeiraAquisicao);
    }
}
