package com.criptoinvest.dao;

import com.criptoinvest.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Superclasse de todos os DAOs: concentra o acesso a conexao, a geracao de
 * chaves pelas sequences, o controle de transacao e as conversoes entre os
 * tipos do Java e os do Oracle.
 *
 * Padrao de transacao usado pelas operacoes que tocam mais de uma tabela:
 *
 *   try (Connection c = abrirConexao()) {
 *       c.setAutoCommit(false);
 *       try {
 *           int id = inserir(c, entidade);
 *           c.commit();
 *           return id;
 *       } catch (SQLException | RuntimeException e) {
 *           desfazer(c);
 *           // + devolver 0 aos ids atribuidos aos objetos nesta transacao
 *           throw e;
 *       }
 *   }
 *
 * O catch inclui RuntimeException (ex.: IllegalStateException de uma
 * pre-condicao conferida no meio da transacao) porque o driver Oracle faz
 * COMMIT implicito ao fechar uma conexao com autoCommit desligado: sem o
 * rollback, o que ja tivesse sido gravado ficaria pela metade no banco.
 *
 * Os metodos com Connection (visibilidade de pacote) nao controlam a
 * transacao nem zeram ids: isso e responsabilidade do metodo publico que
 * abriu a conexao.
 */
public abstract class BaseDAO {

    /** Nome aceito para uma sequence: identificador SQL simples (sem espacos, aspas ou ponto e virgula). */
    private static final Pattern NOME_SEQUENCE = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]*");

    /**
     * Abre uma nova conexao. Os dados de conexao (URL, usuario e senha) ficam
     * somente na ConnectionFactory.
     */
    protected Connection abrirConexao() throws SQLException {
        return ConnectionFactory.getConnection();
    }

    /**
     * Obtem o proximo valor da sequence informada.
     *
     * O nome da sequence e concatenado ao SQL (nao existe parametro "?" para
     * nome de objeto), por isso deve vir sempre de uma constante interna do DAO,
     * nunca de dado do usuario; mesmo assim, nomes fora do padrao de
     * identificador sao recusados.
     *
     * @param conexao  conexao em uso (a mesma da operacao, quando houver transacao)
     * @param sequence nome da sequence, ex.: seq_criptoativo
     * @return o valor de sequence.NEXTVAL
     */
    protected int proximoId(Connection conexao, String sequence) throws SQLException {
        if (sequence == null || !NOME_SEQUENCE.matcher(sequence).matches()) {
            throw new IllegalArgumentException("Nome de sequence invalido: " + sequence);
        }
        String sql = "SELECT " + sequence + ".NEXTVAL FROM dual";
        try (PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Nao foi possivel obter o proximo valor de " + sequence + ".");
        }
    }

    /**
     * Desfaz (rollback) a transacao em andamento sem lancar excecao: uma falha
     * no proprio rollback e apenas registrada, para nao esconder a excecao
     * original, que o chamador relanca em seguida.
     */
    protected static void desfazer(Connection conexao) {
        if (conexao == null) {
            return;
        }
        try {
            conexao.rollback();
        } catch (SQLException e) {
            System.out.println("Falha ao desfazer a transacao (rollback): " + e.getMessage());
        }
    }

    /** Converte boolean para as colunas CHAR(1) CHECK IN ('S','N'). */
    protected static String simNao(boolean valor) {
        return valor ? "S" : "N";
    }

    /** Converte o 'S'/'N' das colunas CHAR(1) para boolean (qualquer outro valor = false). */
    protected static boolean deSimNao(String valor) {
        return "S".equalsIgnoreCase(valor);
    }

    /** LocalDate -> java.sql.Date, para ps.setDate. null continua null. */
    protected static Date paraSql(LocalDate data) {
        return data == null ? null : Date.valueOf(data);
    }

    /** java.sql.Date (rs.getDate) -> LocalDate. null continua null. */
    protected static LocalDate paraLocalDate(Date data) {
        return data == null ? null : data.toLocalDate();
    }

    /**
     * Texto para colunas VARCHAR2 que aceitam NULL. O Oracle grava '' como NULL,
     * entao texto vazio ou so com espacos vira null ja no Java, e a leitura
     * devolve exatamente o que foi gravado.
     */
    protected static String textoOuNulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto;
    }
}
