package com.criptoinvest.view;

import com.criptoinvest.dao.AlertaDAO;
import com.criptoinvest.model.Alerta;
import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Modulo CRIPTOATIVOS (tabela criptoativo): o catalogo de moedas negociadas.
 *
 * Validacoes: nome ate 80 bytes, sigla unica so com letras e numeros (ate 10),
 * preco >= 0 com ate 8 casas, variacao com ate 4 casas e categoria opcional ate
 * 50 bytes. Um criptoativo usado em transacao, posicao ou alerta nao pode ser
 * excluido.
 */
public class MenuCriptoativo extends MenuBase {

    private final AlertaDAO alertaDAO = new AlertaDAO();

    public MenuCriptoativo(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Criptoativos";
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Buscar por sigla", "Atualizar cotacao (preco e variacao 24h)");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        if (indice == 0) {
            buscarPorSigla();
        } else {
            atualizarCotacao();
        }
    }

    @Override
    protected void incluir() throws SQLException {
        String nome = entrada.lerTexto("Nome (ex.: Bitcoin)", 80, null);
        String sigla = lerSiglaDisponivel(null);
        BigDecimal preco = entrada.lerCripto("Preco atual em R$", true, null);
        BigDecimal variacao = entrada.lerVariacao("Variacao nas ultimas 24h em %", Valores.percentual(BigDecimal.ZERO), false);
        String categoria = entrada.lerTextoOpcional("Categoria (ex.: Moeda, Plataforma, Stablecoin)", 50);

        Criptoativo criptoativo = new Criptoativo(0, nome, sigla, preco, categoria);
        criptoativo.setVariacao24h(variacao);
        int id = criptoativoDAO.inserir(criptoativo);
        Tela.sucesso("Criptoativo " + sigla + " cadastrado com ID " + id + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo a alterar");
        detalhar(criptoativo);
        System.out.println();

        criptoativo.setNome(entrada.lerTexto("Nome", 80, criptoativo.getNome()));
        criptoativo.setSigla(lerSiglaDisponivel(criptoativo));
        BigDecimal precoAnterior = criptoativo.getPrecoAtual();
        BigDecimal novoPreco = entrada.lerCripto("Preco atual em R$", true, precoAnterior);
        if (novoPreco.compareTo(precoAnterior) != 0) {
            // a variacao 24h e recalculada a partir do preco anterior
            criptoativo.atualizarPreco(novoPreco);
            Tela.info("Variacao 24h recalculada: " + Tela.variacao(criptoativo.getVariacao24h()));
        }
        criptoativo.setCategoria(entrada.lerTextoOpcional("Categoria", 50, criptoativo.getCategoria()));

        gravarAlteracao(criptoativo);
    }

    @Override
    protected void excluir() throws SQLException {
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo a excluir");
        detalhar(criptoativo);
        if (!confirmarExclusao(criptoativo.getSigla())) {
            return;
        }
        try {
            if (criptoativoDAO.excluir(criptoativo.getIdCripto()) == 0) {
                Tela.erro("O criptoativo ja nao existe no banco.");
                return;
            }
            Tela.sucesso("Criptoativo " + criptoativo.getSigla() + " excluido.");
        } catch (SQLException e) {
            if (!ErrosBanco.registroEmUso(e)) {
                throw e;
            }
            Tela.erro(criptoativo.getSigla() + " nao pode ser excluido: ha transacoes, posicoes ou alertas que o "
                    + "utilizam. Estorne/exclua-os antes (menus Transacoes, Posicoes e Alertas).");
        }
    }

    @Override
    protected void exibirTodos() throws SQLException {
        List<Criptoativo> criptoativos = criptoativoDAO.listarTodos();
        if (criptoativos.isEmpty()) {
            Tela.aviso("Nenhum criptoativo cadastrado.");
            return;
        }
        String formato = "%-5s %-8s %-22s %-14s %24s %12s%n";
        System.out.printf(formato, "ID", "SIGLA", "NOME", "CATEGORIA", "PRECO ATUAL", "VAR. 24H");
        Tela.separador(90);
        for (Criptoativo c : criptoativos) {
            System.out.printf(formato, c.getIdCripto(), c.getSigla(), Tela.cortar(c.getNome(), 22),
                    Tela.cortar(c.getCategoria(), 14), Tela.preco(c.getPrecoAtual()), Tela.variacao(c.getVariacao24h()));
        }
        Tela.info(criptoativos.size() + " criptoativo(s).");
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolherCriptoativo("Criptoativo"));
    }

    private void buscarPorSigla() throws SQLException {
        String sigla = entrada.lerSigla(null);
        Criptoativo criptoativo = criptoativoDAO.buscarPorSigla(sigla);
        if (criptoativo == null) {
            Tela.aviso("Nenhum criptoativo com a sigla " + sigla + ".");
            return;
        }
        detalhar(criptoativo);
    }

    /**
     * Nova cotacao: o preco e, opcionalmente, a variacao informada pela fonte;
     * sem ela, a variacao e calculada a partir do preco anterior. Depois de
     * gravar, mostra os alertas ativos desse criptoativo que dispararam.
     */
    private void atualizarCotacao() throws SQLException {
        Criptoativo criptoativo = escolherCriptoativo("Criptoativo");
        Tela.campo("Cotacao atual", Tela.preco(criptoativo.getPrecoAtual())
                + " (" + Tela.variacao(criptoativo.getVariacao24h()) + " em 24h)");

        BigDecimal novoPreco = entrada.lerCripto("Novo preco em R$", true, null);
        BigDecimal variacao = entrada.lerVariacao("Variacao 24h em % (ENTER calcula pelo preco anterior)", null, true);
        if (variacao == null) {
            criptoativo.atualizarPreco(novoPreco);
        } else {
            criptoativo.atualizarPreco(novoPreco, variacao);
        }
        if (!gravarAlteracao(criptoativo)) {
            return;
        }
        Tela.campo("Nova cotacao", Tela.preco(criptoativo.getPrecoAtual())
                + " (" + Tela.variacao(criptoativo.getVariacao24h()) + " em 24h)");

        int disparados = 0;
        for (Alerta alerta : alertaDAO.listarAtivos()) {
            if (alerta.getCriptoativo().getIdCripto() == criptoativo.getIdCripto() && alerta.verificarDisparo()) {
                Tela.aviso(alerta.mensagemAlerta());
                disparados++;
            }
        }
        if (disparados == 0) {
            Tela.info("Nenhum alerta disparado por esta cotacao.");
        }
    }

    /** Grava o criptoativo alterado e informa o resultado; false quando ele nao existe mais. */
    private boolean gravarAlteracao(Criptoativo criptoativo) throws SQLException {
        if (criptoativoDAO.alterar(criptoativo) == 0) {
            Tela.erro("O criptoativo ja nao existe no banco.");
            return false;
        }
        Tela.sucesso("Criptoativo " + criptoativo.getSigla() + " (ID " + criptoativo.getIdCripto() + ") alterado.");
        return true;
    }

    /** Pede a sigla ate ela estar livre (uk_criptoativo_sigla); na alteracao, a do proprio registro vale. */
    private String lerSiglaDisponivel(Criptoativo atual) throws SQLException {
        while (true) {
            String sigla = entrada.lerSigla(atual == null ? null : atual.getSigla());
            Criptoativo existente = criptoativoDAO.buscarPorSigla(sigla);
            if (existente == null || (atual != null && existente.getIdCripto() == atual.getIdCripto())) {
                return sigla;
            }
            Tela.erro("A sigla " + sigla + " ja pertence ao criptoativo #" + existente.getIdCripto()
                    + " (" + existente.getNome() + ").");
        }
    }

    private static void detalhar(Criptoativo c) {
        Tela.campo("ID", c.getIdCripto());
        Tela.campo("Nome", c.getNome());
        Tela.campo("Sigla", c.getSigla());
        Tela.campo("Categoria", c.getCategoria());
        Tela.campo("Preco atual", Tela.preco(c.getPrecoAtual()));
        Tela.campo("Variacao 24h", Tela.variacao(c.getVariacao24h()));
    }
}
