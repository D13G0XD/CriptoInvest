package com.criptoinvest.dao;

import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Posicao;
import com.criptoinvest.model.Transacao;
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
 * DAO (Data Access Object) da tabela TRANSACAO.
 *
 * Responsavel por INSERIR, ALTERAR, EXCLUIR e EXIBIR (consultar) as transacoes
 * de compra e venda, e por REGISTRAR uma operacao da carteira de forma atomica.
 *
 * Tabela:
 *   transacao (id_transacao PK, id_carteira FK polimorfica, id_cripto FK, tipo,
 *              quantidade, preco_unitario, taxa, data_operacao, observacao)
 * Sequence:
 *   seq_transacao -> geracao da chave primaria
 *
 * Duas formas de gravar:
 *   - registrar(t): o caminho normal. Persiste numa UNICA transacao JDBC o que
 *     Carteira.registrarTransacao ja aplicou em memoria: a linha de transacao,
 *     a posicao do criptoativo e o novo saldo em reais da carteira;
 *   - inserir(t): grava SO a linha de transacao (CRUD basico), sem mexer em
 *     saldo nem em posicao.
 *
 * Na leitura, a carteira vem de CarteiraDAO.buscarPorId(Connection, int) (PF ou
 * PJ, sem o historico; use CarteiraDAO.carregarHistorico quando precisar) e o
 * criptoativo de CriptoativoDAO.buscarPorId(Connection, int), na mesma conexao.
 */
public class TransacaoDAO extends BaseDAO implements CrudDAO<Transacao> {

    private static final String SEQUENCE = "seq_transacao";

    private static final String SQL_INSERT =
            "INSERT INTO transacao (id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, "
          + "                       taxa, data_operacao, observacao) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE transacao "
          + "   SET id_carteira = ?, id_cripto = ?, tipo = ?, quantidade = ?, preco_unitario = ?, "
          + "       taxa = ?, data_operacao = ?, observacao = ? "
          + " WHERE id_transacao = ?";

    private static final String SQL_DELETE =
            "DELETE FROM transacao WHERE id_transacao = ?";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, "
          + "       taxa, data_operacao, observacao "
          + "  FROM transacao WHERE id_transacao = ?";

    private static final String SQL_SELECT_POR_CARTEIRA =
            "SELECT id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, "
          + "       taxa, data_operacao, observacao "
          + "  FROM transacao WHERE id_carteira = ? ORDER BY id_transacao";

    private static final String SQL_SELECT_TODOS =
            "SELECT id_transacao, id_carteira, id_cripto, tipo, quantidade, preco_unitario, "
          + "       taxa, data_operacao, observacao "
          + "  FROM transacao ORDER BY id_transacao";

    private final CriptoativoDAO criptoativoDAO = new CriptoativoDAO();
    private final PosicaoDAO posicaoDAO = new PosicaoDAO();

    /** Criado sob demanda: ver carteiraDAO(). */
    private CarteiraDAO carteiraDAO;

    // ------------------------------------------------------------------------
    // REGISTRAR (transacao + posicao + saldo, atomico)
    // ------------------------------------------------------------------------

    /**
     * Persiste, numa UNICA transacao JDBC, o efeito de Carteira.registrarTransacao
     * ja aplicado em memoria:
     *   1) INSERT da transacao (id de seq_transacao);
     *   2) PosicaoDAO.salvar da posicao da carteira para o criptoativo ("upsert"
     *      pela UK id_carteira + id_cripto);
     *   3) CarteiraDAO.atualizarSaldo com o saldo em reais atual da carteira.
     * Ou grava tudo (commit) ou nada (rollback).
     *
     * Uso:
     *   if (carteira.registrarTransacao(t)) { transacaoDAO.registrar(t); }
     *
     * O saldo e gravado como valor ABSOLUTO (o saldo atual do objeto), nao como
     * diferenca. Por isso quem chama e responsavel por:
     *   - a carteira ter vindo do banco (CarteiraDAO.buscarPorId seguido de
     *     CarteiraDAO.carregarHistorico) ou ter sido criada nesta execucao. O
     *     carregarHistorico vem ANTES de qualquer depositar/sacar na copia
     *     relida (ou o saldo alterado e gravado antes, com atualizarSaldo ou
     *     alterar), porque ele recusa um saldo em memoria diferente do gravado;
     *   - nao existir outra copia em memoria da mesma carteira que tambem
     *     registre transacoes (a ultima a gravar sobrescreveria o saldo da outra).
     * E aqui sao conferidos:
     *   - a transacao e a ULTIMA do historico em memoria e todas as anteriores ja
     *     foram gravadas (id > 0) - senao o saldo e a posicao gravados levariam o
     *     efeito de uma operacao sem a linha de transacao correspondente;
     *   - se a carteira abriu em memoria uma posicao nova para um criptoativo que
     *     ja tem posicao gravada, a operacao e recusada, porque gravar a posicao
     *     parcial da memoria sobrescreveria a do banco; nesse caso a carteira em
     *     memoria fica bloqueada: releia-a com buscarPorId + carregarHistorico e
     *     aplique a operacao de novo.
     *
     * Em caso de falha, o banco volta ao estado anterior, o id da transacao volta
     * a 0 e o da posicao ao valor que tinha antes; o saldo, a posicao e o
     * historico em memoria continuam com a operacao aplicada. A partir dai a
     * carteira fica BLOQUEADA para registrar: a transacao nao gravada (id 0)
     * continua no historico e todo registrar seguinte e recusado ate a carteira
     * ser recarregada do banco (CarteiraDAO.buscarPorId + carregarHistorico).
     *
     * @param transacao transacao ja aceita pela carteira (getCarteira() != null)
     * @return o id gerado para a transacao
     * @throws IllegalStateException quando a transacao ja tem id, nao foi aceita
     *         pela carteira, quando a carteira ou o criptoativo ainda nao foram
     *         persistidos, quando ha no historico em memoria outra transacao
     *         aplicada e nao gravada, ou quando a carteira nao tem o historico
     *         carregado
     */
    public int registrar(Transacao transacao) throws SQLException {
        validarNova(transacao);

        Carteira carteira = transacao.getCarteira();
        Criptoativo criptoativo = transacao.getCriptoativo();
        Posicao posicao = carteira.buscarPosicao(criptoativo.getIdCripto());
        if (posicao == null || !carteira.getTransacoes().contains(transacao)) {
            throw new IllegalStateException("Transacao nao aplicada pela carteira: chame "
                    + "Carteira.registrarTransacao (e confira o retorno true) antes de registrar.");
        }
        exigirHistoricoGravado(carteira, transacao);

        int idPosicaoAnterior = posicao.getIdPosicao();

        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                if (idPosicaoAnterior == 0 && posicaoDAO.buscarIdPorCarteiraECripto(conexao,
                        carteira.getIdCarteira(), criptoativo.getIdCripto()) > 0) {
                    throw new IllegalStateException("A carteira id=" + carteira.getIdCarteira()
                            + " ja tem posicao gravada de " + criptoativo.getSigla()
                            + ", mas a carteira em memoria nao a conhece: releia a carteira "
                            + "(CarteiraDAO.buscarPorId), carregue o historico "
                            + "(CarteiraDAO.carregarHistorico) e aplique a operacao de novo "
                            + "(Carteira.registrarTransacao) antes de registrar.");
                }

                int idTransacao = inserir(conexao, transacao);
                posicaoDAO.salvar(conexao, posicao);
                int linhas = carteiraDAO().atualizarSaldo(conexao,
                        carteira.getIdCarteira(), carteira.getSaldoReais());
                if (linhas == 0) {
                    throw new SQLException("Carteira id=" + carteira.getIdCarteira()
                            + " nao encontrada ao atualizar o saldo.");
                }

                conexao.commit();
                return idTransacao;
            } catch (SQLException | RuntimeException e) {
                // RuntimeException tambem desfaz: o driver Oracle faz COMMIT ao
                // fechar uma conexao com transacao pendente.
                desfazer(conexao);
                transacao.setIdTransacao(0);
                posicao.setIdPosicao(idPosicaoAnterior);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------------
    // INSERIR
    // ------------------------------------------------------------------------

    /**
     * Grava SOMENTE a linha de transacao, sem atualizar saldo nem posicao (para
     * gravar uma operacao da carteira use registrar). O id vem da sequence
     * seq_transacao e so e atribuido ao objeto depois do INSERT.
     *
     * @return o id gerado pela sequence
     * @throws IllegalStateException quando a transacao ja tem id (use alterar),
     *         ou quando a carteira ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int inserir(Transacao transacao) throws SQLException {
        validarNova(transacao);

        try (Connection conexao = abrirConexao()) {
            return inserir(conexao, transacao);
        }
    }

    /**
     * INSERT com a conexao recebida (pode estar dentro de uma transacao). Obtem
     * o id de seq_transacao e so o atribui ao objeto depois do executeUpdate.
     */
    private int inserir(Connection conexao, Transacao transacao) throws SQLException {
        int id = proximoId(conexao, SEQUENCE);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, id);
            ps.setInt(2, transacao.getCarteira().getIdCarteira());
            ps.setInt(3, transacao.getCriptoativo().getIdCripto());
            ps.setString(4, transacao.getTipo());
            ps.setBigDecimal(5, transacao.getQuantidade());
            ps.setBigDecimal(6, transacao.getPrecoUnitario());
            ps.setBigDecimal(7, transacao.getTaxa());
            ps.setDate(8, paraSql(transacao.getDataOperacao()));
            ps.setString(9, textoOuNulo(transacao.getObservacao()));
            ps.executeUpdate();
        }

        transacao.setIdTransacao(id);
        return id;
    }

    // ------------------------------------------------------------------------
    // ALTERAR
    // ------------------------------------------------------------------------

    /**
     * Atualiza todas as colunas da transacao identificada pelo seu id. Corrige
     * apenas o registro: nao refaz o saldo da carteira nem a posicao.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     * @throws IllegalStateException quando a carteira ou o criptoativo ainda nao foram persistidos
     */
    @Override
    public int alterar(Transacao transacao) throws SQLException {
        validarReferencias(transacao);

        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, transacao.getCarteira().getIdCarteira());
            ps.setInt(2, transacao.getCriptoativo().getIdCripto());
            ps.setString(3, transacao.getTipo());
            ps.setBigDecimal(4, transacao.getQuantidade());
            ps.setBigDecimal(5, transacao.getPrecoUnitario());
            ps.setBigDecimal(6, transacao.getTaxa());
            ps.setDate(7, paraSql(transacao.getDataOperacao()));
            ps.setString(8, textoOuNulo(transacao.getObservacao()));
            ps.setInt(9, transacao.getIdTransacao());

            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXCLUIR
    // ------------------------------------------------------------------------

    /**
     * Exclui a transacao pelo id. Nenhuma tabela referencia transacao. Apaga
     * apenas o registro: nao desfaz o efeito no saldo nem na posicao.
     *
     * @return quantidade de linhas excluidas (0 = id inexistente)
     */
    @Override
    public int excluir(int idTransacao) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, idTransacao);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------
    // EXIBIR / CONSULTAR
    // ------------------------------------------------------------------------

    /**
     * Busca uma transacao pela chave primaria, com a carteira (PF ou PJ, sem
     * historico) e o criptoativo montados. Retorna null quando nao existe.
     */
    @Override
    public Transacao buscarPorId(int idTransacao) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, idTransacao);
            List<Transacao> transacoes = lerTransacoes(conexao, ps, new HashMap<>());
            return transacoes.isEmpty() ? null : transacoes.get(0);
        }
    }

    /** Lista todas as transacoes gravadas, ordenadas pelo id. */
    @Override
    public List<Transacao> listarTodos() throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_TODOS)) {

            return lerTransacoes(conexao, ps, new HashMap<>());
        }
    }

    /**
     * Lista o historico de transacoes de uma carteira, na ordem em que foram
     * gravadas (id). A carteira e lida uma vez e compartilhada por todas as
     * transacoes da lista. Carteira inexistente ou sem transacoes devolve lista vazia.
     */
    public List<Transacao> listarPorCarteira(int idCarteira) throws SQLException {
        try (Connection conexao = abrirConexao();
             PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA)) {

            ps.setInt(1, idCarteira);
            return lerTransacoes(conexao, ps, new HashMap<>());
        }
    }

    /**
     * Versao de pacote, com a conexao recebida (usada por
     * CarteiraDAO.carregarHistorico): as transacoes sao reconstruidas apontando
     * para o PROPRIO objeto carteira recebido, sem busca-lo de novo.
     *
     * @throws IllegalArgumentException quando a carteira e nula
     * @throws IllegalStateException    quando a carteira ainda nao foi persistida (id 0)
     */
    List<Transacao> listarPorCarteira(Connection conexao, Carteira carteira) throws SQLException {
        if (carteira == null) {
            throw new IllegalArgumentException("Informe a carteira cujas transacoes serao lidas.");
        }
        if (carteira.getIdCarteira() <= 0) {
            throw new IllegalStateException(
                    "Carteira ainda nao persistida (id 0): nao ha transacoes gravadas para ela.");
        }

        Map<Integer, Carteira> carteiras = new HashMap<>();
        carteiras.put(carteira.getIdCarteira(), carteira);

        try (PreparedStatement ps = conexao.prepareStatement(SQL_SELECT_POR_CARTEIRA)) {
            ps.setInt(1, carteira.getIdCarteira());
            return lerTransacoes(conexao, ps, carteiras);
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
     * Pre-condicao de registrar: a transacao e a ultima do historico em memoria
     * e todas as anteriores ja tem id (foram gravadas). Uma transacao anterior
     * com id 0 foi aplicada em memoria e nao gravada (registrar falhou e o banco
     * voltou, ou nunca foi chamado); como o saldo e a posicao sao gravados pelo
     * valor atual do objeto, registrar agora levaria ao banco o efeito dela sem
     * a linha de transacao.
     */
    private static void exigirHistoricoGravado(Carteira carteira, Transacao transacao) {
        List<Transacao> historico = carteira.getTransacoes();
        boolean ultima = !historico.isEmpty() && historico.get(historico.size() - 1) == transacao;
        boolean anterioresGravadas = true;
        for (int i = 0; i < historico.size() - 1; i++) {
            if (historico.get(i).getIdTransacao() <= 0) {
                anterioresGravadas = false;
                break;
            }
        }
        if (!ultima || !anterioresGravadas) {
            throw new IllegalStateException("A carteira id=" + carteira.getIdCarteira()
                    + " tem transacao aplicada em memoria e nao gravada (registrar anterior falhou ou nao foi "
                    + "chamado): recarregue a carteira do banco (CarteiraDAO.buscarPorId + carregarHistorico) "
                    + "antes de registrar novas transacoes.");
        }
    }

    /** Pre-condicoes de um INSERT: transacao ainda sem id e FKs ja persistidas. */
    private static void validarNova(Transacao transacao) {
        if (transacao.getIdTransacao() > 0) {
            throw new IllegalStateException("Transacao ja persistida (id="
                    + transacao.getIdTransacao() + "): use alterar");
        }
        validarReferencias(transacao);
    }

    /**
     * Pre-condicoes de FK: a transacao precisa ter carteira (so recebe uma quando
     * Carteira.registrarTransacao aceita a operacao) e apontar para carteira e
     * criptoativo ja gravados (id > 0), senao o INSERT/UPDATE violaria
     * fk_transacao_carteira ou fk_transacao_cripto.
     */
    private static void validarReferencias(Transacao transacao) {
        Carteira carteira = transacao.getCarteira();
        if (carteira == null) {
            throw new IllegalStateException("Transacao sem carteira: ela so recebe a carteira quando "
                    + "Carteira.registrarTransacao aceita a operacao.");
        }
        if (carteira.getIdCarteira() <= 0) {
            throw new IllegalStateException(
                    "Transacao exige uma carteira ja persistida: grave a carteira antes da transacao.");
        }
        if (transacao.getCriptoativo().getIdCripto() <= 0) {
            throw new IllegalStateException("Transacao exige um criptoativo ja persistido: grave "
                    + transacao.getCriptoativo().getSigla() + " antes da transacao.");
        }
    }

    /**
     * Executa a consulta ja parametrizada e monta as transacoes. Carteiras e
     * criptoativos ficam guardados por id durante a leitura, para que cada um
     * seja buscado uma unica vez e o mesmo objeto sirva a todas as linhas dele.
     *
     * @param carteiras carteiras ja conhecidas, por id (pode vir preenchido,
     *                  como em listarPorCarteira(Connection, Carteira))
     */
    private List<Transacao> lerTransacoes(Connection conexao, PreparedStatement ps,
                                          Map<Integer, Carteira> carteiras) throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        Map<Integer, Criptoativo> criptoativos = new HashMap<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                transacoes.add(montarTransacao(conexao, rs, carteiras, criptoativos));
            }
        }
        return transacoes;
    }

    /**
     * Converte a linha atual do ResultSet em uma Transacao pelo construtor de
     * reconstrucao (taxa gravada, sem recalcular). As escalas sao normalizadas
     * aqui (quantidade e preco com 8 casas, taxa com 2), porque o Oracle devolve
     * o NUMBER sem a escala da coluna.
     */
    private Transacao montarTransacao(Connection conexao, ResultSet rs, Map<Integer, Carteira> carteiras,
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

        return new Transacao(
                rs.getInt("id_transacao"),
                carteira,
                criptoativo,
                rs.getString("tipo"),
                Valores.cripto(rs.getBigDecimal("quantidade")),
                Valores.cripto(rs.getBigDecimal("preco_unitario")),
                Valores.dinheiro(rs.getBigDecimal("taxa")),
                paraLocalDate(rs.getDate("data_operacao")),
                rs.getString("observacao"));
    }
}
