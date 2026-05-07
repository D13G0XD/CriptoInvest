package com.criptoinvest.model;

public class Main {

    public static void main(String[] args) {

        // --- Criptoativos ---
        Criptoativo btc;
        Criptoativo eth;

        try {
            btc = new Criptoativo(1, "Bitcoin", "BTC", 300000.00, "Moeda");
            eth = new Criptoativo(2, "Ethereum", "ETH", 15000.00, "Plataforma");

            // atualizarPreco com variacao automatica e manual
            btc.atualizarPreco(350000.00);
            eth.atualizarPreco(16000.00, 6.67);

            btc.exibirDados();
            eth.exibirDados();

        } catch (Exception e) {
            System.out.println("Erro ao criar criptoativos: " + e.getMessage());
            return;
        }

        // --- Usuario ---
        Usuario usuario;

        try {
            usuario = new Usuario(1, "Lucas", "lucas@email.com", "senha123", "123.456.789-00");

            // depositar com e sem descricao
            usuario.depositar(10000.00);
            usuario.depositar(5000.00, "Aporte mensal");

            usuario.ativar2FA();
            usuario.exibirDados();

        } catch (Exception e) {
            System.out.println("Erro ao criar usuario: " + e.getMessage());
            return;
        }

        // --- Empresa ---
        Empresa empresa;

        try {
            empresa = new Empresa(2, "ABCD Investimentos", "00.000.000/0001-00");
            usuario.adicionarEmpresa(empresa);
            empresa.exibirDados();

        } catch (Exception e) {
            System.out.println("Erro ao criar empresa: " + e.getMessage());
            return;
        }

        // --- Polimorfismo dinamico ---
        try {
            Titular[] titulares = { usuario, empresa };

            for (Titular titular : titulares) {
                titular.exibirDados();
            }

        } catch (Exception e) {
            System.out.println("Erro ao exibir titulares: " + e.getMessage());
        }

        // --- Transacoes na carteira do usuario ---
        try {
            Transacao t1 = new Transacao(1, "COMPRA", btc, 0.5, "2026-05-07");
            Transacao t2 = new Transacao(2, "COMPRA", eth, 2.0, "2026-05-07");
            Transacao t3 = new Transacao(3, "VENDA", btc, 0.1, "2026-05-07");

            // Polimorfismo estatico: registrarTransacao com e sem observacao
            usuario.getCarteira().registrarTransacao(t1);
            usuario.getCarteira().registrarTransacao(t2, "Aporte em ETH");
            usuario.getCarteira().registrarTransacao(t3);

            t1.exibirDados();
            t2.exibirDados();
            t3.exibirDados();

        } catch (Exception e) {
            System.out.println("Erro ao registrar transacoes: " + e.getMessage());
        }

        // --- Relatorio ---
        try {
            Relatorio relatorio = new Relatorio(1, usuario.getCarteira(), "2026-05-07");
            relatorio.exibirRelatorio();

        } catch (Exception e) {
            System.out.println("Erro ao gerar relatorio: " + e.getMessage());
        }

        // --- Alerta ---
        try {
            Alerta alerta = new Alerta(1, btc, 5.0);
            alerta.exibirDados();

        } catch (Exception e) {
            System.out.println("Erro ao criar alerta: " + e.getMessage());
        }
    }
}