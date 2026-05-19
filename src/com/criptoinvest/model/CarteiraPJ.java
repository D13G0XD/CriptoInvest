package com.criptoinvest.model;

/**
 * Carteira de Pessoa Juridica - filha de Carteira (heranca joined).
 * Atributo proprio: regime tributario (SIMPLES, LUCRO_PRESUMIDO, LUCRO_REAL).
 */
public class CarteiraPJ extends Carteira {

    private static final String[] REGIMES_VALIDOS = { "SIMPLES", "LUCRO_PRESUMIDO", "LUCRO_REAL" };

    private String regimeTributario;

    public CarteiraPJ(int id, String descricao, double saldoInicial, String regimeTributario) {
        super(id, descricao, saldoInicial);
        if (regimeValido(regimeTributario)) {
            this.regimeTributario = regimeTributario;
        } else {
            System.out.println("Aviso: regime invalido, definido como SIMPLES.");
            this.regimeTributario = "SIMPLES";
        }
    }

    public CarteiraPJ(int id, String descricao) {
        this(id, descricao, 0, "SIMPLES");
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
