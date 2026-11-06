package com.criptoinvest.view;

import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Empresa;
import com.criptoinvest.model.Usuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Modulo EMPRESAS (tabela empresa + a carteira PJ 1:1): os clientes pessoa
 * juridica, cada um pertencente a um usuario dono.
 *
 * O cadastro grava a empresa e a sua carteira PJ numa unica transacao
 * (EmpresaDAO). Validacoes: dono existente, razao social ate 150 bytes, CNPJ
 * (numerico ou alfanumerico) com digitos verificadores e unico, regime
 * tributario entre os aceitos pelo banco.
 */
public class MenuEmpresa extends MenuBase {

    public MenuEmpresa(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Empresas (pessoa juridica)";
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Buscar por CNPJ", "Listar empresas de um usuario");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        if (indice == 0) {
            Empresa empresa = empresaDAO.buscarPorCnpj(entrada.lerCnpj(null));
            if (empresa == null) {
                Tela.aviso("Nenhuma empresa com esse CNPJ.");
            } else {
                detalhar(empresa);
            }
        } else {
            Usuario dono = escolherUsuario("ID do usuario");
            imprimirTabela(empresaDAO.listarPorUsuario(dono.getIdUsuario()));
        }
    }

    @Override
    protected void incluir() throws SQLException {
        Usuario dono = escolherUsuario("ID do usuario dono");
        String razaoSocial = entrada.lerTexto("Razao social", 150, null);
        String cnpj = lerCnpjDisponivel(null);
        int regime = entrada.lerEscolha("Regime tributario", CarteiraPJ.REGIMES_VALIDOS, 0);

        Empresa empresa = new Empresa(0, razaoSocial, cnpj, CarteiraPJ.REGIMES_VALIDOS.get(regime), dono);
        int id = empresaDAO.inserir(empresa);
        Tela.sucesso("Empresa cadastrada com ID " + id + " e carteira PJ #" + empresa.getCarteira().getIdCarteira()
                + " (saldo inicial R$ 0,00: faca aportes em Carteiras > Depositar).");
    }

    @Override
    protected void alterar() throws SQLException {
        Empresa empresa = escolher("Empresa a alterar", empresaDAO, Tela::resumo);
        detalhar(empresa);
        System.out.println();

        String nomeAnterior = empresa.getNome();
        empresa.setNome(entrada.lerTexto("Razao social", 150, empresa.getNome()));
        empresa.setCnpj(lerCnpjDisponivel(empresa));
        Usuario dono = empresa.getDono();
        empresa.setDono(escolher("ID do usuario dono", usuarioDAO, Tela::resumo, dono,
                "#" + dono.getIdUsuario() + " " + dono.getNome()));

        CarteiraPJ carteira = empresa.getCarteira();
        int regimeAtual = CarteiraPJ.REGIMES_VALIDOS.indexOf(carteira.getRegimeTributario());
        int regime = entrada.lerEscolha("Regime tributario", CarteiraPJ.REGIMES_VALIDOS, regimeAtual);
        carteira.setRegimeTributario(CarteiraPJ.REGIMES_VALIDOS.get(regime));
        // a descricao gerada no cadastro acompanha a razao social; uma personalizada fica como esta
        if ((Empresa.PREFIXO_CARTEIRA + nomeAnterior).equals(carteira.getDescricao())) {
            carteira.setDescricao(Empresa.PREFIXO_CARTEIRA + empresa.getNome());
        }

        if (empresaDAO.alterar(empresa) == 0) {
            Tela.erro("A empresa ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Empresa #" + empresa.getIdEmpresa() + " alterada.");
    }

    @Override
    protected void excluir() throws SQLException {
        Empresa empresa = escolher("Empresa a excluir", empresaDAO, Tela::resumo);
        detalhar(empresa);
        BigDecimal saldo = empresa.getCarteira().getSaldoReais();
        if (saldo.signum() > 0) {
            Tela.aviso("A carteira PJ tem saldo de " + Tela.reais(saldo) + ", que sera perdido com a exclusao.");
        }
        if (!confirmarExclusao("empresa " + empresa.getNome() + " e da sua carteira PJ")) {
            return;
        }
        try {
            if (empresaDAO.excluir(empresa.getIdEmpresa()) == 0) {
                Tela.erro("A empresa ja nao existe no banco.");
                return;
            }
            Tela.sucesso("Empresa #" + empresa.getIdEmpresa() + " e carteira PJ excluidas.");
        } catch (SQLException e) {
            if (!ErrosBanco.registroEmUso(e)) {
                throw e;
            }
            Tela.erro("A empresa nao pode ser excluida: a carteira PJ tem transacoes, posicoes ou relatorios. "
                    + "Estorne/exclua-os antes (menus Transacoes, Posicoes e Relatorios).");
        }
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(empresaDAO.listarTodos());
    }

    @Override
    protected void exibirUm() throws SQLException {
        detalhar(escolher("ID da empresa", empresaDAO, Tela::resumo));
    }

    private static void imprimirTabela(List<Empresa> empresas) {
        if (empresas.isEmpty()) {
            Tela.aviso("Nenhuma empresa encontrada.");
            return;
        }
        String formato = "%-5s %-24s %-19s %-18s %-16s %18s%n";
        System.out.printf(formato, "ID", "RAZAO SOCIAL", "CNPJ", "DONO", "REGIME", "SALDO CARTEIRA PJ");
        Tela.separador(105);
        for (Empresa e : empresas) {
            System.out.printf(formato, e.getIdEmpresa(), Tela.cortar(e.getNome(), 24), e.getCnpj(),
                    Tela.cortar(e.getDono().getNome(), 18), e.getCarteira().getRegimeTributario(),
                    Tela.reais(e.getCarteira().getSaldoReais()));
        }
        Tela.info(empresas.size() + " empresa(s).");
    }

    /** Pede o CNPJ ate ele ser valido e livre (uk_empresa_cnpj); na alteracao, o da propria empresa vale. */
    private String lerCnpjDisponivel(Empresa atual) throws SQLException {
        while (true) {
            String cnpj = entrada.lerCnpj(atual == null ? null : atual.getCnpj());
            Empresa existente = empresaDAO.buscarPorCnpj(cnpj);
            if (existente == null || (atual != null && existente.getIdEmpresa() == atual.getIdEmpresa())) {
                return cnpj;
            }
            Tela.erro("CNPJ ja cadastrado para a empresa #" + existente.getIdEmpresa()
                    + " (" + existente.getNome() + ").");
        }
    }

    private static void detalhar(Empresa e) {
        CarteiraPJ carteira = e.getCarteira();
        Tela.campo("ID", e.getIdEmpresa());
        Tela.campo("Razao social", e.getNome());
        Tela.campo("CNPJ", e.getCnpj());
        Tela.campo("Dono", "#" + e.getDono().getIdUsuario() + " " + e.getDono().getNome());
        Tela.campo("Carteira PJ", "#" + carteira.getIdCarteira() + " " + carteira.getDescricao());
        Tela.campo("Regime tributario", carteira.getRegimeTributario());
        Tela.campo("Saldo em reais", Tela.reais(carteira.getSaldoReais()));
    }
}
