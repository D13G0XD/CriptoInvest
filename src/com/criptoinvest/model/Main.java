package com.criptoinvest.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        // --- Usuario (carteira PF criada automaticamente) ---
        Usuario usuario;
        try {
            usuario = new Usuario(1, "Lucas", "lucas@email.com", "senha123", "123.456.789-00");
            usuario.getCarteira().depositar(10000.00);
            usuario.getCarteira().depositar(5000.00, "Aporte mensal");
            usuario.ativar2FA();
            usuario.exibirDados();
        } catch (Exception e) {
            System.out.println("Erro ao criar usuario: " + e.getMessage());
            return;
        }

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

        // --- CRIACAO: gravar HashMap de Criptoativos em arquivo ---
        System.out.println("\n--- Gravando criptoativos.txt (HashMap) ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("criptoativos.txt"))) {
            for (Map.Entry<String, Criptoativo> entrada : mapCriptoativos.entrySet()) {
                Criptoativo c = entrada.getValue();
                bw.write(c.getIdCripto() + "|" + c.getNome() + "|" + c.getSigla() + "|"
                        + c.getPrecoAtual() + "|" + c.getVariacao24h() + "|" + c.getCategoria());
                bw.newLine();
            }
            System.out.println("Arquivo criptoativos.txt criado com " + mapCriptoativos.size() + " registro(s).");
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

        // --- ATUALIZACAO: regravar criptoativos.txt com precos atualizados ---
        System.out.println("\n--- Regravando criptoativos.txt com precos atualizados ---");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("criptoativos.txt"))) {
            for (Map.Entry<String, Criptoativo> entrada : mapCriptoativos.entrySet()) {
                Criptoativo c = entrada.getValue();
                bw.write(c.getIdCripto() + "|" + c.getNome() + "|" + c.getSigla() + "|"
                        + c.getPrecoAtual() + "|" + c.getVariacao24h() + "|" + c.getCategoria());
                bw.newLine();
            }
            System.out.println("Arquivo criptoativos.txt atualizado com " + mapCriptoativos.size() + " registro(s).");
        } catch (IOException e) {
            System.out.println("Erro ao regravar criptoativos.txt: " + e.getMessage());
        }
    }
}
