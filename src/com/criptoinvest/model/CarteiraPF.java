package com.criptoinvest.model;

/**
 * Carteira de Pessoa Fisica - filha de Carteira (heranca joined).
 * Atributo proprio: limite diario de saque.
 */
public class CarteiraPF extends Carteira {

    private double limiteDiarioSaque;

    public CarteiraPF(int id, String descricao, double saldoInicial, double limiteDiarioSaque) {
        super(id, descricao, saldoInicial);
        // ck_carteira_pf_lim CHECK (limite_diario_saque >= 0)
        this.limiteDiarioSaque = limiteDiarioSaque < 0 ? 0 : limiteDiarioSaque;
    }

    public CarteiraPF(int id, String descricao) {
        this(id, descricao, 0, 5000);
    }

    public double getLimiteDiarioSaque() { return limiteDiarioSaque; }
    public void setLimiteDiarioSaque(double limite) {
        if (limite < 0) {
            System.out.println("Erro: limite nao pode ser negativo.");
            return;
        }
        this.limiteDiarioSaque = limite;
    }

    @Override
    public String getTipo() { return "PF"; }

    @Override
    public void sacar(double valor) {
        if (valor > limiteDiarioSaque) {
            System.out.println("Erro: saque de R$ " + valor + " excede o limite diario de R$ " + limiteDiarioSaque);
            return;
        }
        super.sacar(valor);
    }
}
