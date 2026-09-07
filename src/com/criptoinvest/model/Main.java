package com.criptoinvest.model;

import com.criptoinvest.dao.CriptoativoDAO;
import com.criptoinvest.factory.ConnectionFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // --- Criptoativos ---
        Criptoativo btc;
        Criptoativo eth;
        try {
            btc = new Criptoativo(1, "Bitcoin", "BTC", 300000.00, "Moeda");
            eth = new Criptoativo(2, "Ethereum", "ETH", 15000.00, "Plataforma");
            btc.atualizarPreco(350000.00);
            eth.atualizarPreco(16000.00, 6.67);
            btc.exibirDados();
            eth.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar criptoativos: " + e.getMessage());
            return;
        }

        // --- HashMap de Criptoativos (sigla -> Criptoativo) ---
        HashMap<String, Criptoativo> mapCriptoativos = new HashMap<>();
        mapCriptoativos.put(btc.getSigla(), btc);
        mapCriptoativos.put(eth.getSigla(), eth);

        // --- ArrayList de Criptoativos (uso explicito de ArrayList #1) ---
        ArrayList<Criptoativo> listaCriptoativos = new ArrayList<>();
        listaCriptoativos.add(btc);
        listaCriptoativos.add(eth);

        // --- Usuarios ---
        Usuario usuario;
        Usuario usuario2;
        try {
            usuario  = new Usuario(1, "Lucas",   "lucas@email.com",   "senha123", "123.456.789-00");
            usuario2 = new Usuario(2, "Ana",     "ana@email.com",     "senha456", "987.654.321-00");
            usuario.getCarteira().depositar(10000.00);
            usuario.getCarteira().depositar(5000.00, "Aporte mensal");
            usuario.ativar2FA();
            usuario.exibirDados();
            usuario2.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar usuario: " + e.getMessage());
            return;
        }

        // --- HashMap de Usuarios (id -> Usuario) ---
        HashMap<Integer, Usuario> mapUsuarios = new HashMap<>();
        mapUsuarios.put(usuario.getId(),  usuario);
        mapUsuarios.put(usuario2.getId(), usuario2);

        // --- ArrayList de Usuarios (uso explicito de ArrayList #2) ---
        ArrayList<Usuario> listaUsuarios = new ArrayList<>();
        listaUsuarios.add(usuario);
        listaUsuarios.add(usuario2);

        // --- Empresa (carteira PJ criada automaticamente; dono obrigatorio) ---
        Empresa empresa;
        try {
            empresa = new Empresa(2, "ABCD Investimentos", "00.000.000/0001-00", "LUCRO_PRESUMIDO", usuario);
            usuario.adicionarEmpresa(empresa);
            empresa.getCarteira().depositar(50000.00, "Capital inicial");
            empresa.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar empresa: " + e.getMessage());
            return;
        }

        // --- Polimorfismo dinamico em Carteira (PF e PJ) ---
        try {
            Carteira[] carteiras = { usuario.getCarteira(), empresa.getCarteira() };
            System.out.println("\n--- Polimorfismo: iterando Carteira[] ---");
            for (Carteira c : carteiras) {
                System.out.println("[Tipo " + c.getTipo() + "] " + c.getDescricao()
                        + " | Saldo R$ " + String.format("%.2f", c.getSaldoReais()));
            }
        } catch (Exception e) {
            System.out.println("Erro no polimorfismo: " + e.getMessage());
        }

        // --- Transacoes na carteira PF do usuario (Posicao atualizada automaticamente) ---
        try {
            Transacao t1 = new Transacao(1, "COMPRA", btc, 0.5, "2026-05-07");
            Transacao t2 = new Transacao(2, "COMPRA", eth, 2.0, "2026-05-07");
            Transacao t3 = new Transacao(3, "VENDA", btc, 0.1, "2026-05-07");

            usuario.getCarteira().registrarTransacao(t1);
            usuario.getCarteira().registrarTransacao(t2, "Aporte em ETH");
            usuario.getCarteira().registrarTransacao(t3);

            t1.exibirDados();
            t2.exibirDados();
            t3.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao registrar transacoes: " + e.getMessage());
        }

        // --- Transacao na carteira PJ (mostra que a FK e polimorfica) ---
        try {
            Transacao tPj = new Transacao(4, "COMPRA", btc, 0.2, "2026-05-07");
            empresa.getCarteira().registrarTransacao(tPj, "Compra institucional ABCD");
            tPj.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao registrar transacao PJ: " + e.getMessage());
        }

        // --- Saque com restricao de limite diario ACUMULADO na CarteiraPF ---
        try {
            System.out.println("\n--- Teste de limite de saque PF (limite diario R$ 5000) ---");
            usuario.getCarteira().sacar(3000.00);   // ok, acumulado=3000
            usuario.getCarteira().sacar(2500.00);   // estoura: 3000+2500=5500 > 5000
            usuario.getCarteira().sacar(1500.00);   // ok: 3000+1500=4500
        } catch (Exception e) {
            System.out.println("Erro no saque: " + e.getMessage());
        }

        // --- Resumo da carteira PF (metricas corrigidas) ---
        try {
            System.out.println();
            usuario.getCarteira().exibirResumo();
        } catch (Exception e) {
            System.out.println("Erro ao exibir resumo: " + e.getMessage());
        }

        // --- Relatorio (sobre a carteira PF do Lucas) ---
        try {
            Relatorio relatorio = new Relatorio(1, usuario.getCarteira(), "2026-05-07");
            relatorio.exibirRelatorio();
        } catch (Exception e) {
            System.out.println("Erro ao gerar relatorio: " + e.getMessage());
        }

        // --- Alerta (Usuario x Criptoativo) ---
        try {
            Alerta alerta = new Alerta(1, usuario, btc, 5.0, "2026-05-07");
            alerta.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar alerta: " + e.getMessage());
        }

        // --- Posicao (gerenciada automaticamente pela carteira) ---
        try {
            Posicao posBtc = usuario.getCarteira().buscarPosicao("BTC");
            if (posBtc != null) {
                posBtc.exibirDados();
            } else {
                System.out.println("Nenhuma posicao em BTC.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao exibir posicao: " + e.getMessage());
        }

        // =====================================================================
        // PERSISTENCIA EM ARQUIVOS DE TEXTO
        // =====================================================================

        // --- CRIACAO: gravar ArrayList de Criptoativos em arquivo ---
        System.out.println("\n--- Gravando criptoativos.txt (ArrayList) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("criptoativos.txt"))) {
            for (Criptoativo c : listaCriptoativos) {
                bw.write(c.getIdCripto() + "|" + c.getNome() + "|" + c.getSigla() + "|"
                        + c.getPrecoAtual() + "|" + c.getVariacao24h() + "|" + c.getCategoria());
                bw.newLine();
            }
            System.out.println("Arquivo criptoativos.txt criado com " + listaCriptoativos.size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao gravar criptoativos.txt: " + e.getMessage());
        }

        // --- CRIACAO: gravar ArrayList de Transacoes (carteira PF) em arquivo ---
        System.out.println("\n--- Gravando transacoes_pf.txt (ArrayList) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("transacoes_pf.txt"))) {
            for (Transacao t : usuario.getCarteira().getTransacoes()) {
                String obs = t.getObservacao() != null ? t.getObservacao() : "";
                bw.write(t.getIdTransacao() + "|" + t.getTipo() + "|"
                        + t.getCriptoativo().getSigla() + "|" + t.getQuantidade() + "|"
                        + t.getPrecoUnitario() + "|" + t.getTaxa() + "|"
                        + t.getDataOperacao() + "|" + obs);
                bw.newLine();
            }
            System.out.println("Arquivo transacoes_pf.txt criado com "
                    + usuario.getCarteira().getTotalTransacoes() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao gravar transacoes_pf.txt: " + e.getMessage());
        }

        // --- CRIACAO: gravar ArrayList de Posicoes (carteira PF) em arquivo ---
        System.out.println("\n--- Gravando posicoes_pf.txt (ArrayList) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("posicoes_pf.txt"))) {
            for (Posicao p : usuario.getCarteira().getPosicoes()) {
                bw.write(p.getIdPosicao() + "|" + p.getCriptoativo().getSigla() + "|"
                        + p.getQuantidadeAtual() + "|" + p.getPrecoMedioCompra() + "|"
                        + p.getDataPrimeiraAquisicao() + "|" + p.getDataUltimaAtualizacao());
                bw.newLine();
            }
            System.out.println("Arquivo posicoes_pf.txt criado com "
                    + usuario.getCarteira().getPosicoes().size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao gravar posicoes_pf.txt: " + e.getMessage());
        }

        // --- CRIACAO: gravar ArrayList de Usuarios em arquivo ---
        System.out.println("\n--- Gravando usuarios.txt (ArrayList) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("usuarios.txt"))) {
            for (Usuario u : listaUsuarios) {
                bw.write(u.getId() + "|" + u.getNome() + "|" + u.getEmail() + "|"
                        + u.getCpf() + "|" + u.isAutenticacaoDoisFatores());
                bw.newLine();
            }
            System.out.println("Arquivo usuarios.txt criado com " + listaUsuarios.size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao gravar usuarios.txt: " + e.getMessage());
        }

        // --- CRIACAO: gravar ArrayList de Empresas do Usuario em arquivo ---
        System.out.println("\n--- Gravando empresas.txt (ArrayList) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("empresas.txt"))) {
            for (Empresa emp : usuario.getEmpresas()) {
                bw.write(emp.getId() + "|" + emp.getNome() + "|" + emp.getCnpj() + "|"
                        + emp.getCarteira().getRegimeTributario() + "|" + emp.getDono().getNome());
                bw.newLine();
            }
            System.out.println("Arquivo empresas.txt criado com "
                    + usuario.getTotalEmpresas() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao gravar empresas.txt: " + e.getMessage());
        }

        // --- ATUALIZACAO: ler criptoativos.txt e atualizar precos no HashMap ---
        System.out.println("\n--- Lendo criptoativos.txt e atualizando HashMap com variacao de +5% ---");
        try (BufferedReader br = new BufferedReader(new FileReader("criptoativos.txt"))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] campos = linha.split("\\|");
                String sigla     = campos[2];
                double precoBase = Double.parseDouble(campos[3]);
                double novoPreco = precoBase * 1.05;
                Criptoativo c = mapCriptoativos.get(sigla);
                if (c != null) {
                    c.atualizarPreco(novoPreco);
                    System.out.println("Atualizado: " + sigla
                            + " | Preco anterior: R$ " + String.format("%.2f", precoBase)
                            + " -> Novo preco: R$ " + String.format("%.2f", c.getPrecoAtual()));
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler criptoativos.txt: " + e.getMessage());
        }

        // --- ATUALIZACAO: regravar criptoativos.txt com precos atualizados (ArrayList) ---
        System.out.println("\n--- Regravando criptoativos.txt com precos atualizados ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("criptoativos.txt"))) {
            for (Criptoativo c : listaCriptoativos) {
                bw.write(c.getIdCripto() + "|" + c.getNome() + "|" + c.getSigla() + "|"
                        + c.getPrecoAtual() + "|" + c.getVariacao24h() + "|" + c.getCategoria());
                bw.newLine();
            }
            System.out.println("Arquivo criptoativos.txt atualizado com " + listaCriptoativos.size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao regravar criptoativos.txt: " + e.getMessage());
        }

        // --- ATUALIZACAO: ler usuarios.txt e atualizar emails no HashMap ---
        System.out.println("\n--- Lendo usuarios.txt e atualizando emails no HashMap ---");
        try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] campos   = linha.split("\\|");
                int id            = Integer.parseInt(campos[0]);
                String emailVelho = campos[2];
                String emailNovo  = "novo." + emailVelho;
                Usuario u = mapUsuarios.get(id);
                if (u != null) {
                    u.setEmail(emailNovo);
                    System.out.println("Atualizado: Usuario " + u.getNome()
                            + " | Email anterior: " + emailVelho
                            + " -> Novo email: " + u.getEmail());
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler usuarios.txt: " + e.getMessage());
        }

        // --- ATUALIZACAO: regravar usuarios.txt com emails atualizados (ArrayList) ---
        System.out.println("\n--- Regravando usuarios.txt com emails atualizados ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("usuarios.txt"))) {
            for (Usuario u : listaUsuarios) {
                bw.write(u.getId() + "|" + u.getNome() + "|" + u.getEmail() + "|"
                        + u.getCpf() + "|" + u.isAutenticacaoDoisFatores());
                bw.newLine();
            }
            System.out.println("Arquivo usuarios.txt atualizado com " + listaUsuarios.size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao regravar usuarios.txt: " + e.getMessage());
        }

        // =====================================================================
        // FASE 5 - INTEGRACAO COM O BANCO DE DADOS ORACLE (classe Criptoativo)
        // =====================================================================
        executarTestesBancoDeDados();
    }

    // =========================================================================
    // METODOS DE TESTE DA INTEGRACAO COM O BANCO DE DADOS
    // Classe escolhida para a integracao: Criptoativo  ->  tabela CRIPTOATIVO
    // =========================================================================

    /** Executa, em sequencia, todos os testes de integracao com o Oracle. */
    public static void executarTestesBancoDeDados() {
        System.out.println("\n============================================================");
        System.out.println(" FASE 5 - INTEGRACAO COM BANCO DE DADOS ORACLE (Criptoativo)");
        System.out.println("============================================================");

        if (!testarConexao()) {
            System.out.println("Testes de banco de dados interrompidos: sem conexao.");
            System.out.println("Confira URL, usuario e senha em com.criptoinvest.factory.ConnectionFactory");
            System.out.println("e se o driver ojdbc esta no classpath.");
            return;
        }

        CriptoativoDAO dao = new CriptoativoDAO();
        try {
            int idNovo = testarInserir(dao);
            testarExibirTodos(dao);
            testarExibirPorId(dao, idNovo);
            testarAlterar(dao, idNovo);
            testarExcluir(dao, idNovo);
            testarExibirTodos(dao);
        } catch (SQLException e) {
            System.out.println("Erro na integracao com o banco de dados: " + e.getMessage());
        }
    }

    /** Teste 1 - abertura da conexao com o Oracle da FIAP. */
    public static boolean testarConexao() {
        System.out.println("\n--- Teste 1: conexao com o banco ---");
        System.out.println("URL....: " + ConnectionFactory.getUrl());
        System.out.println("Usuario: " + ConnectionFactory.getUsuario());
        return ConnectionFactory.testarConexao();
    }

    /** Teste 2 - INSERT: grava um novo criptoativo e devolve o id gerado. */
    public static int testarInserir(CriptoativoDAO dao) throws SQLException {
        System.out.println("\n--- Teste 2: INSERIR criptoativo ---");

        Criptoativo novo = new Criptoativo(0, "Chainlink", "LINK", 85.50, "Oraculo");
        novo.setVariacao24h(2.35);

        int id = dao.inserir(novo);
        System.out.println("Criptoativo inserido com id_cripto = " + id);
        novo.exibirDados();
        return id;
    }

    /** Teste 3 - SELECT: lista todos os criptoativos gravados no banco. */
    public static void testarExibirTodos(CriptoativoDAO dao) throws SQLException {
        System.out.println("\n--- Teste 3: EXIBIR todos os criptoativos ---");

        List<Criptoativo> criptoativos = dao.listarTodos();
        System.out.println("Total de registros: " + criptoativos.size());
        System.out.printf("%-5s %-15s %-8s %15s %10s %-15s%n",
                "ID", "NOME", "SIGLA", "PRECO", "VAR24H", "CATEGORIA");

        for (Criptoativo c : criptoativos) {
            System.out.printf("%-5d %-15s %-8s %15.2f %9.2f%% %-15s%n",
                    c.getIdCripto(), c.getNome(), c.getSigla(),
                    c.getPrecoAtual(), c.getVariacao24h(), c.getCategoria());
        }
    }

    /** Teste 4 - SELECT por chave primaria. */
    public static void testarExibirPorId(CriptoativoDAO dao, int idCripto) throws SQLException {
        System.out.println("\n--- Teste 4: EXIBIR criptoativo por id (" + idCripto + ") ---");

        Criptoativo c = dao.buscarPorId(idCripto);
        if (c == null) {
            System.out.println("Nenhum criptoativo encontrado com id_cripto = " + idCripto);
            return;
        }
        c.exibirDados();
    }

    /** Teste 5 - UPDATE: altera preco, variacao e categoria do registro gravado. */
    public static void testarAlterar(CriptoativoDAO dao, int idCripto) throws SQLException {
        System.out.println("\n--- Teste 5: ALTERAR criptoativo (" + idCripto + ") ---");

        Criptoativo c = dao.buscarPorId(idCripto);
        if (c == null) {
            System.out.println("Nenhum criptoativo encontrado com id_cripto = " + idCripto);
            return;
        }

        System.out.println("Antes da alteracao:");
        c.exibirDados();

        c.atualizarPreco(99.90);              // recalcula a variacao 24h
        c.setCategoria("Oraculo Descentralizado");

        int linhas = dao.alterar(c);
        System.out.println("Linhas alteradas: " + linhas);

        System.out.println("Depois da alteracao (relido do banco):");
        Criptoativo atualizado = dao.buscarPorId(idCripto);
        if (atualizado != null) {
            atualizado.exibirDados();
        }
    }

    /** Teste 6 - DELETE: remove o registro criado pelos testes. */
    public static void testarExcluir(CriptoativoDAO dao, int idCripto) throws SQLException {
        System.out.println("\n--- Teste 6: EXCLUIR criptoativo (" + idCripto + ") ---");

        int linhas = dao.excluir(idCripto);
        System.out.println("Linhas excluidas: " + linhas);

        Criptoativo c = dao.buscarPorId(idCripto);
        System.out.println(c == null
                ? "Confirmado: o registro nao existe mais no banco."
                : "Atencao: o registro ainda existe no banco.");
    }
}
