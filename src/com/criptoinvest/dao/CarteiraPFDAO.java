package com.criptoinvest.dao;

import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.Valores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO da tabela CARTEIRA_PF (filha da heranca Joined) junto com a linha pai
 * em CARTEIRA: toda gravacao toca as duas tabelas na mesma transacao.
 *
 * Tabelas:
 *   carteira    (id_carteira PK, descricao, saldo_reais, tipo = 'PF')
 *   carteira_pf (id_carteira_pf PK e FK -> carteira, limite_diario_saque)
 * Sequence:
 *   seq_carteira (obtida por CarteiraDAO.inserirBase; a filha repete a chave)
 *
 * As versoes de pacote de inserir, alterar e excluir, que recebem a Connection,
 * sao usadas pelo UsuarioDAO (usuario 1:1 carteira_pf) e o inserir tambem pelo
 * CarteiraDAO, dentro da transacao deles. montar(ResultSet) e o mapeamento
 * UNICO linha -> CarteiraPF (com a conferencia tipo = 'PF'), usado tambem por
 * CarteiraDAO e UsuarioDAO para que as leituras nao divirjam.
 * O controle de saques do dia (saquesHoje) nao tem coluna e nao e gravado.
 */
public class CarteiraPFDAO extends BaseDAO implements CrudDAO<CarteiraPF> {

    private static final String SQL_INSERT =
            "INSERT INTO carteira_pf (id_carteira_pf, limite_diario_saque) VALUES (?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE carteira_pf SET limite_diario_saque = ? WHERE id_carteira_pf = ?";

    private static final String SQL_DELETE =
            "DELETE FROM carteira_pf WHERE id_carteira_pf = ?";

    private static final String SQL_SELECT =
            "SELECT c.id_carteira, c.descricao, c.saldo_reais, c.tipo, pf.limite_diario_saque "
          + "  FROM carteira c "
          + "  JOIN carteira_pf pf ON pf.id_carteira_pf = c.id_carteira";

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
     * Insere a carteira PF nova (id 0) numa transacao: linha pai em carteira e
     * linha filha em carteira_pf, com o mesmo id de seq_carteira. Se algo
     * falhar, a transacao e desfeita e o id volta a 0.
     *
     * @return o id gerado por seq_carteira
     * @throws IllegalStateException quando a carteira ja tem id (use alterar)
     */
    @Override
    public int inserir(CarteiraPF carteira) throws SQLException {
        exigirCarteira(carteira);
        if (carteira.getIdCarteira() > 0) {
            throw new IllegalStateException("CarteiraPF ja persistida (id="
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
     * Versao de pacote, com a conexao recebida (usada por UsuarioDAO e
     * CarteiraDAO): grava carteira (CarteiraDAO.inserirBase, que atribui o id)
     * e carteira_pf. Quem controla a transacao desfaz e devolve 0 ao id se
     * algo falhar.
     *
     * @return o id gerado por seq_carteira
     */
    int inserir(Connection conexao, CarteiraPF carteira) throws SQLException {
        int id = carteiraDAO.inserirBase(conexao, carteira);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, id);
            ps.setBigDecimal(2, carteira.getLimiteDiarioSaque());
            ps.executeUpdate();
        }
        return id;
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza descricao e saldo (carteira) e o limite diario (carteira_pf)
     * numa transacao.
     *
     * @return quantidade de linhas alteradas em carteira_pf (0 = id inexistente)
     */
    @Override
    public int alterar(CarteiraPF carteira) throws SQLException {
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
     * Versao de pacote, com a conexao recebida (usada por UsuarioDAO): UPDATE
     * da linha pai e depois da filha. Se a carteira existe mas nao tem linha em
     * carteira_pf (e uma PJ), lanca SQLException para quem controla a
     * transacao desfazer o UPDATE da linha pai.
     *
     * @return quantidade de linhas alteradas em carteira_pf (0 = id inexistente)
     */
    int alterar(Connection conexao, CarteiraPF carteira) throws SQLException {
        if (carteiraDAO.alterarBase(conexao, carteira) == 0) {
            return 0;
        }

        try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
            ps.setBigDecimal(1, carteira.getLimiteDiarioSaque());
            ps.setInt(2, carteira.getIdCarteira());
            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new SQLException("Carteira " + carteira.getIdCarteira()
                        + " nao e uma carteira PF (nao ha linha em carteira_pf).");
            }
            return linhas;
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a carteira PF numa transacao: carteira_pf e depois carteira. Se o
     * usuario titular (ou transacao, posicao, relatorio) ainda apontar para
     * ela, o Oracle recusa com ORA-02292: a transacao e desfeita e a excecao
     * relancada.
     *
     * @return quantidade de linhas excluidas de carteira_pf (0 = id inexistente)
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
     * Versao de pacote, com a conexao recebida (usada por UsuarioDAO depois de
     * apagar o usuario): DELETE em carteira_pf e, so se ela existia, na linha
     * pai - assim um id de carteira PJ nunca e apagado por este DAO.
     *
     * @return quantidade de linhas excluidas de carteira_pf
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

    /** Busca a carteira PF pelo id (sem o historico). Retorna null quando nao existe. */
    @Override
    public CarteiraPF buscarPorId(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return buscarPorId(conexao, idCarteira);
        }
    }

    /**
     * Versao de pacote, com a conexao recebida, para ler a carteira PF dentro da
     * conexao/transacao de outro DAO. O UsuarioDAO nao precisa dela: le a
     * carteira no proprio JOIN e a monta com montar(ResultSet).
     */
    CarteiraPF buscarPorId(Connection conexao, int idCarteira) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idCarteira);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    /** Lista todas as carteiras PF, ordenadas pelo id. */
    @Override
    public List<CarteiraPF> listarTodos() throws SQLException {
        List<CarteiraPF> carteiras = new ArrayList<>();

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
     * Converte a linha atual em CarteiraPF, com as escalas de dinheiro
     * normalizadas. Mapeamento unico da carteira PF, usado tambem por
     * CarteiraDAO e UsuarioDAO: a consulta precisa trazer as colunas
     * id_carteira, descricao, saldo_reais e tipo (de carteira) e
     * limite_diario_saque (de carteira_pf). Recusa a linha cuja carteira pai
     * nao tem tipo PF (heranca incoerente no banco).
     */
    static CarteiraPF montar(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_carteira");
        String tipo = rs.getString("tipo");
        if (tipo == null || !"PF".equals(tipo.trim())) {
            throw new SQLException("Carteira " + id
                    + " inconsistente: tem linha em carteira_pf, mas o tipo e " + tipo + ".");
        }

        return new CarteiraPF(id,
                rs.getString("descricao"),
                Valores.dinheiro(rs.getBigDecimal("saldo_reais")),
                Valores.dinheiro(rs.getBigDecimal("limite_diario_saque")));
    }

    /** Recusa carteira nula com mensagem clara (em vez de NullPointerException). */
    private static void exigirCarteira(CarteiraPF carteira) {
        if (carteira == null) {
            throw new IllegalArgumentException("Carteira PF nula.");
        }
    }
}
