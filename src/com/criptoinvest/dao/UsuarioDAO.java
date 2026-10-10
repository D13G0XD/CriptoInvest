package com.criptoinvest.dao;

import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) da tabela USUARIO.
 *
 * O usuario tem relacao 1:1 obrigatoria com a carteira PF (heranca joined:
 * linha pai em CARTEIRA + linha filha em CARTEIRA_PF). Por isso inserir,
 * alterar e excluir gravam o usuario e a carteira numa UNICA transacao,
 * usando as sobrecargas com Connection do CarteiraPFDAO.
 *
 * Tabelas:
 *   usuario (id_usuario PK, id_carteira_pf FK/UK, nome, email UK, senha, cpf UK, autenticacao_2fa)
 *   carteira (id_carteira PK, descricao, saldo_reais, tipo) + carteira_pf (id_carteira_pf PK/FK, limite_diario_saque)
 * Sequence:
 *   seq_usuario -> geracao da chave primaria (a da carteira vem de seq_carteira, no CarteiraDAO)
 *
 * A leitura faz JOIN com carteira e carteira_pf e reconstroi a CarteiraPF e o
 * Usuario pelos construtores de reconstrucao. As empresas do usuario NAO sao
 * carregadas (use EmpresaDAO.listarPorUsuario).
 */
public class UsuarioDAO extends BaseDAO implements CrudDAO<Usuario> {

    private static final String SEQUENCE = "seq_usuario";

    private static final String SQL_INSERT =
            "INSERT INTO usuario (id_usuario, id_carteira_pf, nome, email, senha, cpf, autenticacao_2fa) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** id_carteira_pf nao muda: a carteira PF pertence ao usuario desde o INSERT (1:1). */
    private static final String SQL_UPDATE =
            "UPDATE usuario "
          + "   SET nome = ?, email = ?, senha = ?, cpf = ?, autenticacao_2fa = ? "
          + " WHERE id_usuario = ?";

    private static final String SQL_DELETE =
            "DELETE FROM usuario WHERE id_usuario = ?";

    private static final String SQL_SELECT_ID_CARTEIRA =
            "SELECT id_carteira_pf FROM usuario WHERE id_usuario = ?";

    /**
     * Usuario + carteira pai + carteira filha PF (base de todas as consultas).
     * Traz c.tipo para CarteiraPFDAO.montar conferir a heranca.
     */
    private static final String SQL_SELECT_BASE =
            "SELECT u.id_usuario, u.nome, u.email, u.senha, u.cpf, u.autenticacao_2fa, "
          + "       c.id_carteira, c.descricao, c.saldo_reais, c.tipo, pf.limite_diario_saque "
          + "  FROM usuario u "
          + "  JOIN carteira c     ON c.id_carteira = u.id_carteira_pf "
          + "  JOIN carteira_pf pf ON pf.id_carteira_pf = u.id_carteira_pf ";

    private static final String SQL_SELECT_POR_ID =
            SQL_SELECT_BASE + " WHERE u.id_usuario = ?";

    private static final String SQL_SELECT_POR_CPF =
            SQL_SELECT_BASE + " WHERE u.cpf = ?";

    private static final String SQL_SELECT_POR_EMAIL =
            SQL_SELECT_BASE + " WHERE u.email = ?";

    private static final String SQL_SELECT_TODOS =
            SQL_SELECT_BASE + " ORDER BY u.id_usuario";

    private final CarteiraPFDAO carteiraPFDAO = new CarteiraPFDAO();

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere um usuario ainda nao persistido (id 0) junto com a sua carteira PF,
     * numa unica transacao: primeiro a carteira (carteira + carteira_pf), depois
     * o usuario apontando para ela. Os ids vem das sequences e so sao atribuidos
     * depois de cada INSERT; se a transacao falhar, ela e desfeita e os ids do
     * usuario e da carteira voltam a 0 antes de a excecao ser relancada.
     *
     * @param usuario usuario a ser gravado (com a carteira PF criada pelo construtor)
     * @return o id gerado pela sequence seq_usuario
     * @throws IllegalStateException quando o usuario ou a carteira ja tem id (ja persistidos)
     */
    @Override
    public int inserir(Usuario usuario) throws SQLException {
        if (usuario.getIdUsuario() > 0) {
            throw new IllegalStateException("Usuario ja persistido (id="
                    + usuario.getIdUsuario() + "): use alterar");
        }
        CarteiraPF carteira = usuario.getCarteira();
        if (carteira.getIdCarteira() > 0) {
            throw new IllegalStateException("A carteira PF do usuario ja foi persistida (id="
                    + carteira.getIdCarteira() + "): cada usuario grava a propria carteira no inserir");
        }

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                carteiraPFDAO.inserir(conexao, carteira);

                int id = proximoId(conexao, SEQUENCE);
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                    ps.setInt(1, id);
                    ps.setInt(2, carteira.getIdCarteira());
                    ps.setString(3, usuario.getNome());
                    ps.setString(4, usuario.getEmail());
                    ps.setString(5, usuario.getSenha());
                    ps.setString(6, usuario.getCpf());
                    ps.setString(7, simNao(usuario.isAutenticacaoDoisFatores()));
                    ps.executeUpdate();
                }
                usuario.setIdUsuario(id);

                conexao.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                desfazer(conexao);
                usuario.setIdUsuario(0);
                carteira.setIdCarteira(0);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza os dados do usuario e da sua carteira PF (descricao, saldo e
     * limite diario de saque) numa unica transacao. Se o usuario nao existir,
     * nada e alterado e o retorno e 0.
     *
     * @return quantidade de linhas alteradas em usuario (0 = id inexistente)
     * @throws IllegalStateException quando a carteira do objeto nao e a carteira
     *         gravada para esse usuario no banco
     */
    @Override
    public int alterar(Usuario usuario) throws SQLException {
        CarteiraPF carteira = usuario.getCarteira();

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int idCarteiraGravada = buscarIdCarteira(conexao, usuario.getIdUsuario());
                if (idCarteiraGravada == 0) {
                    conexao.rollback();
                    return 0;
                }
                if (idCarteiraGravada != carteira.getIdCarteira()) {
                    throw new IllegalStateException("A carteira do objeto (id=" + carteira.getIdCarteira()
                            + ") nao e a carteira PF do usuario " + usuario.getIdUsuario()
                            + " no banco (id=" + idCarteiraGravada + ").");
                }

                int linhas;
                try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
                    ps.setString(1, usuario.getNome());
                    ps.setString(2, usuario.getEmail());
                    ps.setString(3, usuario.getSenha());
                    ps.setString(4, usuario.getCpf());
                    ps.setString(5, simNao(usuario.isAutenticacaoDoisFatores()));
                    ps.setInt(6, usuario.getIdUsuario());
                    linhas = ps.executeUpdate();
                }
                carteiraPFDAO.alterar(conexao, carteira);

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
     * Exclui o usuario e a sua carteira PF numa unica transacao (usuario primeiro,
     * depois carteira_pf e carteira). Se houver empresa ou alerta do usuario, ou
     * transacao/posicao/relatorio na carteira, o Oracle recusa com ORA-02292: a
     * transacao e desfeita (nada e apagado) e a excecao e relancada.
     *
     * @return quantidade de linhas excluidas de usuario (0 = id inexistente)
     */
    @Override
    public int excluir(int idUsuario) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                int idCarteira = buscarIdCarteira(conexao, idUsuario);
                if (idCarteira == 0) {
                    conexao.rollback();
                    return 0;
                }

                int linhas;
                try (PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {
                    ps.setInt(1, idUsuario);
                    linhas = ps.executeUpdate();
                }
                carteiraPFDAO.excluir(conexao, idCarteira);

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

    /** Busca um usuario (com a carteira PF) pela chave primaria. Retorna null quando nao existe. */
    @Override
    public Usuario buscarPorId(int idUsuario) throws SQLException {
        try (Connection conexao = abrirConexao()) {
            return buscarPorId(conexao, idUsuario);
        }
    }

    /**
     * Versao de pacote, com a conexao recebida: usada pelo EmpresaDAO (dono) e
     * pelo AlertaDAO para montar o usuario dentro da mesma conexao.
     */
    Usuario buscarPorId(Connection conexao, int idUsuario) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarUsuario(rs) : null;
            }
        }
    }

    /** Busca um usuario pelo CPF (chave unica), comparado exatamente como foi gravado. Retorna null quando nao existe. */
    public Usuario buscarPorCpf(String cpf) throws SQLException {
        return buscarPorChaveUnica(SQL_SELECT_POR_CPF, cpf);
    }

    /** Busca um usuario pelo e-mail (chave unica), comparado exatamente como foi gravado. Retorna null quando nao existe. */
    public Usuario buscarPorEmail(String email) throws SQLException {
        return buscarPorChaveUnica(SQL_SELECT_POR_EMAIL, email);
    }

    /** Lista todos os usuarios, cada um com a sua carteira PF, ordenados pelo id. */
    @Override
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(montarUsuario(rs));
            }
        }
        return usuarios;
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /** Executa uma das consultas por chave unica (cpf ou email) com o valor informado. */
    private Usuario buscarPorChaveUnica(String sql, String valor) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(sql)) {

            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montarUsuario(rs) : null;
            }
        }
    }

    /**
     * Le o id da carteira PF gravada para o usuario, dentro da transacao em
     * andamento. Retorna 0 quando o usuario nao existe (as sequences comecam em 1).
     */
    private int buscarIdCarteira(Connection conexao, int idUsuario) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_ID_CARTEIRA)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_carteira_pf") : 0;
            }
        }
    }

    /**
     * Converte a linha atual do ResultSet (usuario + carteira + carteira_pf) em
     * um Usuario com a CarteiraPF reconstruida. A carteira vem do mapeamento
     * unico CarteiraPFDAO.montar, que normaliza as escalas (o Oracle devolve o
     * NUMBER sem a escala da coluna) e recusa carteira com tipo diferente de PF.
     */
    private Usuario montarUsuario(ResultSet rs) throws SQLException {
        CarteiraPF carteira = CarteiraPFDAO.montar(rs);

        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha"),
                rs.getString("cpf"),
                deSimNao(rs.getString("autenticacao_2fa")),
                carteira);
    }
}
