package com.criptoinvest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Escalas e arredondamentos dos valores do dominio.
 *
 * Dinheiro, preco e quantidade de cripto sao BigDecimal, nao double: em double,
 * 0,1 + 0,2 da 0,30000000000000004, e o erro se acumula a cada operacao de uma
 * carteira. As escalas abaixo sao as mesmas declaradas no DDL, entao o que a
 * aplicacao calcula e exatamente o que o Oracle consegue guardar.
 *
 * Os metodos de conversao aceitam double apenas na ENTRADA, para os literais da
 * demonstracao continuarem legiveis (0.5 em vez de new BigDecimal("0.5")).
 * BigDecimal.valueOf usa a representacao canonica do double, entao 0.1 vira
 * exatamente "0.1"; dali em diante toda a aritmetica e feita em BigDecimal.
 */
public final class Valores {

    /** Reais: carteira.saldo_reais NUMBER(15,2). */
    public static final int ESCALA_DINHEIRO = 2;

    /** Quantidade e preco de cripto: NUMBER(18,8). */
    public static final int ESCALA_CRIPTO = 8;

    /** Percentuais (variacao 24h, rentabilidade): NUMBER(10,4). */
    public static final int ESCALA_PERCENTUAL = 4;

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
