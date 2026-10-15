package com.criptoinvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Carteira e Criptoativo.
 * Uma Carteira pode possuir varios Criptoativos, e um Criptoativo pode estar em varias Carteiras.
 * Posicao representa o saldo agregado (snapshot) de um criptoativo dentro de uma carteira.
 *
 * Cardinalidade: Carteira (1) ----- (N) Posicao (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Carteira POSSUI Criptoativo" (com quantidade e preco medio)
 * Obrigatoriedade: Posicao exige Carteira e Criptoativo (ambas FKs obrigatorias).
 *
 * Id: vem da sequence seq_posicao, obtida pelo DAO no INSERT. Ate la o id e 0,
 * que significa "ainda nao persistida".
 */
public class Posicao {

    private int idPosicao;            // PK - 0 enquanto nao persistida (seq_posicao)
    private Carteira carteira;        // FK -> Carteira (idCarteira) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private BigDecimal quantidadeAtual;
    private BigDecimal precoMedioCompra;
    private LocalDate dataPrimeiraAquisicao;  // posicao.data_primeira_aquisicao DATE NOT NULL
    private LocalDate dataUltimaAtualizacao;  // posicao.data_ultima_atualizacao DATE NOT NULL

    /**
     * Posicao nova, aberta pela primeira compra (ver Carteira.registrarTransacao).
     *
     * @throws IllegalArgumentException quando a data de aquisicao e nula
     */
    public Posicao(Carteira carteira, Criptoativo criptoativo,
                   BigDecimal quantidadeInicial, BigDecimal precoInicial, LocalDate dataAquisicao) {
        if (dataAquisicao == null) {
            throw new IllegalArgumentException("Posicao exige a data de aquisicao.");
        }
        this.idPosicao = 0;
        this.carteira = carteira;
        this.criptoativo = criptoativo;
        this.quantidadeAtual = Valores.cripto(quantidadeInicial);
        this.precoMedioCompra = Valores.cripto(precoInicial);
        this.dataPrimeiraAquisicao = dataAquisicao;
        this.dataUltimaAtualizacao = dataAquisicao;
    }

    /**
     * Sobrecarga de conveniencia para os literais da demonstracao: quantidade e
     * preco em double e data no texto "yyyy-MM-dd" (ver Datas.de). Como em
     * Transacao, a data em texto acompanha so a sobrecarga double, para que
     * nenhuma chamada fique ambigua.
     */
    public Posicao(Carteira carteira, Criptoativo criptoativo,
                   double quantidadeInicial, double precoInicial, String dataAquisicao) {
        this(carteira, criptoativo, Valores.de(quantidadeInicial), Valores.de(precoInicial),
                Datas.de(dataAquisicao));
    }

    /**
     * Reconstrucao de uma posicao ja gravada no banco (uso da camada de persistencia).
     * Apenas atribui os valores lidos, normalizando as escalas, sem regra de
     * negocio nem avisos.
     *
     * @throws IllegalArgumentException quando alguma das datas e nula
     */
    public Posicao(int idPosicao, Carteira carteira, Criptoativo criptoativo, BigDecimal quantidadeAtual,
                   BigDecimal precoMedioCompra, LocalDate dataPrimeiraAquisicao, LocalDate dataUltimaAtualizacao) {
        if (dataPrimeiraAquisicao == null || dataUltimaAtualizacao == null) {
            throw new IllegalArgumentException("Posicao exige as datas de primeira aquisicao e de ultima atualizacao.");
        }
        this.idPosicao = idPosicao;
        this.carteira = carteira;
        this.criptoativo = criptoativo;
        this.quantidadeAtual = Valores.cripto(quantidadeAtual);
        this.precoMedioCompra = Valores.cripto(precoMedioCompra);
        this.dataPrimeiraAquisicao = dataPrimeiraAquisicao;
        this.dataUltimaAtualizacao = dataUltimaAtualizacao;
    }

    public int getIdPosicao() { return idPosicao; }
    public void setIdPosicao(int idPosicao) { this.idPosicao = idPosicao; }
    public Carteira getCarteira() { return carteira; }
    public Criptoativo getCriptoativo() { return criptoativo; }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public BigDecimal getPrecoMedioCompra() { return precoMedioCompra; }
    public LocalDate getDataPrimeiraAquisicao() { return dataPrimeiraAquisicao; }
    public LocalDate getDataUltimaAtualizacao() { return dataUltimaAtualizacao; }

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

    public void setDataUltimaAtualizacao(LocalDate data) {
        // posicao.data_ultima_atualizacao e NOT NULL
        if (data == null) {
            System.out.println("Erro: data_ultima_atualizacao nao pode ser nula.");
            return;
        }
        this.dataUltimaAtualizacao = data;
    }

    /** Soma a quantidade comprada e recalcula o preco medio ponderado. */
    public void aplicarCompra(BigDecimal quantidade, BigDecimal precoUnitario, LocalDate data) {
        BigDecimal valorAtual = quantidadeAtual.multiply(precoMedioCompra);
        BigDecimal valorCompra = Valores.ouZero(quantidade).multiply(Valores.ouZero(precoUnitario));

        this.quantidadeAtual = Valores.cripto(quantidadeAtual.add(quantidade));
        this.precoMedioCompra = valorAtual.add(valorCompra)
                .divide(this.quantidadeAtual, Valores.ESCALA_CRIPTO, Valores.ARREDONDAMENTO);
        setDataUltimaAtualizacao(data);
    }

    public void aplicarVenda(BigDecimal quantidade, LocalDate data) {
        if (Valores.maior(quantidade, quantidadeAtual)) {
            System.out.println("Erro: quantidade superior ao saldo da posicao.");
            return;
        }
        this.quantidadeAtual = Valores.cripto(quantidadeAtual.subtract(quantidade));
        setDataUltimaAtualizacao(data);
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
