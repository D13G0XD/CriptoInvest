package com.criptoinvest.dao;

import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Relatorio;
import com.criptoinvest.model.Valores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO (Data Access Object) da tabela RELATORIO.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) os
 * relatorios, snapshots de desempenho de uma carteira na data de geracao.
 *
 * Tabela:
 *   relatorio (id_relatorio PK, id_carteira FK polimorfica, data_geracao,
 *              valor_total_carteira, total_investido, total_vendido, total_taxas,
 *              lucro_total, rentabilidade_percentual)
 * Sequence:
 *   seq_relatorio -> geracao da chave primaria
 *
 * O relatorio e imutavel: o DAO grava os numeros calculados quando o objeto foi
 * criado e, na leitura, usa o construtor de reconstrucao com os valores
 * gravados, sem recalcular nada a partir da carteira. A carteira vem de
 * CarteiraDAO.buscarPorId(Connection, int) (PF ou PJ, sem o historico de
 * transacoes e posicoes; use CarteiraDAO.carregarHistorico quando precisar).
 */
public class RelatorioDAO extends BaseDAO implements CrudDAO<Relatorio> {

    private static final String SEQUENCE = "seq_relatorio";

    private static final String SQL_INSERT =
            "INSERT INTO relatorio (id_relatorio, id_carteira, data_geracao, valor_total_carteira, "
          + "                       total_investido, total_vendido, total_taxas, lucro_total, "
          + "                       rentabilidade_percentual) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE relatorio "
          + "   SET id_carteira = ?, data_geracao = ?, valor_total_carteira = ?, total_investido = ?, "
          + "       total_vendido = ?, total_taxas = ?, lucro_total = ?, rentabilidade_percentual = ? "
          + " WHERE id_relatorio = ?";

    private static final String SQL_DELETE =
            "DELETE FROM relatorio WHERE id_relatorio = ?";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, "
          + "       total_vendido, total_taxas, lucro_total, rentabilidade_percentual "
          + "  FROM relatorio WHERE id_relatorio = ?";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, "
          + "       total_vendido, total_taxas, lucro_total, rentabilidade_percentual "
          + "  FROM relatorio ORDER BY id_relatorio";

    private static final String SQL_SELECT_POR_CARTEIRA =
            "SELECT id_relatorio, id_carteira, data_geracao, valor_total_carteira, total_investido, "
          + "       total_vendido, total_taxas, lucro_total, rentabilidade_percentual "
          + "  FROM relatorio WHERE id_carteira = ? ORDER BY id_relatorio";

    private final CarteiraDAO carteiraDAO = new CarteiraDAO();

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Grava o snapshot de um relatorio ainda nao persistido (id 0). A carteira
     * precisa ja estar gravada. O id vem da sequence seq_relatorio e so e
     * atribuido ao objeto depois do INSERT.
     *
     * @return o id gerado pela sequence
     * @throws IllegalStateException quando o relatorio ja tem id, ou quando a
     *         carteira ainda nao foi persistida
     */
    @Override
    public int inserir(Relatorio relatorio) throws SQLException {
        if (relatorio.getIdRelatorio() > 0) {
            throw new IllegalStateException("Relatorio ja persistido (id="
                    + relatorio.getIdRelatorio() + "): use alterar");
        }
        validarCarteira(relatorio);

        try (Connection conexao = abrirConexao()) {
            int id = proximoId(conexao, SEQUENCE);

            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                ps.setInt(1, id);
                ps.setInt(2, relatorio.getCarteira().getIdCarteira());
                ps.setDate(3, paraSql(relatorio.getDataGeracao()));
                ps.setBigDecimal(4, relatorio.getValorTotalCarteira());
                ps.setBigDecimal(5, relatorio.getTotalInvestido());
                ps.setBigDecimal(6, relatorio.getTotalVendido());
                ps.setBigDecimal(7, relatorio.getTotalTaxas());
                ps.setBigDecimal(8, relatorio.getLucroTotal());
                ps.setBigDecimal(9, relatorio.getRentabilidadePercentual());
                ps.executeUpdate();
            }

            relatorio.setIdRelatorio(id);
            return id;
        }
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Grava por cima, pelo id, todas as colunas do relatorio. Como o objeto e
     * imutavel, o uso esperado e gerar de novo o snapshot da mesma carteira com
     * new Relatorio(idExistente, carteira, data) e passa-lo para este metodo.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     * @throws IllegalStateException quando a carteira ainda nao foi persistida
     */
    @Override
    public int alterar(Relatorio relatorio) throws SQLException {
        validarCarteira(relatorio);

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, relatorio.getCarteira().getIdCarteira());
            ps.setDate(2, paraSql(relatorio.getDataGeracao()));
            ps.setBigDecimal(3, relatorio.getValorTotalCarteira());
            ps.setBigDecimal(4, relatorio.getTotalInvestido());
            ps.setBigDecimal(5, relatorio.getTotalVendido());
            ps.setBigDecimal(6, relatorio.getTotalTaxas());
            ps.setBigDecimal(7, relatorio.getLucroTotal());
            ps.setBigDecimal(8, relatorio.getRentabilidadePercentual());
            ps.setInt(9, relatorio.getIdRelatorio());

            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui o relatorio pelo id. Nenhuma tabela referencia relatorio, entao a
     * exclusao nao depende de outras linhas.
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    @Override
    public int excluir(int idRelatorio) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idRelatorio);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /**
     * Busca um relatorio pela chave primaria, com os valores gravados e a
     * carteira (PF ou PJ) montada. Retorna null quando nao existe.
     */
    @Override
    public Relatorio buscarPorId(int idRelatorio) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idRelatorio);
            List<Relatorio> relatorios = lerRelatorios(conexao, ps);
            return relatorios.isEmpty() ? null : relatorios.get(0);
        }
    }

    /** Lista todos os relatorios gravados, ordenados pelo id. */
    @Override
    public List<Relatorio> listarTodos() throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS)) {

            return lerRelatorios(conexao, ps);
        }
    }

    /**
     * Lista o historico de relatorios de uma carteira, ordenado pelo id. A
     * carteira e lida uma vez e compartilhada por todos os relatorios da lista.
     * Carteira inexistente ou sem relatorios devolve lista vazia.
     */
    public List<Relatorio> listarPorCarteira(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA)) {

            ps.setInt(1, idCarteira);
            return lerRelatorios(conexao, ps);
        }
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Pre-condicao de FK: o relatorio precisa apontar para uma carteira ja
     * gravada (id > 0), senao o INSERT/UPDATE violaria fk_relatorio_carteira.
     */
    private static void validarCarteira(Relatorio relatorio) {
        if (relatorio.getCarteira().getIdCarteira() <= 0) {
            throw new IllegalStateException(
                    "Relatorio exige uma carteira ja persistida: grave a carteira antes do relatorio.");
        }
    }

    /**
     * Executa a consulta ja parametrizada e monta os relatorios. As carteiras
     * ficam guardadas por id durante a leitura, para que cada uma seja buscada
     * uma unica vez e o mesmo objeto sirva a todos os relatorios dela.
     */
    private List<Relatorio> lerRelatorios(Connection conexao, PreparedStatement ps) throws SQLException {
        List<Relatorio> relatorios = new ArrayList<>();
        Map<Integer, Carteira> carteiras = new HashMap<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                relatorios.add(montarRelatorio(conexao, rs, carteiras));
            }
        }
        return relatorios;
    }

    /**
     * Converte a linha atual do ResultSet em um Relatorio pelo construtor de
     * reconstrucao (valores gravados, sem recalcular). As escalas sao
     * normalizadas aqui, porque o Oracle devolve o NUMBER sem a escala da
     * coluna; colunas NULL chegam como zero (Valores.ouZero).
     */
    private Relatorio montarRelatorio(Connection conexao, ResultSet rs,
                                      Map<Integer, Carteira> carteiras) throws SQLException {
        int idCarteira = rs.getInt("id_carteira");
        Carteira carteira = carteiras.get(idCarteira);
        if (carteira == null) {
            carteira = carteiraDAO.buscarPorId(conexao, idCarteira);
            carteiras.put(idCarteira, carteira);
        }

        return new Relatorio(
                rs.getInt("id_relatorio"),
                carteira,
                paraLocalDate(rs.getDate("data_geracao")),
                Valores.dinheiro(rs.getBigDecimal("valor_total_carteira")),
                Valores.dinheiro(rs.getBigDecimal("total_investido")),
                Valores.dinheiro(rs.getBigDecimal("total_vendido")),
                Valores.dinheiro(rs.getBigDecimal("total_taxas")),
                Valores.dinheiro(rs.getBigDecimal("lucro_total")),
                Valores.percentual(rs.getBigDecimal("rentabilidade_percentual")));
    }
}
