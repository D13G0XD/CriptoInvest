package com.criptoinvest.dao;

import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Valores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) da tabela CRIPTOATIVO.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) os dados da
 * tabela CRIPTOATIVO, mapeando cada coluna para os atributos de Criptoativo.
 *
 * Tabela:
 *   criptoativo (id_cripto PK, nome, sigla UK, preco_atual, variacao_24h, categoria)
 * Sequence:
 *   seq_criptoativo -> geracao da chave primaria
 *
 * Os outros DAOs montam o criptoativo das suas linhas (transacao, posicao,
 * alerta) com buscarPorId(Connection, int), reaproveitando a conexao deles.
 */
public class CriptoativoDAO extends BaseDAO implements CrudDAO<Criptoativo> {

    private static final String SEQUENCE = "seq_criptoativo";

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

    /** A sigla e gravada sempre em maiusculas (Criptoativo.normalizarSigla), entao a comparacao e direta. */
    private static final String SQL_SELECT_POR_SIGLA =
            "SELECT id_cripto, nome, sigla, preco_atual, variacao_24h, categoria "
          + "  FROM criptoativo WHERE sigla = ?";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_cripto, nome, sigla, preco_atual, variacao_24h, categoria "
          + "  FROM criptoativo ORDER BY id_cripto";

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere um criptoativo ainda nao persistido (id 0). O id vem sempre da
     * sequence seq_criptoativo e so e atribuido ao objeto depois do INSERT: se
     * o INSERT falhar (sigla repetida, por exemplo), o objeto continua com id 0.
     *
     * @param criptoativo objeto a ser gravado
     * @return o id gerado pela sequence
     * @throws IllegalStateException quando o objeto ja tem id (ja persistido: use alterar)
     */
    @Override
    public int inserir(Criptoativo criptoativo) throws SQLException {
        if (criptoativo.getIdCripto() > 0) {
            throw new IllegalStateException("Criptoativo ja persistido (id="
                    + criptoativo.getIdCripto() + "): use alterar");
        }

        try (Connection conexao = abrirConexao()) {
            int id = proximoId(conexao, SEQUENCE);

            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                ps.setInt(1, id);
                ps.setString(2, criptoativo.getNome());
                ps.setString(3, criptoativo.getSigla());
                ps.setBigDecimal(4, criptoativo.getPrecoAtual());
                ps.setBigDecimal(5, criptoativo.getVariacao24h());
                ps.setString(6, textoOuNulo(criptoativo.getCategoria()));
                ps.executeUpdate();
            }

            criptoativo.setIdCripto(id);
            return id;
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
    @Override
    public int alterar(Criptoativo criptoativo) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, criptoativo.getNome());
            ps.setString(2, criptoativo.getSigla());
            ps.setBigDecimal(3, criptoativo.getPrecoAtual());
            ps.setBigDecimal(4, criptoativo.getVariacao24h());
            ps.setString(5, textoOuNulo(criptoativo.getCategoria()));
            ps.setInt(6, criptoativo.getIdCripto());

            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui o criptoativo pelo id. Se houver transacao, posicao ou alerta
     * apontando para ele, o Oracle recusa com ORA-02292 (a excecao e relancada).
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    @Override
    public int excluir(int idCripto) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idCripto);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /** Busca um criptoativo pela chave primaria. Retorna null quando nao existe. */
    @Override
    public Criptoativo buscarPorId(int idCripto) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return buscarPorId(conexao, idCripto);
        }
    }

    /**
     * Versao de pacote, com a conexao recebida: usada pelos DAOs de transacao,
     * posicao e alerta para montar o criptoativo dentro da mesma conexao.
     */
    Criptoativo buscarPorId(Connection conexao, int idCripto) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idCripto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarCriptoativo(rs) : null;
            }
        }
    }

    /**
     * Busca um criptoativo pela sigla (chave unica). A sigla informada e
     * normalizada como no modelo (sem espacos, em maiusculas), entao "btc" acha
     * "BTC". Retorna null quando nao existe.
     */
    public Criptoativo buscarPorSigla(String sigla) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_SIGLA)) {

            ps.setString(1, Criptoativo.normalizarSigla(sigla));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarCriptoativo(rs) : null;
            }
        }
    }

    /** Lista todos os criptoativos cadastrados, ordenados pelo id. */
    @Override
    public List<Criptoativo> listarTodos() throws SQLException {
        List<Criptoativo> criptoativos = new ArrayList<>();

        try (Connection conexao = abrirConexao();
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
     * perda, e o dominio trabalha em BigDecimal (ver Valores). O Oracle devolve
     * o NUMBER sem a escala da coluna (350000 em vez de 350000.00000000), por
     * isso a escala e normalizada aqui, como no restante do dominio.
     */
    private Criptoativo montarCriptoativo(ResultSet rs) throws SQLException {
        Criptoativo criptoativo = new Criptoativo(
                rs.getInt("id_cripto"),
                rs.getString("nome"),
                rs.getString("sigla"),
                Valores.cripto(rs.getBigDecimal("preco_atual")),
                rs.getString("categoria"));

        criptoativo.setVariacao24h(Valores.percentual(rs.getBigDecimal("variacao_24h")));
        return criptoativo;
    }
}
