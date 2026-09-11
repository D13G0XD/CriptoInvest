package com.criptoinvest.factory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Fabrica de conexoes com o banco de dados Oracle da FIAP.
 *
 * Centraliza os dados de conexao (URL, usuario e senha) em um unico ponto,
 * de modo que as classes DAO nao precisem conhecer detalhes de infraestrutura.
 *
 * Os dados de conexao sao resolvidos nesta ordem de precedencia:
 *
 *   1) Propriedades de sistema, que sempre vencem:
 *      java -Ddb.user=rmXXXXXX -Ddb.password=ddmmaa -cp ... com.criptoinvest.model.Main
 *
 *   2) Arquivo .env na raiz do projeto (NAO versionado - ver .gitignore):
 *      DB_URL = jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
 *      DB_USER = rmXXXXXX
 *      DB_PASSWORD = ddmmaa
 *
 *   3) As constantes _PADRAO declaradas abaixo.
 *
 * O .env e opcional: quando nao existe, valem apenas os itens 1 e 3, sem erro.
 * Seu caminho tambem pode ser informado por -Denv.file=/caminho/para/.env
 */
public class ConnectionFactory {

    /** URL JDBC do servidor Oracle da FIAP. */
    private static final String URL_PADRAO = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL";

    /** Usuario do banco (padrao FIAP: RM do aluno). Informe o seu no .env. */
    private static final String USUARIO_PADRAO = "SEU_RM";

    /** Senha do banco (padrao FIAP: data de nascimento ddmmaa). Informe a sua no .env. */
    private static final String SENHA_PADRAO = "SUA_SENHA";

    /**
     * Desde o JDBC 4 o driver se registra sozinho pelo ServiceLoader, entao o
     * Class.forName do bloco estatico nao e obrigatorio - ele fica so para
     * avisar cedo, e com mensagem clara, quando o ojdbc nao esta no classpath.
     */
    private static final String DRIVER = "oracle.jdbc.OracleDriver";

    /** Tempo maximo (em segundos) de espera pela abertura da conexao. */
    private static final int TIMEOUT_LOGIN = 15;

    /** Nome do arquivo de credenciais locais. */
    private static final String NOME_ARQUIVO_ENV = ".env";

    /** Quantos diretorios acima do atual sao vasculhados em busca do .env. */
    private static final int NIVEIS_BUSCA_ENV = 3;

    /** Arquivo .env localizado, ou null quando nenhum foi encontrado. */
    private static final Path ARQUIVO_ENV = localizarEnv();

    /** Pares chave/valor lidos do .env. Fica vazio quando o arquivo nao existe. */
    private static final Map<String, String> ENV = carregarEnv(ARQUIVO_ENV);

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

    // ------------------------------------------------------------------------
    // Leitura do arquivo .env
    // ------------------------------------------------------------------------

    /**
     * Procura o arquivo .env: primeiro no caminho indicado por -Denv.file e, na
     * falta dele, no diretorio atual e em ate NIVEIS_BUSCA_ENV diretorios acima
     * (assim a aplicacao tambem acha o arquivo quando rodada de dentro de out/).
     *
     * @return caminho do arquivo encontrado, ou null
     */
    private static Path localizarEnv() {
        String informado = System.getProperty("env.file");
        if (informado != null && !informado.trim().isEmpty()) {
            Path caminho = Paths.get(informado.trim());
            return Files.isRegularFile(caminho) ? caminho : null;
        }

        Path diretorio = Paths.get("").toAbsolutePath();
        for (int nivel = 0; nivel <= NIVEIS_BUSCA_ENV && diretorio != null; nivel++) {
            Path candidato = diretorio.resolve(NOME_ARQUIVO_ENV);
            if (Files.isRegularFile(candidato)) {
                return candidato;
            }
            diretorio = diretorio.getParent();
        }
        return null;
    }

    /**
     * Le o arquivo .env no formato CHAVE=valor. Linhas vazias e comentarios
     * iniciados por # sao ignorados; o prefixo "export" e as aspas em volta do
     * valor, quando presentes, sao descartados.
     *
     * @param arquivo arquivo a ser lido (pode ser null)
     * @return mapa com os valores lidos, nunca null
     */
    private static Map<String, String> carregarEnv(Path arquivo) {
        Map<String, String> valores = new HashMap<>();
        if (arquivo == null) {
            return valores;
        }
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String texto = linha.trim();
                if (texto.isEmpty() || texto.startsWith("#")) {
                    continue;
                }
                if (texto.startsWith("export ")) {
                    texto = texto.substring("export ".length()).trim();
                }
                int igual = texto.indexOf('=');
                if (igual <= 0) {
                    continue;
                }
                String chave = texto.substring(0, igual).trim();
                String valor = removerAspas(texto.substring(igual + 1).trim());
                valores.put(chave, valor);
            }
        } catch (IOException e) {
            System.out.println("Nao foi possivel ler o arquivo " + arquivo + ": " + e.getMessage());
        }
        return valores;
    }

    /** Remove um par de aspas simples ou duplas em volta do valor, se houver. */
    private static String removerAspas(String valor) {
        boolean aspasDuplas = valor.startsWith("\"") && valor.endsWith("\"");
        boolean aspasSimples = valor.startsWith("'") && valor.endsWith("'");
        if (valor.length() >= 2 && (aspasDuplas || aspasSimples)) {
            return valor.substring(1, valor.length() - 1);
        }
        return valor;
    }

    /**
     * Resolve um dado de conexao na ordem: propriedade de sistema, .env, padrao.
     *
     * Vale a primeira fonte que DEFINE a chave, ainda que com valor vazio: assim
     * -Ddb.password= continua significando "senha em branco", como antes do .env.
     * Os valores vindos do .env ja chegam aqui sem os espacos em volta.
     *
     * @param propriedade nome da propriedade de sistema (ex.: db.user)
     * @param chaveEnv    nome da chave no .env (ex.: DB_USER)
     * @param padrao      valor usado quando nenhuma das fontes anteriores define a chave
     */
    private static String resolver(String propriedade, String chaveEnv, String padrao) {
        String valor = System.getProperty(propriedade);
        if (valor != null) {
            return valor;
        }
        valor = ENV.get(chaveEnv);
        if (valor != null) {
            return valor;
        }
        return padrao;
    }

    // ------------------------------------------------------------------------
    // Dados de conexao
    // ------------------------------------------------------------------------

    public static String getUrl() {
        return resolver("db.url", "DB_URL", URL_PADRAO);
    }

    public static String getUsuario() {
        return resolver("db.user", "DB_USER", USUARIO_PADRAO);
    }

    private static String getSenha() {
        return resolver("db.password", "DB_PASSWORD", SENHA_PADRAO);
    }

    /**
     * Informa qual arquivo .env foi carregado, para fins de diagnostico.
     *
     * @return caminho do .env em uso, ou null se nenhum foi encontrado
     */
    public static String getArquivoEnv() {
        return ARQUIVO_ENV == null ? null : ARQUIVO_ENV.toString();
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
