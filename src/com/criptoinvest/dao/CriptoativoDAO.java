package com.criptoinvest.dao;

import com.criptoinvest.factory.ConnectionFactory;
import com.criptoinvest.model.Criptoativo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) da classe escolhida para a integracao com o banco: CRIPTOATIVO.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) os dados da
 * tabela CRIPTOATIVO, mapeando cada coluna para os atributos de Criptoativo.
 *
 * Tabela:
 *   criptoativo (id_cripto PK, nome, sigla UK, preco_atual, variacao_24h, categoria)
 * Sequence:
 *   seq_criptoativo -> geracao da chave primaria
 */
public class CriptoativoDAO {

    private static final String SQL_PROXIMO_ID =
            "SELECT seq_criptoativo.NEXTVAL FROM dual";

    private static final String SQL_INSERT =
            "INSERT INTO criptoativo (id_cripto, nome, sigla, preco_atual, variacao_24h, categoria) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE criptoativo "
          + "   SET nome = ?, sigla = ?, preco_atual = ?, variacao_24h = ?, categoria = ? "
          + " WHERE id_cripto = ?";

    private static final String SQL_DELETE =
            "DELETE FROM criptoativo WHERE id_cripto = ?";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id_cripto, nome, sigla, preco_atual, variacao_24h, categoria "
          + "  FROM criptoativo WHERE id_cripto = ?";

    private static final String SQL_SELECT_POR_SIGLA =
            "SELECT id_cripto, nome, sigla, preco_atual, variacao_24h, categoria "
          + "  FROM criptoativo WHERE UPPER(sigla) = UPPER(?)";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_cripto, nome, sigla, preco_atual, variacao_24h, categoria "
          + "  FROM criptoativo ORDER BY id_cripto";

    // ---------------  ---------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere um criptoativo na tabela. Quando o id do objeto for menor ou igual
     * a zero, um novo id e obtido da sequence seq_criptoativo e atribuido ao objeto.
     *
     * @param criptoativo objeto a ser gravado
     * @return o id gravado no banco
     */
    public int inserir(Criptoativo criptoativo) throws SQLException {
        try (Connection conexao = ConnectionFactory.getConnection()) {

            int id = criptoativo.getIdCripto();
            if (id <= 0) {
                id = proximoId(conexao);
                criptoativo.setIdCripto(id);
            }

            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                ps.setInt(1, id);
                ps.setString(2, criptoativo.getNome());
                ps.setString(3, criptoativo.getSigla());
                ps.setBigDecimal(4, criptoativo.getPrecoAtual());
                ps.setBigDecimal(5, criptoativo.getVariacao24h());
                ps.setString(6, criptoativo.getCategoria());
                ps.executeUpdate();
            }
            return id;
        }
    }

    /** Obtem o proximo valor da sequence usada pela PK da tabela. */
    private int proximoId(Connection conexao) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_PROXIMO_ID);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Nao foi possivel obter o proximo valor de seq_criptoativo.");
        }
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza todos os atributos do criptoativo identificado pelo seu id.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     */
    public int alterar(Criptoativo criptoativo) throws SQLException {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, criptoativo.getNome());
            ps.setString(2, criptoativo.getSigla());
            ps.setBigDecimal(3, criptoativo.getPrecoAtual());
            ps.setBigDecimal(4, criptoativo.getVariacao24h());
            ps.setString(5, criptoativo.getCategoria());
            ps.setInt(6, criptoativo.getIdCripto());

            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui o criptoativo pelo id.
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    public int excluir(int idCripto) throws SQLException {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idCripto);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /** Busca um criptoativo pela chave primaria. Retorna null quando nao existe. */
    public Criptoativo buscarPorId(int idCripto) throws SQLException {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idCripto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarCriptoativo(rs) : null;
            }
        }
    }

    /** Busca um criptoativo pela sigla (chave unica). Retorna null quando nao existe. */
    public Criptoativo buscarPorSigla(String sigla) throws SQLException {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_SIGLA)) {

            ps.setString(1, sigla);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarCriptoativo(rs) : null;
            }
        }
    }

    /** Lista todos os criptoativos cadastrados, ordenados pelo id. */
    public List<Criptoativo> listarTodos() throws SQLException {
        List<Criptoativo> criptoativos = new ArrayList<>();

        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                criptoativos.add(montarCriptoativo(rs));
            }
        }
        return criptoativos;
    }

    /**
     * Converte a linha atual do ResultSet em um objeto Criptoativo.
     * Usa getBigDecimal, e nao getDouble: NUMBER(18,8) nao cabe em double sem
     * perda, e o dominio trabalha em BigDecimal (ver Valores).
     */
    private Criptoativo montarCriptoativo(ResultSet rs) throws SQLException {
        BigDecimal preco = rs.getBigDecimal("preco_atual");
        BigDecimal variacao = rs.getBigDecimal("variacao_24h");

        Criptoativo criptoativo = new Criptoativo(
                rs.getInt("id_cripto"),
                rs.getString("nome"),
                rs.getString("sigla"),
                preco,
                rs.getString("categoria"));

        criptoativo.setVariacao24h(variacao);
        return criptoativo;
    }
}
