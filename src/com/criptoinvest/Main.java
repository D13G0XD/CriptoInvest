package com.criptoinvest;

import com.criptoinvest.dao.CriptoativoDAO;
import com.criptoinvest.factory.ConnectionFactory;
import com.criptoinvest.view.Entrada;
import com.criptoinvest.view.ErrosBanco;
import com.criptoinvest.view.MenuAlerta;
import com.criptoinvest.view.MenuBase;
import com.criptoinvest.view.MenuCarteira;
import com.criptoinvest.view.MenuCriptoativo;
import com.criptoinvest.view.MenuEmpresa;
import com.criptoinvest.view.MenuPosicao;
import com.criptoinvest.view.MenuRelatorio;
import com.criptoinvest.view.MenuTransacao;
import com.criptoinvest.view.MenuUsuario;
import com.criptoinvest.view.Tela;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Fase 7: programa console do CriptoInvest integrado ao Oracle.
 *
 * Confere a conexao com o banco e abre o menu principal, com um modulo para
 * cada classe do dominio. Cada modulo leva a um submenu (pacote view) com
 * Inclusao, Alteracao, Exclusao, Exibicao de todos e Exibicao de um registro,
 * alem das opcoes proprias dele. Toda leitura e gravacao passa pelos DAOs, entao
 * o que aparece na tela vem sempre das tabelas.
 *
 * A Main da Fase 6 (teste automatico de INSERT/SELECT/UPDATE/DELETE nas 10
 * tabelas) continua no historico do git, no commit a608f1c.
 */
public class Main {

    public static void main(String[] args) {
        Entrada entrada = new Entrada(System.in);
        try {
            Tela.titulo("CriptoInvest - Gestao de investimentos em criptoativos");
            System.out.println("  VOLTZ Engenharia de Software - Fase 7");

            if (!conectar(entrada)) {
                System.out.println("Programa encerrado sem conexao com o banco.");
                return;
            }
            executarMenuPrincipal(entrada, criarModulos(entrada));
            System.out.println("Ate logo!");
        } catch (NoSuchElementException e) {
            // a entrada acabou (Ctrl+Z / Ctrl+D ou fim de um arquivo redirecionado)
            System.out.println();
            System.out.println("Fim da entrada de dados. Programa encerrado.");
        }
    }

    /** Um submenu por classe do dominio, na ordem do menu principal. */
    private static List<MenuBase> criarModulos(Entrada entrada) {
        List<MenuBase> modulos = new ArrayList<>();
        modulos.add(new MenuUsuario(entrada));
        modulos.add(new MenuEmpresa(entrada));
        modulos.add(new MenuCarteira(entrada));
        modulos.add(new MenuCriptoativo(entrada));
        modulos.add(new MenuTransacao(entrada));
        modulos.add(new MenuPosicao(entrada));
        modulos.add(new MenuAlerta(entrada));
        modulos.add(new MenuRelatorio(entrada));
        return modulos;
    }

    private static void executarMenuPrincipal(Entrada entrada, List<MenuBase> modulos) {
        while (true) {
            Tela.titulo("Menu principal");
            for (int i = 0; i < modulos.size(); i++) {
                System.out.println("  " + (i + 1) + " - " + modulos.get(i).getTitulo());
            }
            System.out.println("  0 - Sair");

            int opcao = entrada.lerOpcao(0, modulos.size());
            if (opcao == 0) {
                return;
            }
            modulos.get(opcao - 1).executar();
        }
    }

    /**
     * Confere a conexao e se o DDL foi aplicado antes de abrir o menu. Sem
     * conexao, explica como configurar as credenciais e deixa tentar de novo.
     *
     * @return true quando o banco esta pronto para uso
     */
    private static boolean conectar(Entrada entrada) {
        while (true) {
            System.out.println();
            System.out.println("Banco: " + ConnectionFactory.getUrl() + " | usuario: " + ConnectionFactory.getUsuario());
            String env = ConnectionFactory.getArquivoEnv();
            System.out.println("Credenciais: " + (env == null ? "nenhum arquivo .env encontrado" : env));

            if (ConnectionFactory.testarConexao() && tabelasCriadas()) {
                return true;
            }
            System.out.println();
            System.out.println("Confira:");
            System.out.println("  - o arquivo .env na raiz do projeto (copie o .env_exemplo) com DB_URL, DB_USER e DB_PASSWORD;");
            System.out.println("  - o driver ojdbc8 no classpath (mvn compile exec:java ou lib/ojdbc8.jar);");
            System.out.println("  - o acesso a oracle.fiap.com.br:1521 e o script sql/criptoinvest_ddl.sql aplicado.");
            System.out.println("  1 - Tentar novamente");
            System.out.println("  0 - Sair");
            if (entrada.lerOpcao(0, 1) == 0) {
                return false;
            }
        }
    }

    /** Uma consulta simples confirma que as tabelas existem (sem o DDL, o Oracle devolve ORA-00942). */
    private static boolean tabelasCriadas() {
        try {
            new CriptoativoDAO().listarTodos();
            return true;
        } catch (SQLException e) {
            Tela.erro(ErrosBanco.explicar(e));
            return false;
        }
    }
}
