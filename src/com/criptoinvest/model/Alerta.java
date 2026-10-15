package com.criptoinvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Usuario e Criptoativo.
 * Um Usuario pode configurar varios Alertas sobre criptos diferentes, e um Criptoativo
 * pode ser monitorado por varios Usuarios (cada um com seu proprio limite).
 *
 * Cardinalidade: Usuario (1) ----- (N) Alerta (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Usuario MONITORA Criptoativo (com limite de variacao)"
 * Obrigatoriedade: Alerta exige Usuario e Criptoativo (ambas FKs obrigatorias).
 * Unicidade: um alerta por par usuario x criptoativo (uk_alerta_usuario_cripto).
 *
 * Reconstrucao a partir do banco: construtor principal + setIdAlerta/setAtivado
 * (o construtor sempre cria o alerta ativado).
 */
public class Alerta {

    private int idAlerta;             // PK - 0 enquanto nao persistido (seq_alerta)
    private Usuario usuario;          // FK -> Usuario (idUsuario) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private BigDecimal limiteVariacao;
    private boolean ativado;
    private LocalDate dataConfiguracao;   // alerta.data_configuracao DATE NOT NULL

    public Alerta(int id, Usuario usuario, Criptoativo criptoativo,
                  BigDecimal limiteVariacao, LocalDate dataConfiguracao) {
        if (usuario == null || criptoativo == null) {
            throw new IllegalArgumentException("Alerta exige usuario e criptoativo.");
        }
        if (dataConfiguracao == null) {
            throw new IllegalArgumentException("Alerta exige a data de configuracao.");
        }
        this.idAlerta = id;
        this.usuario = usuario;
        this.criptoativo = criptoativo;
        // arredonda antes de validar: um limite positivo menor que a ultima casa
        // viraria zero no setScale e violaria o ck_alerta_limite (limite_variacao > 0)
        BigDecimal limiteArredondado = Valores.percentual(limiteVariacao);
        if (Valores.naoPositivo(limiteArredondado)) {
            System.out.println("Aviso: limiteVariacao deve ser positivo, ajustado para 5%.");
            this.limiteVariacao = Valores.percentual(Valores.de(5));
        } else if (Valores.maior(limiteArredondado, Valores.TETO_PERCENTUAL)) {
            // limite_variacao NUMBER(10,4): acima do teto o Oracle recusaria com ORA-01438
            System.out.println("Aviso: limiteVariacao nao cabe em NUMBER(10,4), ajustado para "
                    + Valores.TETO_PERCENTUAL.toPlainString() + "%.");
            this.limiteVariacao = Valores.TETO_PERCENTUAL;
        } else {
            this.limiteVariacao = limiteArredondado;
        }
        this.dataConfiguracao = dataConfiguracao;
        this.ativado = true;
    }

    /**
     * Sobrecarga de conveniencia para os literais da demonstracao: limite em
     * double e data no texto "yyyy-MM-dd" (ver Datas.de). A data em texto
     * acompanha so a sobrecarga double, para que nenhuma chamada fique ambigua.
     */
    public Alerta(int id, Usuario usuario, Criptoativo criptoativo,
                  double limiteVariacao, String dataConfiguracao) {
        this(id, usuario, criptoativo, Valores.de(limiteVariacao), Datas.de(dataConfiguracao));
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
        // arredonda antes de validar (ck_alerta_limite: limite_variacao > 0)
        BigDecimal limiteArredondado = Valores.percentual(limiteVariacao);
        if (Valores.naoPositivo(limiteArredondado)) {
            System.out.println("Erro: limiteVariacao deve ser positivo.");
            return;
        }
        // limite_variacao NUMBER(10,4): acima do teto o Oracle recusaria com ORA-01438
        if (Valores.maior(limiteArredondado, Valores.TETO_PERCENTUAL)) {
            System.out.println("Erro: limiteVariacao nao cabe em NUMBER(10,4) (maximo "
                    + Valores.TETO_PERCENTUAL.toPlainString() + "%).");
            return;
        }
        this.limiteVariacao = limiteArredondado;
    }

    public void setLimiteVariacao(double limiteVariacao) {
        setLimiteVariacao(Valores.de(limiteVariacao));
    }

    public LocalDate getDataConfiguracao() { return dataConfiguracao; }
    public void setDataConfiguracao(LocalDate dataConfiguracao) {
        // alerta.data_configuracao e NOT NULL
        if (dataConfiguracao == null) {
            System.out.println("Erro: data de configuracao nao pode ser nula.");
            return;
        }
        this.dataConfiguracao = dataConfiguracao;
    }

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