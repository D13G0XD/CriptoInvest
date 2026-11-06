package com.criptoinvest.view;

import com.criptoinvest.model.Alerta;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Empresa;
import com.criptoinvest.model.Posicao;
import com.criptoinvest.model.Relatorio;
import com.criptoinvest.model.Transacao;
import com.criptoinvest.model.Usuario;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Saida no console: titulos, mensagens padronizadas e a formatacao dos valores
 * no padrao brasileiro (R$ 1.234,56 - 0,5 BTC - 16,67% - 05/11/2026).
 *
 * Os textos ficam sem acentos, como no restante do projeto: o console do
 * Windows nem sempre usa UTF-8 e mostraria os acentos trocados.
 */
public final class Tela {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final int LARGURA = 78;

    /** Classe utilitaria: nao deve ser instanciada. */
    private Tela() {
    }

    // ------------------------------------------------------------------------
    // Estrutura da tela
    // ------------------------------------------------------------------------

    public static void titulo(String texto) {
        System.out.println();
        System.out.println(repetir('=', LARGURA));
        System.out.println("  " + texto.toUpperCase(BRASIL));
        System.out.println(repetir('=', LARGURA));
    }

    public static void secao(String texto) {
        System.out.println();
        System.out.println("--- " + texto + " " + repetir('-', Math.max(3, LARGURA - texto.length() - 5)));
    }

    /** Linha de separacao sob o cabecalho das tabelas. */
    public static void separador(int largura) {
        System.out.println(repetir('-', largura));
    }

    /** Par rotulo: valor, alinhado, para exibir um registro. */
    public static void campo(String rotulo, Object valor) {
        System.out.printf("  %-26s %s%n", rotulo + ":", valor == null ? "-" : valor);
    }

    public static void info(String mensagem) {
        System.out.println(mensagem);
    }

    public static void sucesso(String mensagem) {
        System.out.println("[OK] " + mensagem);
    }

    public static void aviso(String mensagem) {
        System.out.println("[!] " + mensagem);
    }

    public static void erro(String mensagem) {
        System.out.println("[ERRO] " + mensagem);
    }

    // ------------------------------------------------------------------------
    // Formatacao de valores
    // ------------------------------------------------------------------------

    /** Dinheiro com 2 casas: R$ 1.234,56. */
    public static String reais(BigDecimal valor) {
        return "R$ " + String.format(BRASIL, "%,.2f", Valores.dinheiro(valor));
    }

    /** Preco de criptoativo: de 2 a 8 casas (R$ 350.000,00 ou R$ 0,00012345). */
    public static String preco(BigDecimal valor) {
        return "R$ " + decimal(valor, "#,##0.00######");
    }

    /** Quantidade de criptoativo: ate 8 casas, sem zeros inuteis (0,5 ou 1.250). */
    public static String cripto(BigDecimal valor) {
        return decimal(valor, "#,##0.########");
    }

    /** Percentual com 2 a 4 casas: 16,67% ou 0,1234%. */
    public static String percentual(BigDecimal valor) {
        return decimal(valor, "#,##0.00##") + "%";
    }

    /** Variacao com sinal explicito: +16,67% ou -3,25%. */
    public static String variacao(BigDecimal valor) {
        return (Valores.ouZero(valor).signum() > 0 ? "+" : "") + percentual(valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "-" : DATA.format(data);
    }

    public static String simNao(boolean valor) {
        return valor ? "Sim" : "Nao";
    }

    /** Corta o texto para caber numa coluna de tabela (null vira "-"). */
    public static String cortar(String texto, int largura) {
        if (texto == null || texto.isEmpty()) {
            return "-";
        }
        return texto.length() <= largura ? texto : texto.substring(0, largura - 3) + "...";
    }

    private static String decimal(BigDecimal valor, String padrao) {
        DecimalFormat formato = new DecimalFormat(padrao, DecimalFormatSymbols.getInstance(BRASIL));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(Valores.ouZero(valor));
    }

    private static String repetir(char caractere, int vezes) {
        return String.valueOf(caractere).repeat(vezes);
    }

    // ------------------------------------------------------------------------
    // Resumo de cada entidade em uma linha (listas de selecao)
    // ------------------------------------------------------------------------

    public static String resumo(Usuario u) {
        return "#" + u.getIdUsuario() + " " + u.getNome() + " - CPF " + u.getCpf();
    }

    public static String resumo(Empresa e) {
        return "#" + e.getIdEmpresa() + " " + e.getNome() + " - CNPJ " + e.getCnpj()
                + " (dono: " + e.getDono().getNome() + ")";
    }

    public static String resumo(Criptoativo c) {
        return "#" + c.getIdCripto() + " " + c.getSigla() + " - " + c.getNome() + " (" + preco(c.getPrecoAtual()) + ")";
    }

    public static String resumo(Carteira c) {
        return "#" + c.getIdCarteira() + " [" + c.getTipo() + "] " + c.getDescricao() + " - saldo " + reais(c.getSaldoReais());
    }

    public static String resumo(Transacao t) {
        return "#" + t.getIdTransacao() + " " + data(t.getDataOperacao()) + " " + t.getTipo() + " "
                + cripto(t.getQuantidade()) + " " + t.getCriptoativo().getSigla()
                + " - carteira #" + t.getCarteira().getIdCarteira();
    }

    public static String resumo(Posicao p) {
        return "#" + p.getIdPosicao() + " carteira #" + p.getCarteira().getIdCarteira() + " - "
                + cripto(p.getQuantidadeAtual()) + " " + p.getCriptoativo().getSigla();
    }

    public static String resumo(Alerta a) {
        return "#" + a.getIdAlerta() + " " + a.getUsuario().getNome() + " monitora " + a.getCriptoativo().getSigla()
                + " (limite " + percentual(a.getLimiteVariacao()) + ", " + (a.isAtivado() ? "ativo" : "inativo") + ")";
    }

    public static String resumo(Relatorio r) {
        return "#" + r.getIdRelatorio() + " " + data(r.getDataGeracao()) + " - " + r.getCarteira().getDescricao();
    }

    /** Atributo proprio da subclasse da carteira: limite diario (PF) ou regime tributario (PJ). */
    public static String atributoProprio(Carteira c) {
        if (c instanceof CarteiraPF) {
            return "limite " + reais(((CarteiraPF) c).getLimiteDiarioSaque()) + "/dia";
        }
        if (c instanceof CarteiraPJ) {
            return ((CarteiraPJ) c).getRegimeTributario();
        }
        return "-";
    }
}
