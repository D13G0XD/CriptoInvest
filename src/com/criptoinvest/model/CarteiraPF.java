package com.criptoinvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carteira de Pessoa Fisica - filha de Carteira (heranca joined).
 * Atributo proprio: limite diario de saque.
 * O limite e acumulado no dia: cada saque consome o limite restante;
 * resetar ocorre automaticamente quando muda o dia (LocalDate.now()).
 *
 * Limitacao: o controle de saques do dia (saquesHoje/dataUltimoSaque) fica so
 * em memoria e nao e persistido (carteira_pf nao tem coluna para ele); uma
 * carteira relida do banco (buscarPorId) recomeca com o acumulado zerado, entao
 * o limite so vale dentro de um mesmo objeto. Persistir o acumulado (ex.:
 * colunas saques_hoje e data_ultimo_saque em carteira_pf) fica como melhoria futura.
 */
public class CarteiraPF extends Carteira {

    private BigDecimal limiteDiarioSaque;
    private BigDecimal saquesHoje;        // controle em memoria: nao e persistido
    private LocalDate dataUltimoSaque;    // controle em memoria: nao e persistido

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

    /**
     * Reconstrucao de uma carteira PF ja gravada (carteira + carteira_pf).
     * Uso da camada de persistencia: apenas atribui os valores lidos, normalizando
     * as escalas, sem regra de negocio nem avisos. O controle de saques do dia
     * nao e persistido, entao recomeca zerado.
     */
    public CarteiraPF(int idCarteira, String descricao, BigDecimal saldoReais, BigDecimal limiteDiarioSaque) {
        super(idCarteira, descricao, saldoReais);
        this.limiteDiarioSaque = Valores.dinheiro(limiteDiarioSaque);
        this.saquesHoje = Valores.ZERO_DINHEIRO;
        this.dataUltimoSaque = null;
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
        LocalDate hoje = LocalDate.now();
        if (!hoje.equals(dataUltimoSaque)) {
            saquesHoje = Valores.ZERO_DINHEIRO;
            dataUltimoSaque = hoje;
        }
    }
}
