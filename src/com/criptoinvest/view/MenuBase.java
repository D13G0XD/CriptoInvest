package com.criptoinvest.view;

import com.criptoinvest.dao.CarteiraDAO;
import com.criptoinvest.dao.CriptoativoDAO;
import com.criptoinvest.dao.CrudDAO;
import com.criptoinvest.dao.EmpresaDAO;
import com.criptoinvest.dao.UsuarioDAO;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Usuario;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Submenu de um modulo do sistema (template method).
 *
 * Todo submenu tem as cinco operacoes pedidas - Inclusao, Alteracao, Exclusao,
 * Exibicao de todos e Exibicao de um unico registro - nas opcoes 1 a 5, e as
 * opcoes extras do modulo a partir da 6. Esta classe desenha o submenu, le a
 * opcao e trata os erros num so lugar:
 *   - OperacaoCancelada (o usuario digitou CANCELAR): nada foi gravado;
 *   - SQLException: mensagem traduzida por ErrosBanco;
 *   - IllegalArgumentException / IllegalStateException: regra de negocio
 *     recusada pelo modelo ou pelo DAO (a mensagem ja explica o motivo).
 * Um erro nunca derruba o programa: o usuario volta ao submenu.
 *
 * Tambem oferece a selecao de registros por ID usada por varios modulos
 * (usuario, carteira, criptoativo...), com ENTER listando os cadastrados.
 */
public abstract class MenuBase {

    private static final String[] ROTULOS_PADRAO = {
            "Incluir", "Alterar", "Excluir", "Exibir todos", "Exibir um (por ID)"
    };

    protected final Entrada entrada;

    // DAOs usados na selecao de registros de varios modulos. Os DAOs nao tem
    // estado nem conexao aberta: cada metodo abre e fecha a propria conexao.
    protected final UsuarioDAO usuarioDAO = new UsuarioDAO();
    protected final EmpresaDAO empresaDAO = new EmpresaDAO();
    protected final CarteiraDAO carteiraDAO = new CarteiraDAO();
    protected final CriptoativoDAO criptoativoDAO = new CriptoativoDAO();

    protected MenuBase(Entrada entrada) {
        this.entrada = entrada;
    }

    /** Nome do modulo, mostrado no menu principal e no titulo do submenu. */
    public abstract String getTitulo();

    protected abstract void incluir() throws SQLException;

    protected abstract void alterar() throws SQLException;

    protected abstract void excluir() throws SQLException;

    protected abstract void exibirTodos() throws SQLException;

    protected abstract void exibirUm() throws SQLException;

    /** Rotulos das opcoes 1 a 5; um modulo pode detalha-los (ex.: "Estornar (excluir)"). */
    protected String[] getRotulos() {
        return ROTULOS_PADRAO.clone();
    }

    /** Opcoes proprias do modulo, numeradas a partir de 6. */
    protected List<String> getOpcoesExtras() {
        return Collections.emptyList();
    }

    /** Executa a opcao extra de indice informado (0 = primeira de getOpcoesExtras). */
    protected void executarExtra(int indice) throws SQLException {
        // modulo sem opcoes extras
    }

    /** Laco do submenu: mostra as opcoes ate o usuario escolher 0 (voltar). */
    public void executar() {
        String[] rotulos = getRotulos();
        List<String> extras = getOpcoesExtras();
        while (true) {
            Tela.titulo(getTitulo());
            for (int i = 0; i < rotulos.length; i++) {
                System.out.println("  " + (i + 1) + " - " + rotulos[i]);
            }
            for (int i = 0; i < extras.size(); i++) {
                System.out.println("  " + (i + 1 + rotulos.length) + " - " + extras.get(i));
            }
            System.out.println("  0 - Voltar ao menu principal");

            int opcao = entrada.lerOpcao(0, rotulos.length + extras.size());
            if (opcao == 0) {
                return;
            }
            Tela.secao(opcao <= rotulos.length ? rotulos[opcao - 1] : extras.get(opcao - 1 - rotulos.length));
            if (opcao != 4) {
                // "Exibir todos" nao tem campos para preencher
                System.out.println("(digite " + Entrada.CANCELAR + " em qualquer campo para desistir; "
                        + "ENTER mantem o valor entre colchetes)");
            }
            executarOpcao(opcao);
            entrada.pausar();
        }
    }

    private void executarOpcao(int opcao) {
        try {
            switch (opcao) {
                case 1 -> incluir();
                case 2 -> alterar();
                case 3 -> excluir();
                case 4 -> exibirTodos();
                case 5 -> exibirUm();
                default -> executarExtra(opcao - 6);
            }
        } catch (OperacaoCancelada e) {
            Tela.aviso("Operacao cancelada: nada foi gravado.");
        } catch (SQLException e) {
            Tela.erro(ErrosBanco.explicar(e));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Tela.erro(e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Selecao de registros
    // ------------------------------------------------------------------------

    /**
     * Pede o ID ate encontrar um registro existente. ENTER (ou ?) lista os
     * cadastrados, uma linha cada, para consulta.
     */
    protected <T> T escolher(String rotulo, CrudDAO<T> dao, Function<T, String> resumo) throws SQLException {
        return escolher(rotulo, dao, resumo, null, null);
    }

    /**
     * Como escolher, mas com um valor atual (alteracao): ENTER mantem o atual e
     * ? lista os cadastrados.
     */
    protected <T> T escolher(String rotulo, CrudDAO<T> dao, Function<T, String> resumo,
                             T atual, String descricaoAtual) throws SQLException {
        String sufixo = atual == null ? " (ENTER lista)" : " [" + descricaoAtual + "] (? lista)";
        while (true) {
            String linha = entrada.lerCampo(rotulo + sufixo + ": ");
            if (linha.isEmpty() && atual != null) {
                return atual;
            }
            if (linha.isEmpty() || linha.equals("?")) {
                listar(dao.listarTodos(), resumo);
                continue;
            }
            Integer id = Entrada.inteiro(linha);
            if (id == null) {
                Tela.erro("Informe o numero do ID.");
                continue;
            }
            T registro = dao.buscarPorId(id);
            if (registro == null) {
                Tela.erro("Nenhum registro com ID " + id + ".");
                continue;
            }
            return registro;
        }
    }

    protected Usuario escolherUsuario(String rotulo) throws SQLException {
        return escolher(rotulo, usuarioDAO, Tela::resumo);
    }

    protected Carteira escolherCarteira(String rotulo) throws SQLException {
        return escolher(rotulo, carteiraDAO, Tela::resumo);
    }

    /** Criptoativo pelo ID ou pela sigla (BTC, eth...). ENTER lista os cadastrados. */
    protected Criptoativo escolherCriptoativo(String rotulo) throws SQLException {
        while (true) {
            String linha = entrada.lerCampo(rotulo + " - ID ou sigla (ENTER lista): ");
            if (linha.isEmpty() || linha.equals("?")) {
                listar(criptoativoDAO.listarTodos(), Tela::resumo);
                continue;
            }
            Integer id = Entrada.inteiro(linha);
            Criptoativo criptoativo = id != null ? criptoativoDAO.buscarPorId(id) : criptoativoDAO.buscarPorSigla(linha);
            if (criptoativo == null) {
                Tela.erro("Criptoativo nao encontrado: " + linha + ".");
                continue;
            }
            return criptoativo;
        }
    }

    /** Lista os registros em uma linha cada (ou avisa que nao ha nenhum). */
    protected static <T> void listar(List<T> registros, Function<T, String> resumo) {
        if (registros.isEmpty()) {
            System.out.println("  (nenhum registro cadastrado - digite " + Entrada.CANCELAR + " para voltar)");
            return;
        }
        for (T registro : registros) {
            System.out.println("  " + resumo.apply(registro));
        }
    }

    /** Pergunta de confirmacao das exclusoes; avisa quando o usuario desiste. */
    protected boolean confirmarExclusao(String descricao) {
        if (entrada.confirmar("Confirma a exclusao de " + descricao + "?")) {
            return true;
        }
        Tela.aviso("Exclusao cancelada: nada foi alterado.");
        return false;
    }
}
