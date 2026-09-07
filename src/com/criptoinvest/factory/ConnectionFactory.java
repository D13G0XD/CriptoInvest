package com.criptoinvest.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fabrica de conexoes com o banco de dados Oracle da FIAP.
 *
 * Centraliza os dados de conexao (URL, usuario e senha) em um unico ponto,
 * de modo que as classes DAO nao precisem conhecer detalhes de infraestrutura.
 *
 * As credenciais podem ser informadas de duas formas:
 *   1) alterando as constantes USUARIO e SENHA abaixo; ou
 *   2) por propriedades de sistema, sem alterar o codigo:
 *      java -Ddb.user=rm561636 -Ddb.password=ddmmaa -cp ... com.criptoinvest.model.Main
 */
public class ConnectionFactory {

    /** URL JDBC do servidor Oracle da FIAP. */
    private static final String URL_PADRAO = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL";

    /** Usuario do banco (padrao FIAP: RM do aluno). */
    private static final String USUARIO_PADRAO = "rm561636";

    /** Senha do banco (padrao FIAP: data de nascimento no formato ddmmaa). */
    private static final String SENHA_PADRAO = "SUA_SENHA";

    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";

    /** Tempo maximo (em segundos) de espera pela abertura da conexao. */
    private static final int TIMEOUT_LOGIN = 15;

    /** Classe utilitaria: nao deve ser instanciada. */
    private ConnectionFactory() {
    }

    static {
        try {
            Class.forName(DRIVER);
            DriverManager.setLoginTimeout(TIMEOUT_LOGIN);
        } catch (ClassNotFoundException e) {
            System.out.println("Driver JDBC da Oracle nao encontrado no classpath: " + e.getMessage());
        }
    }

    public static String getUrl() {
        return System.getProperty("db.url", URL_PADRAO);
    }

    public static String getUsuario() {
        return System.getProperty("db.user", USUARIO_PADRAO);
    }

    private static String getSenha() {
        return System.getProperty("db.password", SENHA_PADRAO);
    }

    /**
     * Abre uma nova conexao com o banco de dados.
     *
     * @return conexao aberta com o Oracle
     * @throws SQLException se a conexao nao puder ser estabelecida
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
    }

    /**
     * Testa a conexao com o banco executando uma consulta simples.
     *
     * @return true se o banco respondeu corretamente
     */
    public static boolean testarConexao() {
        try (Connection conexao = getConnection();
             Statement st = conexao.createStatement();
             ResultSet rs = st.executeQuery("SELECT USER, SYSDATE FROM dual")) {

            if (rs.next()) {
                System.out.println("Conexao estabelecida com sucesso!");
                System.out.println("Usuario do banco: " + rs.getString(1));
                System.out.println("Data do servidor: " + rs.getDate(2));
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Falha ao conectar no banco: " + e.getMessage());
            return false;
        }
    }

    /** Fecha uma conexao com seguranca, ignorando conexoes nulas ou ja fechadas. */
    public static void fecharConexao(Connection conexao) {
        if (conexao == null) {
            return;
        }
        try {
            conexao.close();
        } catch (SQLException e) {
            System.out.println("Erro ao fechar a conexao: " + e.getMessage());
        }
    }
}
