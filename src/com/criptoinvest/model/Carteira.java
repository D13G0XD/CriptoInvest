package com.criptoinvest.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe abstrata pai da heranca Joined: CarteiraPF e CarteiraPJ.
 * Concentra o que e comum a todas as carteiras (descricao, saldo em reais,
 * historico de transacoes, posicoes agregadas por criptoativo e calculos derivados).
 *
 * Posicao e fonte de verdade do saldo cripto. As Transacoes alimentam as Posicoes
 * automaticamente via registrarTransacao().
 *
 * Id: a tabela carteira e unica para PF e PJ, entao o id nao pode vir do titular
 * (usuario 2 e empresa 2 gerariam a mesma PK). Ele vem da sequence seq_carteira,
 * obtida pelo DAO no INSERT; ate la o id e 0, que significa "ainda nao persistida".
 */
public abstract class Carteira {

    protected int idCarteira;          // PK - 0 enquanto nao persistida (seq_carteira)
    protected String descricao;
    protected BigDecimal saldoReais;
    protected List<Transacao> transacoes;
    protected List<Posicao> posicoes;

    /** Carteira nova, ainda nao persistida: id 0 e historico vazio. */
    protected Carteira(String descricao, BigDecimal saldoInicial) {
        this.idCarteira = 0;
        this.descricao = descricao;
        // ck_carteira_saldo CHECK (saldo_reais >= 0)
        this.saldoReais = Valores.negativo(saldoInicial)
                ? Valores.ZERO_DINHEIRO
                : Valores.dinheiro(saldoInicial);
        this.transacoes = new ArrayList<>();
        this.posicoes = new ArrayList<>();
    }

    /**
     * Reconstrucao de uma carteira ja gravada no banco (uso da camada de persistencia).
     *
     * Apenas atribui os valores lidos, normalizando a escala do saldo: nao aplica
     * regra de negocio nem imprime avisos, porque o banco ja garantiu as restricoes
     * (ck_carteira_saldo). O historico comeca vazio e pode ser preenchido depois
     * por carregarHistorico().
     */
    protected Carteira(int idCarteira, String descricao, BigDecimal saldoReais) {
        this.idCarteira = idCarteira;
        this.descricao = descricao;
        this.saldoReais = Valores.dinheiro(saldoReais);
        this.transacoes = new ArrayList<>();
        this.posicoes = new ArrayList<>();
    }

    public int getIdCarteira() { return idCarteira; }
    public void setIdCarteira(int idCarteira) { this.idCarteira = idCarteira; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getSaldoReais() { return saldoReais; }

    /**
     * Somente leitura: o historico muda por registrarTransacao() (regra de negocio)
     * ou por carregarHistorico() (uso exclusivo da persistencia).
     */
    public List<Transacao> getTransacoes() { return Collections.unmodifiableList(transacoes); }
    public int getTotalTransacoes() { return transacoes.size(); }

    /**
     * Somente leitura: as posicoes mudam a partir das transacoes registradas ou
     * por carregarHistorico() (uso exclusivo da persistencia).
     */
    public List<Posicao> getPosicoes() { return Collections.unmodifiableList(posicoes); }

    /**
     * Substitui o historico (transacoes e posicoes) pelo que foi lido do banco.
     *
     * USO EXCLUSIVO DA CAMADA DE PERSISTENCIA (CarteiraDAO.carregarHistorico).
     * Diferente de registrarTransacao(), este metodo nao aplica nenhuma regra de
     * negocio: nao mexe no saldo em reais, nao recalcula posicoes e nao valida as
     * operacoes, porque o saldo_reais e as linhas de posicao gravadas ja refletem
     * essas transacoes. As listas recebidas sao copiadas (alterar a lista original
     * depois nao afeta a carteira); null equivale a lista vazia.
     *
     * @param transacoes transacoes gravadas desta carteira
     * @param posicoes   posicoes gravadas desta carteira
     */
    public void carregarHistorico(List<Transacao> transacoes, List<Posicao> posicoes) {
        this.transacoes = transacoes == null ? new ArrayList<>() : new ArrayList<>(transacoes);
        this.posicoes = posicoes == null ? new ArrayList<>() : new ArrayList<>(posicoes);
    }

    /** Discriminador da heranca (carteira.tipo IN ('PF','PJ')). */
    public abstract String getTipo();

    public void depositar(BigDecimal valor) {
        if (Valores.naoPositivo(valor)) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais = Valores.dinheiro(saldoReais.add(valor));
        System.out.println("Deposito de R$ " + Valores.formatar(valor) + " em " + descricao
                + ". Saldo: R$ " + Valores.formatar(saldoReais));
    }

    public void depositar(BigDecimal valor, String descricaoOperacao) {
        if (Valores.naoPositivo(valor)) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais = Valores.dinheiro(saldoReais.add(valor));
        System.out.println("Deposito de R$ " + Valores.formatar(valor) + " (" + descricaoOperacao
                + ") em " + descricao + ". Saldo: R$ " + Valores.formatar(saldoReais));
    }

    /** Sobrecargas de conveniencia para os literais da demonstracao. */
    public void depositar(double valor) {
        depositar(Valores.de(valor));
    }

    public void depositar(double valor, String descricaoOperacao) {
        depositar(Valores.de(valor), descricaoOperacao);
    }

    public void sacar(BigDecimal valor) {
        if (Valores.naoPositivo(valor)) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        if (Valores.maior(valor, saldoReais)) {
            System.out.println("Erro: saldo insuficiente.");
            return;
        }
        this.saldoReais = Valores.dinheiro(saldoReais.subtract(valor));
        System.out.println("Saque de R$ " + Valores.formatar(valor) + " em " + descricao
                + ". Saldo: R$ " + Valores.formatar(saldoReais));
    }

    public void sacar(double valor) {
        sacar(Valores.de(valor));
    }

    /**
     * Busca pelo id de um criptoativo ja persistido (id > 0). Id 0 significa
     * "ainda nao persistido" e nao identifica um criptoativo: a carteira localiza
     * a posicao de uma transacao pelo proprio objeto (ver buscarPosicao(Criptoativo)).
     */
    public Posicao buscarPosicao(int idCripto) {
        for (Posicao p : posicoes) {
            if (p.getCriptoativo().getIdCripto() == idCripto) return p;
        }
        return null;
    }

    /**
     * Posicao do criptoativo informado, usada ao aplicar uma transacao.
     *
     * Id 0 significa "criptoativo ainda nao persistido" (o id so vem da
     * sequence no INSERT), entao dois criptoativos novos e diferentes teriam o
     * mesmo id 0 e cairiam na mesma posicao se a comparacao fosse so pelo id.
     * Por isso o criptoativo casa pelo proprio objeto e, apenas quando ja tem
     * id persistido (> 0), tambem pelo id (outro objeto lido do banco para o
     * mesmo registro).
     */
    private Posicao buscarPosicao(Criptoativo c) {
        for (Posicao p : posicoes) {
            Criptoativo daPosicao = p.getCriptoativo();
            if (daPosicao == c || (c.getIdCripto() > 0 && daPosicao.getIdCripto() == c.getIdCripto())) {
                return p;
            }
        }
        return null;
    }

    /** Busca pela sigla, normalizada como em Criptoativo (sem espacos, em maiusculas). */
    public Posicao buscarPosicao(String sigla) {
        String procurada = Criptoativo.normalizarSigla(sigla);
        for (Posicao p : posicoes) {
            if (p.getCriptoativo().getSigla().equals(procurada)) return p;
        }
        return null;
    }

    /**
     * Registra a transacao na carteira, movimentando o saldo em reais e a posicao
     * em custodia: a compra debita o valor com taxa, a venda credita o liquido.
     *
     * A operacao so entra no historico depois de passar pelas duas validacoes -
     * saldo em reais suficiente na compra e posicao suficiente na venda. Uma
     * transacao recusada nao altera nada e nao contamina os totais de vendas,
     * taxas, lucro e rentabilidade.
     *
     * @return true quando a transacao foi efetivada
     */
    public boolean registrarTransacao(Transacao transacao) {
        if (transacao == null) {
            System.out.println("Erro: transacao nula.");
            return false;
        }

        boolean compra = "COMPRA".equals(transacao.getTipo());
        BigDecimal valor = transacao.calcularValorComTaxa();

        // ck_carteira_saldo CHECK (saldo_reais >= 0): a compra nao pode estourar o saldo
        if (compra && Valores.maior(valor, saldoReais)) {
            System.out.println("Erro: compra de "
                    + transacao.getQuantidade().stripTrailingZeros().toPlainString() + " "
                    + transacao.getCriptoativo().getSigla() + " recusada - saldo insuficiente (R$ "
                    + Valores.formatar(saldoReais) + " disponivel, R$ "
                    + Valores.formatar(valor) + " necessario).");
            return false;
        }
        if (!aplicarNaPosicao(transacao)) {
            return false;
        }

        this.saldoReais = Valores.dinheiro(compra ? saldoReais.subtract(valor) : saldoReais.add(valor));
        transacao.setCarteira(this);
        transacoes.add(transacao);
        return true;
    }

    /**
     * Registra a transacao com uma observacao. A observacao que nao cabe em
     * transacao.observacao VARCHAR2(200) recusa a operacao inteira antes de
     * qualquer efeito em memoria (saldo, posicao e historico ficam intactos),
     * em vez de a falha so aparecer na gravacao.
     *
     * @return true quando a transacao foi efetivada
     */
    public boolean registrarTransacao(Transacao transacao, String observacao) {
        if (transacao != null) {
            if (!Transacao.observacaoValida(observacao)) {
                System.out.println("Erro: transacao recusada - observacao com mais de "
                        + Transacao.TAMANHO_MAXIMO_OBSERVACAO + " bytes (transacao.observacao VARCHAR2(200)).");
                return false;
            }
            transacao.setObservacao(observacao);
        }
        return registrarTransacao(transacao);
    }

    /**
     * Aplica a transacao na posicao correspondente.
     *
     * @return true se a posicao aceitou a operacao; false quando a venda
     *         excede o saldo em custodia (nada e alterado nesse caso)
     */
    private boolean aplicarNaPosicao(Transacao t) {
        Criptoativo c = t.getCriptoativo();
        Posicao p = buscarPosicao(c);

        if ("COMPRA".equals(t.getTipo())) {
            if (p == null) {
                posicoes.add(new Posicao(this, c,
                        t.getQuantidade(), t.getPrecoUnitario(), t.getDataOperacao()));
            } else {
                p.aplicarCompra(t.getQuantidade(), t.getPrecoUnitario(), t.getDataOperacao());
            }
            return true;
        }

        if (p == null || Valores.menor(p.getQuantidadeAtual(), t.getQuantidade())) {
            System.out.println("Erro: venda de "
                    + t.getQuantidade().stripTrailingZeros().toPlainString() + " "
                    + c.getSigla() + " recusada - sem posicao suficiente.");
            return false;
        }
        p.aplicarVenda(t.getQuantidade(), t.getDataOperacao());
        return true;
    }

    public BigDecimal calcularSaldoCripto(String sigla) {
        Posicao p = buscarPosicao(sigla);
        return p == null ? Valores.ZERO_CRIPTO : p.getQuantidadeAtual();
    }

    public BigDecimal calcularValorTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Posicao p : posicoes) total = total.add(p.calcularValorAtual());
        return Valores.dinheiro(total);
    }

    public BigDecimal calcularTotalInvestido() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transacao t : transacoes) {
            if ("COMPRA".equals(t.getTipo())) total = total.add(t.calcularValorComTaxa());
        }
        return Valores.dinheiro(total);
    }

    public BigDecimal calcularTotalVendido() {
        BigDecimal total = BigDecimal.ZERO;
        for (Transacao t : transacoes) {
            if ("VENDA".equals(t.getTipo())) total = total.add(t.calcularValorComTaxa());
        }
        return Valores.dinheiro(total);
    }

    public BigDecimal calcularTotalTaxas() {
        BigDecimal taxas = BigDecimal.ZERO;
        for (Transacao t : transacoes) taxas = taxas.add(t.getTaxa());
        return Valores.dinheiro(taxas);
    }

    /**
     * Lucro com as taxas ja descontadas: a compra entra por bruto + taxa e a
     * venda por bruto - taxa, entao o valor abaixo NAO deve ser somado ou
     * subtraido de calcularTotalTaxas() de novo.
     */
    public BigDecimal calcularLucroTotal() {
        return Valores.dinheiro(
                calcularValorTotal().add(calcularTotalVendido()).subtract(calcularTotalInvestido()));
    }

    /**
     * Rentabilidade percentual (lucro / investido x 100), na escala 4 de
     * relatorio.rentabilidade_percentual NUMBER(10,4). Uma compra a preco quase
     * zero que depois se valoriza gera percentuais acima de 999999.9999, que o
     * Oracle recusaria com ORA-01438 ao gravar o relatorio: nesse caso avisa e
     * usa o teto da coluna, mantendo o sinal.
     */
    public BigDecimal calcularRentabilidade() {
        BigDecimal investido = calcularTotalInvestido();
        if (Valores.zero(investido)) return Valores.percentual(BigDecimal.ZERO);
        return Valores.percentualNoTeto(calcularLucroTotal()
                .multiply(BigDecimal.valueOf(100))
                .divide(investido, Valores.ESCALA_PERCENTUAL, Valores.ARREDONDAMENTO),
                "rentabilidade_percentual");
    }

    public void exibirResumo() {
        System.out.println("=== Carteira " + getTipo() + ": " + descricao + " ===");
        System.out.println("Saldo em Reais: R$ " + Valores.formatar(saldoReais));
        System.out.println("Total de Transacoes: " + transacoes.size());
        System.out.println("Posicoes ativas: " + posicoes.size());
        System.out.println("Total Investido: R$ " + Valores.formatar(calcularTotalInvestido()));
        System.out.println("Total Vendido: R$ " + Valores.formatar(calcularTotalVendido()));
        System.out.println("Valor Atual: R$ " + Valores.formatar(calcularValorTotal()));
        System.out.println("Taxas Pagas: R$ " + Valores.formatar(calcularTotalTaxas()));
        System.out.println("Lucro Total: R$ " + Valores.formatar(calcularLucroTotal()));
        System.out.println("Rentabilidade: " + String.format("%.2f", calcularRentabilidade()) + "%");
    }
}
