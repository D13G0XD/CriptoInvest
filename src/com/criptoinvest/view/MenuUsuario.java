package com.criptoinvest.view;

import com.criptoinvest.dao.AlertaDAO;
import com.criptoinvest.model.Alerta;
import com.criptoinvest.model.CarteiraPF;
import com.criptoinvest.model.Empresa;
import com.criptoinvest.model.Usuario;
import com.criptoinvest.model.Valores;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Modulo USUARIOS (tabela usuario + a carteira PF 1:1): os clientes pessoa fisica.
 *
 * O cadastro grava o usuario e a sua carteira PF numa unica transacao
 * (UsuarioDAO). Validacoes: nome so com letras, CPF com digitos verificadores,
 * e-mail valido, CPF e e-mail unicos, senha com letras e numeros, saldo e
 * limite nao negativos. O saldo nao e editado aqui: ele muda por deposito,
 * saque (menu Carteiras) ou compra e venda (menu Transacoes).
 */
public class MenuUsuario extends MenuBase {

    private final AlertaDAO alertaDAO = new AlertaDAO();

    public MenuUsuario(Entrada entrada) {
        super(entrada);
    }

    @Override
    public String getTitulo() {
        return "Usuarios (pessoa fisica)";
    }

    @Override
    protected List<String> getOpcoesExtras() {
        return List.of("Buscar por CPF", "Buscar por e-mail", "Listar empresas de um usuario");
    }

    @Override
    protected void executarExtra(int indice) throws SQLException {
        switch (indice) {
            case 0 -> mostrar(usuarioDAO.buscarPorCpf(entrada.lerCpf(null)));
            case 1 -> mostrar(usuarioDAO.buscarPorEmail(entrada.lerEmail(null)));
            default -> listarEmpresas(escolherUsuario("ID do usuario"));
        }
    }

    @Override
    protected void incluir() throws SQLException {
        String nome = entrada.lerNomePessoa("Nome completo", null);
        String cpf = lerCpfDisponivel(null);
        String email = lerEmailDisponivel(null);
        String senha = entrada.lerSenha(null);
        boolean doisFatores = entrada.lerSimNao("Ativar autenticacao em dois fatores (2FA)?", Boolean.FALSE);
        BigDecimal saldo = entrada.lerReais("Saldo inicial da carteira em R$", true, Valores.ZERO_DINHEIRO);
        BigDecimal limite = entrada.lerReais("Limite diario de saque em R$", true, Valores.dinheiro(Valores.de(5000)));

        Usuario usuario = new Usuario(0, nome, email, senha, cpf, saldo, limite);
        usuario.setAutenticacaoDoisFatores(doisFatores);
        int id = usuarioDAO.inserir(usuario);
        Tela.sucesso("Usuario cadastrado com ID " + id + " e carteira PF #" + usuario.getCarteira().getIdCarteira() + ".");
    }

    @Override
    protected void alterar() throws SQLException {
        Usuario usuario = escolherUsuario("Usuario a alterar");
        detalhar(usuario);
        System.out.println();

        String nomeAnterior = usuario.getNome();
        usuario.setNome(entrada.lerNomePessoa("Nome completo", usuario.getNome()));
        usuario.setCpf(lerCpfDisponivel(usuario));
        usuario.setEmail(lerEmailDisponivel(usuario));
        usuario.setSenha(entrada.lerSenha(usuario.getSenha()));
        usuario.setAutenticacaoDoisFatores(entrada.lerSimNao("Autenticacao em dois fatores (2FA)?",
                usuario.isAutenticacaoDoisFatores()));

        CarteiraPF carteira = usuario.getCarteira();
        carteira.setLimiteDiarioSaque(entrada.lerReais("Limite diario de saque em R$", true,
                carteira.getLimiteDiarioSaque()));
        // a descricao gerada no cadastro acompanha a troca de nome; uma personalizada fica como esta
        if ((Usuario.PREFIXO_CARTEIRA + nomeAnterior).equals(carteira.getDescricao())) {
            carteira.setDescricao(Usuario.PREFIXO_CARTEIRA + usuario.getNome());
        }

        if (usuarioDAO.alterar(usuario) == 0) {
            Tela.erro("O usuario ja nao existe no banco.");
            return;
        }
        Tela.sucesso("Usuario #" + usuario.getIdUsuario() + " alterado.");
    }

    @Override
    protected void excluir() throws SQLException {
        Usuario usuario = escolherUsuario("Usuario a excluir");
        detalhar(usuario);

        int idUsuario = usuario.getIdUsuario();
        List<Empresa> empresas = empresaDAO.listarPorUsuario(idUsuario);
        List<Alerta> alertas = alertaDAO.listarPorUsuario(idUsuario);
        if (!empresas.isEmpty() || !alertas.isEmpty()) {
            Tela.erro("O usuario nao pode ser excluido: possui " + empresas.size() + " empresa(s) e "
                    + alertas.size() + " alerta(s). Exclua-os antes (menus Empresas e Alertas).");
            return;
        }
        BigDecimal saldo = usuario.getCarteira().getSaldoReais();
        if (saldo.signum() > 0) {
            Tela.aviso("A carteira PF tem saldo de " + Tela.reais(saldo) + ", que sera perdido com a exclusao.");
        }
        if (!confirmarExclusao("usuario " + usuario.getNome() + " e da sua carteira PF")) {
            return;
        }
        try {
            if (usuarioDAO.excluir(idUsuario) == 0) {
                Tela.erro("O usuario ja nao existe no banco.");
                return;
            }
            Tela.sucesso("Usuario #" + idUsuario + " e carteira PF excluidos.");
        } catch (SQLException e) {
            if (!ErrosBanco.registroEmUso(e)) {
                throw e;
            }
            Tela.erro("O usuario nao pode ser excluido: a carteira PF tem transacoes, posicoes ou relatorios. "
                    + "Estorne/exclua-os antes (menus Transacoes, Posicoes e Relatorios).");
        }
    }

    @Override
    protected void exibirTodos() throws SQLException {
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        if (usuarios.isEmpty()) {
            Tela.aviso("Nenhum usuario cadastrado.");
            return;
        }
        String formato = "%-5s %-24s %-15s %-26s %-4s %20s%n";
        System.out.printf(formato, "ID", "NOME", "CPF", "E-MAIL", "2FA", "SALDO CARTEIRA PF");
        Tela.separador(99);
        for (Usuario u : usuarios) {
            System.out.printf(formato, u.getIdUsuario(), Tela.cortar(u.getNome(), 24), u.getCpf(),
                    Tela.cortar(u.getEmail(), 26), Tela.simNao(u.isAutenticacaoDoisFatores()),
                    Tela.reais(u.getCarteira().getSaldoReais()));
        }
        Tela.info(usuarios.size() + " usuario(s).");
    }

    @Override
    protected void exibirUm() throws SQLException {
        Usuario usuario = escolherUsuario("ID do usuario");
        detalhar(usuario);
        listarEmpresas(usuario);
    }

    private void mostrar(Usuario usuario) {
        if (usuario == null) {
            Tela.aviso("Nenhum usuario encontrado.");
            return;
        }
        detalhar(usuario);
    }

    private void listarEmpresas(Usuario usuario) throws SQLException {
        List<Empresa> empresas = empresaDAO.listarPorUsuario(usuario.getIdUsuario());
        System.out.println("  Empresas de " + usuario.getNome() + ":");
        if (empresas.isEmpty()) {
            System.out.println("    (nenhuma)");
        }
        for (Empresa e : empresas) {
            System.out.println("    #" + e.getIdEmpresa() + " " + e.getNome() + " - CNPJ " + e.getCnpj());
        }
    }

    /** Pede o CPF ate ele ser valido e livre (uk_usuario_cpf); na alteracao, o do proprio usuario vale. */
    private String lerCpfDisponivel(Usuario atual) throws SQLException {
        while (true) {
            String cpf = entrada.lerCpf(atual == null ? null : atual.getCpf());
            Usuario dono = usuarioDAO.buscarPorCpf(cpf);
            if (dono == null || (atual != null && dono.getIdUsuario() == atual.getIdUsuario())) {
                return cpf;
            }
            Tela.erro("CPF ja cadastrado para o usuario #" + dono.getIdUsuario() + " (" + dono.getNome() + ").");
        }
    }

    /** Pede o e-mail ate ele ser valido e livre (uk_usuario_email); na alteracao, o do proprio usuario vale. */
    private String lerEmailDisponivel(Usuario atual) throws SQLException {
        while (true) {
            String email = entrada.lerEmail(atual == null ? null : atual.getEmail());
            Usuario dono = usuarioDAO.buscarPorEmail(email);
            if (dono == null || (atual != null && dono.getIdUsuario() == atual.getIdUsuario())) {
                return email;
            }
            Tela.erro("E-mail ja cadastrado para o usuario #" + dono.getIdUsuario() + " (" + dono.getNome() + ").");
        }
    }

    private static void detalhar(Usuario u) {
        CarteiraPF carteira = u.getCarteira();
        Tela.campo("ID", u.getIdUsuario());
        Tela.campo("Nome", u.getNome());
        Tela.campo("CPF", u.getCpf());
        Tela.campo("E-mail", u.getEmail());
        Tela.campo("Senha", "******");
        Tela.campo("2FA", Tela.simNao(u.isAutenticacaoDoisFatores()));
        Tela.campo("Carteira PF", "#" + carteira.getIdCarteira() + " " + carteira.getDescricao());
        Tela.campo("Saldo em reais", Tela.reais(carteira.getSaldoReais()));
        Tela.campo("Limite diario de saque", Tela.reais(carteira.getLimiteDiarioSaque()));
    }
}
