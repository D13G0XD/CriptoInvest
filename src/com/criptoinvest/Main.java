package com.criptoinvest;

import com.criptoinvest.dao.*;
import com.criptoinvest.factory.ConnectionFactory;
import com.criptoinvest.model.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.*;

/**
 * Fase 6: teste da integracao JDBC com o Oracle nas 10 tabelas do DDL.
 *
 * Para cada tabela: INSERT (id da sequence), SELECT (por id e listagem em ArrayList), UPDATE relido do banco e,
 * na limpeza (finally, ordem reversa das FKs), DELETE confirmado por buscarPorId == null; alem de chaves
 * naturais, UK/FK, polimorfismo das carteiras e regras de transacao, posicao e relatorio. Cada verificacao roda
 * isolada em testar(): uma falha nao derruba as demais e quem depende dela fica "nao executado".
 * Os dados levam um sufixo unico por execucao, entao o programa roda varias vezes no mesmo banco (com ou sem o
 * DML). Credenciais so na ConnectionFactory (-D ou .env); a senha nunca e exibida.
 */
public class Main {

    private static final String INSERT = "INSERT";
    private static final String SELECT = "SELECT";
    private static final String UPDATE = "UPDATE";
    private static final String DELETE = "DELETE";
    /** Colunas da matriz; as demais verificacoes vao para a coluna OUTRAS. */
    private static final List<String> OPERACOES = List.of(INSERT, SELECT, UPDATE, DELETE);
    /** As 10 tabelas do DDL, na ordem em que os testes as populam. */
    private static final List<String> TABELAS = List.of("criptoativo", "carteira_pf", "carteira_pj", "usuario",
            "empresa", "carteira", "transacao", "posicao", "relatorio", "alerta");

    private static final String OK = "OK";
    private static final String FALHA = "FALHA";
    private static final String NAO_EXECUTADO = "--";

    /** Um passo de teste: devolve o detalhe anotado na matriz (ou o resultado da operacao testada). */
    @FunctionalInterface
    private interface Passo<T> {
        T executar() throws Exception;
    }

    /** Pre-requisito ausente: o passo fica "nao executado" (nao conta como falha). */
    private static class NaoExecutado extends RuntimeException {
        private static final long serialVersionUID = 1L;

        NaoExecutado(String motivo) {
            super(motivo);
        }
    }

    private final CriptoativoDAO criptoativoDAO = new CriptoativoDAO();
    private final CarteiraPFDAO carteiraPFDAO = new CarteiraPFDAO();
    private final CarteiraPJDAO carteiraPJDAO = new CarteiraPJDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private final CarteiraDAO carteiraDAO = new CarteiraDAO();
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final PosicaoDAO posicaoDAO = new PosicaoDAO();
    private final RelatorioDAO relatorioDAO = new RelatorioDAO();
    private final AlertaDAO alertaDAO = new AlertaDAO();
    /** O DAO de cada tabela, na mesma ordem de TABELAS. */
    private final List<CrudDAO<?>> daos = List.of(criptoativoDAO, carteiraPFDAO, carteiraPJDAO, usuarioDAO,
            empresaDAO, carteiraDAO, transacaoDAO, posicaoDAO, relatorioDAO, alertaDAO);

    /** tabela -> (operacao -> "OK - detalhe", "FALHA - motivo" ou "-- - motivo"), na ordem de execucao. */
    private final Map<String, Map<String, String>> resultados = new LinkedHashMap<>();
    private final Map<String, Integer> contagemInicial = new LinkedHashMap<>();

    /** Sufixo das chaves naturais (8 ultimos digitos de currentTimeMillis); pontuado, vira CPF e CNPJ. */
    private final String sufixo = String.format("%08d", System.currentTimeMillis() % 100_000_000L);
    private final String sufixoPontuado = sufixo.substring(0, 2) + "." + sufixo.substring(2, 5) + "."
            + sufixo.substring(5);
    private final LocalDate hoje = LocalDate.now();

    // Objetos criados nesta execucao (a limpeza parte deles)
    private Criptoativo criptoA, criptoB;
    private CarteiraPF carteiraPFAvulsa, carteiraPFPolimorfica;
    private CarteiraPJ carteiraPJAvulsa, carteiraPJPolimorfica;
    private Usuario usuario;
    private Empresa empresa1, empresa2;
    private Transacao compra1, compra2, venda, compraPJ, transacaoAvulsa;
    private Posicao posicaoAvulsa;
    private Relatorio relatorio;
    private Alerta alerta1, alerta2;

    public static void main(String[] args) {
        new Main().executar();
    }

    /** Roteiro completo; a limpeza (testes de DELETE) roda no finally. */
    private void executar() {
        System.out.println("CriptoInvest - Fase 6 - teste de integracao JDBC (10 tabelas)");
        System.out.println("Sufixo das chaves naturais desta execucao: " + sufixo);
        if (!testarConexao() || !contarLinhasIniciais()) {
            return;
        }
        try {
            testarCriptoativo();
            testarCarteiraPF();
            testarCarteiraPJ();
            testarUsuario();
            testarEmpresa();
            testarCarteiraPolimorfica();
            testarTransacao();
            testarPosicao();
            testarRelatorio();
            testarAlerta();
            testarIntegridadeReferencial();
        } finally {
            limpar();
        }
        conferirLimpeza();
        imprimirResumo();
    }

    /** Testa a conexao (mostra URL e usuario, nunca a senha); sem conexao, explica como configurar. */
    private boolean testarConexao() {
        secao("0. CONEXAO (ConnectionFactory)");
        String arquivoEnv = ConnectionFactory.getArquivoEnv();
        System.out.println("URL ..........: " + ConnectionFactory.getUrl());
        System.out.println("Usuario ......: " + ConnectionFactory.getUsuario());
        System.out.println("Arquivo .env .: " + (arquivoEnv == null ? "(nenhum encontrado)" : arquivoEnv));
        if (ConnectionFactory.testarConexao()) {
            return true;
        }
        System.out.println();
        System.out.println("Sem conexao com o banco: os testes nao serao executados. Configure as credenciais:");
        System.out.println("  1) arquivo .env na raiz do projeto (copie o .env_exemplo; nunca versione o .env):");
        System.out.println("       DB_URL = jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL");
        System.out.println("       DB_USER = rmXXXXXX");
        System.out.println("       DB_PASSWORD = ddmmaa");
        System.out.println("  2) ou -D, com precedencia sobre o .env (-Denv.file=/caminho/.env aponta outro .env):");
        System.out.println("       java -Ddb.user=rmXXXXXX -Ddb.password=ddmmaa -cp \"out;lib/ojdbc8.jar\" "
                + "com.criptoinvest.Main");
        System.out.println("Confira tambem se o driver ojdbc8 esta no classpath e se a rede alcanca o servidor.");
        return false;
    }

    /** Guarda as linhas de cada tabela antes dos testes; sem o DDL, ja falha aqui (ORA-00942). */
    private boolean contarLinhasIniciais() {
        try {
            for (int i = 0; i < TABELAS.size(); i++) {
                contagemInicial.put(TABELAS.get(i), daos.get(i).listarTodos().size());
            }
            System.out.println("Linhas por tabela antes dos testes: " + contagemInicial);
            return true;
        } catch (SQLException | RuntimeException e) {
            System.out.println("Nao foi possivel ler as tabelas: " + motivo(e));
            System.out.println("Execute sql/criptoinvest_ddl.sql (e, se quiser, sql/criptoinvest_dml.sql) "
                    + "no schema " + ConnectionFactory.getUsuario() + " e rode de novo.");
            return false;
        }
    }

    private void testarCriptoativo() {
        String t = "criptoativo";
        secao("1. CRIPTOATIVO (CriptoativoDAO)");
        boolean inserido = testar(t, INSERT, () -> {
            criptoA = new Criptoativo(0, "Moeda Teste Alfa " + sufixo, "TA" + sufixo, new BigDecimal("250.50"), "Teste");
            criptoA.setVariacao24h(new BigDecimal("1.25"));
            // sigla minuscula (o modelo normaliza) e categoria em branco (grava NULL)
            criptoB = new Criptoativo(0, "Moeda Teste Beta " + sufixo, "tb" + sufixo, new BigDecimal("12.345678"), " ");
            int idA = gravar(criptoativoDAO, criptoA, Criptoativo::getIdCripto);
            int idB = gravar(criptoativoDAO, criptoB, Criptoativo::getIdCripto);
            return criptoA.getSigla() + " id=" + idA + ", " + criptoB.getSigla() + " id=" + idB;
        });
        testar(t, SELECT, inserido, () -> {
            Criptoativo a = ler(criptoativoDAO, criptoA.getIdCripto());
            verificarIgual(criptoA.getNome(), a.getNome(), "nome");
            verificarIgual(criptoA.getSigla(), a.getSigla(), "sigla");
            verificarIgual(new BigDecimal("250.50000000"), a.getPrecoAtual(), "preco_atual (escala 8)");
            verificarIgual(new BigDecimal("1.2500"), a.getVariacao24h(), "variacao_24h (escala 4)");
            verificarIgual("Teste", a.getCategoria(), "categoria");
            Criptoativo b = ler(criptoativoDAO, criptoB.getIdCripto());
            verificarIgual("TB" + sufixo, b.getSigla(), "sigla normalizada em maiusculas");
            verificar(b.getCategoria() == null, "categoria em branco gravada como NULL");
            return "buscarPorId + " + listar("criptoativoDAO.listarTodos()", criptoativoDAO.listarTodos(),
                    Criptoativo::getIdCripto, List.of(a.getIdCripto(), b.getIdCripto()), c -> String.format(
                    "%3d  %-12s %-26s preco %-12s var 24h %-9s %s", c.getIdCripto(), c.getSigla(), c.getNome(),
                    semZeros(c.getPrecoAtual()), semZeros(c.getVariacao24h()) + "%", texto(c.getCategoria())));
        });
        testar(t, "chave natural: buscarPorSigla", inserido, () -> {
            Criptoativo porSigla = criptoativoDAO.buscarPorSigla(" ta" + sufixo + " ");
            verificar(porSigla != null && porSigla.getIdCripto() == criptoA.getIdCripto(), "buscarPorSigla normaliza");
            verificar(criptoativoDAO.buscarPorSigla("ZZ" + sufixo) == null, "sigla inexistente devolve null");
            return "\" ta" + sufixo + " \" -> id " + criptoA.getIdCripto() + "; sigla inexistente -> null";
        });
        testar(t, UPDATE, inserido, () -> {
            criptoA.atualizarPreco(new BigDecimal("300.00"));
            criptoA.setCategoria("Teste (alterada)");
            verificarIgual(1, criptoativoDAO.alterar(criptoA), "linhas alteradas");
            Criptoativo relido = ler(criptoativoDAO, criptoA.getIdCripto());
            verificarIgual(new BigDecimal("300.00000000"), relido.getPrecoAtual(), "preco_atual relido");
            verificarIgual(criptoA.getVariacao24h(), relido.getVariacao24h(), "variacao_24h relida");
            verificarIgual("Teste (alterada)", relido.getCategoria(), "categoria relida");
            return "relido: preco 250.50 -> 300, variacao 1.25% -> " + semZeros(relido.getVariacao24h()) + "%";
        });
        testar(t, "UK sigla duplicada (ORA-00001)", inserido, () -> {
            Criptoativo duplicado = new Criptoativo(0, "Sigla repetida", "ta" + sufixo, BigDecimal.ONE, null);
            // se o banco aceitar, a falha reportada e essa; a limpeza apaga a linha (a sigla leva o sufixo)
            String erro = esperarRecusa(() -> criptoativoDAO.inserir(duplicado), 1, "23505",
                    "o banco aceitou a sigla repetida " + duplicado.getSigla());
            verificarIgual(0, duplicado.getIdCripto(), "id do objeto recusado");
            return "recusada e o objeto continua com id 0 - " + erro;
        });
    }

    /** Carteira PF avulsa: CarteiraPFDAO grava carteira + carteira_pf na mesma transacao. */
    private void testarCarteiraPF() {
        String t = "carteira_pf";
        secao("2. CARTEIRA_PF (CarteiraPFDAO) - carteira PF avulsa");
        boolean inserido = testar(t, INSERT, () -> {
            carteiraPFAvulsa = new CarteiraPF("Carteira PF avulsa " + sufixo, new BigDecimal("1000"),
                    new BigDecimal("2000"));
            return "id=" + gravar(carteiraPFDAO, carteiraPFAvulsa, Carteira::getIdCarteira) + " (pai + filha)";
        });
        testar(t, SELECT, inserido, () -> {
            int id = carteiraPFAvulsa.getIdCarteira();
            CarteiraPF lida = ler(carteiraPFDAO, id);
            verificarIgual(carteiraPFAvulsa.getDescricao(), lida.getDescricao(), "descricao");
            verificarIgual(new BigDecimal("1000.00"), lida.getSaldoReais(), "saldo_reais (escala 2)");
            verificarIgual(new BigDecimal("2000.00"), lida.getLimiteDiarioSaque(), "limite_diario_saque");
            verificar(carteiraPJDAO.buscarPorId(id) == null, "o CarteiraPJDAO nao le uma carteira PF");
            return "buscarPorId (o CarteiraPJDAO nao le a PF) + " + listar("carteiraPFDAO.listarTodos()",
                    carteiraPFDAO.listarTodos(), Carteira::getIdCarteira, List.of(id), Main::linhaCarteira);
        });
        testar(t, UPDATE, inserido, () -> {
            carteiraPFAvulsa.setDescricao(carteiraPFAvulsa.getDescricao() + " (alterada)");
            carteiraPFAvulsa.setLimiteDiarioSaque(new BigDecimal("3500"));
            carteiraPFAvulsa.depositar(new BigDecimal("500"));
            verificarIgual(1, carteiraPFDAO.alterar(carteiraPFAvulsa), "linhas alteradas em carteira_pf");
            CarteiraPF relida = ler(carteiraPFDAO, carteiraPFAvulsa.getIdCarteira());
            verificarIgual(carteiraPFAvulsa.getDescricao(), relida.getDescricao(), "descricao relida");
            verificarIgual(new BigDecimal("1500.00"), relida.getSaldoReais(), "saldo relido");
            verificarIgual(new BigDecimal("3500.00"), relida.getLimiteDiarioSaque(), "limite relido");
            return "relido: limite 2000.00 -> 3500.00, saldo 1000.00 -> 1500.00 (pai + filha)";
        });
    }

    /** Carteira PJ avulsa: CarteiraPJDAO grava carteira + carteira_pj na mesma transacao. */
    private void testarCarteiraPJ() {
        String t = "carteira_pj";
        secao("3. CARTEIRA_PJ (CarteiraPJDAO) - carteira PJ avulsa");
        boolean inserido = testar(t, INSERT, () -> {
            carteiraPJAvulsa = new CarteiraPJ("Carteira PJ avulsa " + sufixo, new BigDecimal("20000"), "SIMPLES");
            return "id=" + gravar(carteiraPJDAO, carteiraPJAvulsa, Carteira::getIdCarteira) + " (pai + filha)";
        });
        testar(t, SELECT, inserido, () -> {
            int id = carteiraPJAvulsa.getIdCarteira();
            CarteiraPJ lida = ler(carteiraPJDAO, id);
            verificarIgual(carteiraPJAvulsa.getDescricao(), lida.getDescricao(), "descricao");
            verificarIgual(new BigDecimal("20000.00"), lida.getSaldoReais(), "saldo_reais (escala 2)");
            verificarIgual("SIMPLES", lida.getRegimeTributario(), "regime_tributario");
            verificar(carteiraPFDAO.buscarPorId(id) == null, "o CarteiraPFDAO nao le uma carteira PJ");
            return "buscarPorId (o CarteiraPFDAO nao le a PJ) + " + listar("carteiraPJDAO.listarTodos()",
                    carteiraPJDAO.listarTodos(), Carteira::getIdCarteira, List.of(id), Main::linhaCarteira);
        });
        testar(t, UPDATE, inserido, () -> {
            carteiraPJAvulsa.setRegimeTributario("LUCRO_REAL");
            carteiraPJAvulsa.sacar(new BigDecimal("2000"));
            verificarIgual(1, carteiraPJDAO.alterar(carteiraPJAvulsa), "linhas alteradas em carteira_pj");
            CarteiraPJ relida = ler(carteiraPJDAO, carteiraPJAvulsa.getIdCarteira());
            verificarIgual("LUCRO_REAL", relida.getRegimeTributario(), "regime relido");
            verificarIgual(new BigDecimal("18000.00"), relida.getSaldoReais(), "saldo relido");
            return "relido: regime SIMPLES -> LUCRO_REAL, saldo 20000.00 -> 18000.00 (pai + filha)";
        });
    }

    private void testarUsuario() {
        String t = "usuario";
        secao("4. USUARIO (UsuarioDAO) - 1:1 com carteira_pf");
        boolean inserido = testar(t, INSERT, () -> {
            usuario = new Usuario(0, "Usuario Teste " + sufixo, "teste." + sufixo + "@voltz.test", "senha-de-teste",
                    "0" + sufixoPontuado + "-01", new BigDecimal("100000"), new BigDecimal("5000"));
            int id = gravar(usuarioDAO, usuario, Usuario::getIdUsuario);
            return "id=" + id + " com carteira PF id=" + usuario.getCarteira().getIdCarteira() + " (mesma transacao)";
        });
        testar(t, SELECT, inserido, () -> {
            Usuario lido = ler(usuarioDAO, usuario.getIdUsuario());
            verificarIgual(usuario.getNome(), lido.getNome(), "nome");
            verificarIgual(usuario.getEmail(), lido.getEmail(), "email");
            verificarIgual(usuario.getCpf(), lido.getCpf(), "cpf");
            verificar(!lido.isAutenticacaoDoisFatores(), "autenticacao_2fa 'N' -> false");
            verificarIgual(usuario.getCarteira().getIdCarteira(), lido.getCarteira().getIdCarteira(), "carteira PF");
            verificarIgual(new BigDecimal("100000.00"), lido.getCarteira().getSaldoReais(), "saldo da carteira");
            verificarIgual(new BigDecimal("5000.00"), lido.getCarteira().getLimiteDiarioSaque(), "limite da carteira");
            return "buscarPorId (com a carteira PF) + " + listar("usuarioDAO.listarTodos()", usuarioDAO.listarTodos(),
                    Usuario::getIdUsuario, List.of(lido.getIdUsuario()), u -> String.format("%3d  %-34s %-26s %s  "
                    + "2FA %s  carteira PF %d, saldo %s", u.getIdUsuario(), u.getNome(), u.getEmail(), u.getCpf(),
                    u.isAutenticacaoDoisFatores() ? "S" : "N", u.getCarteira().getIdCarteira(),
                    u.getCarteira().getSaldoReais()));
        });
        testar(t, "chave natural: buscarPorCpf/Email", inserido, () -> {
            Usuario porCpf = usuarioDAO.buscarPorCpf(usuario.getCpf());
            Usuario porEmail = usuarioDAO.buscarPorEmail(usuario.getEmail());
            verificar(porCpf != null && porCpf.getIdUsuario() == usuario.getIdUsuario(), "buscarPorCpf");
            verificar(porEmail != null && porEmail.getIdUsuario() == usuario.getIdUsuario(), "buscarPorEmail");
            return usuario.getCpf() + " e " + usuario.getEmail() + " -> id " + usuario.getIdUsuario();
        });
        testar(t, UPDATE, inserido, () -> {
            usuario.setNome(usuario.getNome() + " (alterado)");
            usuario.ativar2FA();
            usuario.getCarteira().setLimiteDiarioSaque(new BigDecimal("6000"));
            verificarIgual(1, usuarioDAO.alterar(usuario), "linhas alteradas em usuario");
            Usuario relido = ler(usuarioDAO, usuario.getIdUsuario());
            verificarIgual(usuario.getNome(), relido.getNome(), "nome relido");
            verificar(relido.isAutenticacaoDoisFatores(), "autenticacao_2fa relida 'S' -> true");
            verificarIgual(new BigDecimal("6000.00"), relido.getCarteira().getLimiteDiarioSaque(), "limite relido");
            return "relido: nome '" + relido.getNome() + "', 2FA N -> S, limite da carteira 5000.00 -> 6000.00";
        });
    }

    private void testarEmpresa() {
        String t = "empresa";
        secao("5. EMPRESA (EmpresaDAO) - N:1 usuario, 1:1 carteira_pj");
        boolean inserido = testar(t, INSERT, inseriu("usuario"), () -> {
            empresa1 = new Empresa(0, "Empresa Teste A " + sufixo, sufixoPontuado + "/0001-01", "LUCRO_PRESUMIDO",
                    usuario);
            empresa1.getCarteira().depositar(new BigDecimal("50000"), "Capital inicial");
            empresa2 = new Empresa(0, "Empresa Teste B " + sufixo, sufixoPontuado + "/0001-02", usuario);
            usuario.adicionarEmpresa(empresa1);
            usuario.adicionarEmpresa(empresa2);
            int id1 = gravar(empresaDAO, empresa1, Empresa::getIdEmpresa);
            int id2 = gravar(empresaDAO, empresa2, Empresa::getIdEmpresa);
            verificar(id(empresa1.getCarteira()) > 0 && id(empresa2.getCarteira()) > 0, "carteiras PJ gravadas");
            return "ids " + id1 + " e " + id2 + " (mesmo dono), carteiras PJ " + empresa1.getCarteira().getIdCarteira()
                    + " e " + empresa2.getCarteira().getIdCarteira() + " (mesma transacao)";
        });
        testar(t, SELECT, inserido, () -> {
            Empresa lida = ler(empresaDAO, empresa1.getIdEmpresa());
            verificarIgual(empresa1.getNome(), lida.getNome(), "nome");
            verificarIgual(empresa1.getCnpj(), lida.getCnpj(), "cnpj");
            verificarIgual(usuario.getIdUsuario(), lida.getDono().getIdUsuario(), "dono (id_usuario)");
            verificarIgual(empresa1.getCarteira().getIdCarteira(), lida.getCarteira().getIdCarteira(), "carteira PJ");
            verificarIgual("LUCRO_PRESUMIDO", lida.getCarteira().getRegimeTributario(), "regime da carteira");
            verificarIgual(new BigDecimal("50000.00"), lida.getCarteira().getSaldoReais(), "saldo da carteira");
            return "buscarPorId + " + listar("empresaDAO.listarTodos()", empresaDAO.listarTodos(), Empresa::getIdEmpresa,
                    List.of(empresa1.getIdEmpresa(), empresa2.getIdEmpresa()),
                    e -> String.format("%3d  %-36s %s  dono %-34s carteira PJ %d %s, saldo %s", e.getIdEmpresa(),
                    e.getNome(), e.getCnpj(), e.getDono().getNome(), e.getCarteira().getIdCarteira(),
                    e.getCarteira().getRegimeTributario(), e.getCarteira().getSaldoReais()));
        });
        testar(t, "chave natural: buscarPorCnpj", inserido, () -> {
            Empresa porCnpj = empresaDAO.buscarPorCnpj(empresa2.getCnpj());
            verificar(porCnpj != null && porCnpj.getIdEmpresa() == empresa2.getIdEmpresa(), "buscarPorCnpj");
            return empresa2.getCnpj() + " -> id " + empresa2.getIdEmpresa();
        });
        testar(t, "listarPorUsuario", inserido, () -> {
            List<Empresa> doUsuario = empresaDAO.listarPorUsuario(usuario.getIdUsuario());
            List<Integer> ids = idsDe(doUsuario, Empresa::getIdEmpresa);
            verificarIgual(List.of(empresa1.getIdEmpresa(), empresa2.getIdEmpresa()), ids, "empresas do usuario");
            verificar(doUsuario.get(0).getDono() == doUsuario.get(1).getDono(), "dono lido uma vez e compartilhado");
            return "ids " + ids + " do usuario " + usuario.getIdUsuario() + ", com o mesmo objeto dono";
        });
        testar(t, UPDATE, inserido, () -> {
            empresa2.setNome(empresa2.getNome() + " (alterada)");
            empresa2.getCarteira().setRegimeTributario("LUCRO_REAL");
            verificarIgual(1, empresaDAO.alterar(empresa2), "linhas alteradas em empresa");
            Empresa relida = ler(empresaDAO, empresa2.getIdEmpresa());
            verificarIgual(empresa2.getNome(), relida.getNome(), "nome relido");
            verificarIgual("LUCRO_REAL", relida.getCarteira().getRegimeTributario(), "regime relido");
            return "relido: nome '" + relida.getNome() + "', regime da carteira PJ SIMPLES -> LUCRO_REAL";
        });
    }

    /** CarteiraDAO: heranca joined (pai carteira + filha PF/PJ) com INSERT e leitura polimorficos. */
    private void testarCarteiraPolimorfica() {
        String t = "carteira";
        secao("6. CARTEIRA (CarteiraDAO) - heranca joined e leitura polimorfica");
        boolean inserido = testar(t, INSERT, () -> {
            carteiraPFPolimorfica = new CarteiraPF("Carteira PF polimorfica " + sufixo, new BigDecimal("300"),
                    new BigDecimal("1000"));
            carteiraPJPolimorfica = new CarteiraPJ("Carteira PJ polimorfica " + sufixo, new BigDecimal("700"),
                    "LUCRO_PRESUMIDO");
            int idPF = gravar(carteiraDAO, carteiraPFPolimorfica, Carteira::getIdCarteira);   // o mesmo metodo
            int idPJ = gravar(carteiraDAO, carteiraPJPolimorfica, Carteira::getIdCarteira);   // grava PF e PJ
            return "inserir(Carteira) polimorfico: PF id=" + idPF + ", PJ id=" + idPJ;
        });
        testar(t, SELECT, inserido, () -> {
            Carteira lidaPF = ler(carteiraDAO, carteiraPFPolimorfica.getIdCarteira());
            Carteira lidaPJ = ler(carteiraDAO, carteiraPJPolimorfica.getIdCarteira());
            verificar(lidaPF instanceof CarteiraPF && lidaPJ instanceof CarteiraPJ, "devolve CarteiraPF e CarteiraPJ");
            verificarIgual(new BigDecimal("1000.00"), ((CarteiraPF) lidaPF).getLimiteDiarioSaque(), "limite da PF");
            verificarIgual("LUCRO_PRESUMIDO", ((CarteiraPJ) lidaPJ).getRegimeTributario(), "regime da PJ");
            verificarIgual(new BigDecimal("700.00"), lidaPJ.getSaldoReais(), "saldo da PJ");
            List<Carteira> todas = carteiraDAO.listarTodos();
            for (Carteira criada : carteirasCriadas()) {   // cada carteira volta na sua subclasse
                verificar(todas.stream().anyMatch(c -> c.getIdCarteira() == criada.getIdCarteira()
                        && c.getClass() == criada.getClass()), "carteira " + criada.getIdCarteira() + " listada");
            }
            return "buscarPorId e " + listar("carteiraDAO.listarTodos() - PF e PJ na mesma List<Carteira>", todas,
                    Carteira::getIdCarteira, List.of(lidaPF.getIdCarteira(), lidaPJ.getIdCarteira()),
                    Main::linhaCarteira) + " polimorficos: cada linha na subclasse certa";
        });
        testar(t, "polimorfismo: listarPorTipo", inserido, () -> {
            List<Carteira> pfs = carteiraDAO.listarPorTipo("PF");
            List<Carteira> pjs = carteiraDAO.listarPorTipo(" pj ");
            pfs.forEach(c -> verificar(c instanceof CarteiraPF && "PF".equals(c.getTipo()), "PF so traz CarteiraPF"));
            pjs.forEach(c -> verificar(c instanceof CarteiraPJ && "PJ".equals(c.getTipo()), "PJ so traz CarteiraPJ"));
            List<Integer> idsPF = idsDe(pfs, Carteira::getIdCarteira);
            List<Integer> idsPJ = idsDe(pjs, Carteira::getIdCarteira);
            int idPF = carteiraPFPolimorfica.getIdCarteira();
            int idPJ = carteiraPJPolimorfica.getIdCarteira();
            verificar(idsPF.contains(idPF) && !idsPF.contains(idPJ), "a PF nova esta so na lista PF");
            verificar(idsPJ.contains(idPJ) && !idsPJ.contains(idPF), "a PJ nova esta so na lista PJ");
            verificarIgual(carteiraDAO.listarTodos().size(), pfs.size() + pjs.size(), "PF + PJ = total de carteiras");
            return "PF=" + pfs.size() + " (CarteiraPF), PJ=" + pjs.size() + " (CarteiraPJ), soma = listarTodos";
        });
        testar(t, UPDATE, inserido, () -> {
            carteiraPJPolimorfica.setDescricao(carteiraPJPolimorfica.getDescricao() + " (alterada)");
            carteiraPJPolimorfica.depositar(new BigDecimal("100"));
            carteiraPJPolimorfica.setRegimeTributario("LUCRO_REAL");   // da tabela filha: nao deve ser gravado
            verificarIgual(1, carteiraDAO.alterar(carteiraPJPolimorfica), "linhas alteradas em carteira");
            CarteiraPJ relida = (CarteiraPJ) ler(carteiraDAO, carteiraPJPolimorfica.getIdCarteira());
            verificarIgual(carteiraPJPolimorfica.getDescricao(), relida.getDescricao(), "descricao relida");
            verificarIgual(new BigDecimal("800.00"), relida.getSaldoReais(), "saldo relido");
            verificarIgual("LUCRO_PRESUMIDO", relida.getRegimeTributario(), "regime intocado (alterar so grava o pai)");
            return "relido: descricao e saldo 700.00 -> 800.00 na tabela pai; regime da filha intocado";
        });
        testar(t, "atualizarSaldo", inserido, () -> {
            int idPF = carteiraPFPolimorfica.getIdCarteira();
            verificarIgual(1, carteiraDAO.atualizarSaldo(idPF, new BigDecimal("999.99")), "linhas de atualizarSaldo");
            CarteiraPF relida = (CarteiraPF) ler(carteiraDAO, idPF);
            verificarIgual(new BigDecimal("999.99"), relida.getSaldoReais(), "saldo relido");
            verificarIgual(new BigDecimal("1000.00"), relida.getLimiteDiarioSaque(), "limite intocado");
            return "relido: saldo da carteira " + idPF + " 300.00 -> 999.99, limite intocado";
        });
    }

    /** TransacaoDAO.registrar grava transacao + posicao + saldo numa so transacao; inserir grava so a linha. */
    private void testarTransacao() {
        String t = "transacao";
        secao("7. TRANSACAO (TransacaoDAO) - registrar: transacao + posicao + saldo");
        // INSERT = registrar; posicao, relatorio e a FK do criptoativo dependem so dele, e nao do inserir (CRUD)
        boolean inserido = testar(t, INSERT, inseriu("criptoativo", "usuario", "empresa"), () -> {
            CarteiraPF carteira = usuario.getCarteira();
            compra1 = new Transacao(0, "COMPRA", criptoA, new BigDecimal("10"), new BigDecimal("280"), hoje.minusDays(3));
            compra2 = new Transacao(0, "COMPRA", criptoA, new BigDecimal("5"), new BigDecimal("320"), hoje.minusDays(2));
            venda = new Transacao(0, "VENDA", criptoA, new BigDecimal("6"), new BigDecimal("310"), hoje.minusDays(1));
            registrarNaCarteira(carteira, compra1, null);
            registrarNaCarteira(carteira, compra2, "Aporte em " + criptoA.getSigla());
            registrarNaCarteira(carteira, venda, null);
            // FK polimorfica: a mesma tabela transacao aponta tambem para uma carteira PJ
            compraPJ = new Transacao(0, "COMPRA", criptoB, new BigDecimal("100"), new BigDecimal("12.345678"),
                    hoje.minusDays(1));
            registrarNaCarteira(empresa1.getCarteira(), compraPJ, "Compra institucional");
            return "registrar: ids " + idsDe(List.of(compra1, compra2, venda, compraPJ), Transacao::getIdTransacao)
                    + " (3 na carteira PF do usuario, 1 na carteira PJ da empresa)";
        });
        // INSERT direto do CRUD (TransacaoDAO.inserir) na carteira PF avulsa: grava SO a linha
        boolean inseridaCrud = testar(t, "inserir (CRUD): so a linha", inseriu("criptoativo", "carteira_pf"), () -> {
            int idAvulsa = carteiraPFAvulsa.getIdCarteira();
            BigDecimal saldoAntes = carteiraDAO.buscarPorId(idAvulsa).getSaldoReais();
            transacaoAvulsa = new Transacao(0, "COMPRA", criptoB, new BigDecimal("3"), new BigDecimal("12"),
                    hoje.minusDays(4));
            transacaoAvulsa.setCarteira(carteiraPFAvulsa);
            Transacao lida = ler(transacaoDAO, gravar(transacaoDAO, transacaoAvulsa, Transacao::getIdTransacao));
            verificarIgual(idAvulsa, lida.getCarteira().getIdCarteira(), "carteira da transacao inserida");
            verificarIgual(transacaoAvulsa.getTaxa(), lida.getTaxa(), "taxa da transacao inserida");
            verificarIgual(saldoAntes, carteiraDAO.buscarPorId(idAvulsa).getSaldoReais(), "saldo intocado");
            verificar(posicaoDAO.buscarPorCarteiraECripto(idAvulsa, criptoB.getIdCripto()) == null, "sem posicao");
            return "id=" + lida.getIdTransacao() + " na carteira avulsa " + idAvulsa + ", saldo e posicao intocados";
        });
        testar(t, "registrar: saldo e posicao relidos", inserido, () -> {
            int idCarteira = usuario.getCarteira().getIdCarteira();
            // 100000.00 - (2800 + 2.80) - (1600 + 1.60) + (1860 - 1.86) = 97453.74
            verificarIgual(new BigDecimal("97453.74"), usuario.getCarteira().getSaldoReais(), "saldo em memoria");
            verificarIgual(usuario.getCarteira().getSaldoReais(), carteiraDAO.buscarPorId(idCarteira).getSaldoReais(),
                    "saldo_reais gravado = saldo em memoria");
            Posicao posicao = posicaoDAO.buscarPorCarteiraECripto(idCarteira, criptoA.getIdCripto());
            verificar(posicao != null, "posicao de " + criptoA.getSigla() + " gravada");
            verificarIgual(new BigDecimal("9.00000000"), posicao.getQuantidadeAtual(), "quantidade (10 + 5 - 6)");
            verificarIgual(new BigDecimal("293.33333333"), posicao.getPrecoMedioCompra(), "preco medio (2800+1600)/15");
            // 50000.00 - (1234.57 + 1.23) = 48764.20
            verificarIgual(new BigDecimal("48764.20"),
                    carteiraDAO.buscarPorId(empresa1.getCarteira().getIdCarteira()).getSaldoReais(), "saldo da PJ");
            return "relido = memoria: saldo PF 97453.74, posicao 9 @ 293.33333333, saldo PJ 48764.20";
        });
        testar(t, "recusada: saldo insuficiente", inserido, () -> {
            int idCarteira = usuario.getCarteira().getIdCarteira();
            int transacoesAntes = transacaoDAO.listarPorCarteira(idCarteira).size();
            BigDecimal saldoAntes = carteiraDAO.buscarPorId(idCarteira).getSaldoReais();
            Transacao recusada = new Transacao(0, "COMPRA", criptoA, new BigDecimal("1000"), new BigDecimal("300"), hoje);
            verificar(!usuario.getCarteira().registrarTransacao(recusada), "a carteira recusa a compra acima do saldo");
            verificar(recusada.getCarteira() == null, "a transacao recusada nao recebe carteira");
            try {
                transacaoDAO.registrar(recusada);
                throw new AssertionError("TransacaoDAO.registrar gravou uma transacao recusada");
            } catch (IllegalStateException e) {
                System.out.println("TransacaoDAO.registrar tambem recusa: " + e.getMessage());
            }
            verificarIgual(transacoesAntes, transacaoDAO.listarPorCarteira(idCarteira).size(), "transacoes no banco");
            verificarIgual(saldoAntes, carteiraDAO.buscarPorId(idCarteira).getSaldoReais(), "saldo no banco");
            verificarIgual(0, recusada.getIdTransacao(), "id da transacao recusada");
            return "nada gravado: continua com " + transacoesAntes + " transacoes e saldo " + saldoAntes;
        });
        testar(t, SELECT, inserido, () -> {
            Transacao lida = ler(transacaoDAO, compra2.getIdTransacao());
            verificarIgual("COMPRA", lida.getTipo(), "tipo");
            verificarIgual(compra2.getQuantidade(), lida.getQuantidade(), "quantidade (escala 8)");
            verificarIgual(compra2.getPrecoUnitario(), lida.getPrecoUnitario(), "preco_unitario (escala 8)");
            verificarIgual(compra2.getTaxa(), lida.getTaxa(), "taxa (escala 2)");
            verificarIgual(compra2.getDataOperacao(), lida.getDataOperacao(), "data_operacao");
            verificarIgual(compra2.getObservacao(), lida.getObservacao(), "observacao");
            verificarIgual(criptoA.getIdCripto(), lida.getCriptoativo().getIdCripto(), "criptoativo");
            verificarIgual(idsDe(List.of(compra1, compra2, venda), Transacao::getIdTransacao), idsDe(transacaoDAO
                    .listarPorCarteira(usuario.getCarteira().getIdCarteira()), Transacao::getIdTransacao),
                    "listarPorCarteira na ordem de gravacao");
            List<Integer> criadas = idsDe(List.of(compra1, compra2, venda, compraPJ), Transacao::getIdTransacao);
            if (inseridaCrud) {   // a transacao do inserir (CRUD) tambem deve estar na listagem
                criadas.add(transacaoAvulsa.getIdTransacao());
            }
            return "buscarPorId, listarPorCarteira e " + listar("transacaoDAO.listarTodos()", transacaoDAO.listarTodos(),
                    Transacao::getIdTransacao, criadas, x -> String.format(
                    "%3d  %s %-3d %-12s %-6s %-6s @ %-10s taxa %-7s %s  %s", x.getIdTransacao(),
                    x.getCarteira().getTipo(), x.getCarteira().getIdCarteira(), x.getCriptoativo().getSigla(),
                    x.getTipo(), semZeros(x.getQuantidade()), semZeros(x.getPrecoUnitario()), x.getTaxa(),
                    x.getDataOperacao(), texto(x.getObservacao())));
        });
        testar(t, "FK polimorfica (PF e PJ)", inserido, () -> {
            Carteira daPF = ler(transacaoDAO, compra2.getIdTransacao()).getCarteira();
            Carteira daPJ = ler(transacaoDAO, compraPJ.getIdTransacao()).getCarteira();
            verificar(daPF instanceof CarteiraPF && daPF.getIdCarteira() == usuario.getCarteira().getIdCarteira(),
                    "a transacao da PF volta com uma CarteiraPF");
            verificar(daPJ instanceof CarteiraPJ, "a transacao da empresa volta com uma CarteiraPJ");
            return "transacao.id_carteira lida como CarteiraPF e CarteiraPJ";
        });
        testar(t, UPDATE, inserido, () -> {
            compra1.setObservacao("Observacao alterada " + sufixo);
            verificarIgual(1, transacaoDAO.alterar(compra1), "linhas alteradas");
            Transacao relida = ler(transacaoDAO, compra1.getIdTransacao());
            verificarIgual(compra1.getObservacao(), relida.getObservacao(), "observacao relida");
            verificarIgual(compra1.getTaxa(), relida.getTaxa(), "taxa relida (nao recalculada)");
            return "relido: observacao NULL -> '" + relida.getObservacao() + "'";
        });
    }

    private void testarPosicao() {
        String t = "posicao";
        secao("8. POSICAO (PosicaoDAO) - associativa carteira x criptoativo");
        boolean inserido = testar(t, INSERT, inseriu("transacao", "carteira_pf"), () -> {
            Posicao posicaoPF = usuario.getCarteira().buscarPosicao(criptoA.getIdCripto());
            Posicao posicaoPJ = empresa1.getCarteira().buscarPosicao(criptoB.getIdCripto());
            verificar(posicaoPF.getIdPosicao() > 0 && posicaoPJ.getIdPosicao() > 0, "registrar gravou as posicoes");
            // INSERT direto do CRUD (o caminho normal e TransacaoDAO.registrar)
            posicaoAvulsa = new Posicao(carteiraPFAvulsa, criptoB, new BigDecimal("2.5"), new BigDecimal("10"),
                    hoje.minusDays(5));
            int id = gravar(posicaoDAO, posicaoAvulsa, Posicao::getIdPosicao);
            return "inserir id=" + id + " + posicoes " + posicaoPF.getIdPosicao() + " e " + posicaoPJ.getIdPosicao()
                    + " gravadas por registrar";
        });
        testar(t, SELECT, inserido, () -> {
            Posicao lida = ler(posicaoDAO, posicaoAvulsa.getIdPosicao());
            verificar(lida.getCarteira() instanceof CarteiraPF
                    && lida.getCarteira().getIdCarteira() == carteiraPFAvulsa.getIdCarteira(), "carteira PF montada");
            verificarIgual(criptoB.getIdCripto(), lida.getCriptoativo().getIdCripto(), "criptoativo");
            verificarIgual(new BigDecimal("2.50000000"), lida.getQuantidadeAtual(), "quantidade_atual (escala 8)");
            verificarIgual(new BigDecimal("10.00000000"), lida.getPrecoMedioCompra(), "preco_medio_compra");
            verificarIgual(hoje.minusDays(5), lida.getDataPrimeiraAquisicao(), "data_primeira_aquisicao");
            int idCarteira = usuario.getCarteira().getIdCarteira();
            int idPosicaoPF = usuario.getCarteira().buscarPosicao(criptoA.getIdCripto()).getIdPosicao();
            int idPosicaoPJ = empresa1.getCarteira().buscarPosicao(criptoB.getIdCripto()).getIdPosicao();
            Posicao porUK = posicaoDAO.buscarPorCarteiraECripto(idCarteira, criptoA.getIdCripto());
            verificar(porUK != null && porUK.getIdPosicao() == idPosicaoPF, "buscarPorCarteiraECripto (UK)");
            verificarIgual(List.of(idPosicaoPF), idsDe(posicaoDAO.listarPorCarteira(idCarteira), Posicao::getIdPosicao),
                    "listarPorCarteira da carteira PF do usuario");
            // as duas posicoes gravadas por registrar (PF do usuario e PJ da empresa) e a do inserir
            List<Integer> esperados = List.of(idPosicaoPF, idPosicaoPJ, lida.getIdPosicao());
            return "buscarPorId, buscarPorCarteiraECripto, listarPorCarteira e " + listar("posicaoDAO.listarTodos()",
                    posicaoDAO.listarTodos(), Posicao::getIdPosicao, esperados, p -> String.format(
                    "%3d  %s %-3d %-12s qtde %-6s preco medio %-13s valor %-10s desde %s, atualizada %s",
                    p.getIdPosicao(), p.getCarteira().getTipo(), p.getCarteira().getIdCarteira(),
                    p.getCriptoativo().getSigla(), semZeros(p.getQuantidadeAtual()), semZeros(p.getPrecoMedioCompra()),
                    p.calcularValorAtual(), p.getDataPrimeiraAquisicao(), p.getDataUltimaAtualizacao()));
        });
        testar(t, UPDATE, inserido, () -> {
            posicaoAvulsa.setQuantidadeAtual(new BigDecimal("4"));
            posicaoAvulsa.setPrecoMedioCompra(new BigDecimal("11.5"));
            posicaoAvulsa.setDataUltimaAtualizacao(hoje);
            verificarIgual(1, posicaoDAO.alterar(posicaoAvulsa), "linhas alteradas");
            Posicao relida = ler(posicaoDAO, posicaoAvulsa.getIdPosicao());
            verificarIgual(new BigDecimal("4.00000000"), relida.getQuantidadeAtual(), "quantidade relida");
            verificarIgual(new BigDecimal("11.50000000"), relida.getPrecoMedioCompra(), "preco medio relido");
            verificarIgual(hoje, relida.getDataUltimaAtualizacao(), "data_ultima_atualizacao relida");
            return "relido: quantidade 2.5 -> 4, preco medio 10 -> 11.5, atualizada em " + hoje;
        });
    }

    /** RelatorioDAO grava o snapshot da carteira; CarteiraDAO.carregarHistorico reconstroi a carteira do banco. */
    private void testarRelatorio() {
        String t = "relatorio";
        secao("9. RELATORIO (RelatorioDAO) + CarteiraDAO.carregarHistorico");
        boolean inserido = testar(t, INSERT, inseriu("transacao"), () -> {
            relatorio = new Relatorio(0, usuario.getCarteira(), hoje.minusDays(1));
            int id = gravar(relatorioDAO, relatorio, Relatorio::getIdRelatorio);
            return "snapshot da carteira " + usuario.getCarteira().getIdCarteira() + " id=" + id;
        });
        testar(t, SELECT, inserido, () -> {
            int idCarteira = usuario.getCarteira().getIdCarteira();
            Relatorio lido = ler(relatorioDAO, relatorio.getIdRelatorio());
            verificarIgual(numeros(relatorio), numeros(lido), "numeros do relatorio relido");
            verificarIgual(relatorio.getDataGeracao(), lido.getDataGeracao(), "data_geracao");
            verificar(lido.getCarteira() instanceof CarteiraPF && lido.getCarteira().getIdCarteira() == idCarteira,
                    "carteira PF montada");
            verificar(idsDe(relatorioDAO.listarPorCarteira(idCarteira), Relatorio::getIdRelatorio)
                    .contains(lido.getIdRelatorio()), "listarPorCarteira contem o relatorio");
            return "buscarPorId (numeros gravados, sem recalculo), listarPorCarteira e " + listar(
                    "relatorioDAO.listarTodos()", relatorioDAO.listarTodos(), Relatorio::getIdRelatorio,
                    List.of(lido.getIdRelatorio()), r -> String.format("%3d  %s %-3d %s  valor %s, investido %s, "
                    + "vendido %s, taxas %s, lucro %s, rentab %s%%  (%s)", r.getIdRelatorio(), r.getCarteira().getTipo(),
                    r.getCarteira().getIdCarteira(), r.getDataGeracao(), r.getValorTotalCarteira(),
                    r.getTotalInvestido(), r.getTotalVendido(), r.getTotalTaxas(), r.getLucroTotal(),
                    r.getRentabilidadePercentual(), r.getCarteira().getDescricao()));
        });
        boolean historico = testar(t, "carregarHistorico + Relatorio relido", inserido, () -> {
            CarteiraPF carteira = usuario.getCarteira();
            Carteira relida = carteiraDAO.buscarPorId(carteira.getIdCarteira());
            verificar(relida.getTotalTransacoes() == 0 && relida.getPosicoes().isEmpty(), "buscarPorId sem historico");
            carteiraDAO.carregarHistorico(relida);
            verificarIgual(carteira.getTotalTransacoes(), relida.getTotalTransacoes(), "transacoes carregadas");
            verificarIgual(carteira.getPosicoes().size(), relida.getPosicoes().size(), "posicoes carregadas");
            verificarIgual(carteira.getSaldoReais(), relida.getSaldoReais(), "saldo da carteira relida");
            Relatorio doBanco = new Relatorio(0, relida, relatorio.getDataGeracao());
            System.out.println("Relatorio (valor total, investido, vendido, taxas, lucro, rentabilidade %):");
            System.out.println("  da carteira em memoria ...............: " + numeros(relatorio));
            System.out.println("  da carteira relida (carregarHistorico): " + numeros(doBanco));
            verificarIgual(numeros(relatorio), numeros(doBanco), "relatorio da carteira relida x da memoria");
            return relida.getTotalTransacoes() + " transacoes e " + relida.getPosicoes().size()
                    + " posicao carregadas; relatorios identicos";
        });
        testar(t, UPDATE, historico, () -> {
            // a carteira relida com o historico pode operar e ser persistida de novo
            Carteira relida = carteiraDAO.buscarPorId(usuario.getCarteira().getIdCarteira());
            carteiraDAO.carregarHistorico(relida);
            Transacao novaCompra = new Transacao(0, "COMPRA", criptoB, new BigDecimal("50"), new BigDecimal("12"), hoje);
            registrarNaCarteira(relida, novaCompra, "Compra apos carregarHistorico");
            Relatorio atualizado = new Relatorio(relatorio.getIdRelatorio(), relida, hoje);
            verificarIgual(1, relatorioDAO.alterar(atualizado), "linhas alteradas");
            Relatorio relido = ler(relatorioDAO, relatorio.getIdRelatorio());
            verificarIgual(numeros(atualizado), numeros(relido), "snapshot regravado");
            verificarIgual(hoje, relido.getDataGeracao(), "data_geracao regravada");
            verificar(relatorio.getTotalInvestido().compareTo(relido.getTotalInvestido()) != 0, "investido mudou");
            return "snapshot regravado apos nova compra (relido): investido " + relatorio.getTotalInvestido() + " -> "
                    + relido.getTotalInvestido() + ", data -> " + relido.getDataGeracao();
        });
    }

    private void testarAlerta() {
        String t = "alerta";
        secao("10. ALERTA (AlertaDAO) - associativa usuario x criptoativo");
        boolean inserido = testar(t, INSERT, inseriu("usuario", "criptoativo"), () -> {
            alerta1 = new Alerta(0, usuario, criptoA, new BigDecimal("5"), hoje);
            alerta2 = new Alerta(0, usuario, criptoB, new BigDecimal("2.5"), hoje);
            int id1 = gravar(alertaDAO, alerta1, Alerta::getIdAlerta);
            int id2 = gravar(alertaDAO, alerta2, Alerta::getIdAlerta);
            return "ids " + id1 + " (" + criptoA.getSigla() + ") e " + id2 + " (" + criptoB.getSigla() + ")";
        });
        testar(t, SELECT, inserido, () -> {
            Alerta lido = ler(alertaDAO, alerta1.getIdAlerta());
            verificarIgual(usuario.getIdUsuario(), lido.getUsuario().getIdUsuario(), "usuario");
            verificarIgual(criptoA.getIdCripto(), lido.getCriptoativo().getIdCripto(), "criptoativo");
            verificarIgual(new BigDecimal("5.0000"), lido.getLimiteVariacao(), "limite_variacao (escala 4)");
            verificar(lido.isAtivado(), "ativado 'S' -> true");
            verificarIgual(hoje, lido.getDataConfiguracao(), "data_configuracao");
            List<Integer> ids = List.of(alerta1.getIdAlerta(), alerta2.getIdAlerta());
            verificarIgual(ids, idsDe(alertaDAO.listarPorUsuario(usuario.getIdUsuario()), Alerta::getIdAlerta),
                    "listarPorUsuario");
            return "buscarPorId, listarPorUsuario e " + listar("alertaDAO.listarTodos()", alertaDAO.listarTodos(),
                    Alerta::getIdAlerta, ids, a -> String.format("%3d  %-34s %-12s limite %-5s ativado %s  desde %s  "
                    + "dispara %s", a.getIdAlerta(), a.getUsuario().getNome(), a.getCriptoativo().getSigla(),
                    semZeros(a.getLimiteVariacao()) + "%", a.isAtivado() ? "S" : "N", a.getDataConfiguracao(),
                    a.verificarDisparo() ? "sim" : "nao")) + "; " + lido.mensagemAlerta();
        });
        boolean desativado = testar(t, UPDATE, inserido, () -> {
            alerta2.desativar();
            alerta2.setLimiteVariacao(new BigDecimal("7.5"));
            verificarIgual(1, alertaDAO.alterar(alerta2), "linhas alteradas");
            Alerta relido = ler(alertaDAO, alerta2.getIdAlerta());
            verificar(!relido.isAtivado(), "ativado relido 'N' -> false");
            verificarIgual(new BigDecimal("7.5000"), relido.getLimiteVariacao(), "limite relido");
            return "relido: ativado S -> N, limite 2.5 -> 7.5";
        });
        testar(t, "listarAtivos (S/N)", desativado, () -> {
            List<Alerta> ativos = alertaDAO.listarAtivos();
            ativos.forEach(a -> verificar(a.isAtivado(), "listarAtivos so traz alertas ativados"));
            List<Integer> ids = idsDe(ativos, Alerta::getIdAlerta);
            verificar(ids.contains(alerta1.getIdAlerta()), "o alerta ativo aparece em listarAtivos");
            verificar(!ids.contains(alerta2.getIdAlerta()), "o alerta desativado sai de listarAtivos");
            return ativos.size() + " ativo(s); o alerta " + alerta2.getIdAlerta() + " saiu da lista ao ser desativado";
        });
        testar(t, "UK usuario x cripto (ORA-00001)", inserido, () -> {
            Alerta repetido = new Alerta(0, usuario, criptoA, new BigDecimal("9"), hoje);
            String erro = esperarRecusa(() -> alertaDAO.inserir(repetido), 1, "23505",
                    "o banco aceitou um segundo alerta para o mesmo usuario e criptoativo");
            verificarIgual(0, repetido.getIdAlerta(), "id do alerta recusado");
            return "segundo alerta do mesmo par recusado - " + erro;
        });
    }

    /** Excluir um registro com dependentes deve ser recusado (ORA-02292) sem apagar nada. */
    private void testarIntegridadeReferencial() {
        secao("11. INTEGRIDADE REFERENCIAL (ORA-02292)");
        // sem os dependentes gravados o DELETE seria aceito: por isso exigem o INSERT de transacao/empresa
        testar("criptoativo", "FK: excluir em uso (ORA-02292)", inseriu("transacao"), () -> {
            int id = criptoA.getIdCripto();
            String erro = esperarRecusa(() -> criptoativoDAO.excluir(id), 2292, "23503",
                    "o banco excluiu um criptoativo com transacoes, posicoes e alertas");
            verificar(criptoativoDAO.buscarPorId(id) != null, "o criptoativo continua gravado");
            return "exclusao do criptoativo " + id + " recusada, ele continua gravado - " + erro;
        });
        testar("usuario", "FK: excluir com dependentes (rollback)", inseriu("empresa"), () -> {
            int id = usuario.getIdUsuario();
            String erro = esperarRecusa(() -> usuarioDAO.excluir(id), 2292, "23503",
                    "o banco excluiu um usuario com empresas e alertas");
            verificar(usuarioDAO.buscarPorId(id) != null, "o usuario continua gravado (rollback)");
            verificar(carteiraPFDAO.buscarPorId(usuario.getCarteira().getIdCarteira()) != null,
                    "a carteira PF continua gravada (rollback)");
            return "exclusao do usuario " + id + " recusada, usuario e carteira PF intactos - " + erro;
        });
    }

    /** Exclui, na ordem reversa das FKs, tudo o que foi gravado (dependentes localizados por carteira/usuario). */
    private void limpar() {
        secao("12. LIMPEZA = testes de DELETE (ordem reversa das FKs)");
        int idUsuario = usuario == null ? 0 : usuario.getIdUsuario();
        List<Integer> carteiras = idsDe(carteirasCriadas(), Carteira::getIdCarteira);
        excluir("alerta", alertaDAO, Alerta::getIdAlerta, a -> a.getUsuario().getIdUsuario() == idUsuario);
        excluir("relatorio", relatorioDAO, Relatorio::getIdRelatorio,
                r -> carteiras.contains(r.getCarteira().getIdCarteira()));
        excluir("posicao", posicaoDAO, Posicao::getIdPosicao, p -> carteiras.contains(p.getCarteira().getIdCarteira()));
        excluir("transacao", transacaoDAO, Transacao::getIdTransacao,
                x -> carteiras.contains(x.getCarteira().getIdCarteira()));
        excluir("empresa", empresaDAO, Empresa::getIdEmpresa, e -> e.getDono().getIdUsuario() == idUsuario);
        excluir("usuario", usuarioDAO, Usuario::getIdUsuario, u -> u.getIdUsuario() == idUsuario);
        excluir("carteira_pf", carteiraPFDAO, Carteira::getIdCarteira, c -> c.getIdCarteira() == id(carteiraPFAvulsa));
        excluir("carteira_pj", carteiraPJDAO, Carteira::getIdCarteira, c -> c.getIdCarteira() == id(carteiraPJAvulsa));
        excluir("carteira", carteiraDAO, Carteira::getIdCarteira,
                c -> c.getIdCarteira() == id(carteiraPFPolimorfica) || c.getIdCarteira() == id(carteiraPJPolimorfica));
        excluir("criptoativo", criptoativoDAO, Criptoativo::getIdCripto, c -> c.getSigla().endsWith(sufixo));
    }

    /**
     * DELETE pelo CrudDAO das linhas desta execucao (filtradas do listarTodos), confirmando buscarPorId == null e,
     * para usuario, empresa e carteiras, que a carteira (pai e filha) saiu junto. Um id com erro nao para os demais.
     */
    private <T> void excluir(String tabela, CrudDAO<T> dao, ToIntFunction<T> id, Predicate<T> desta) {
        testar(tabela, DELETE, () -> {
            List<T> linhas = new ArrayList<>(dao.listarTodos());
            linhas.removeIf(desta.negate());
            if (linhas.isEmpty()) {
                throw new NaoExecutado("nada desta execucao para excluir");
            }
            List<String> problemas = new ArrayList<>();
            for (T linha : linhas) {
                int i = id.applyAsInt(linha);
                int c = id(carteiraDe(linha));
                try {
                    verificarIgual(1, dao.excluir(i), "linhas excluidas do id " + i);
                    verificar(dao.buscarPorId(i) == null, "buscarPorId(" + i + ") == null apos o DELETE");
                    verificar(c == 0 || (carteiraDAO.buscarPorId(c) == null && carteiraPFDAO.buscarPorId(c) == null
                            && carteiraPJDAO.buscarPorId(c) == null), "a carteira " + c + " saiu junto (pai e filha)");
                } catch (Exception | AssertionError e) {
                    problemas.add("id " + i + ": " + motivo(e));
                }
            }
            verificar(problemas.isEmpty(), String.join(" | ", problemas));
            return linhas.size() + " linha(s) " + idsDe(linhas, id) + " excluida(s), buscarPorId == null"
                    + (carteiraDe(linhas.get(0)) == null ? "" : "; a carteira (pai e filha) saiu junto");
        });
    }

    /** Carteira que sai junto com a linha excluida (a do usuario, a da empresa ou ela mesma), ou null. */
    private static Carteira carteiraDe(Object linha) {
        if (linha instanceof Usuario dono) {
            return dono.getCarteira();
        }
        if (linha instanceof Empresa empresa) {
            return empresa.getCarteira();
        }
        return linha instanceof Carteira carteira ? carteira : null;
    }

    // ---- CONFERENCIA FINAL E RESUMO -------------------------------------------

    /** Cada tabela deve voltar a quantidade de linhas do inicio (nada ficou para tras). */
    private void conferirLimpeza() {
        for (int i = 0; i < TABELAS.size(); i++) {
            String tabela = TABELAS.get(i);
            CrudDAO<?> dao = daos.get(i);
            testar(tabela, "limpeza: contagem final = inicial", () -> {
                int depois = dao.listarTodos().size();
                verificarIgual(contagemInicial.get(tabela), depois, "linhas no fim (esperado: as do inicio)");
                return depois + " linha(s), como no inicio";
            });
        }
    }

    /** Matriz tabela x operacao, demais verificacoes, detalhe das falhas e total. */
    private void imprimirResumo() {
        secao("13. RESUMO - tabela x operacao");
        String formato = "  %-12s %-7s %-7s %-7s %-7s %s%n";
        System.out.printf(formato, "TABELA", INSERT, SELECT, UPDATE, DELETE, "OUTRAS");
        List<String> outras = new ArrayList<>();
        List<String> falhas = new ArrayList<>();
        int ok = 0;
        int naoExecutadas = 0;
        for (String tabela : TABELAS) {
            Map<String, String> celulas = new LinkedHashMap<>();
            OPERACOES.forEach(operacao -> celulas.put(operacao, null));   // CRUD sem resultado conta como "--"
            celulas.putAll(resultados.getOrDefault(tabela, Map.of()));
            List<Object> linha = new ArrayList<>(List.of(tabela));
            int outrasOk = 0;
            for (Map.Entry<String, String> celula : celulas.entrySet()) {
                String situacao = situacao(celula.getValue());
                ok += OK.equals(situacao) ? 1 : 0;
                naoExecutadas += NAO_EXECUTADO.equals(situacao) ? 1 : 0;
                if (FALHA.equals(situacao)) {
                    falhas.add("  " + tabela + " / " + celula.getKey() + ": " + celula.getValue());
                }
                if (OPERACOES.contains(celula.getKey())) {
                    linha.add(situacao);
                } else {
                    outrasOk += OK.equals(situacao) ? 1 : 0;
                    outras.add(String.format("  %-5s %-12s %s", situacao, tabela, celula.getKey()));
                }
            }
            int outrasTotal = celulas.size() - OPERACOES.size();
            linha.add(outrasOk + "/" + outrasTotal + (outrasOk == outrasTotal ? " OK" : " FALHA"));
            System.out.printf(formato, linha.toArray());
        }
        System.out.println();
        System.out.println("Outras verificacoes:");
        outras.forEach(System.out::println);
        if (!falhas.isEmpty()) {
            System.out.println();
            System.out.println("Falhas (detalhe):");
            falhas.forEach(System.out::println);
        }
        System.out.println();
        System.out.println("Verificacoes: " + ok + " OK | " + falhas.size() + " FALHA | " + naoExecutadas
                + " nao executadas");
        System.out.println("TOTAL DE FALHAS: " + falhas.size());
        System.out.println(falhas.isEmpty() && naoExecutadas == 0
                ? "RESULTADO: integracao JDBC aprovada nas " + TABELAS.size() + " tabelas."
                : "RESULTADO: ha falhas ou operacoes nao executadas - veja o detalhe acima.");
    }

    // ---- EXECUCAO DOS PASSOS E VERIFICACOES -----------------------------------

    private boolean testar(String tabela, String operacao, Passo<String> passo) {
        return testar(tabela, operacao, true, passo);
    }

    /**
     * Roda um passo e anota na matriz OK + detalhe, FALHA + motivo ou "nao executado" (pre-requisito, um passo
     * anterior, nao passou). Nenhuma excecao escapa daqui. Devolve true quando passou (pre-requisito dos seguintes).
     */
    private boolean testar(String tabela, String operacao, boolean preRequisito, Passo<String> passo) {
        String situacao;
        String detalhe;
        try {
            if (!preRequisito) {
                throw new NaoExecutado("depende de um passo anterior que falhou");
            }
            detalhe = passo.executar();
            situacao = OK;
        } catch (NaoExecutado e) {
            situacao = NAO_EXECUTADO;
            detalhe = "nao executado - " + e.getMessage();
        } catch (Exception | AssertionError e) {
            situacao = FALHA;
            detalhe = motivo(e);
        }
        resultados.computeIfAbsent(tabela, k -> new LinkedHashMap<>()).put(operacao, situacao + " - " + detalhe);
        System.out.println("[" + situacao + "] " + tabela + " / " + operacao + ": " + detalhe);
        return OK.equals(situacao);
    }

    /** true quando o INSERT de todas as tabelas informadas passou. */
    private boolean inseriu(String... tabelas) {
        for (String tabela : tabelas) {
            if (!OK.equals(situacao(resultados.getOrDefault(tabela, Map.of()).get(INSERT)))) {
                return false;
            }
        }
        return true;
    }

    /** OK, FALHA ou -- (nao executado / sem resultado) de uma celula da matriz. */
    private static String situacao(String resultado) {
        return resultado == null ? NAO_EXECUTADO : resultado.substring(0, resultado.indexOf(" - "));
    }

    /** Operacao que o banco deve recusar com o erro indicado: devolve a explicacao do erro; se for aceita, falha. */
    private static String esperarRecusa(Passo<?> operacao, int codigoOracle, String sqlState, String seAceitar)
            throws Exception {
        try {
            operacao.executar();
        } catch (SQLException e) {
            if (!ehErro(e, codigoOracle, sqlState)) {
                throw e;
            }
            return explicar(e);
        }
        throw new AssertionError(seAceitar);
    }

    /** INSERT pelo CrudDAO, conferindo que o id veio da sequence e so foi atribuido ao objeto depois. */
    private static <T> int gravar(CrudDAO<T> dao, T entidade, ToIntFunction<T> id) throws SQLException {
        int gerado = dao.inserir(entidade);
        verificar(gerado > 0 && id.applyAsInt(entidade) == gerado, "id " + gerado + " da sequence atribuido ao objeto");
        return gerado;
    }

    /** buscarPorId que exige a linha: falha com mensagem clara quando ela nao existe. */
    private static <T> T ler(CrudDAO<T> dao, int id) throws SQLException {
        T lido = dao.buscarPorId(id);
        verificar(lido != null, "buscarPorId(" + id + ") encontrou a linha");
        return lido;
    }

    /** Uso previsto: a carteira em memoria aplica as regras e, se aceitar, TransacaoDAO.registrar persiste. */
    private void registrarNaCarteira(Carteira carteira, Transacao t, String observacao) throws SQLException {
        boolean aceita = observacao == null ? carteira.registrarTransacao(t) : carteira.registrarTransacao(t, observacao);
        String operacao = t.getTipo() + " " + semZeros(t.getQuantidade()) + " " + t.getCriptoativo().getSigla();
        verificar(aceita, "a carteira " + carteira.getIdCarteira() + " aceitou " + operacao);
        int id = transacaoDAO.registrar(t);
        verificar(id > 0 && t.getIdTransacao() == id, "registrar atribuiu o id " + id + " da sequence");
        Posicao posicao = carteira.buscarPosicao(t.getCriptoativo().getIdCripto());
        System.out.println("registrar: transacao " + id + " " + operacao + " @ " + semZeros(t.getPrecoUnitario())
                + " (taxa " + t.getTaxa() + ") na carteira " + carteira.getTipo() + " " + carteira.getIdCarteira()
                + " -> saldo " + carteira.getSaldoReais() + ", posicao " + posicao.getIdPosicao() + " = "
                + semZeros(posicao.getQuantidadeAtual()));
    }

    /** Todas as carteiras gravadas nesta execucao (id > 0), PF e PJ. */
    private List<Carteira> carteirasCriadas() {
        List<Carteira> carteiras = new ArrayList<>(Arrays.asList(usuario == null ? null : usuario.getCarteira(),
                empresa1 == null ? null : empresa1.getCarteira(), empresa2 == null ? null : empresa2.getCarteira(),
                carteiraPFAvulsa, carteiraPJAvulsa, carteiraPFPolimorfica, carteiraPJPolimorfica));
        carteiras.removeIf(c -> id(c) == 0);
        return carteiras;
    }

    /** Id da carteira, ou 0 quando ela nem chegou a ser criada. */
    private static int id(Carteira carteira) {
        return carteira == null ? 0 : carteira.getIdCarteira();
    }

    /** Ids de uma lista de entidades, na mesma ordem. */
    private static <T> List<Integer> idsDe(List<T> entidades, ToIntFunction<T> id) {
        List<Integer> ids = new ArrayList<>();
        entidades.forEach(e -> ids.add(id.applyAsInt(e)));
        return ids;
    }

    /** Os seis numeros do snapshot (valor total, investido, vendido, taxas, lucro, rentabilidade). */
    private static List<BigDecimal> numeros(Relatorio r) {
        return Arrays.asList(r.getValorTotalCarteira(), r.getTotalInvestido(), r.getTotalVendido(),
                r.getTotalTaxas(), r.getLucroTotal(), r.getRentabilidadePercentual());
    }

    /** Interrompe o passo atual (AssertionError) quando a condicao nao vale. */
    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) {
            throw new AssertionError("verificacao falhou: " + descricao);
        }
    }

    /** Compara com equals (para BigDecimal, valor E escala) e mostra os dois valores na falha. */
    private static void verificarIgual(Object esperado, Object obtido, String descricao) {
        if (!Objects.equals(esperado, obtido)) {
            throw new AssertionError("verificacao falhou: " + descricao + " - esperado " + texto(esperado)
                    + ", obtido " + texto(obtido));
        }
    }

    // ---- MENSAGENS DE ERRO ----------------------------------------------------

    /** Motivo de uma falha: SQLException traduzida ou a mensagem da excecao. */
    private static String motivo(Throwable erro) {
        if (erro instanceof SQLException sql) {
            return explicar(sql);
        }
        if (erro instanceof AssertionError) {
            return erro.getMessage();
        }
        return erro.getClass().getSimpleName() + (erro.getMessage() == null ? "" : ": " + erro.getMessage());
    }

    /** O erro e o ORA indicado (getErrorCode no Oracle) ou o SQLState equivalente (H2, na validacao local). */
    private static boolean ehErro(SQLException e, int codigoOracle, String sqlState) {
        return e.getErrorCode() == codigoOracle || sqlState.equals(e.getSQLState());
    }

    /** Mensagem clara para os erros SQL mais comuns, seguida do codigo, do SQLState e da mensagem do driver. */
    private static String explicar(SQLException e) {
        String causa;
        if (ehErro(e, 2292, "23503")) {
            causa = "ORA-02292 (FK): o registro ainda e referenciado por outra tabela - exclua antes os dependentes.";
        } else if (ehErro(e, 1, "23505")) {
            causa = "ORA-00001 (UK/PK): ja existe um registro com o mesmo valor em uma chave unica "
                    + "(sigla, cpf, email, cnpj ou par usuario x criptoativo).";
        } else if (ehErro(e, 2291, "23506")) {
            causa = "ORA-02291 (FK): o registro pai nao existe - grave-o antes.";
        } else if (ehErro(e, 2290, "23513")) {
            causa = "ORA-02290 (CHECK): valor fora do permitido pela restricao da coluna.";
        } else if (ehErro(e, 1400, "23502")) {
            causa = "ORA-01400 (NOT NULL): coluna obrigatoria sem valor.";
        } else if (e.getErrorCode() == 1438) {
            causa = "ORA-01438: numero maior que a precisao da coluna (ex.: percentual acima de 999999.9999).";
        } else if (e.getErrorCode() == 12899) {
            causa = "ORA-12899: texto maior que o tamanho da coluna (ex.: observacao acima de 200 bytes).";
        } else if ("22001".equals(e.getSQLState())) {   // o H2 usa 22001 para os dois casos
            causa = "ORA-01438/ORA-12899: valor maior que a precisao ou o tamanho da coluna.";
        } else if (ehErro(e, 942, "42S02") || ehErro(e, 2289, "90036") || "42S04".equals(e.getSQLState())) {
            causa = "ORA-00942/ORA-02289: tabela ou sequence inexistente - execute sql/criptoinvest_ddl.sql.";
        } else {
            causa = "SQLException.";
        }
        String mensagem = e.getMessage() == null ? "" : e.getMessage().split("\n")[0].trim();
        mensagem = mensagem.length() > 160 ? mensagem.substring(0, 157) + "..." : mensagem;
        return causa + " [codigo " + e.getErrorCode() + ", SQLState " + e.getSQLState() + "] " + mensagem;
    }

    // ---- IMPRESSAO ------------------------------------------------------------

    private static void secao(String titulo) {
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println(" " + titulo);
        System.out.println("=".repeat(78));
    }

    /** Imprime a lista do DAO (* = linha com o sufixo desta execucao) e confere que ela contem os ids esperados. */
    private <T> String listar(String titulo, List<T> lista, ToIntFunction<T> id, List<Integer> esperados,
                              Function<T, String> linha) {
        String colecao = lista.size() + " linha(s) em " + lista.getClass().getSimpleName();
        System.out.println(titulo + " - " + colecao + " (* = criada nesta execucao)");
        for (T item : lista) {
            String texto = linha.apply(item);
            System.out.println((texto.contains(sufixo) ? "  * " : "    ") + texto);
        }
        verificar(idsDe(lista, id).containsAll(esperados), titulo + " contem os ids " + esperados);
        return "listarTodos (" + colecao + ")";
    }

    /** Carteira em uma linha: getTipo(), classe concreta (polimorfismo) e o atributo da tabela filha. */
    private static String linhaCarteira(Carteira c) {
        String filha = c instanceof CarteiraPF pf ? "limite " + pf.getLimiteDiarioSaque()
                : "regime " + ((CarteiraPJ) c).getRegimeTributario();
        return String.format("%3d  %s %-10s %-40s saldo %-10s %s", c.getIdCarteira(), c.getTipo(),
                c.getClass().getSimpleName(), c.getDescricao(), c.getSaldoReais(), filha);
    }

    /** Texto para exibicao: BigDecimal sem notacao cientifica, null como "(null)". */
    private static String texto(Object valor) {
        if (valor instanceof BigDecimal numero) {
            return numero.toPlainString();
        }
        return valor == null ? "(null)" : String.valueOf(valor);
    }

    /** Quantidade, preco de cripto e percentual sem os zeros a direita da escala (8 ou 4 casas). */
    private static String semZeros(BigDecimal valor) {
        return valor == null ? "(null)" : valor.stripTrailingZeros().toPlainString();
    }
}
