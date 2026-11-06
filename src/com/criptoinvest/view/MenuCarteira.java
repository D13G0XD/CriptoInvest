package com.criptoinvest.view;

import com.criptoinvest.dao.CarteiraPFDAO;
import com.criptoinvest.dao.CarteiraPJDAO;
import com.criptoinvest.model.Carteira;
import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.CarteiraPJ;
import com.criptoinvest.model.Empresa;
import com.criptoinvest.model.Posicao;
import com.criptoinvest.model.Usuario;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modulo CARTEIRAS (carteira + carteira_pf | carteira_pj, heranca joined).
 *
 * As carteiras de usuarios e empresas nascem junto com o titular (menus
 * Usuarios e Empresas); aqui tambem e possivel criar carteiras avulsas, sem
 * titular. E o modulo das movimentacoes em reais - deposito e saque - e do
 * resumo completo da carteira (posicoes, totais, lucro e rentabilidade).
 *
 * Validacoes: descricao ate 200 bytes, saldo e limite nao negativos, regime
 * entre os aceitos, saque limitado ao saldo e (PF) ao limite diario, saldo
 * dentro de NUMBER(15,2). Carteira com titular so e excluida junto com ele.
 */
public class MenuCarteira extends MenuBase {

    /** Maior saldo que cabe em carteira.saldo_reais NUMBER(15,2). */
    private static final BigDecimal SALDO_MAXIMO = new BigDecimal("9999999999999.99");

    private static final List<String> TIPOS = List.of("PF - Pessoa Fisica", "PJ - Pessoa Juridica");

    private final CarteiraPFDAO carteiraPFDAO = new CarteiraPFDAO();
    private final CarteiraPJDAO carteiraPJDAO = new CarteiraPJDAO();

    public MenuCarteira(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Carteiras";
    }

    @Override
    protected String[] getRotulos() {
        return new String[] {
                "Incluir carteira avulsa (sem titular)", "Alterar", "Excluir", "Exibir todas",
                "Exibir uma (resumo com posicoes e rentabilidade)"
        };
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Depositar", "Sacar", "Listar por tipo (PF ou PJ)");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        switch (indice) {
            case 0 -> depositar();
            case 1 -> sacar();
            default -> {
                String tipo = entrada.lerEscolha("Tipo", TIPOS, -1) == 0 ? "PF" : "PJ";
                imprimirTabela(carteiraDAO.listarPorTipo(tipo));
            }
        }
    }

    @Override
    protected void incluir() throws SQLException {
        Tela.info("As carteiras de usuarios e empresas sao criadas junto com o cadastro deles. Aqui voce cria");
        Tela.info("uma carteira avulsa, sem titular (ex.: carteira de reserva ou de simulacao).");
        boolean pf = entrada.lerEscolha("Tipo da carteira", TIPOS, -1) == 0;
        String descricao = entrada.lerTexto("Descricao", 200, null);
        BigDecimal saldo = entrada.lerReais("Saldo inicial em R$", true, Valores.ZERO_DINHEIRO);

        Carteira carteira;
        if (pf) {
            BigDecimal limite = entrada.lerReais("Limite diario de saque em R$", true, Valores.dinheiro(Valores.de(5000)));
            carteira = new CarteiraPF(descricao, saldo, limite);
        } else {
            int regime = entrada.lerEscolha("Regime tributario", CarteiraPJ.REGIMES_VALIDOS, 0);
            carteira = new CarteiraPJ(descricao, saldo, CarteiraPJ.REGIMES_VALIDOS.get(regime));
        }
        int id = carteiraDAO.inserir(carteira);
        Tela.sucesso("Carteira " + carteira.getTipo() + " cadastrada com ID " + id + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Carteira carteira = escolherCarteira("Carteira a alterar");
        detalhar(carteira, titular(carteira));
        Tela.info("O saldo muda por deposito, saque ou compra e venda - nao por edicao direta.");
        System.out.println();

        carteira.setDescricao(entrada.lerTexto("Descricao", 200, carteira.getDescricao()));
        int linhas;
        if (carteira instanceof CarteiraPF pf) {
            pf.setLimiteDiarioSaque(entrada.lerReais("Limite diario de saque em R$", true, pf.getLimiteDiarioSaque()));
            linhas = carteiraPFDAO.alterar(pf);
        } else {
            CarteiraPJ pj = (CarteiraPJ) carteira;
            int atual = CarteiraPJ.REGIMES_VALIDOS.indexOf(pj.getRegimeTributario());
            int regime = entrada.lerEscolha("Regime tributario", CarteiraPJ.REGIMES_VALIDOS, atual);
            pj.setRegimeTributario(CarteiraPJ.REGIMES_VALIDOS.get(regime));
            linhas = carteiraPJDAO.alterar(pj);
        }
        if (linhas == 0) {
            Tela.erro("A carteira ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Carteira #" + carteira.getIdCarteira() + " alterada.");
    }

    @Override
    protected void excluir() throws SQLException {
        Carteira carteira = escolherCarteira("Carteira a excluir");
        String titular = titular(carteira);
        detalhar(carteira, titular);
        if (titular != null) {
            Tela.erro("A carteira pertence a " + titular + ": ela e excluida junto com o titular, "
                    + "pelo menu " + ("PF".equals(carteira.getTipo()) ? "Usuarios" : "Empresas") + ".");
            return;
        }
        if (carteira.getSaldoReais().signum() > 0) {
            Tela.aviso("A carteira tem saldo de " + Tela.reais(carteira.getSaldoReais())
                    + ", que sera perdido com a exclusao.");
        }
        if (!confirmarExclusao("carteira #" + carteira.getIdCarteira())) {
            return;
        }
        try {
            if (carteiraDAO.excluir(carteira.getIdCarteira()) == 0) {
                Tela.erro("A carteira ja nao existe no banco.");
                return;
            }
            Tela.sucesso("Carteira #" + carteira.getIdCarteira() + " excluida.");
        } catch (SQLException e) {
            if (!ErrosBanco.registroEmUso(e)) {
                throw e;
            }
            Tela.erro("A carteira nao pode ser excluida: tem transacoes, posicoes ou relatorios. "
                    + "Estorne/exclua-os antes (menus Transacoes, Posicoes e Relatorios).");
        }
    }

    @Override
    protected void exibirTodos() throws SQLException {
        imprimirTabela(carteiraDAO.listarTodos());
    }

    /** Resumo completo: dados, titular, totais calculados sobre o historico e as posicoes atuais. */
    @Override
    protected void exibirUm() throws SQLException {
        Carteira carteira = escolherCarteira("ID da carteira");
        carteiraDAO.carregarHistorico(carteira);
        detalhar(carteira, titular(carteira));

        Tela.campo("Transacoes registradas", carteira.getTotalTransacoes());
        Tela.campo("Total investido (compras)", Tela.reais(carteira.calcularTotalInvestido()));
        Tela.campo("Total vendido (liquido)", Tela.reais(carteira.calcularTotalVendido()));
        Tela.campo("Taxas pagas", Tela.reais(carteira.calcularTotalTaxas()));
        Tela.campo("Valor atual em cripto", Tela.reais(carteira.calcularValorTotal()));
        Tela.campo("Lucro total", Tela.reais(carteira.calcularLucroTotal()));
        Tela.campo("Rentabilidade", Tela.variacao(carteira.calcularRentabilidade()));

        List<Posicao> posicoes = carteira.getPosicoes();
        System.out.println();
        System.out.println("  Posicoes:");
        if (posicoes.isEmpty()) {
            System.out.println("    (nenhuma)");
            return;
        }
        String formato = "    %-8s %18s %24s %20s%n";
        System.out.printf(formato, "CRIPTO", "QUANTIDADE", "PRECO MEDIO", "VALOR ATUAL");
        for (Posicao p : posicoes) {
            System.out.printf(formato, p.getCriptoativo().getSigla(), Tela.cripto(p.getQuantidadeAtual()),
                    Tela.preco(p.getPrecoMedioCompra()), Tela.reais(p.calcularValorAtual()));
        }
    }

    /** Deposito em reais: regra do modelo (Carteira.depositar) e gravacao do novo saldo. */
    private void depositar() throws SQLException {
        Carteira carteira = escolherCarteira("Carteira para o deposito");
        Tela.campo("Saldo atual", Tela.reais(carteira.getSaldoReais()));
        BigDecimal valor = entrada.lerReais("Valor do deposito em R$", false, null);
        if (Valores.maior(carteira.getSaldoReais().add(valor), SALDO_MAXIMO)) {
            Tela.erro("O saldo passaria do maximo permitido (" + Tela.reais(SALDO_MAXIMO) + ").");
            return;
        }
        carteira.depositar(valor);
        gravarSaldo(carteira);
    }

    /**
     * Saque em reais: limitado ao saldo e, na carteira PF, ao limite diario.
     * O acumulado de saques do dia fica so em memoria (limitacao conhecida da
     * carteira_pf), entao aqui o limite vale para cada saque.
     */
    private void sacar() throws SQLException {
        Carteira carteira = escolherCarteira("Carteira para o saque");
        Tela.campo("Saldo atual", Tela.reais(carteira.getSaldoReais()));
        if (carteira instanceof CarteiraPF pf) {
            Tela.campo("Limite diario de saque", Tela.reais(pf.getLimiteDiarioSaque()));
        }
        if (carteira.getSaldoReais().signum() == 0) {
            Tela.erro("A carteira nao tem saldo para saque.");
            return;
        }
        BigDecimal valor = entrada.lerReais("Valor do saque em R$", false, null);
        if (Valores.maior(valor, carteira.getSaldoReais())) {
            Tela.erro("Saldo insuficiente: disponivel " + Tela.reais(carteira.getSaldoReais()) + ".");
            return;
        }
        if (carteira instanceof CarteiraPF pf && Valores.maior(valor, pf.getLimiteDiarioSaque())) {
            Tela.erro("O saque excede o limite diario de " + Tela.reais(pf.getLimiteDiarioSaque()) + ".");
            return;
        }
        BigDecimal saldoAntes = carteira.getSaldoReais();
        carteira.sacar(valor);
        if (carteira.getSaldoReais().compareTo(saldoAntes) == 0) {
            Tela.erro("Saque nao realizado.");
            return;
        }
        gravarSaldo(carteira);
    }

    private void gravarSaldo(Carteira carteira) throws SQLException {
        if (carteiraDAO.atualizarSaldo(carteira.getIdCarteira(), carteira.getSaldoReais()) == 0) {
            Tela.erro("A carteira ja nao existe no banco: nada foi gravado.");
            return;
        }
        Tela.sucesso("Saldo gravado: " + Tela.reais(carteira.getSaldoReais()) + ".");
    }

    /** Tabela de carteiras com o titular de cada uma (mapas id da carteira -> titular, uma consulta cada). */
    private void imprimirTabela(List<Carteira> carteiras) throws SQLException {
        if (carteiras.isEmpty()) {
            Tela.aviso("Nenhuma carteira encontrada.");
            return;
        }
        Map<Integer, String> titulares = new HashMap<>();
        for (Usuario u : usuarioDAO.listarTodos()) {
            titulares.put(u.getCarteira().getIdCarteira(), "Usuario: " + u.getNome());
        }
        for (Empresa e : empresaDAO.listarTodos()) {
            titulares.put(e.getCarteira().getIdCarteira(), "Empresa: " + e.getNome());
        }

        String formato = "%-5s %-4s %-28s %-24s %18s  %s%n";
        System.out.printf(formato, "ID", "TIPO", "DESCRICAO", "TITULAR", "SALDO", "LIMITE / REGIME");
        Tela.separador(110);
        for (Carteira c : carteiras) {
            System.out.printf(formato, c.getIdCarteira(), c.getTipo(), Tela.cortar(c.getDescricao(), 28),
                    Tela.cortar(titulares.getOrDefault(c.getIdCarteira(), "(avulsa)"), 24),
                    Tela.reais(c.getSaldoReais()), Tela.atributoProprio(c));
        }
        Tela.info(carteiras.size() + " carteira(s).");
    }

    /** Titular da carteira ("usuario #1 Ana"), ou null quando a carteira e avulsa. */
    private String titular(Carteira carteira) throws SQLException {
        if (carteira instanceof CarteiraPF) {
            Usuario usuario = usuarioDAO.buscarPorCarteira(carteira.getIdCarteira());
            return usuario == null ? null : "usuario #" + usuario.getIdUsuario() + " " + usuario.getNome();
        }
        Empresa empresa = empresaDAO.buscarPorCarteira(carteira.getIdCarteira());
        return empresa == null ? null : "empresa #" + empresa.getIdEmpresa() + " " + empresa.getNome();
    }

    private static void detalhar(Carteira c, String titular) {
        Tela.campo("ID", c.getIdCarteira());
        Tela.campo("Tipo", "PF".equals(c.getTipo()) ? "PF - Pessoa Fisica" : "PJ - Pessoa Juridica");
        Tela.campo("Descricao", c.getDescricao());
        Tela.campo("Titular", titular == null ? "(carteira avulsa)" : titular);
        Tela.campo("Saldo em reais", Tela.reais(c.getSaldoReais()));
        if (c instanceof CarteiraPF pf) {
            Tela.campo("Limite diario de saque", Tela.reais(pf.getLimiteDiarioSaque()));
        } else if (c instanceof CarteiraPJ pj) {
            Tela.campo("Regime tributario", pj.getRegimeTributario());
        }
    }
}
