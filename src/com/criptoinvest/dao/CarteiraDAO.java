package com.criptoinvest.dao;

import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Posicao;
import com.criptoinvest.model.Transacao;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DAO da tabela CARTEIRA, pai da heranca Joined (carteira -> carteira_pf | carteira_pj).
 *
 * A leitura e POLIMORFICA: o SELECT faz LEFT JOIN nas duas tabelas filhas e a
 * coluna tipo decide se a linha vira CarteiraPF ou CarteiraPJ. Como o banco nao
 * amarra o discriminador a tabela filha, a coerencia e conferida aqui: tipo PF
 * sem linha em carteira_pf (ou com linha tambem em carteira_pj) gera
 * SQLException com mensagem clara, em vez de um objeto montado pela metade.
 *
 * Tabelas:
 *   carteira    (id_carteira PK, descricao, saldo_reais, tipo 'PF'/'PJ')
 *   carteira_pf (id_carteira_pf PK/FK, limite_diario_saque)
 *   carteira_pj (id_carteira_pj PK/FK, regime_tributario)
 * Sequence:
 *   seq_carteira -> chave primaria da carteira, repetida na tabela filha
 *
 * Os metodos de pacote que recebem a Connection (inserirBase, alterarBase,
 * excluirBase, buscarPorId, atualizarSaldo) sao usados, na conexao/transacao
 * de quem chama, pelos DAOs filhos (inserirBase, alterarBase, excluirBase - e,
 * por meio deles, por UsuarioDAO e EmpresaDAO), por TransacaoDAO (buscarPorId,
 * atualizarSaldo) e por PosicaoDAO e RelatorioDAO (buscarPorId).
 *
 * Esta classe nao guarda outros DAOs em atributos: TransacaoDAO, PosicaoDAO,
 * RelatorioDAO e os DAOs filhos guardam um CarteiraDAO, entao criar os deles
 * aqui no construtor formaria um ciclo infinito de instanciacao. Eles sao criados
 * dentro dos metodos que precisam deles (os DAOs nao tem estado).
 */
public class CarteiraDAO extends BaseDAO implements CrudDAO<Carteira> {

    private static final String SEQUENCE = "seq_carteira";

    private static final String SQL_INSERT =
            "INSERT INTO carteira (id_carteira, descricao, saldo_reais, tipo) "
          + "VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE carteira SET descricao = ?, saldo_reais = ? WHERE id_carteira = ?";

    private static final String SQL_UPDATE_SALDO =
            "UPDATE carteira SET saldo_reais = ? WHERE id_carteira = ?";

    private static final String SQL_DELETE_PF =
            "DELETE FROM carteira_pf WHERE id_carteira_pf = ?";

    private static final String SQL_DELETE_PJ =
            "DELETE FROM carteira_pj WHERE id_carteira_pj = ?";

    private static final String SQL_DELETE =
            "DELETE FROM carteira WHERE id_carteira = ?";

    /** Leitura polimorfica: as colunas das duas filhas vem nulas quando a linha nao existe. */
    private static final String SQL_SELECT =
            "SELECT c.id_carteira, c.descricao, c.saldo_reais, c.tipo, "
          + "       pf.id_carteira_pf, pf.limite_diario_saque, "
          + "       pj.id_carteira_pj, pj.regime_tributario "
          + "  FROM carteira c "
          + "  LEFT JOIN carteira_pf pf ON pf.id_carteira_pf = c.id_carteira "
          + "  LEFT JOIN carteira_pj pj ON pj.id_carteira_pj = c.id_carteira";

    private static final String SQL_SELECT_POR_ID =
            SQL_SELECT + " WHERE c.id_carteira = ?";

    private static final String SQL_SELECT_TODOS =
            SQL_SELECT + " ORDER BY c.id_carteira";

    private static final String SQL_SELECT_POR_TIPO =
            SQL_SELECT + " WHERE c.tipo = ? ORDER BY c.id_carteira";

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere uma carteira nova (id 0) de forma polimorfica: CarteiraPF grava
     * carteira + carteira_pf, CarteiraPJ grava carteira + carteira_pj, tudo numa
     * unica transacao. Se algo falhar, a transacao e desfeita e o id volta a 0.
     *
     * @return o id gerado por seq_carteira
     * @throws IllegalStateException quando a carteira ja tem id (use alterar)
     */
    @Override
    public int inserir(Carteira carteira) throws SQLException {
        exigirCarteira(carteira);
        exigirNaoPersistida(carteira);
        if (!(carteira instanceof CarteiraPF) && !(carteira instanceof CarteiraPJ)) {
            throw new IllegalArgumentException("Tipo de carteira nao suportado: "
                    + carteira.getClass().getSimpleName());
        }

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int id;
                if (carteira instanceof CarteiraPF) {
                    id = new CarteiraPFDAO().inserir(conexao, (CarteiraPF) carteira);
                } else {
                    id = new CarteiraPJDAO().inserir(conexao, (CarteiraPJ) carteira);
                }
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
     * Grava somente a linha da tabela pai, com tipo = getTipo(). Uso dos DAOs
     * filhos, dentro da transacao deles: obtem o id de seq_carteira e o atribui
     * ao objeto depois do INSERT, para a linha filha usar a mesma chave. Se a
     * transacao falhar depois, quem a controla desfaz e devolve 0 ao id.
     *
     * @return o id gerado por seq_carteira
     * @throws IllegalStateException quando a carteira ja tem id (use alterar)
     */
    int inserirBase(Connection conexao, Carteira carteira) throws SQLException {
        exigirCarteira(carteira);
        exigirNaoPersistida(carteira);

        int id = proximoId(conexao, SEQUENCE);
        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, id);
            ps.setString(2, carteira.getDescricao());
            ps.setBigDecimal(3, carteira.getSaldoReais());
            ps.setString(4, carteira.getTipo());
            ps.executeUpdate();
        }

        carteira.setIdCarteira(id);
        return id;
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza somente as colunas da tabela pai (descricao e saldo_reais). O
     * limite da PF e o regime da PJ sao alterados por CarteiraPFDAO / CarteiraPJDAO.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     */
    @Override
    public int alterar(Carteira carteira) throws SQLException {
        exigirCarteira(carteira);
        try (Connection conexao = abrirConexao()) {
            return alterarBase(conexao, carteira);
        }
    }

    /** UPDATE de descricao e saldo_reais com a conexao recebida (uso dos DAOs filhos). */
    int alterarBase(Connection conexao, Carteira carteira) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, carteira.getDescricao());
            ps.setBigDecimal(2, carteira.getSaldoReais());
            ps.setInt(3, carteira.getIdCarteira());
            return ps.executeUpdate();
        }
    }

    /**
     * Grava o saldo em reais da carteira (apos depositos, saques ou transacoes
     * registradas em memoria).
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     */
    public int atualizarSaldo(int idCarteira, BigDecimal saldo) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return atualizarSaldo(conexao, idCarteira, saldo);
        }
    }

    /**
     * Versao de pacote, com a conexao recebida: usada por TransacaoDAO.registrar
     * na mesma transacao do INSERT da transacao e da posicao. O saldo nulo e
     * recusado (viraria zero e apagaria o saldo real).
     */
    int atualizarSaldo(Connection conexao, int idCarteira, BigDecimal saldo) throws SQLException {
        if (saldo == null) {
            throw new IllegalArgumentException("Saldo nulo: informe o saldo em reais da carteira "
                    + idCarteira + ".");
        }
        try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE_SALDO)) {
            ps.setBigDecimal(1, Valores.dinheiro(saldo));
            ps.setInt(2, idCarteira);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a carteira numa transacao: primeiro a linha filha (os dois DELETEs
     * sao executados; o da tabela que nao tem a carteira afeta 0 linhas) e
     * depois a linha pai. Se um usuario, empresa, transacao, posicao ou
     * relatorio ainda apontar para ela, o Oracle recusa com ORA-02292: a
     * transacao e desfeita e a excecao relancada.
     *
     * @return quantidade de linhas excluidas de carteira (0 = id inexistente)
     */
    @Override
    public int excluir(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                executarDelete(conexao, SQL_DELETE_PF, idCarteira);
                executarDelete(conexao, SQL_DELETE_PJ, idCarteira);
                int linhas = excluirBase(conexao, idCarteira);
                conexao.commit();
                return linhas;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                throw e;
            }
        }
    }

    /**
     * DELETE da linha pai com a conexao recebida (uso dos DAOs filhos, depois de
     * apagarem a linha da tabela filha).
     *
     * @return quantidade de linhas excluidas de carteira
     */
    int excluirBase(Connection conexao, int idCarteira) throws SQLException {
        return executarDelete(conexao, SQL_DELETE, idCarteira);
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /**
     * Busca a carteira pelo id, ja instanciada como CarteiraPF ou CarteiraPJ
     * conforme a coluna tipo. O historico (transacoes e posicoes) nao e lido:
     * use carregarHistorico quando precisar dele. Retorna null quando nao existe.
     */
    @Override
    public Carteira buscarPorId(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return buscarPorId(conexao, idCarteira);
        }
    }

    /**
     * Versao de pacote da leitura polimorfica, com a conexao recebida: usada por
     * TransacaoDAO, PosicaoDAO e RelatorioDAO para montar a carteira das suas linhas.
     */
    Carteira buscarPorId(Connection conexao, int idCarteira) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idCarteira);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarCarteira(rs) : null;
            }
        }
    }

    /** Lista todas as carteiras, PF e PJ misturadas, ordenadas pelo id. */
    @Override
    public List<Carteira> listarTodos() throws SQLException {
        List<Carteira> carteiras = new ArrayList<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                carteiras.add(montarCarteira(rs));
            }
        }
        return carteiras;
    }

    /**
     * Lista as carteiras de um tipo, ordenadas pelo id.
     *
     * @param tipo "PF" ou "PJ" (espacos e minusculas sao aceitos)
     * @throws IllegalArgumentException quando o tipo nao e PF nem PJ
     */
    public List<Carteira> listarPorTipo(String tipo) throws SQLException {
        String tipoNormalizado = tipo == null ? null : tipo.trim().toUpperCase(Locale.ROOT);
        if (!"PF".equals(tipoNormalizado) && !"PJ".equals(tipoNormalizado)) {
            throw new IllegalArgumentException("Tipo de carteira invalido: " + tipo + " (use PF ou PJ).");
        }

        List<Carteira> carteiras = new ArrayList<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_TIPO)) {

            ps.setString(1, tipoNormalizado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    carteiras.add(montarCarteira(rs));
                }
            }
        }
        return carteiras;
    }

    /**
     * Preenche o historico da carteira com o que esta gravado: as transacoes
     * (TransacaoDAO) e as posicoes (PosicaoDAO) sao lidas com a mesma conexao,
     * ja apontando para o objeto recebido, e entregues a
     * Carteira.carregarHistorico, que substitui as listas sem mexer no saldo.
     *
     * Como o saldo nao e relido, antes das listagens a carteira gravada e
     * conferida: ela precisa existir, ser do mesmo tipo e ter o MESMO saldo do
     * objeto. Uma copia antiga (saldo diferente do gravado) ficaria com o
     * historico atual e o saldo velho, e o proximo TransacaoDAO.registrar
     * gravaria esse saldo velho. Para atualizar uma copia antiga, releia a
     * carteira com buscarPorId e carregue o historico do objeto relido.
     * Um deposito ou saque feito em memoria e ainda nao gravado tambem deixa
     * os saldos diferentes: nesse caso grave o saldo antes (atualizarSaldo ou
     * alterar) e depois carregue o historico; reler com buscarPorId perderia
     * esse deposito/saque.
     *
     * @throws IllegalStateException quando a carteira ainda nao foi persistida
     *         (id 0), quando ela nao existe mais no banco, quando o tipo gravado
     *         e outro, ou quando o saldo em memoria difere do saldo gravado
     */
    public void carregarHistorico(Carteira carteira) throws SQLException {
        exigirCarteira(carteira);
        if (carteira.getIdCarteira() <= 0) {
            throw new IllegalStateException("Carteira nao persistida (id="
                    + carteira.getIdCarteira() + "): nao ha historico gravado para carregar.");
        }

        try (Connection conexao = abrirConexao()) {
            int id = carteira.getIdCarteira();
            Carteira gravada = buscarPorId(conexao, id);
            if (gravada == null) {
                throw new IllegalStateException("Carteira id=" + id
                        + " inexistente no banco: nao ha historico para carregar.");
            }
            if (!gravada.getTipo().equals(carteira.getTipo())) {
                throw new IllegalStateException("Carteira id=" + id + " e " + gravada.getTipo()
                        + " no banco, mas o objeto e " + carteira.getTipo() + ".");
            }
            // compareTo, e nao equals: 175 e 175.00 sao o mesmo saldo
            if (gravada.getSaldoReais().compareTo(carteira.getSaldoReais()) != 0) {
                throw new IllegalStateException("Saldo em memoria R$ "
                        + Valores.formatar(carteira.getSaldoReais()) + " difere do gravado R$ "
                        + Valores.formatar(gravada.getSaldoReais()) + " na carteira id=" + id
                        + ": se ha deposito/saque em memoria ainda nao gravado, grave-o antes "
                        + "(atualizarSaldo(id, saldo) ou alterar); se a copia esta desatualizada, "
                        + "releia-a com buscarPorId.");
            }

            List<Transacao> transacoes = new TransacaoDAO().listarPorCarteira(conexao, carteira);
            List<Posicao> posicoes = new PosicaoDAO().listarPorCarteira(conexao, carteira);
            carteira.carregarHistorico(transacoes, posicoes);
        }
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Converte a linha atual do ResultSet (SQL_SELECT) na subclasse indicada
     * pela coluna tipo, conferindo se a linha filha correspondente existe e se
     * a carteira nao aparece tambem na outra tabela filha. A criacao do objeto,
     * com as escalas normalizadas, fica no mapeamento unico de cada filha
     * (CarteiraPFDAO.montar e CarteiraPJDAO.montar), o mesmo usado por
     * UsuarioDAO e EmpresaDAO.
     */
    private Carteira montarCarteira(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_carteira");
        String tipo = rs.getString("tipo");
        tipo = tipo == null ? null : tipo.trim();

        rs.getInt("id_carteira_pf");
        boolean temLinhaPF = !rs.wasNull();
        rs.getInt("id_carteira_pj");
        boolean temLinhaPJ = !rs.wasNull();

        if ("PF".equals(tipo)) {
            if (!temLinhaPF) {
                throw new SQLException("Carteira " + id
                        + " inconsistente: tipo PF, mas nao ha linha em carteira_pf.");
            }
            if (temLinhaPJ) {
                throw new SQLException("Carteira " + id
                        + " inconsistente: tipo PF, mas tambem ha linha em carteira_pj.");
            }
            return CarteiraPFDAO.montar(rs);
        }

        if ("PJ".equals(tipo)) {
            if (!temLinhaPJ) {
                throw new SQLException("Carteira " + id
                        + " inconsistente: tipo PJ, mas nao ha linha em carteira_pj.");
            }
            if (temLinhaPF) {
                throw new SQLException("Carteira " + id
                        + " inconsistente: tipo PJ, mas tambem ha linha em carteira_pf.");
            }
            return CarteiraPJDAO.montar(rs);
        }

        throw new SQLException("Carteira " + id + " com tipo desconhecido: " + tipo + " (esperado PF ou PJ).");
    }

    /** Executa um DELETE ... WHERE <pk> = ? e devolve as linhas afetadas. */
    private static int executarDelete(Connection conexao, String sql, int idCarteira) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setInt(1, idCarteira);
            return ps.executeUpdate();
        }
    }

    /** Recusa carteira nula com mensagem clara (em vez de NullPointerException). */
    private static void exigirCarteira(Carteira carteira) {
        if (carteira == null) {
            throw new IllegalArgumentException("Carteira nula.");
        }
    }

    /** O caminho de uma carteira que ja tem id e alterar, nao inserir. */
    private static void exigirNaoPersistida(Carteira carteira) {
        if (carteira.getIdCarteira() > 0) {
            throw new IllegalStateException(carteira.getClass().getSimpleName()
                    + " ja persistida (id=" + carteira.getIdCarteira() + "): use alterar");
        }
    }
}
