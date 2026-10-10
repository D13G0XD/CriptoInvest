package com.criptoinvest.dao;

import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Empresa;
import com.criptoinvest.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO (Data Access Object) da tabela EMPRESA.
 *
 * A empresa pertence a um usuario (N:1 obrigatorio) e tem relacao 1:1
 * obrigatoria com a carteira PJ (heranca joined: linha pai em CARTEIRA + linha
 * filha em CARTEIRA_PJ). Por isso inserir, alterar e excluir gravam a empresa e
 * a carteira numa UNICA transacao, usando as sobrecargas com Connection do
 * CarteiraPJDAO.
 *
 * Tabelas:
 *   empresa (id_empresa PK, id_usuario FK, id_carteira_pj FK/UK, nome, cnpj UK)
 *   carteira (id_carteira PK, descricao, saldo_reais, tipo) + carteira_pj (id_carteira_pj PK/FK, regime_tributario)
 * Sequence:
 *   seq_empresa -> geracao da chave primaria (a da carteira vem de seq_carteira, no CarteiraDAO)
 *
 * A leitura faz JOIN com carteira e carteira_pj e monta o dono com
 * UsuarioDAO.buscarPorId(Connection, int), na mesma conexao. A lista de empresas
 * do dono (Usuario.getEmpresas) nao e preenchida pelo DAO.
 */
public class EmpresaDAO extends BaseDAO implements CrudDAO<Empresa> {

    private static final String SEQUENCE = "seq_empresa";

    private static final String SQL_INSERT =
            "INSERT INTO empresa (id_empresa, id_usuario, id_carteira_pj, nome, cnpj) "
          + "VALUES (?, ?, ?, ?, ?)";

    /** id_carteira_pj nao muda: a carteira PJ pertence a empresa desde o INSERT (1:1). */
    private static final String SQL_UPDATE =
            "UPDATE empresa "
          + "   SET id_usuario = ?, nome = ?, cnpj = ? "
          + " WHERE id_empresa = ?";

    private static final String SQL_DELETE =
            "DELETE FROM empresa WHERE id_empresa = ?";

    private static final String SQL_SELECT_ID_CARTEIRA =
            "SELECT id_carteira_pj FROM empresa WHERE id_empresa = ?";

    /**
     * Empresa + carteira pai + carteira filha PJ (base de todas as consultas).
     * Traz c.tipo para CarteiraPJDAO.montar conferir a heranca.
     */
    private static final String SQL_SELECT_BASE =
            "SELECT e.id_empresa, e.id_usuario, e.nome, e.cnpj, "
          + "       c.id_carteira, c.descricao, c.saldo_reais, c.tipo, pj.regime_tributario "
          + "  FROM empresa e "
          + "  JOIN carteira c     ON c.id_carteira = e.id_carteira_pj "
          + "  JOIN carteira_pj pj ON pj.id_carteira_pj = e.id_carteira_pj ";

    private static final String SQL_SELECT_POR_ID =
            SQL_SELECT_BASE + " WHERE e.id_empresa = ?";

    private static final String SQL_SELECT_POR_CNPJ =
            SQL_SELECT_BASE + " WHERE e.cnpj = ?";

    private static final String SQL_SELECT_POR_USUARIO =
            SQL_SELECT_BASE + " WHERE e.id_usuario = ? ORDER BY e.id_empresa";

    private static final String SQL_SELECT_TODOS =
            SQL_SELECT_BASE + " ORDER BY e.id_empresa";

    private final CarteiraPJDAO carteiraPJDAO = new CarteiraPJDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere uma empresa ainda nao persistida (id 0) junto com a sua carteira PJ,
     * numa unica transacao: primeiro a carteira (carteira + carteira_pj), depois
     * a empresa apontando para ela e para o dono. Os ids vem das sequences e so
     * sao atribuidos depois de cada INSERT; se a transacao falhar, ela e desfeita
     * e os ids da empresa e da carteira voltam a 0 antes de a excecao ser relancada.
     *
     * @param empresa empresa a ser gravada (com a carteira PJ criada pelo construtor)
     * @return o id gerado pela sequence seq_empresa
     * @throws IllegalStateException quando a empresa ou a carteira ja tem id, ou
     *         quando o dono ainda nao foi persistido (id 0)
     */
    @Override
    public int inserir(Empresa empresa) throws SQLException {
        if (empresa.getIdEmpresa() > 0) {
            throw new IllegalStateException("Empresa ja persistida (id="
                    + empresa.getIdEmpresa() + "): use alterar");
        }
        exigirDonoPersistido(empresa);
        CarteiraPJ carteira = empresa.getCarteira();
        if (carteira.getIdCarteira() > 0) {
            throw new IllegalStateException("A carteira PJ da empresa ja foi persistida (id="
                    + carteira.getIdCarteira() + "): cada empresa grava a propria carteira no inserir");
        }

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                carteiraPJDAO.inserir(conexao, carteira);

                int id = proximoId(conexao, SEQUENCE);
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                    ps.setInt(1, id);
                    ps.setInt(2, empresa.getDono().getIdUsuario());
                    ps.setInt(3, carteira.getIdCarteira());
                    ps.setString(4, empresa.getNome());
                    ps.setString(5, empresa.getCnpj());
                    ps.executeUpdate();
                }
                empresa.setIdEmpresa(id);

                conexao.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                empresa.setIdEmpresa(0);
                carteira.setIdCarteira(0);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza o dono, a razao social e o CNPJ da empresa e a sua carteira PJ
     * (descricao, saldo e regime tributario) numa unica transacao. Se a empresa
     * nao existir, nada e alterado e o retorno e 0.
     *
     * @return quantidade de linhas alteradas em empresa (0 = id inexistente)
     * @throws IllegalStateException quando o dono ainda nao foi persistido (id 0)
     *         ou quando a carteira do objeto nao e a carteira gravada para essa
     *         empresa no banco
     */
    @Override
    public int alterar(Empresa empresa) throws SQLException {
        exigirDonoPersistido(empresa);
        CarteiraPJ carteira = empresa.getCarteira();

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int idCarteiraGravada = buscarIdCarteira(conexao, empresa.getIdEmpresa());
                if (idCarteiraGravada == 0) {
                    conexao.rollback();
                    return 0;
                }
                if (idCarteiraGravada != carteira.getIdCarteira()) {
                    throw new IllegalStateException("A carteira do objeto (id=" + carteira.getIdCarteira()
                            + ") nao e a carteira PJ da empresa " + empresa.getIdEmpresa()
                            + " no banco (id=" + idCarteiraGravada + ").");
                }

                int linhas;
                try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
                    ps.setInt(1, empresa.getDono().getIdUsuario());
                    ps.setString(2, empresa.getNome());
                    ps.setString(3, empresa.getCnpj());
                    ps.setInt(4, empresa.getIdEmpresa());
                    linhas = ps.executeUpdate();
                }
                carteiraPJDAO.alterar(conexao, carteira);

                conexao.commit();
                return linhas;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a empresa e a sua carteira PJ numa unica transacao (empresa
     * primeiro, depois carteira_pj e carteira). Se houver transacao, posicao ou
     * relatorio na carteira, o Oracle recusa com ORA-02292: a transacao e
     * desfeita (nada e apagado) e a excecao e relancada. O dono nao e afetado.
     *
     * @return quantidade de linhas excluidas de empresa (0 = id inexistente)
     */
    @Override
    public int excluir(int idEmpresa) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int idCarteira = buscarIdCarteira(conexao, idEmpresa);
                if (idCarteira == 0) {
                    conexao.rollback();
                    return 0;
                }

                int linhas;
                try (PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {
                    ps.setInt(1, idEmpresa);
                    linhas = ps.executeUpdate();
                }
                carteiraPJDAO.excluir(conexao, idCarteira);

                conexao.commit();
                return linhas;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /** Busca uma empresa (com a carteira PJ e o dono) pela chave primaria. Retorna null quando nao existe. */
    @Override
    public Empresa buscarPorId(int idEmpresa) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idEmpresa);
            return primeiraEmpresa(conexao, ps);
        }
    }

    /** Busca uma empresa pelo CNPJ (chave unica), comparado exatamente como foi gravado. Retorna null quando nao existe. */
    public Empresa buscarPorCnpj(String cnpj) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CNPJ)) {

            ps.setString(1, cnpj);
            return primeiraEmpresa(conexao, ps);
        }
    }

    /** Lista todas as empresas, ordenadas pelo id. Empresas do mesmo dono compartilham o mesmo objeto Usuario. */
    @Override
    public List<Empresa> listarTodos() throws SQLException {
        List<Empresa> empresas = new ArrayList<>();
        Map<Integer, Usuario> donos = new HashMap<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idUsuario = rs.getInt("id_usuario");
                Usuario dono = donos.get(idUsuario);
                if (dono == null) {
                    dono = carregarDono(conexao, idUsuario);
                    donos.put(idUsuario, dono);
                }
                empresas.add(montarEmpresa(rs, dono));
            }
        }
        return empresas;
    }

    /**
     * Lista as empresas de um usuario, ordenadas pelo id. O dono e lido uma
     * unica vez e reaproveitado em todas as empresas da lista. Retorna lista
     * vazia quando o usuario nao tem empresas (ou nao existe).
     */
    public List<Empresa> listarPorUsuario(int idUsuario) throws SQLException {
        List<Empresa> empresas = new ArrayList<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_USUARIO)) {

            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                Usuario dono = null;
                while (rs.next()) {
                    if (dono == null) {
                        dono = carregarDono(conexao, idUsuario);
                    }
                    empresas.add(montarEmpresa(rs, dono));
                }
            }
        }
        return empresas;
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Pre-condicao de FK (empresa.id_usuario NOT NULL): o dono precisa estar
     * gravado antes da empresa.
     */
    private static void exigirDonoPersistido(Empresa empresa) {
        Usuario dono = empresa.getDono();
        if (dono == null || dono.getIdUsuario() <= 0) {
            throw new IllegalStateException("O dono da empresa ainda nao foi persistido (id 0): "
                    + "grave o usuario com UsuarioDAO.inserir antes da empresa");
        }
    }

    /**
     * Executa a consulta ja parametrizada (por id ou por CNPJ, que devolvem no
     * maximo uma linha) e monta a empresa, lendo o dono na mesma conexao.
     */
    private Empresa primeiraEmpresa(Connection conexao, PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Usuario dono = carregarDono(conexao, rs.getInt("id_usuario"));
            return montarEmpresa(rs, dono);
        }
    }

    /**
     * Le o dono pela versao de pacote do UsuarioDAO, na conexao recebida. A FK
     * fk_empresa_usuario garante que ele existe; a verificacao so protege
     * contra um banco inconsistente.
     */
    private Usuario carregarDono(Connection conexao, int idUsuario) throws SQLException {
        Usuario dono = usuarioDAO.buscarPorId(conexao, idUsuario);
        if (dono == null) {
            throw new IllegalStateException("Usuario dono (id=" + idUsuario
                    + ") nao encontrado para a empresa: banco inconsistente.");
        }
        return dono;
    }

    /**
     * Le o id da carteira PJ gravada para a empresa, dentro da transacao em
     * andamento. Retorna 0 quando a empresa nao existe (as sequences comecam em 1).
     */
    private int buscarIdCarteira(Connection conexao, int idEmpresa) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_ID_CARTEIRA)) {
            ps.setInt(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_carteira_pj") : 0;
            }
        }
    }

    /**
     * Converte a linha atual do ResultSet (empresa + carteira + carteira_pj) em
     * uma Empresa com a CarteiraPJ reconstruida e o dono informado. A carteira
     * vem do mapeamento unico CarteiraPJDAO.montar, que normaliza a escala do
     * saldo (o Oracle devolve o NUMBER sem a escala da coluna) e recusa carteira
     * com tipo diferente de PJ.
     */
    private Empresa montarEmpresa(ResultSet rs, Usuario dono) throws SQLException {
        CarteiraPJ carteira = CarteiraPJDAO.montar(rs);

        return new Empresa(
                rs.getInt("id_empresa"),
                rs.getString("nome"),
                rs.getString("cnpj"),
                dono,
                carteira);
    }
}
