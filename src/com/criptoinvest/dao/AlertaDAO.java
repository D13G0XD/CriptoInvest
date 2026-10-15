package com.criptoinvest.dao;

import com.criptoinvest.model.Alerta;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Usuario;
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
 * DAO (Data Access Object) da tabela ALERTA.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) os alertas,
 * a entidade associativa N:N entre USUARIO e CRIPTOATIVO.
 *
 * Tabela:
 *   alerta (id_alerta PK, id_usuario FK, id_cripto FK, limite_variacao,
 *           ativado CHAR(1) 'S'/'N', data_configuracao)
 *   UK uk_alerta_usuario_cripto (id_usuario, id_cripto): um alerta por par
 * Sequence:
 *   seq_alerta -> geracao da chave primaria
 *
 * Na leitura, o usuario vem de UsuarioDAO.buscarPorId(Connection, int) e o
 * criptoativo de CriptoativoDAO.buscarPorId(Connection, int), com a mesma
 * conexao. Nas listagens, cada usuario e cada criptoativo e lido uma unica vez
 * por chamada e o mesmo objeto e reaproveitado pelos alertas que o referenciam.
 */
public class AlertaDAO extends BaseDAO implements CrudDAO<Alerta> {

    private static final String SEQUENCE = "seq_alerta";

    private static final String SQL_INSERT =
            "INSERT INTO alerta (id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao) "
          + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE alerta "
          + "   SET id_usuario = ?, id_cripto = ?, limite_variacao = ?, ativado = ?, data_configuracao = ? "
          + " WHERE id_alerta = ?";

    private static final String SQL_DELETE =
            "DELETE FROM alerta WHERE id_alerta = ?";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao "
          + "  FROM alerta WHERE id_alerta = ?";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao "
          + "  FROM alerta ORDER BY id_alerta";

    private static final String SQL_SELECT_POR_USUARIO =
            "SELECT id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao "
          + "  FROM alerta WHERE id_usuario = ? ORDER BY id_alerta";

    private static final String SQL_SELECT_POR_ATIVADO =
            "SELECT id_alerta, id_usuario, id_cripto, limite_variacao, ativado, data_configuracao "
          + "  FROM alerta WHERE ativado = ? ORDER BY id_alerta";

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final CriptoativoDAO criptoativoDAO = new CriptoativoDAO();

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere um alerta ainda nao persistido (id 0). O usuario e o criptoativo
     * precisam ja estar gravados. O id vem da sequence seq_alerta e so e
     * atribuido ao objeto depois do INSERT. Um segundo alerta para o mesmo par
     * usuario x criptoativo e recusado pelo Oracle (ORA-00001), que e relancado.
     *
     * @return o id gerado pela sequence
     * @throws IllegalStateException quando o alerta ja tem id, ou quando o
     *         usuario ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int inserir(Alerta alerta) throws SQLException {
        if (alerta.getIdAlerta() > 0) {
            throw new IllegalStateException("Alerta ja persistido (id="
                    + alerta.getIdAlerta() + "): use alterar");
        }
        validarReferencias(alerta);

        try (Connection conexao = abrirConexao()) {
            int id = proximoId(conexao, SEQUENCE);

            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
                ps.setInt(1, id);
                ps.setInt(2, alerta.getUsuario().getIdUsuario());
                ps.setInt(3, alerta.getCriptoativo().getIdCripto());
                ps.setBigDecimal(4, alerta.getLimiteVariacao());
                ps.setString(5, simNao(alerta.isAtivado()));
                ps.setDate(6, paraSql(alerta.getDataConfiguracao()));
                ps.executeUpdate();
            }

            alerta.setIdAlerta(id);
            return id;
        }
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza usuario, criptoativo, limite, situacao (S/N) e data do alerta
     * identificado pelo seu id.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     * @throws IllegalStateException quando o usuario ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int alterar(Alerta alerta) throws SQLException {
        validarReferencias(alerta);

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, alerta.getUsuario().getIdUsuario());
            ps.setInt(2, alerta.getCriptoativo().getIdCripto());
            ps.setBigDecimal(3, alerta.getLimiteVariacao());
            ps.setString(4, simNao(alerta.isAtivado()));
            ps.setDate(5, paraSql(alerta.getDataConfiguracao()));
            ps.setInt(6, alerta.getIdAlerta());

            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui o alerta pelo id. Nenhuma tabela referencia alerta, entao a
     * exclusao nao depende de outras linhas.
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    @Override
    public int excluir(int idAlerta) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idAlerta);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /**
     * Busca um alerta pela chave primaria, ja com o usuario e o criptoativo
     * montados. Retorna null quando nao existe.
     */
    @Override
    public Alerta buscarPorId(int idAlerta) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idAlerta);
            List<Alerta> alertas = lerAlertas(conexao, ps);
            return alertas.isEmpty() ? null : alertas.get(0);
        }
    }

    /** Lista todos os alertas (ativados ou nao), ordenados pelo id. */
    @Override
    public List<Alerta> listarTodos() throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS)) {

            return lerAlertas(conexao, ps);
        }
    }

    /**
     * Lista os alertas configurados por um usuario, ordenados pelo id. O
     * usuario e lido uma vez e compartilhado por todos os alertas da lista.
     * Usuario inexistente ou sem alertas devolve lista vazia.
     */
    public List<Alerta> listarPorUsuario(int idUsuario) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_USUARIO)) {

            ps.setInt(1, idUsuario);
            return lerAlertas(conexao, ps);
        }
    }

    /** Lista somente os alertas ativados (ativado = 'S'), ordenados pelo id. */
    public List<Alerta> listarAtivos() throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ATIVADO)) {

            ps.setString(1, simNao(true));
            return lerAlertas(conexao, ps);
        }
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * Pre-condicoes de FK: o alerta precisa apontar para um usuario e um
     * criptoativo ja gravados (id > 0), senao o INSERT/UPDATE violaria
     * fk_alerta_usuario ou fk_alerta_cripto.
     */
    private static void validarReferencias(Alerta alerta) {
        Usuario usuario = alerta.getUsuario();
        if (usuario == null || usuario.getIdUsuario() <= 0) {
            throw new IllegalStateException(
                    "Alerta exige um usuario ja persistido: grave o usuario (UsuarioDAO.inserir) antes do alerta.");
        }
        Criptoativo criptoativo = alerta.getCriptoativo();
        if (criptoativo == null || criptoativo.getIdCripto() <= 0) {
            throw new IllegalStateException(
                    "Alerta exige um criptoativo ja persistido: grave o criptoativo (CriptoativoDAO.inserir) antes do alerta.");
        }
    }

    /**
     * Executa a consulta ja parametrizada e monta os alertas. Usuarios e
     * criptoativos ficam guardados por id durante a leitura, para que cada um
     * seja buscado uma unica vez e o mesmo objeto sirva a todos os alertas.
     */
    private List<Alerta> lerAlertas(Connection conexao, PreparedStatement ps) throws SQLException {
        List<Alerta> alertas = new ArrayList<>();
        Map<Integer, Usuario> usuarios = new HashMap<>();
        Map<Integer, Criptoativo> criptoativos = new HashMap<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alertas.add(montarAlerta(conexao, rs, usuarios, criptoativos));
            }
        }
        return alertas;
    }

    /**
     * Converte a linha atual do ResultSet em um Alerta. A reconstrucao usa o
     * construtor principal (que sempre cria o alerta ativado) seguido de
     * setAtivado com o 'S'/'N' gravado. O limite e normalizado para a escala de
     * percentual, porque o Oracle devolve o NUMBER sem a escala da coluna.
     */
    private Alerta montarAlerta(Connection conexao, ResultSet rs,
                                Map<Integer, Usuario> usuarios,
                                Map<Integer, Criptoativo> criptoativos) throws SQLException {
        int idUsuario = rs.getInt("id_usuario");
        Usuario usuario = usuarios.get(idUsuario);
        if (usuario == null) {
            usuario = usuarioDAO.buscarPorId(conexao, idUsuario);
            usuarios.put(idUsuario, usuario);
        }

        int idCripto = rs.getInt("id_cripto");
        Criptoativo criptoativo = criptoativos.get(idCripto);
        if (criptoativo == null) {
            criptoativo = criptoativoDAO.buscarPorId(conexao, idCripto);
            criptoativos.put(idCripto, criptoativo);
        }

        Alerta alerta = new Alerta(
                rs.getInt("id_alerta"),
                usuario,
                criptoativo,
                Valores.percentual(rs.getBigDecimal("limite_variacao")),
                paraLocalDate(rs.getDate("data_configuracao")));

        alerta.setAtivado(deSimNao(rs.getString("ativado")));
        return alerta;
    }
}
