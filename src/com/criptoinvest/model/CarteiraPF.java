package com.criptoinvest.model;

import java.time.LocalDate;

/**
 * Carteira de Pessoa Fisica - filha de Carteira (heranca joined).
 * Atributo proprio: limite diario de saque.
 * O limite e acumulado no dia: cada saque consome o limite restante;
 * resetar ocorre automaticamente quando muda o dia (LocalDate.now()).
 */
public class CarteiraPF extends Carteira {

    private double limiteDiarioSaque;
    private double saquesHoje;
    private String dataUltimoSaque;

    public CarteiraPF(int id, String descricao, double saldoInicial, double limiteDiarioSaque) {
        super(id, descricao, saldoInicial);
        // ck_carteira_pf_lim CHECK (limite_diario_saque >= 0)
        this.limiteDiarioSaque = limiteDiarioSaque < 0 ? 0 : limiteDiarioSaque;
        this.saquesHoje = 0;
        this.dataUltimoSaque = null;
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

    public double getSaquesHoje() {
        resetarSeNovoDia();
        return saquesHoje;
    }

    @Override
    public String getTipo() { return "PF"; }

    @Override
    public void sacar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        resetarSeNovoDia();
        double acumulado = saquesHoje + valor;
        if (acumulado > limiteDiarioSaque) {
            System.out.println("Erro: saque de R$ " + valor
                    + " somado aos saques de hoje (R$ " + saquesHoje
                    + ") excede o limite diario de R$ " + limiteDiarioSaque);
            return;
        }
        double saldoAntes = getSaldoReais();
        super.sacar(valor);
        if (getSaldoReais() < saldoAntes) {
            saquesHoje += valor;
        }
    }

    private void resetarSeNovoDia() {
        String hoje = LocalDate.now().toString();
        if (!hoje.equals(dataUltimoSaque)) {
            saquesHoje = 0;
            dataUltimoSaque = hoje;
        }
    }
}
