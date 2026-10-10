package com.criptoinvest.dao;

import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Posicao;
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
 * DAO (Data Access Object) da tabela POSICAO.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) as posicoes:
 * o saldo agregado de cada criptoativo dentro de uma carteira (associativa
 * N:N carteira x criptoativo).
 *
 * Tabela:
 *   posicao (id_posicao PK, id_carteira FK polimorfica, id_cripto FK,
 *            quantidade_atual, preco_medio_compra, data_primeira_aquisicao,
 *            data_ultima_atualizacao; UK (id_carteira, id_cripto))
 * Sequence:
 *   seq_posicao -> geracao da chave primaria
 *
 * A posicao deriva das transacoes: no fluxo normal ela e gravada por
 * TransacaoDAO.registrar, que chama salvar(Connection, Posicao) na mesma
 * transacao do INSERT da transacao e do UPDATE do saldo. Uma posicao zerada
 * (venda de todo o saldo) continua gravada com quantidade 0, como no modelo.
 *
 * Na leitura, a carteira vem de CarteiraDAO.buscarPorId(Connection, int) (PF ou
 * PJ, sem o historico; use CarteiraDAO.carregarHistorico quando precisar) e o
 * criptoativo de CriptoativoDAO.buscarPorId(Connection, int), na mesma conexao.
 */
public class PosicaoDAO extends BaseDAO implements CrudDAO<Posicao> {

    private static final String SEQUENCE = "seq_posicao";

    private static final String SQL_INSERT =
            "INSERT INTO posicao (id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, "
          + "                     data_primeira_aquisicao, data_ultima_atualizacao) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE posicao "
          + "   SET quantidade_atual = ?, preco_medio_compra = ?, data_primeira_aquisicao = ?, "
          + "       data_ultima_atualizacao = ?, id_carteira = ?, id_cripto = ? "
          + " WHERE id_posicao = ?";

    private static final String SQL_DELETE =
            "DELETE FROM posicao WHERE id_posicao = ?";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, "
          + "       data_primeira_aquisicao, data_ultima_atualizacao "
          + "  FROM posicao WHERE id_posicao = ?";

    private static final String SQL_SELECT_POR_CARTEIRA_E_CRIPTO =
            "SELECT id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, "
          + "       data_primeira_aquisicao, data_ultima_atualizacao "
          + "  FROM posicao WHERE id_carteira = ? AND id_cripto = ?";

    /** So a PK da linha (id_carteira, id_cripto): usada pelo "upsert" de salvar. */
    private static final String SQL_SELECT_ID_POR_CARTEIRA_E_CRIPTO =
            "SELECT id_posicao FROM posicao WHERE id_carteira = ? AND id_cripto = ?";

    private static final String SQL_SELECT_POR_CARTEIRA =
            "SELECT id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, "
          + "       data_primeira_aquisicao, data_ultima_atualizacao "
          + "  FROM posicao WHERE id_carteira = ? ORDER BY id_posicao";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_posicao, id_carteira, id_cripto, quantidade_atual, preco_medio_compra, "
          + "       data_primeira_aquisicao, data_ultima_atualizacao "
          + "  FROM posicao ORDER BY id_posicao";

    private final CriptoativoDAO criptoativoDAO = new CriptoativoDAO();

    /** Criado sob demanda: ver carteiraDAO(). */
    private CarteiraDAO carteiraDAO;

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Insere uma posicao ainda nao persistida (id 0). Carteira e criptoativo
     * precisam ja estar gravados. O id vem da sequence seq_posicao e so e
     * atribuido ao objeto depois do INSERT: se ja houver posicao do mesmo par
     * carteira x criptoativo, o banco recusa (ORA-00001 em uk_posicao_cart_cripto),
     * a excecao e relancada e o objeto continua com id 0.
     *
     * Para gravar o efeito de uma transacao use TransacaoDAO.registrar.
     *
     * @return o id gerado pela sequence
     * @throws IllegalStateException quando a posicao ja tem id (use alterar), ou
     *         quando a carteira ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int inserir(Posicao posicao) throws SQLException {
        if (posicao.getIdPosicao() > 0) {
            throw new IllegalStateException("Posicao ja persistida (id="
                    + posicao.getIdPosicao() + "): use alterar");
        }
        validarReferencias(posicao);

        try (Connection conexao = abrirConexao()) {
            return inserir(conexao, posicao);
        }
    }

    /**
     * INSERT com a conexao recebida (pode estar dentro de uma transacao). Obtem
     * o id de seq_posicao e so o atribui ao objeto depois do executeUpdate.
     */
    private int inserir(Connection conexao, Posicao posicao) throws SQLException {
        int id = proximoId(conexao, SEQUENCE);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, id);
            ps.setInt(2, posicao.getCarteira().getIdCarteira());
            ps.setInt(3, posicao.getCriptoativo().getIdCripto());
            ps.setBigDecimal(4, posicao.getQuantidadeAtual());
            ps.setBigDecimal(5, posicao.getPrecoMedioCompra());
            ps.setDate(6, paraSql(posicao.getDataPrimeiraAquisicao()));
            ps.setDate(7, paraSql(posicao.getDataUltimaAtualizacao()));
            ps.executeUpdate();
        }

        posicao.setIdPosicao(id);
        return id;
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza todas as colunas da posicao identificada pelo seu id.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     * @throws IllegalStateException quando a carteira ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int alterar(Posicao posicao) throws SQLException {
        validarReferencias(posicao);

        try (Connection conexao = abrirConexao()) {
            return alterar(conexao, posicao);
        }
    }

    /** UPDATE com a conexao recebida (pode estar dentro de uma transacao). */
    private int alterar(Connection conexao, Posicao posicao) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {
            ps.setBigDecimal(1, posicao.getQuantidadeAtual());
            ps.setBigDecimal(2, posicao.getPrecoMedioCompra());
            ps.setDate(3, paraSql(posicao.getDataPrimeiraAquisicao()));
            ps.setDate(4, paraSql(posicao.getDataUltimaAtualizacao()));
            ps.setInt(5, posicao.getCarteira().getIdCarteira());
            ps.setInt(6, posicao.getCriptoativo().getIdCripto());
            ps.setInt(7, posicao.getIdPosicao());

            return ps.executeUpdate();
        }
    }

    /**
     * "Upsert" pela chave unica (id_carteira, id_cripto), com a conexao recebida
     * (usado por TransacaoDAO.registrar, dentro da transacao dele).
     *
     * Se ja existe linha do par, o id dela e atribuido ao objeto (a UK manda
     * sobre o id que o objeto trazia) e a linha recebe um UPDATE com os valores
     * do objeto; senao a posicao e inserida com um id novo de seq_posicao.
     * Sem MERGE, para o SQL continuar portavel entre Oracle e H2.
     *
     * @return o id da posicao gravada
     * @throws IllegalStateException quando a carteira ou o criptoativo ainda nao foram persistidos
     */
    int salvar(Connection conexao, Posicao posicao) throws SQLException {
        validarReferencias(posicao);

        int idExistente = buscarIdPorCarteiraECripto(conexao,
                posicao.getCarteira().getIdCarteira(), posicao.getCriptoativo().getIdCripto());

        if (idExistente > 0) {
            posicao.setIdPosicao(idExistente);
            alterar(conexao, posicao);
            return idExistente;
        }
        return inserir(conexao, posicao);
    }

    /**
     * Id da posicao gravada para o par carteira x criptoativo, com a conexao
     * recebida. Retorna 0 quando o par ainda nao tem posicao.
     */
    int buscarIdPorCarteiraECripto(Connection conexao, int idCarteira, int idCripto) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_ID_POR_CARTEIRA_E_CRIPTO)) {
            ps.setInt(1, idCarteira);
            ps.setInt(2, idCripto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id_posicao") : 0;
            }
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a posicao pelo id. Nenhuma tabela referencia posicao. Atencao: o
     * saldo da posicao deriva das transacoes, que continuam gravadas.
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    @Override
    public int excluir(int idPosicao) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idPosicao);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /**
     * Busca uma posicao pela chave primaria, com a carteira (PF ou PJ) e o
     * criptoativo montados. Retorna null quando nao existe.
     */
    @Override
    public Posicao buscarPorId(int idPosicao) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idPosicao);
            List<Posicao> posicoes = lerPosicoes(conexao, ps, new HashMap<>());
            return posicoes.isEmpty() ? null : posicoes.get(0);
        }
    }

    /**
     * Busca a posicao de um criptoativo em uma carteira (chave unica
     * id_carteira + id_cripto). Retorna null quando a carteira nao tem posicao
     * desse criptoativo.
     */
    public Posicao buscarPorCarteiraECripto(int idCarteira, int idCripto) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA_E_CRIPTO)) {

            ps.setInt(1, idCarteira);
            ps.setInt(2, idCripto);
            List<Posicao> posicoes = lerPosicoes(conexao, ps, new HashMap<>());
            return posicoes.isEmpty() ? null : posicoes.get(0);
        }
    }

    /** Lista todas as posicoes gravadas, ordenadas pelo id. */
    @Override
    public List<Posicao> listarTodos() throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS)) {

            return lerPosicoes(conexao, ps, new HashMap<>());
        }
    }

    /**
     * Lista as posicoes de uma carteira, ordenadas pelo id. A carteira e lida
     * uma vez e compartilhada por todas as posicoes da lista. Carteira
     * inexistente ou sem posicoes devolve lista vazia.
     */
    public List<Posicao> listarPorCarteira(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA)) {

            ps.setInt(1, idCarteira);
            return lerPosicoes(conexao, ps, new HashMap<>());
        }
    }

    /**
     * Versao de pacote, com a conexao recebida (usada por
     * CarteiraDAO.carregarHistorico): as posicoes sao reconstruidas apontando
     * para o PROPRIO objeto carteira recebido, sem busca-lo de novo.
     *
     * @throws IllegalArgumentException quando a carteira e nula
     * @throws IllegalStateException    quando a carteira ainda nao foi persistida (id 0)
     */
    List<Posicao> listarPorCarteira(Connection conexao, Carteira carteira) throws SQLException {
        if (carteira == null) {
            throw new IllegalArgumentException("Informe a carteira cujas posicoes serao lidas.");
        }
        if (carteira.getIdCarteira() <= 0) {
            throw new IllegalStateException(
                    "Carteira ainda nao persistida (id 0): nao ha posicoes gravadas para ela.");
        }

        Map<Integer, Carteira> carteiras = new HashMap<>();
        carteiras.put(carteira.getIdCarteira(), carteira);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA)) {
            ps.setInt(1, carteira.getIdCarteira());
            return lerPosicoes(conexao, ps, carteiras);
        }
    }

    // ------------------------------------------------------------------------
    // AUXILIARES
    // ------------------------------------------------------------------------

    /**
     * CarteiraDAO criado na primeira vez em que e preciso. Nao e um atributo
     * inicializado na declaracao porque o CarteiraDAO usa este DAO em
     * carregarHistorico: dois DAOs que se criam mutuamente ao serem construidos
     * entrariam em recursao infinita.
     */
    private CarteiraDAO carteiraDAO() {
        if (carteiraDAO == null) {
            carteiraDAO = new CarteiraDAO();
        }
        return carteiraDAO;
    }

    /**
     * Pre-condicoes de FK: a posicao precisa apontar para uma carteira e um
     * criptoativo ja gravados (id > 0), senao o INSERT/UPDATE violaria
     * fk_posicao_carteira ou fk_posicao_cripto.
     */
    private static void validarReferencias(Posicao posicao) {
        Carteira carteira = posicao.getCarteira();
        if (carteira == null || carteira.getIdCarteira() <= 0) {
            throw new IllegalStateException(
                    "Posicao exige uma carteira ja persistida: grave a carteira antes da posicao.");
        }
        Criptoativo criptoativo = posicao.getCriptoativo();
        if (criptoativo == null || criptoativo.getIdCripto() <= 0) {
            throw new IllegalStateException(
                    "Posicao exige um criptoativo ja persistido: grave o criptoativo antes da posicao.");
        }
    }

    /**
     * Executa a consulta ja parametrizada e monta as posicoes. Carteiras e
     * criptoativos ficam guardados por id durante a leitura, para que cada um
     * seja buscado uma unica vez e o mesmo objeto sirva a todas as linhas dele.
     *
     * @param carteiras carteiras ja conhecidas, por id (pode vir preenchido,
     *                  como em listarPorCarteira(Connection, Carteira))
     */
    private List<Posicao> lerPosicoes(Connection conexao, PreparedStatement ps,
                                      Map<Integer, Carteira> carteiras) throws SQLException {
        List<Posicao> posicoes = new ArrayList<>();
        Map<Integer, Criptoativo> criptoativos = new HashMap<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posicoes.add(montarPosicao(conexao, rs, carteiras, criptoativos));
            }
        }
        return posicoes;
    }

    /**
     * Converte a linha atual do ResultSet em uma Posicao pelo construtor de
     * reconstrucao. As escalas sao normalizadas aqui (quantidade e preco medio
     * com 8 casas), porque o Oracle devolve o NUMBER sem a escala da coluna.
     */
    private Posicao montarPosicao(Connection conexao, ResultSet rs, Map<Integer, Carteira> carteiras,
                                  Map<Integer, Criptoativo> criptoativos) throws SQLException {
        int idCarteira = rs.getInt("id_carteira");
        Carteira carteira = carteiras.get(idCarteira);
        if (carteira == null) {
            carteira = carteiraDAO().buscarPorId(conexao, idCarteira);
            carteiras.put(idCarteira, carteira);
        }

        int idCripto = rs.getInt("id_cripto");
        Criptoativo criptoativo = criptoativos.get(idCripto);
        if (criptoativo == null) {
            criptoativo = criptoativoDAO.buscarPorId(conexao, idCripto);
            criptoativos.put(idCripto, criptoativo);
        }

        return new Posicao(
                rs.getInt("id_posicao"),
                carteira,
                criptoativo,
                Valores.cripto(rs.getBigDecimal("quantidade_atual")),
                Valores.cripto(rs.getBigDecimal("preco_medio_compra")),
                paraLocalDate(rs.getDate("data_primeira_aquisicao")),
                paraLocalDate(rs.getDate("data_ultima_atualizacao")));
    }
}
