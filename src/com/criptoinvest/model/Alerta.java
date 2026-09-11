package com.criptoinvest.model;

import java.math.BigDecimal;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Usuario e Criptoativo.
 * Um Usuario pode configurar varios Alertas sobre criptos diferentes, e um Criptoativo
 * pode ser monitorado por varios Usuarios (cada um com seu proprio limite).
 *
 * Cardinalidade: Usuario (1) ----- (N) Alerta (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Usuario MONITORA Criptoativo (com limite de variacao)"
 * Obrigatoriedade: Alerta exige Usuario e Criptoativo (ambas FKs obrigatorias).
 */
public class Alerta {

    private int idAlerta;             // PK
    private Usuario usuario;          // FK -> Usuario (idUsuario) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private BigDecimal limiteVariacao;
    private boolean ativado;
    private String dataConfiguracao;

    public Alerta(int id, Usuario usuario, Criptoativo criptoativo,
                  BigDecimal limiteVariacao, String dataConfiguracao) {
        if (usuario == null || criptoativo == null) {
            throw new IllegalArgumentException("Alerta exige usuario e criptoativo.");
        }
        this.idAlerta = id;
        this.usuario = usuario;
        this.criptoativo = criptoativo;
        if (Valores.naoPositivo(limiteVariacao)) {
            System.out.println("Aviso: limiteVariacao deve ser positivo, ajustado para 5%.");
            this.limiteVariacao = Valores.percentual(Valores.de(5));
        } else {
            this.limiteVariacao = Valores.percentual(limiteVariacao);
        }
        this.dataConfiguracao = dataConfiguracao;
        this.ativado = true;
    }

    /** Sobrecarga de conveniencia para os literais da demonstracao. */
    public Alerta(int id, Usuario usuario, Criptoativo criptoativo,
                  double limiteVariacao, String dataConfiguracao) {
        this(id, usuario, criptoativo, Valores.de(limiteVariacao), dataConfiguracao);
    }

    public int getIdAlerta() { return idAlerta; }
    public void setIdAlerta(int idAlerta) { this.idAlerta = idAlerta; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Criptoativo getCriptoativo() { return criptoativo; }
    public void setCriptoativo(Criptoativo criptoativo) { this.criptoativo = criptoativo; }

    public boolean isAtivado() { return ativado; }
    public void setAtivado(boolean ativado) { this.ativado = ativado; }

    public BigDecimal getLimiteVariacao() { return limiteVariacao; }
    public void setLimiteVariacao(BigDecimal limiteVariacao) {
        if (Valores.naoPositivo(limiteVariacao)) {
            System.out.println("Erro: limiteVariacao deve ser positivo.");
            return;
        }
        this.limiteVariacao = Valores.percentual(limiteVariacao);
    }

    public void setLimiteVariacao(double limiteVariacao) {
        setLimiteVariacao(Valores.de(limiteVariacao));
    }

    public String getDataConfiguracao() { return dataConfiguracao; }
    public void setDataConfiguracao(String dataConfiguracao) { this.dataConfiguracao = dataConfiguracao; }

    public boolean verificarDisparo() {
        if (!ativado) return false;

        // mesma regra da consulta 4.9 do DML: ABS(variacao_24h) >= limite_variacao
        BigDecimal variacao = criptoativo.getVariacao24h();
        return variacao.abs().compareTo(limiteVariacao) >= 0;
    }

    public String mensagemAlerta() {
        if (!verificarDisparo()) {
            return "Nenhum alerta no momento.";
        }

        String direcao = criptoativo.getVariacao24h().signum() > 0 ? "subiu" : "caiu";

        return "ALERTA para " + usuario.getNome() + ": " + criptoativo.getSigla() + " " + direcao + " " +
                String.format("%.2f", criptoativo.getVariacao24h().abs()) +
                "% (limite: " + limiteVariacao.stripTrailingZeros().toPlainString() + "%)";
    }

    public void desativar() {
        this.ativado = false;
    }

    public void ativar() {
        this.ativado = true;
    }

    public void exibirDados() {
        System.out.println("=== Alerta ===");
        System.out.println("Usuario: " + usuario.getNome());
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Limite: " + limiteVariacao.stripTrailingZeros().toPlainString() + "%");
        System.out.println("Ativado: " + ativado);
        System.out.println("Configurado em: " + dataConfiguracao);
        System.out.println("Status: " + mensagemAlerta());
    }
}