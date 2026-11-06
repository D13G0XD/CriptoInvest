package com.criptoinvest.view;

import com.criptoinvest.dao.PosicaoDAO;
import com.criptoinvest.dao.TransacaoDAO;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Posicao;
import com.criptoinvest.model.Transacao;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo TRANSACOES (tabela transacao): compras e vendas de criptoativos.
 *
 * Incluir = registrar a operacao pela regra da carteira
 * (Carteira.registrarTransacao) e gravar transacao, posicao e saldo numa
 * unica transacao JDBC (TransacaoDAO.registrar). Antes de confirmar, o usuario
 * ve o resumo (bruto, taxa de 0,1% e saldo resultante).
 *
 * Validacoes: quantidade e preco maiores que zero; compra limitada ao saldo em
 * reais; venda limitada a quantidade em custodia; data entre 2009 e hoje;
 * observacao ate 200 bytes.
 *
 * Tipo, criptoativo, quantidade e preco nao sao alterados (ja movimentaram o
 * saldo e a posicao): alterar muda so a data e a observacao. Excluir e um
 * ESTORNO (TransacaoDAO.estornar), que desfaz o efeito no saldo e na posicao.
 */
public class MenuTransacao extends MenuBase {

    private static final List<String> TIPOS = List.of("COMPRA", "VENDA");
    private static final BigDecimal SALDO_MAXIMO = new BigDecimal("9999999999999.99");

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final PosicaoDAO posicaoDAO = new PosicaoDAO();

    public MenuTransacao(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Transacoes (compra e venda)";
    }

    @Override
    protected String[] getRotulos() {
        return new String[] {
                "Registrar compra ou venda (incluir)", "Alterar data / observacao",
                "Estornar transacao (excluir)", "Exibir todas", "Exibir uma (por ID)"
        };
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Extrato de uma carteira");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        Tela.info("Extrato de " + Tela.resumo(carteira));
        List<Transacao> transacoes = transacaoDAO.listarPorCarteira(carteira.getIdCarteira());
        imprimirTabela(transacoes);

        BigDecimal compras = BigDecimal.ZERO;
        BigDecimal vendas = BigDecimal.ZERO;
        BigDecimal taxas = BigDecimal.ZERO;
        for (Transacao t : transacoes) {
            if ("COMPRA".equals(t.getTipo())) {
                compras = compras.add(t.calcularValorComTaxa());
            } else {
                vendas = vendas.add(t.calcularValorComTaxa());
            }
            taxas = taxas.add(t.getTaxa());
        }
        if (!transacoes.isEmpty()) {
            Tela.campo("Total em compras", Tela.reais(compras));
            Tela.campo("Total em vendas", Tela.reais(vendas));
            Tela.campo("Taxas pagas", Tela.reais(taxas));
        }
    }

    @Override
    protected void incluir() throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        carteiraDAO.carregarHistorico(carteira);
        Tela.campo("Saldo disponivel", Tela.reais(carteira.getSaldoReais()));

        boolean compra = entrada.lerEscolha("Tipo da operacao", TIPOS, -1) == 0;
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo");
        String sigla = criptoativo.getSigla();

        Posicao posicao = carteira.buscarPosicao(criptoativo.getIdCripto());
        BigDecimal emCustodia = posicao == null ? Valores.ZERO_CRIPTO : posicao.getQuantidadeAtual();
        Tela.campo("Em custodia", Tela.cripto(emCustodia) + " " + sigla);
        if (!compra && emCustodia.signum() == 0) {
            Tela.erro("A carteira nao tem " + sigla + " para vender.");
            return;
        }

        BigDecimal quantidade = entrada.lerCripto("Quantidade de " + sigla, false, null);
        while (!compra && Valores.maior(quantidade, emCustodia)) {
            Tela.erro("Quantidade maior que a disponivel em custodia (" + Tela.cripto(emCustodia) + ").");
            quantidade = entrada.lerCripto("Quantidade de " + sigla, false, null);
        }
        // a cotacao atual e sugerida como preco; se ela for zero, o preco e obrigatorio
        BigDecimal cotacao = criptoativo.getPrecoAtual().signum() > 0 ? criptoativo.getPrecoAtual() : null;
        BigDecimal preco = entrada.lerCripto("Preco unitario em R$", false, cotacao);
        LocalDate data = entrada.lerData("Data da operacao", LocalDate.now());
        String observacao = entrada.lerTextoOpcional("Observacao", Transacao.TAMANHO_MAXIMO_OBSERVACAO);

        Transacao transacao = new Transacao(0, compra ? "COMPRA" : "VENDA", criptoativo, quantidade, preco, data);
        if (Valores.zero(transacao.calcularValorBruto())) {
            // quantidade x preco arredondado a centavos daria R$ 0,00: uma compra sairia de graca
            Tela.erro("Operacao abaixo de R$ 0,01: aumente a quantidade ou o preco.");
            return;
        }
        BigDecimal total = transacao.calcularValorComTaxa();
        BigDecimal saldoApos = compra
                ? carteira.getSaldoReais().subtract(total)
                : carteira.getSaldoReais().add(total);

        Tela.secao("Resumo da operacao");
        Tela.campo("Operacao", transacao.getTipo() + " de " + Tela.cripto(quantidade) + " " + sigla);
        Tela.campo("Preco unitario", Tela.preco(preco));
        Tela.campo("Valor bruto", Tela.reais(transacao.calcularValorBruto()));
        Tela.campo("Taxa (0,1%)", Tela.reais(transacao.getTaxa()));
        Tela.campo(compra ? "Total a debitar" : "Total a creditar", Tela.reais(total));
        Tela.campo("Saldo apos a operacao", Tela.reais(saldoApos));

        if (saldoApos.signum() < 0) {
            Tela.erro("Saldo insuficiente: faltam " + Tela.reais(saldoApos.negate())
                    + ". Faca um deposito em Carteiras > Depositar.");
            return;
        }
        if (Valores.maior(saldoApos, SALDO_MAXIMO)) {
            Tela.erro("O saldo passaria do maximo permitido (" + Tela.reais(SALDO_MAXIMO) + ").");
            return;
        }
        if (!entrada.confirmar("Confirma a operacao?")) {
            Tela.aviso("Operacao nao registrada.");
            return;
        }

        if (!carteira.registrarTransacao(transacao, observacao)) {
            Tela.erro("A carteira recusou a operacao: nada foi gravado.");
            return;
        }
        int id = transacaoDAO.registrar(transacao);
        Posicao depois = carteira.buscarPosicao(criptoativo.getIdCripto());
        Tela.sucesso("Transacao #" + id + " registrada. Saldo: " + Tela.reais(carteira.getSaldoReais())
                + " | " + sigla + " em custodia: " + Tela.cripto(depois.getQuantidadeAtual()) + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Transacao transacao = escolher("Transacao a alterar", transacaoDAO, Tela::resumo);
        detalhar(transacao);
        Tela.info("Tipo, criptoativo, quantidade e preco nao podem ser alterados: eles ja movimentaram o");
        Tela.info("saldo e a posicao. Para corrigi-los, estorne a transacao e registre-a de novo.");
        System.out.println();

        transacao.setDataOperacao(entrada.lerData("Data da operacao", transacao.getDataOperacao()));
        transacao.setObservacao(entrada.lerTextoOpcional("Observacao", Transacao.TAMANHO_MAXIMO_OBSERVACAO,
                transacao.getObservacao()));

        if (transacaoDAO.alterar(transacao) == 0) {
            Tela.erro("A transacao ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Transacao #" + transacao.getIdTransacao() + " alterada.");
    }

    /** Exclusao por estorno: apaga a transacao e desfaz o efeito no saldo e na posicao, atomicamente. */
    @Override
    protected void excluir() throws SQLException {
        Transacao transacao = escolher("Transacao a estornar", transacaoDAO, Tela::resumo);
        detalhar(transacao);

        String sigla = transacao.getCriptoativo().getSigla();
        String quantidade = Tela.cripto(transacao.getQuantidade()) + " " + sigla;
        String valor = Tela.reais(transacao.calcularValorComTaxa());
        Tela.info("O estorno exclui a transacao e desfaz o seu efeito na carteira:");
        if ("COMPRA".equals(transacao.getTipo())) {
            Tela.info("  devolve " + valor + " ao saldo e retira " + quantidade + " da posicao.");
        } else {
            Tela.info("  retira " + valor + " do saldo e devolve " + quantidade + " a posicao.");
        }
        if (!entrada.confirmar("Confirma o estorno da transacao #" + transacao.getIdTransacao() + "?")) {
            Tela.aviso("Estorno cancelado: nada foi alterado.");
            return;
        }

        if (transacaoDAO.estornar(transacao.getIdTransacao()) == 0) {
            Tela.erro("A transacao ja nao existe no banco.");
            return;
        }
        int idCarteira = transacao.getCarteira().getIdCarteira();
        Carteira carteira = carteiraDAO.buscarPorId(idCarteira);
        Posicao posicao = posicaoDAO.buscarPorCarteiraECripto(idCarteira, transacao.getCriptoativo().getIdCripto());
        Tela.sucesso("Transacao #" + transacao.getIdTransacao() + " estornada. Saldo: "
                + Tela.reais(carteira.getSaldoReais()) + " | " + sigla + " em custodia: "
                + (posicao == null ? "0 (posicao encerrada)" : Tela.cripto(posicao.getQuantidadeAtual())) + ".");
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(transacaoDAO.listarTodos());
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolher("ID da transacao", transacaoDAO, Tela::resumo));
    }

    private static void imprimirTabela(List<Transacao> transacoes) {
        if (transacoes.isEmpty()) {
            Tela.aviso("Nenhuma transacao encontrada.");
            return;
        }
        String formato = "%-5s %-10s %-8s %-6s %-6s %16s %22s %18s%n";
        System.out.printf(formato, "ID", "DATA", "CARTEIRA", "TIPO", "CRIPTO", "QUANTIDADE", "PRECO UNITARIO", "TOTAL C/ TAXA");
        Tela.separador(100);
        for (Transacao t : transacoes) {
            System.out.printf(formato, t.getIdTransacao(), Tela.data(t.getDataOperacao()),
                    "#" + t.getCarteira().getIdCarteira(), t.getTipo(), t.getCriptoativo().getSigla(),
                    Tela.cripto(t.getQuantidade()), Tela.preco(t.getPrecoUnitario()), Tela.reais(t.calcularValorComTaxa()));
        }
        Tela.info(transacoes.size() + " transacao(oes).");
    }

    private static void detalhar(Transacao t) {
        Tela.campo("ID", t.getIdTransacao());
        Tela.campo("Data", Tela.data(t.getDataOperacao()));
        Tela.campo("Carteira", Tela.resumo(t.getCarteira()));
        Tela.campo("Tipo", t.getTipo());
        Tela.campo("Criptoativo", t.getCriptoativo().getSigla() + " - " + t.getCriptoativo().getNome());
        Tela.campo("Quantidade", Tela.cripto(t.getQuantidade()));
        Tela.campo("Preco unitario", Tela.preco(t.getPrecoUnitario()));
        Tela.campo("Valor bruto", Tela.reais(t.calcularValorBruto()));
        Tela.campo("Taxa (0,1%)", Tela.reais(t.getTaxa()));
        Tela.campo("COMPRA".equals(t.getTipo()) ? "Total debitado" : "Total creditado", Tela.reais(t.calcularValorComTaxa()));
        Tela.campo("Observacao", t.getObservacao());
    }
}
