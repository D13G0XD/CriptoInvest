package com.criptoinvest.dao;

import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Valores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da tabela CARTEIRA_PJ (filha da heranca Joined) junto com a linha pai
 * em CARTEIRA: toda gravacao toca as duas tabelas na mesma transacao.
 * Espelho do CarteiraPFDAO, com regime_tributario no lugar do limite de saque.
 *
 * Tabelas:
 *   carteira    (id_carteira PK, descricao, saldo_reais, tipo = 'PJ')
 *   carteira_pj (id_carteira_pj PK e FK -> carteira, regime_tributario)
 * Sequence:
 *   seq_carteira (obtida por CarteiraDAO.inserirBase; a filha repete a chave)
 *
 * As versoes de pacote de inserir, alterar e excluir, que recebem a Connection,
 * sao usadas pelo EmpresaDAO (empresa 1:1 carteira_pj) e o inserir tambem pelo
 * CarteiraDAO, dentro da transacao deles. montar(ResultSet) e o mapeamento
 * UNICO linha -> CarteiraPJ (com a conferencia tipo = 'PJ'), usado tambem por
 * CarteiraDAO e EmpresaDAO para que as leituras nao divirjam.
 */
public class CarteiraPJDAO extends BaseDAO implements CrudDAO<CarteiraPJ> {

    private static final String SQL_INSERT =
            "INSERT INTO carteira_pj (id_carteira_pj, regime_tributario) VALUES (?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE carteira_pj SET regime_tributario = ? WHERE id_carteira_pj = ?";

    private static final String SQL_DELETE =
            "DELETE FROM carteira_pj WHERE id_carteira_pj = ?";

    private static final String SQL_SELECT =
            "SELECT c.id_carteira, c.descricao, c.saldo_reais, c.tipo, pj.regime_tributario "
          + "  FROM carteira c "
          + "  JOIN carteira_pj pj ON pj.id_carteira_pj = c.id_carteira";

    private static final String SQL_SELECT_POR_ID =
            SQL_SELECT + " WHERE c.id_carteira = ?";

    private static final String SQL_SELECT_TODOS =
            SQL_SELECT + " ORDER BY c.id_carteira";

    /** Grava e le a linha pai (carteira). Nao guarda DAOs que guardem este, entao nao ha ciclo. */
    private final CarteiraDAO carteiraDAO = new CarteiraDAO();

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere a carteira PJ nova (id 0) numa transacao: linha pai em carteira e
     * linha filha em carteira_pj, com o mesmo id de seq_carteira. Se algo
     * falhar, a transacao e desfeita e o id volta a 0.
     *
     * @return o id gerado por seq_carteira
     * @throws IllegalStateException quando a carteira ja tem id (use alterar)
     */
    @Override
    public int inserir(CarteiraPJ carteira) throws SQLException {
        exigirCarteira(carteira);
        if (carteira.getIdCarteira() > 0) {
            throw new IllegalStateException("CarteiraPJ ja persistida (id="
                    + carteira.getIdCarteira() + "): use alterar");
        }

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int id = inserir(conexao, carteira);
                conexao.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                carteira.setIdCarteira(0);
                throw e;
            }
        }
    }

    /**
     * Versao de pacote, com a conexao recebida (usada por EmpresaDAO e
     * CarteiraDAO): grava carteira (CarteiraDAO.inserirBase, que atribui o id)
     * e carteira_pj. Quem controla a transacao desfaz e devolve 0 ao id se
     * algo falhar.
     *
     * @return o id gerado por seq_carteira
     */
    int inserir(Connection conexao, CarteiraPJ carteira) throws SQLException {
        int id = carteiraDAO.inserirBase(conexao, carteira);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, id);
            ps.setString(2, carteira.getRegimeTributario());
            ps.executeUpdate();
        }
        return id;
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza descricao e saldo (carteira) e o regime tributario (carteira_pj)
     * numa transacao.
     *
     * @return quantidade de linhas alteradas em carteira_pj (0 = id inexistente)
     */
    @Override
    public int alterar(CarteiraPJ carteira) throws SQLException {
        exigirCarteira(carteira);
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int linhas = alterar(conexao, carteira);
                conexao.commit();
                return linhas;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                throw e;
            }
        }
    }

    /**
     * Versao de pacote, com a conexao recebida (usada por EmpresaDAO): UPDATE
     * da linha pai e depois da filha. Se a carteira existe mas nao tem linha em
     * carteira_pj (e uma PF), lanca SQLException para quem controla a
     * transacao desfazer o UPDATE da linha pai.
     *
     * @return quantidade de linhas alteradas em carteira_pj (0 = id inexistente)
     */
    int alterar(Connection conexao, CarteiraPJ carteira) throws SQLException {
        if (carteiraDAO.alterarBase(conexao, carteira) == 0) {
            return 0;
        }

        try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, carteira.getRegimeTributario());
            ps.setInt(2, carteira.getIdCarteira());
            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Carteira " + carteira.getIdCarteira()
                        + " nao e uma carteira PJ (nao ha linha em carteira_pj).");
            }
            return linhas;
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a carteira PJ numa transacao: carteira_pj e depois carteira. Se a
     * empresa titular (ou transacao, posicao, relatorio) ainda apontar para
     * ela, o Oracle recusa com ORA-02292: a transacao e desfeita e a excecao
     * relancada.
     *
     * @return quantidade de linhas excluidas de carteira_pj (0 = id inexistente)
     */
    @Override
    public int excluir(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int linhas = excluir(conexao, idCarteira);
                conexao.commit();
                return linhas;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                throw e;
            }
        }
    }

    /**
     * Versao de pacote, com a conexao recebida (usada por EmpresaDAO depois de
     * apagar a empresa): DELETE em carteira_pj e, so se ela existia, na linha
     * pai - assim um id de carteira PF nunca e apagado por este DAO.
     *
     * @return quantidade de linhas excluidas de carteira_pj
     */
    int excluir(Connection conexao, int idCarteira) throws SQLException {
        int linhas;
        try (PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, idCarteira);
            linhas = ps.executeUpdate();
        }

        if (linhas > 0) {
            carteiraDAO.excluirBase(conexao, idCarteira);
        }
        return linhas;
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /** Busca a carteira PJ pelo id (sem o historico). Retorna null quando nao existe. */
    @Override
    public CarteiraPJ buscarPorId(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return buscarPorId(conexao, idCarteira);
        }
    }

    /**
     * Versao de pacote, com a conexao recebida, para ler a carteira PJ dentro da
     * conexao/transacao de outro DAO. O EmpresaDAO nao precisa dela: le a
     * carteira no proprio JOIN e a monta com montar(ResultSet).
     */
    CarteiraPJ buscarPorId(Connection conexao, int idCarteira) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idCarteira);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    /** Lista todas as carteiras PJ, ordenadas pelo id. */
    @Override
    public List<CarteiraPJ> listarTodos() throws SQLException {
        List<CarteiraPJ> carteiras = new ArrayList<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                carteiras.add(montar(rs));
            }
        }
        return carteiras;
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Converte a linha atual em CarteiraPJ, com a escala do saldo normalizada.
     * Mapeamento unico da carteira PJ, usado tambem por CarteiraDAO e
     * EmpresaDAO: a consulta precisa trazer as colunas id_carteira, descricao,
     * saldo_reais e tipo (de carteira) e regime_tributario (de carteira_pj).
     * Recusa a linha cuja carteira pai nao tem tipo PJ (heranca incoerente no banco).
     */
    static CarteiraPJ montar(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_carteira");
        String tipo = rs.getString("tipo");
        if (tipo == null || !"PJ".equals(tipo.trim())) {
            throw new SQLException("Carteira " + id
                    + " inconsistente: tem linha em carteira_pj, mas o tipo e " + tipo + ".");
        }

        return new CarteiraPJ(id,
                rs.getString("descricao"),
                Valores.dinheiro(rs.getBigDecimal("saldo_reais")),
                rs.getString("regime_tributario"));
    }

    /** Recusa carteira nula com mensagem clara (em vez de NullPointerException). */
    private static void exigirCarteira(CarteiraPJ carteira) {
        if (carteira == null) {
            throw new IllegalArgumentException("Carteira PJ nula.");
        }
    }
}
