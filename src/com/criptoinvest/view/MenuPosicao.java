package com.criptoinvest.view;

import com.criptoinvest.dao.PosicaoDAO;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Posicao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo POSICOES (tabela posicao): quanto de cada criptoativo uma carteira
 * tem em custodia, com o preco medio de compra.
 *
 * As compras e vendas (menu Transacoes) atualizam as posicoes sozinhas. Este
 * modulo cobre o que acontece fora delas: registrar uma custodia que a
 * carteira ja possuia (transferencia de outra corretora, por exemplo) e
 * ajustar quantidade ou preco medio numa conciliacao - sem gerar transacao e
 * sem mexer no saldo em reais.
 *
 * Validacoes: uma posicao por par carteira x criptoativo, quantidade e preco
 * medio nao negativos com ate 8 casas, data entre 2009 e hoje.
 */
public class MenuPosicao extends MenuBase {

    private final PosicaoDAO posicaoDAO = new PosicaoDAO();

    public MenuPosicao(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Posicoes (custodia de criptoativos)";
    }

    @Override
    protected String[] getRotulos() {
        return new String[] {
                "Incluir posicao (custodia ja existente)", "Alterar (ajuste de quantidade / preco medio)",
                "Excluir", "Exibir todas", "Exibir uma (por ID)"
        };
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Listar posicoes de uma carteira");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        imprimirTabela(posicaoDAO.listarPorCarteira(carteira.getIdCarteira()));
    }

    @Override
    protected void incluir() throws SQLException {
        Tela.info("Compras e vendas atualizam as posicoes automaticamente (menu Transacoes). Use esta opcao");
        Tela.info("para registrar criptoativos que a carteira ja possuia, sem movimentar o saldo em reais.");
        Carteira carteira = escolherCarteira("ID da carteira");
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo");

        Posicao existente = posicaoDAO.buscarPorCarteiraECripto(carteira.getIdCarteira(), criptoativo.getIdCripto());
        if (existente != null) {
            Tela.erro("A carteira ja tem posicao de " + criptoativo.getSigla() + " (ID " + existente.getIdPosicao()
                    + "): use Alterar para ajusta-la ou registre uma compra.");
            return;
        }

        BigDecimal quantidade = entrada.lerCripto("Quantidade", false, null);
        BigDecimal precoMedio = entrada.lerCripto("Preco medio de compra em R$", true, criptoativo.getPrecoAtual());
        LocalDate aquisicao = entrada.lerData("Data da primeira aquisicao", LocalDate.now());

        Posicao posicao = new Posicao(carteira, criptoativo, quantidade, precoMedio, aquisicao);
        int id = posicaoDAO.inserir(posicao);
        Tela.sucesso("Posicao cadastrada com ID " + id + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Posicao posicao = escolher("Posicao a alterar", posicaoDAO, Tela::resumo);
        detalhar(posicao);
        Tela.info("Ajuste manual: corrige a custodia sem gerar transacao e sem mexer no saldo em reais.");
        System.out.println();

        posicao.setQuantidadeAtual(entrada.lerCripto("Quantidade atual", true, posicao.getQuantidadeAtual()));
        posicao.setPrecoMedioCompra(entrada.lerCripto("Preco medio de compra em R$", true, posicao.getPrecoMedioCompra()));
        posicao.setDataUltimaAtualizacao(LocalDate.now());

        if (posicaoDAO.alterar(posicao) == 0) {
            Tela.erro("A posicao ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Posicao #" + posicao.getIdPosicao() + " alterada.");
    }

    @Override
    protected void excluir() throws SQLException {
        Posicao posicao = escolher("Posicao a excluir", posicaoDAO, Tela::resumo);
        detalhar(posicao);
        if (posicao.getQuantidadeAtual().signum() > 0) {
            Tela.aviso("A posicao ainda tem " + Tela.cripto(posicao.getQuantidadeAtual()) + " "
                    + posicao.getCriptoativo().getSigla() + " (" + Tela.reais(posicao.calcularValorAtual())
                    + "), que deixarao de constar na carteira.");
        }
        if (!confirmarExclusao("posicao #" + posicao.getIdPosicao())) {
            return;
        }
        if (posicaoDAO.excluir(posicao.getIdPosicao()) == 0) {
            Tela.erro("A posicao ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Posicao #" + posicao.getIdPosicao() + " excluida.");
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(posicaoDAO.listarTodos());
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolher("ID da posicao", posicaoDAO, Tela::resumo));
    }

    private static void imprimirTabela(List<Posicao> posicoes) {
        if (posicoes.isEmpty()) {
            Tela.aviso("Nenhuma posicao encontrada.");
            return;
        }
        String formato = "%-5s %-8s %-6s %16s %22s %18s %18s%n";
        System.out.printf(formato, "ID", "CARTEIRA", "CRIPTO", "QUANTIDADE", "PRECO MEDIO", "VALOR ATUAL", "LUCRO N/ REAL.");
        Tela.separador(98);
        for (Posicao p : posicoes) {
            System.out.printf(formato, p.getIdPosicao(), "#" + p.getCarteira().getIdCarteira(),
                    p.getCriptoativo().getSigla(), Tela.cripto(p.getQuantidadeAtual()), Tela.preco(p.getPrecoMedioCompra()),
                    Tela.reais(p.calcularValorAtual()), Tela.reais(p.calcularLucroNaoRealizado()));
        }
        Tela.info(posicoes.size() + " posicao(oes).");
    }

    private static void detalhar(Posicao p) {
        Tela.campo("ID", p.getIdPosicao());
        Tela.campo("Carteira", Tela.resumo(p.getCarteira()));
        Tela.campo("Criptoativo", p.getCriptoativo().getSigla() + " - " + p.getCriptoativo().getNome());
        Tela.campo("Quantidade", Tela.cripto(p.getQuantidadeAtual()));
        Tela.campo("Preco medio de compra", Tela.preco(p.getPrecoMedioCompra()));
        Tela.campo("Cotacao atual", Tela.preco(p.getCriptoativo().getPrecoAtual()));
        Tela.campo("Valor atual", Tela.reais(p.calcularValorAtual()));
        Tela.campo("Lucro nao realizado", Tela.reais(p.calcularLucroNaoRealizado()));
        Tela.campo("Primeira aquisicao", Tela.data(p.getDataPrimeiraAquisicao()));
        Tela.campo("Ultima atualizacao", Tela.data(p.getDataUltimaAtualizacao()));
    }
}
