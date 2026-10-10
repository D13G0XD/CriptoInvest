package com.criptoinvest.model;

import java.math.BigDecimal;
import java.util.Locale;

public class Criptoativo {

    private int idCripto;        // PK
    private String nome;
    private String sigla;
    private BigDecimal precoAtual;
    private BigDecimal variacao24h;
    private String categoria;

    public Criptoativo(int id, String nome, String sigla, BigDecimal precoAtual, String categoria) {
        this.idCripto = id;
        this.nome = nome;
        this.sigla = normalizarSigla(sigla);
        // ck_criptoativo_preco CHECK (preco_atual >= 0)
        if (Valores.negativo(precoAtual)) {
            System.out.println("Aviso: preco_atual negativo, ajustado para 0.");
            this.precoAtual = Valores.ZERO_CRIPTO;
        } else {
            this.precoAtual = Valores.cripto(precoAtual);
        }
        this.variacao24h = Valores.percentual(BigDecimal.ZERO);
        this.categoria = categoria;
    }

    /** Sobrecarga de conveniencia para os literais da demonstracao. */
    public Criptoativo(int id, String nome, String sigla, double precoAtual, String categoria) {
        this(id, nome, sigla, Valores.de(precoAtual), categoria);
    }

    public int getIdCripto() {
        return idCripto;
    }

    public void setIdCripto(int idCripto) {
        this.idCripto = idCripto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = normalizarSigla(sigla);
    }

    /**
     * Forma canonica da sigla: sem espacos nas pontas e em maiusculas.
     * uk_criptoativo_sigla diferencia maiusculas de minusculas, entao sem esta
     * normalizacao 'BTC' e 'btc' poderiam coexistir no banco. Locale.ROOT evita
     * conversoes dependentes do idioma da maquina. null continua null.
     */
    public static String normalizarSigla(String sigla) {
        return sigla == null ? null : sigla.trim().toUpperCase(Locale.ROOT);
    }

    public BigDecimal getPrecoAtual() {
        return precoAtual;
    }

    public void setPrecoAtual(BigDecimal precoAtual) {
        // ck_criptoativo_preco CHECK (preco_atual >= 0)
        if (Valores.negativo(precoAtual)) {
            System.out.println("Erro: preco_atual nao pode ser negativo.");
            return;
        }
        this.precoAtual = Valores.cripto(precoAtual);
    }

    public BigDecimal getVariacao24h() {
        return variacao24h;
    }

    public void setVariacao24h(BigDecimal variacao24h) {
        this.variacao24h = Valores.percentualNoTeto(variacao24h, "variacao_24h");
    }

    public void setVariacao24h(double variacao24h) {
        setVariacao24h(Valores.de(variacao24h));
    }

    /** Atualiza o preco calculando a variacao a partir do preco anterior. */
    public void atualizarPreco(BigDecimal novoPreco) {
        if (Valores.negativo(novoPreco)) {
            System.out.println("Erro: preco nao pode ser negativo.");
            return;
        }

        if (Valores.zero(this.precoAtual)) {
            this.variacao24h = Valores.percentual(BigDecimal.ZERO);
        } else {
            // um preco que parte de quase zero gera percentuais enormes (de 0.01
            // para 1000 a variacao e de 9999900%): percentualNoTeto usa o teto
            // de variacao_24h NUMBER(10,4) em vez de deixar o Oracle recusar
            BigDecimal diferenca = Valores.ouZero(novoPreco).subtract(this.precoAtual);
            this.variacao24h = Valores.percentualNoTeto(
                    diferenca.multiply(BigDecimal.valueOf(100))
                             .divide(this.precoAtual, Valores.ESCALA_PERCENTUAL, Valores.ARREDONDAMENTO),
                    "variacao_24h");
        }
        this.precoAtual = Valores.cripto(novoPreco);
    }

    public void atualizarPreco(double novoPreco) {
        atualizarPreco(Valores.de(novoPreco));
    }

    /** Atualiza o preco com uma variacao informada pela fonte de cotacao. */
    public void atualizarPreco(BigDecimal novoPreco, BigDecimal variacao) {
        if (Valores.negativo(novoPreco)) {
            System.out.println("Erro: preco nao pode ser negativo.");
            return;
        }
        this.precoAtual = Valores.cripto(novoPreco);
        this.variacao24h = Valores.percentualNoTeto(variacao, "variacao_24h");
    }

    public void atualizarPreco(double novoPreco, double variacao) {
        atualizarPreco(Valores.de(novoPreco), Valores.de(variacao));
    }

    public void exibirDados() {
        System.out.println("=== Criptoativo ===");
        System.out.println("Nome: " + nome + " (" + sigla + ")");
        System.out.println("Categoria: " + categoria);
        System.out.println("Preco Atual: R$ " + Valores.formatar(precoAtual));
        System.out.println("Variacao 24h: " + String.format("%.2f", variacao24h) + "%");
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
