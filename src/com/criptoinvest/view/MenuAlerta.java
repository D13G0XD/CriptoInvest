package com.criptoinvest.view;

import com.criptoinvest.dao.AlertaDAO;
import com.criptoinvest.model.Alerta;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Usuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Modulo ALERTAS (tabela alerta): o usuario monitora a variacao de 24h de um
 * criptoativo e o alerta dispara quando ela atinge o limite configurado.
 *
 * Validacoes: um alerta por par usuario x criptoativo
 * (uk_alerta_usuario_cripto), limite maior que zero com ate 4 casas e dentro
 * de NUMBER(10,4). A data de configuracao e a do dia da inclusao ou da ultima
 * alteracao.
 */
public class MenuAlerta extends MenuBase {

    private final AlertaDAO alertaDAO = new AlertaDAO();

    public MenuAlerta(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Alertas de variacao";
    }

    @Override
    protected String[] getRotulos() {
        return new String[] {
                "Incluir", "Alterar (limite / ativar ou desativar)", "Excluir", "Exibir todos", "Exibir um (por ID)"
        };
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Listar alertas ativos", "Listar alertas de um usuario", "Verificar alertas disparados");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        switch (indice) {
            case 0 -> imprimirTabela(alertaDAO.listarAtivos());
            case 1 -> imprimirTabela(alertaDAO.listarPorUsuario(escolherUsuario("ID do usuario").getIdUsuario()));
            default -> verificarDisparados();
        }
    }

    @Override
    protected void incluir() throws SQLException {
        Usuario usuario = escolherUsuario("ID do usuario");
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo a monitorar");
        for (Alerta existente : alertaDAO.listarPorUsuario(usuario.getIdUsuario())) {
            if (existente.getCriptoativo().getIdCripto() == criptoativo.getIdCripto()) {
                Tela.erro(usuario.getNome() + " ja monitora " + criptoativo.getSigla() + " (alerta #"
                        + existente.getIdAlerta() + "): use Alterar para mudar o limite.");
                return;
            }
        }
        Tela.campo("Variacao 24h atual", Tela.variacao(criptoativo.getVariacao24h()));
        BigDecimal limite = entrada.lerPercentualPositivo("Disparar quando a variacao (alta ou queda) atingir, em %", null);

        Alerta alerta = new Alerta(0, usuario, criptoativo, limite, LocalDate.now());
        int id = alertaDAO.inserir(alerta);
        Tela.sucesso("Alerta cadastrado com ID " + id + ".");
        if (alerta.verificarDisparo()) {
            Tela.aviso(alerta.mensagemAlerta());
        }
    }

    @Override
    protected void alterar() throws SQLException {
        Alerta alerta = escolher("Alerta a alterar", alertaDAO, Tela::resumo);
        detalhar(alerta);
        System.out.println();

        BigDecimal limiteAnterior = alerta.getLimiteVariacao();
        boolean ativadoAnterior = alerta.isAtivado();
        alerta.setLimiteVariacao(entrada.lerPercentualPositivo("Limite de variacao em %", limiteAnterior));
        alerta.setAtivado(entrada.lerSimNao("Alerta ativado?", ativadoAnterior));
        if (alerta.getLimiteVariacao().compareTo(limiteAnterior) == 0 && alerta.isAtivado() == ativadoAnterior) {
            Tela.aviso("Nenhum dado foi alterado.");
            return;
        }
        alerta.setDataConfiguracao(LocalDate.now());

        if (alertaDAO.alterar(alerta) == 0) {
            Tela.erro("O alerta ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Alerta #" + alerta.getIdAlerta() + " alterado.");
    }

    @Override
    protected void excluir() throws SQLException {
        Alerta alerta = escolher("Alerta a excluir", alertaDAO, Tela::resumo);
        detalhar(alerta);
        if (!confirmarExclusao("alerta #" + alerta.getIdAlerta())) {
            return;
        }
        if (alertaDAO.excluir(alerta.getIdAlerta()) == 0) {
            Tela.erro("O alerta ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Alerta #" + alerta.getIdAlerta() + " excluido.");
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(alertaDAO.listarTodos());
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolher("ID do alerta", alertaDAO, Tela::resumo));
    }

    /** Alertas ativos cuja variacao 24h atual atingiu o limite (mesma regra da consulta 4.9 do DML). */
    private void verificarDisparados() throws SQLException {
        int disparados = 0;
        for (Alerta alerta : alertaDAO.listarAtivos()) {
            if (alerta.verificarDisparo()) {
                Tela.aviso(alerta.mensagemAlerta());
                disparados++;
            }
        }
        Tela.info(disparados == 0 ? "Nenhum alerta disparado." : disparados + " alerta(s) disparado(s).");
    }

    private static void imprimirTabela(List<Alerta> alertas) {
        if (alertas.isEmpty()) {
            Tela.aviso("Nenhum alerta encontrado.");
            return;
        }
        String formato = "%-5s %-24s %-7s %12s %-6s %12s %-10s %-10s%n";
        System.out.printf(formato, "ID", "USUARIO", "CRIPTO", "LIMITE", "ATIVO", "VAR. 24H", "SITUACAO", "DESDE");
        Tela.separador(94);
        for (Alerta a : alertas) {
            System.out.printf(formato, a.getIdAlerta(), Tela.cortar(a.getUsuario().getNome(), 24),
                    a.getCriptoativo().getSigla(), Tela.percentual(a.getLimiteVariacao()), Tela.simNao(a.isAtivado()),
                    Tela.variacao(a.getCriptoativo().getVariacao24h()), a.verificarDisparo() ? "DISPARADO" : "-",
                    Tela.data(a.getDataConfiguracao()));
        }
        Tela.info(alertas.size() + " alerta(s).");
    }

    private static void detalhar(Alerta a) {
        Tela.campo("ID", a.getIdAlerta());
        Tela.campo("Usuario", Tela.resumo(a.getUsuario()));
        Tela.campo("Criptoativo", a.getCriptoativo().getSigla() + " - " + a.getCriptoativo().getNome());
        Tela.campo("Limite de variacao", Tela.percentual(a.getLimiteVariacao()));
        Tela.campo("Ativado", Tela.simNao(a.isAtivado()));
        Tela.campo("Configurado em", Tela.data(a.getDataConfiguracao()));
        Tela.campo("Variacao 24h atual", Tela.variacao(a.getCriptoativo().getVariacao24h()));
        Tela.campo("Situacao", a.mensagemAlerta());
    }
}
