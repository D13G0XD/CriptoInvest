package com.criptoinvest.view;

/**
 * Lancada pela Entrada quando o usuario digita CANCELAR em qualquer campo de
 * uma operacao. O MenuBase a captura, avisa que nada foi gravado e volta ao
 * submenu - assim o usuario nunca fica preso num formulario que nao quer
 * terminar.
 */
public class OperacaoCancelada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperacaoCancelada() {
        super("Operacao cancelada pelo usuario.");
    }
}
