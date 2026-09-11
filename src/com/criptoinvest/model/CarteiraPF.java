package com.criptoinvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carteira de Pessoa Fisica - filha de Carteira (heranca joined).
 * Atributo proprio: limite diario de saque.
 * O limite e acumulado no dia: cada saque consome o limite restante;
 * resetar ocorre automaticamente quando muda o dia (LocalDate.now()).
 */
public class CarteiraPF extends Carteira {

    private BigDecimal limiteDiarioSaque;
    private BigDecimal saquesHoje;
    private String dataUltimoSaque;

    public CarteiraPF(String descricao, BigDecimal saldoInicial, BigDecimal limiteDiarioSaque) {
        super(descricao, saldoInicial);
        // ck_carteira_pf_lim CHECK (limite_diario_saque >= 0)
        this.limiteDiarioSaque = Valores.negativo(limiteDiarioSaque)
                ? Valores.ZERO_DINHEIRO
                : Valores.dinheiro(limiteDiarioSaque);
        this.saquesHoje = Valores.ZERO_DINHEIRO;
        this.dataUltimoSaque = null;
    }

    /** Sobrecargas de conveniencia para os literais da demonstracao. */
    public CarteiraPF(String descricao, double saldoInicial, double limiteDiarioSaque) {
        this(descricao, Valores.de(saldoInicial), Valores.de(limiteDiarioSaque));
    }

    public CarteiraPF(String descricao) {
        this(descricao, Valores.ZERO_DINHEIRO, Valores.de(5000));
    }

    public BigDecimal getLimiteDiarioSaque() { return limiteDiarioSaque; }

    public void setLimiteDiarioSaque(BigDecimal limite) {
        if (Valores.negativo(limite)) {
            System.out.println("Erro: limite nao pode ser negativo.");
            return;
        }
        this.limiteDiarioSaque = Valores.dinheiro(limite);
    }

    public void setLimiteDiarioSaque(double limite) {
        setLimiteDiarioSaque(Valores.de(limite));
    }

    public BigDecimal getSaquesHoje() {
        resetarSeNovoDia();
        return saquesHoje;
    }

    @Override
    public String getTipo() { return "PF"; }

    @Override
    public void sacar(BigDecimal valor) {
        if (Valores.naoPositivo(valor)) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        resetarSeNovoDia();

        BigDecimal acumulado = saquesHoje.add(valor);
        if (Valores.maior(acumulado, limiteDiarioSaque)) {
            System.out.println("Erro: saque de R$ " + Valores.formatar(valor)
                    + " somado aos saques de hoje (R$ " + Valores.formatar(saquesHoje)
                    + ") excede o limite diario de R$ " + Valores.formatar(limiteDiarioSaque));
            return;
        }

        BigDecimal saldoAntes = getSaldoReais();
        super.sacar(valor);
        if (Valores.menor(getSaldoReais(), saldoAntes)) {
            saquesHoje = Valores.dinheiro(acumulado);
        }
    }

    private void resetarSeNovoDia() {
        String hoje = LocalDate.now().toString();
        if (!hoje.equals(dataUltimoSaque)) {
            saquesHoje = Valores.ZERO_DINHEIRO;
            dataUltimoSaque = hoje;
        }
    }
}
