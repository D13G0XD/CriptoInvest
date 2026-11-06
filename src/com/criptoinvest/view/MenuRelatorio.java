package com.criptoinvest.view;

import com.criptoinvest.dao.RelatorioDAO;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Relatorio;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo RELATORIOS (tabela relatorio): retratos do desempenho de uma carteira.
 *
 * O relatorio e um snapshot imutavel: os numeros sao calculados uma vez, na
 * geracao, a partir do historico gravado da carteira (transacoes e posicoes).
 * Alterar significa gerar de novo o retrato, com os numeros e a data de hoje,
 * mantendo o mesmo ID - nao ha edicao manual dos valores.
 */
public class MenuRelatorio extends MenuBase {

    private final RelatorioDAO relatorioDAO = new RelatorioDAO();

    public MenuRelatorio(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Relatorios de desempenho";
    }

    @Override
    protected String[] getRotulos() {
        return new String[] {
                "Gerar relatorio de uma carteira (incluir)", "Atualizar relatorio com os numeros de hoje (alterar)",
                "Excluir", "Exibir todos", "Exibir um (por ID)"
        };
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Listar relatorios de uma carteira");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        imprimirTabela(relatorioDAO.listarPorCarteira(carteira.getIdCarteira()));
    }

    @Override
    protected void incluir() throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        carteiraDAO.carregarHistorico(carteira);
        Relatorio relatorio = new Relatorio(0, carteira, LocalDate.now());
        int id = relatorioDAO.inserir(relatorio);
        detalhar(relatorio);
        Tela.sucesso("Relatorio gravado com ID " + id + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Relatorio anterior = escolher("Relatorio a atualizar", relatorioDAO, Tela::resumo);
        detalhar(anterior);
        Tela.info("O relatorio e um retrato da carteira no dia em que foi gerado. Atualizar = gerar de novo o");
        Tela.info("retrato com os numeros de hoje, mantendo o mesmo ID.");
        if (!entrada.confirmar("Atualizar o relatorio #" + anterior.getIdRelatorio() + "?")) {
            Tela.aviso("Relatorio mantido como estava.");
            return;
        }

        Carteira carteira = anterior.getCarteira();
        carteiraDAO.carregarHistorico(carteira);
        Relatorio atualizado = new Relatorio(anterior.getIdRelatorio(), carteira, LocalDate.now());
        if (relatorioDAO.alterar(atualizado) == 0) {
            Tela.erro("O relatorio ja nao existe no banco.");
            return;
        }
        Tela.secao("Relatorio atualizado");
        detalhar(atualizado);
        Tela.sucesso("Relatorio #" + atualizado.getIdRelatorio() + " atualizado.");
    }

    @Override
    protected void excluir() throws SQLException {
        Relatorio relatorio = escolher("Relatorio a excluir", relatorioDAO, Tela::resumo);
        detalhar(relatorio);
        if (!confirmarExclusao("relatorio #" + relatorio.getIdRelatorio())) {
            return;
        }
        if (relatorioDAO.excluir(relatorio.getIdRelatorio()) == 0) {
            Tela.erro("O relatorio ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Relatorio #" + relatorio.getIdRelatorio() + " excluido.");
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(relatorioDAO.listarTodos());
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolher("ID do relatorio", relatorioDAO, Tela::resumo));
    }

    private static void imprimirTabela(List<Relatorio> relatorios) {
        if (relatorios.isEmpty()) {
            Tela.aviso("Nenhum relatorio encontrado.");
            return;
        }
        String formato = "%-5s %-10s %-8s %18s %18s %18s %12s%n";
        System.out.printf(formato, "ID", "DATA", "CARTEIRA", "INVESTIDO", "VALOR ATUAL", "LUCRO", "RENTAB.");
        Tela.separador(95);
        for (Relatorio r : relatorios) {
            System.out.printf(formato, r.getIdRelatorio(), Tela.data(r.getDataGeracao()),
                    "#" + r.getCarteira().getIdCarteira(), Tela.reais(r.getTotalInvestido()),
                    Tela.reais(r.getValorTotalCarteira()), Tela.reais(r.getLucroTotal()),
                    Tela.variacao(r.getRentabilidadePercentual()));
        }
        Tela.info(relatorios.size() + " relatorio(s).");
    }

    private static void detalhar(Relatorio r) {
        Tela.campo("ID", r.getIdRelatorio() == 0 ? "(novo)" : r.getIdRelatorio());
        Tela.campo("Data de geracao", Tela.data(r.getDataGeracao()));
        Tela.campo("Carteira", Tela.resumo(r.getCarteira()));
        Tela.campo("Total investido", Tela.reais(r.getTotalInvestido()));
        Tela.campo("Total vendido", Tela.reais(r.getTotalVendido()));
        Tela.campo("Valor atual em cripto", Tela.reais(r.getValorTotalCarteira()));
        Tela.campo("Taxas pagas", Tela.reais(r.getTotalTaxas()));
        Tela.campo("Lucro bruto (antes taxas)", Tela.reais(r.calcularLucroBruto()));
        Tela.campo("Lucro liquido", Tela.reais(r.getLucroTotal()));
        Tela.campo("Rentabilidade", Tela.variacao(r.getRentabilidadePercentual()));
    }
}
