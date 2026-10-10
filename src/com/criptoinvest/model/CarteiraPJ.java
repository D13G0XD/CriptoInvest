package com.criptoinvest.model;

import java.math.BigDecimal;

/**
 * Carteira de Pessoa Juridica - filha de Carteira (heranca joined).
 * Atributo proprio: regime tributario (SIMPLES, LUCRO_PRESUMIDO, LUCRO_REAL).
 */
public class CarteiraPJ extends Carteira {

    private static final String[] REGIMES_VALIDOS = { "SIMPLES", "LUCRO_PRESUMIDO", "LUCRO_REAL" };

    private String regimeTributario;

    public CarteiraPJ(String descricao, BigDecimal saldoInicial, String regimeTributario) {
        super(descricao, saldoInicial);
        if (regimeValido(regimeTributario)) {
            this.regimeTributario = regimeTributario;
        } else {
            System.out.println("Aviso: regime invalido, definido como SIMPLES.");
            this.regimeTributario = "SIMPLES";
        }
    }

    /** Sobrecargas de conveniencia para os literais da demonstracao. */
    public CarteiraPJ(String descricao, double saldoInicial, String regimeTributario) {
        this(descricao, Valores.de(saldoInicial), regimeTributario);
    }

    public CarteiraPJ(String descricao) {
        this(descricao, Valores.ZERO_DINHEIRO, "SIMPLES");
    }

    /**
     * Reconstrucao de uma carteira PJ ja gravada (carteira + carteira_pj).
     * Uso da camada de persistencia: apenas atribui os valores lidos, normalizando
     * a escala do saldo, sem regra de negocio nem avisos - o regime ja foi validado
     * pelo ck_carteira_pj_regime do banco.
     */
    public CarteiraPJ(int idCarteira, String descricao, BigDecimal saldoReais, String regimeTributario) {
        super(idCarteira, descricao, saldoReais);
        this.regimeTributario = regimeTributario;
    }

    public String getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(String regime) {
        if (regimeValido(regime)) {
            this.regimeTributario = regime;
        } else {
            System.out.println("Erro: regime deve ser SIMPLES, LUCRO_PRESUMIDO ou LUCRO_REAL.");
        }
    }

    private static boolean regimeValido(String regime) {
        if (regime == null) return false;
        for (String r : REGIMES_VALIDOS) {
            if (r.equals(regime)) return true;
        }
        return false;
    }

    @Override
    public String getTipo() { return "PJ"; }
}
