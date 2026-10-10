package com.criptoinvest.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Conversao das datas do dominio.
 *
 * As datas das entidades sao LocalDate, que corresponde as colunas DATE do
 * Oracle (dia, sem hora) e nao aceita valores como "2026-02-30" ou "07/05/2026".
 * As sobrecargas de conveniencia dos construtores aceitam tambem o texto
 * "yyyy-MM-dd" - o mesmo formato dos literais DATE 'aaaa-mm-dd' do DML - e o
 * convertem aqui, no mesmo espirito das sobrecargas double de Valores.de.
 */
public final class Datas {

    /** Formato aceito pelas sobrecargas que recebem a data como texto. */
    public static final String FORMATO = "yyyy-MM-dd";

    /** Classe utilitaria: nao deve ser instanciada. */
    private Datas() {
    }

    /**
     * Converte o texto "yyyy-MM-dd" em LocalDate.
     *
     * null continua null: quem recusa a data ausente e o construtor que a
     * recebe, com a mensagem da propria entidade.
     *
     * @throws IllegalArgumentException quando o texto nao e uma data valida no formato yyyy-MM-dd
     */
    public static LocalDate de(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Data invalida: '" + texto + "' (use o formato " + FORMATO + ").", e);
        }
    }
}
