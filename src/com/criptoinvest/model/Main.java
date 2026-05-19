package com.criptoinvest.model;

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

        // --- Usuario (carteira PF criada automaticamente) ---
        Usuario usuario;
        try {
            usuario = new Usuario(1, "Lucas", "lucas@email.com", "senha123", "123.456.789-00");
            // depositos vao para a CARTEIRA (nao mais no usuario)
            usuario.getCarteira().depositar(10000.00);
            usuario.getCarteira().depositar(5000.00, "Aporte mensal");
            usuario.ativar2FA();
            usuario.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar usuario: " + e.getMessage());
            return;
        }

        // --- Empresa (carteira PJ criada automaticamente) ---
        Empresa empresa;
        try {
            empresa = new Empresa(2, "ABCD Investimentos", "00.000.000/0001-00", "LUCRO_PRESUMIDO");
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

        // --- Transacoes na carteira PF do usuario ---
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

        // --- Saque com restricao de limite na CarteiraPF ---
        try {
            System.out.println("\n--- Teste de limite de saque PF ---");
            usuario.getCarteira().sacar(3000.00);    // ok
            usuario.getCarteira().sacar(10000.00);   // estoura limite diario
        } catch (Exception e) {
            System.out.println("Erro no saque: " + e.getMessage());
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

        // --- Posicao (Carteira x Criptoativo) ---
        try {
            Posicao posBtc = new Posicao(1, usuario.getCarteira(), btc, 0.5, 300000.00, "2026-05-07");
            posBtc.aplicarVenda(0.1, "2026-05-07");
            posBtc.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar posicao: " + e.getMessage());
        }
    }
}
