package com.criptoinvest.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Transacao funciona tambem como entidade associativa em nivel de evento
 * entre Carteira e Criptoativo (registro historico de cada operacao).
 * A associativa Posicao agrega o saldo atual; Transacao guarda o evento.
 */
public class Transacao {

    /** Taxa de corretagem de 0,1% sobre o valor bruto. */
    private static final BigDecimal PERCENTUAL_TAXA = new BigDecimal("0.001");

    /** transacao.observacao VARCHAR2(200): tamanho maximo, em bytes (ver observacaoValida). */
    public static final int TAMANHO_MAXIMO_OBSERVACAO = 200;

    private int idTransacao;          // PK - 0 enquanto nao persistida (seq_transacao)
    private Carteira carteira;        // FK -> Carteira (idCarteira) - atribuida via Carteira.registrarTransacao
                                      // ou pelo construtor de reconstrucao (DAO)
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria
    private String tipo;              // COMPRA, VENDA
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal taxa;          // derivada de quantidade * precoUnitario * 0.1% (escala 2, como transacao.taxa)
    private LocalDate dataOperacao;   // transacao.data_operacao DATE NOT NULL
    private String observacao;

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     BigDecimal quantidade, LocalDate dataOperacao) {
        // precoAtualDe valida o criptoativo antes de este construtor delegar,
        // evitando um NullPointerException cru na leitura do preco.
        this(id, tipo, criptoativo, quantidade, precoAtualDe(criptoativo), dataOperacao);
    }

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     BigDecimal quantidade, BigDecimal precoUnitario, LocalDate dataOperacao) {

        if (criptoativo == null) {
            throw new IllegalArgumentException("Transacao exige um Criptoativo.");
        }
        if (dataOperacao == null) {
            throw new IllegalArgumentException("Transacao exige a data da operacao.");
        }

        this.idTransacao = id;
        this.criptoativo = criptoativo;

        // arredonda antes de validar: um valor positivo menor que a ultima casa
        // viraria zero no setScale e violaria o ck_transacao_qtde (quantidade > 0)
        BigDecimal quantidadeArredondada = Valores.cripto(quantidade);
        if (Valores.naoPositivo(quantidadeArredondada)) {
            System.out.println("Aviso: quantidade deve ser positiva, ajustada para 1.");
            this.quantidade = Valores.cripto(BigDecimal.ONE);
        } else {
            this.quantidade = quantidadeArredondada;
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

    /**
     * Sobrecargas de conveniencia para os literais da demonstracao: quantidade e
     * preco em double e data no texto "yyyy-MM-dd" (ver Datas.de).
     *
     * A data em texto acompanha so as sobrecargas double, e a LocalDate so as
     * BigDecimal: assim nenhuma chamada - nem com null na data - fica ambigua.
     */
    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, String dataOperacao) {
        this(id, tipo, criptoativo, Valores.de(quantidade), Datas.de(dataOperacao));
    }

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, double precoUnitario, String dataOperacao) {
        this(id, tipo, criptoativo, Valores.de(quantidade), Valores.de(precoUnitario), Datas.de(dataOperacao));
    }

    /**
     * Reconstrucao de uma transacao ja gravada no banco (uso da camada de persistencia).
     *
     * Recebe TODOS os campos gravados e apenas os atribui, normalizando as escalas:
     * nao recalcula a taxa (vale a que foi cobrada na operacao), nao valida tipo
     * nem quantidade (o banco ja garantiu os CHECKs) e nao imprime avisos. Tambem
     * nao passa pela carteira: o saldo e a posicao gravados ja refletem a operacao.
     *
     * @throws IllegalArgumentException quando o criptoativo ou a data sao nulos
     */
    public Transacao(int id, Carteira carteira, Criptoativo criptoativo, String tipo, BigDecimal quantidade,
                     BigDecimal precoUnitario, BigDecimal taxa, LocalDate dataOperacao, String observacao) {
        if (criptoativo == null) {
            throw new IllegalArgumentException("Transacao exige um Criptoativo.");
        }
        if (dataOperacao == null) {
            throw new IllegalArgumentException("Transacao exige a data da operacao.");
        }
        this.idTransacao = id;
        this.carteira = carteira;
        this.criptoativo = criptoativo;
        this.tipo = tipo;
        this.quantidade = Valores.cripto(quantidade);
        this.precoUnitario = Valores.cripto(precoUnitario);
        this.taxa = Valores.dinheiro(taxa);
        this.dataOperacao = dataOperacao;
        this.observacao = observacao;
    }

    /** Le o preco atual do criptoativo, recusando a transacao sem criptoativo. */
    private static BigDecimal precoAtualDe(Criptoativo criptoativo) {
        if (criptoativo == null) {
            throw new IllegalArgumentException("Transacao exige um Criptoativo.");
        }
        return criptoativo.getPrecoAtual();
    }

    /**
     * Recalcula a taxa a partir da quantidade e do preco. Usa o helper estatico,
     * e nao calcularValorBruto() (publico, sobrescrevivel), porque e chamado no
     * construtor, antes de uma eventual subclasse estar inicializada.
     */
    private void recalcularTaxa() {
        this.taxa = Valores.dinheiro(valorBruto(quantidade, precoUnitario).multiply(PERCENTUAL_TAXA));
    }

    private static BigDecimal valorBruto(BigDecimal quantidade, BigDecimal precoUnitario) {
        return Valores.dinheiro(quantidade.multiply(precoUnitario));
    }

    public int getIdTransacao() { return idTransacao; }
    public void setIdTransacao(int idTransacao) { this.idTransacao = idTransacao; }

    public Carteira getCarteira() { return carteira; }
    public void setCarteira(Carteira carteira) { this.carteira = carteira; }

    public String getTipo() { return tipo; }

    public Criptoativo getCriptoativo() { return criptoativo; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) {
        // arredonda antes de validar (ck_transacao_qtde: quantidade > 0)
        BigDecimal quantidadeArredondada = Valores.cripto(quantidade);
        if (Valores.naoPositivo(quantidadeArredondada)) {
            System.out.println("Erro: quantidade deve ser positiva.");
            return;
        }
        this.quantidade = quantidadeArredondada;
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

    public LocalDate getDataOperacao() { return dataOperacao; }
    public void setDataOperacao(LocalDate dataOperacao) {
        // transacao.data_operacao e NOT NULL
        if (dataOperacao == null) {
            System.out.println("Erro: data da operacao nao pode ser nula.");
            return;
        }
        this.dataOperacao = dataOperacao;
    }

    public String getObservacao() { return observacao; }

    /**
     * Texto maior que transacao.observacao VARCHAR2(200) e recusado (o valor
     * anterior e mantido), para a operacao nao falhar so na gravacao.
     */
    public void setObservacao(String observacao) {
        if (!observacaoValida(observacao)) {
            System.out.println("Erro: observacao com mais de " + TAMANHO_MAXIMO_OBSERVACAO
                    + " bytes (transacao.observacao VARCHAR2(200)).");
            return;
        }
        this.observacao = observacao;
    }

    /**
     * true quando a observacao cabe em transacao.observacao VARCHAR2(200). O
     * Oracle conta o VARCHAR2 em BYTES (semantica padrao), entao a conferencia
     * usa o tamanho em UTF-8: um texto com acentos ocupa mais bytes do que
     * caracteres. null (sem observacao) e valido.
     */
    public static boolean observacaoValida(String observacao) {
        return observacao == null
                || observacao.getBytes(StandardCharsets.UTF_8).length <= TAMANHO_MAXIMO_OBSERVACAO;
    }

    public BigDecimal calcularValorBruto() {
        return valorBruto(quantidade, precoUnitario);
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
