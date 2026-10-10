package com.criptoinvest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Escalas e arredondamentos dos valores do dominio.
 *
 * Dinheiro, preco e quantidade de cripto sao BigDecimal, nao double: em double,
 * 0,1 + 0,2 da 0,30000000000000004, e o erro se acumula a cada operacao de uma
 * carteira. As escalas abaixo acompanham as declaradas no DDL (o numero de
 * casas decimais e o mesmo); a PRECISAO (quantos digitos cabem antes da
 * virgula) nao e garantida pelo setScale: um valor acima da precisao da
 * coluna e recusado pelo Oracle com ORA-01438. Dinheiro e quantidade/preco de
 * cripto nao sao conferidos aqui (o limite fica so no Oracle). So os
 * percentuais calculados (variacao 24h, rentabilidade), que nao tem limite
 * natural, sao limitados ao teto da coluna pelo proprio modelo (ver
 * percentualNoTeto); o limite do alerta e comparado com TETO_PERCENTUAL no
 * Alerta.
 *
 * Os metodos de conversao aceitam double apenas na ENTRADA, para os literais da
 * demonstracao continuarem legiveis (0.5 em vez de new BigDecimal("0.5")).
 * BigDecimal.valueOf usa a representacao canonica do double, entao 0.1 vira
 * exatamente "0.1"; dali em diante toda a aritmetica e feita em BigDecimal.
 */
public final class Valores {

    /**
     * Reais: saldo_reais, limite_diario_saque, taxa e total_taxas NUMBER(15,2);
     * demais totais do relatorio NUMBER(18,2).
     */
    public static final int ESCALA_DINHEIRO = 2;

    /** Quantidade e preco de cripto: NUMBER(18,8). */
    public static final int ESCALA_CRIPTO = 8;

    /** Percentuais (variacao 24h, limite do alerta, rentabilidade): NUMBER(10,4). */
    public static final int ESCALA_PERCENTUAL = 4;

    /** Maior percentual que cabe em NUMBER(10,4): 999999.9999. */
    public static final BigDecimal TETO_PERCENTUAL = new BigDecimal("999999.9999");

    /** Arredondamento comercial, o mesmo que o Oracle aplica ao gravar. */
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;

    public static final BigDecimal ZERO_DINHEIRO = dinheiro(BigDecimal.ZERO);
    public static final BigDecimal ZERO_CRIPTO = cripto(BigDecimal.ZERO);

    /** Classe utilitaria: nao deve ser instanciada. */
    private Valores() {
    }

    /** Converte um literal double sem passar pelo erro de representacao binaria. */
    public static BigDecimal de(double valor) {
        return BigDecimal.valueOf(valor);
    }

    /** Trata null como zero, para nao espalhar checagem pelas entidades. */
    public static BigDecimal ouZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public static BigDecimal dinheiro(BigDecimal valor) {
        return ouZero(valor).setScale(ESCALA_DINHEIRO, ARREDONDAMENTO);
    }

    public static BigDecimal cripto(BigDecimal valor) {
        return ouZero(valor).setScale(ESCALA_CRIPTO, ARREDONDAMENTO);
    }

    public static BigDecimal percentual(BigDecimal valor) {
        return ouZero(valor).setScale(ESCALA_PERCENTUAL, ARREDONDAMENTO);
    }

    /**
     * Percentual na escala 4 e dentro do teto de NUMBER(10,4). Acima de
     * TETO_PERCENTUAL em modulo o Oracle recusaria o valor com ORA-01438: nesse
     * caso avisa e devolve +TETO_PERCENTUAL ou -TETO_PERCENTUAL, conforme o
     * sinal. A conferencia e feita no valor ja arredondado, entao um valor que
     * arredonda para cima (999999.99995 -> 1000000.0000) tambem e ajustado.
     * null conta como zero.
     *
     * @param coluna nome da coluna, usado so na mensagem de aviso
     */
    public static BigDecimal percentualNoTeto(BigDecimal valor, String coluna) {
        BigDecimal arredondado = percentual(valor);
        if (!maior(arredondado.abs(), TETO_PERCENTUAL)) {
            return arredondado;
        }
        BigDecimal teto = arredondado.signum() < 0 ? TETO_PERCENTUAL.negate() : TETO_PERCENTUAL;
        System.out.println("Aviso: " + arredondado.toPlainString() + "% nao cabe em " + coluna
                + " NUMBER(10,4), ajustado para " + teto.toPlainString() + "%.");
        return teto;
    }

    /** true quando o valor e menor ou igual a zero (compareTo ignora a escala). */
    public static boolean naoPositivo(BigDecimal valor) {
        return ouZero(valor).signum() <= 0;
    }

    /** true quando o valor e negativo. */
    public static boolean negativo(BigDecimal valor) {
        return ouZero(valor).signum() < 0;
    }

    /** true quando o valor e exatamente zero, independentemente da escala. */
    public static boolean zero(BigDecimal valor) {
        return ouZero(valor).signum() == 0;
    }

    /** true quando a > b. */
    public static boolean maior(BigDecimal a, BigDecimal b) {
        return ouZero(a).compareTo(ouZero(b)) > 0;
    }

    /** true quando a < b. */
    public static boolean menor(BigDecimal a, BigDecimal b) {
        return ouZero(a).compareTo(ouZero(b)) < 0;
    }

    /** Formata para exibicao, ja com a escala de dinheiro. */
    public static String formatar(BigDecimal valor) {
        return String.format("%.2f", dinheiro(valor));
    }
}
