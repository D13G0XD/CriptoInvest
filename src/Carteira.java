public class Carteira {
    public int getIdCarteira() {
        return idCarteira;
    }

    public void setIdCarteira(int idCarteira) {
        this.idCarteira = idCarteira;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Transacao[] getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(Transacao[] transacoes) {
        this.transacoes = transacoes;
    }

    int idCarteira;

    public int getTotalTransacoes() {
        return totalTransacoes;
    }

    public void setTotalTransacoes(int totalTransacoes) {
        this.totalTransacoes = totalTransacoes;
    }

    String descricao;
    Transacao[] transacoes;
    int totalTransacoes;

    public Carteira(int id, String descricao) {
        this.idCarteira = id;
        this.descricao = descricao;
        this.transacoes = new Transacao[100];
        this.totalTransacoes = 0;
    }

    public void registrarTransacao(Transacao transacao) {
        if (totalTransacoes < 100) {
            transacoes[totalTransacoes] = transacao;
            totalTransacoes++;
        } else {
            System.out.println("Erro: limite de transacoes atingido.");
        }
    }

    public double calcularSaldoCripto(String sigla) {
        double saldo = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].criptoativo.sigla.equals(sigla)) {
                if (transacoes[i].tipo.equals("COMPRA")) {
                    saldo += transacoes[i].quantidade;
                } else if (transacoes[i].tipo.equals("VENDA")) {
                    saldo -= transacoes[i].quantidade;
                }
            }
        }
        return saldo;
    }

    public double calcularValorTotal() {
        double total = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].tipo.equals("COMPRA")) {
                String sigla = transacoes[i].criptoativo.sigla;
                double saldoCripto = calcularSaldoCripto(sigla);
                if (saldoCripto > 0) {
                    total += saldoCripto * transacoes[i].criptoativo.precoAtual;
                }
            }
        }
        return total;
    }

    public double calcularTotalInvestido() {
        double investido = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            if (transacoes[i].tipo.equals("COMPRA")) {
                investido += transacoes[i].calcularValorComTaxa();
            }
        }
        return investido;
    }

    public double calcularTotalTaxas() {
        double taxas = 0;
        for (int i = 0; i < totalTransacoes; i++) {
            taxas += transacoes[i].taxa;
        }
        return taxas;
    }

    public double calcularRentabilidade() {
        double investido = calcularTotalInvestido();
        if (investido == 0) return 0;
        double atual = calcularValorTotal();
        return ((atual - investido) / investido) * 100;
    }

    public void exibirResumo() {
        System.out.println("=== Carteira: " + descricao + " ===");
        System.out.println("Total de Transacoes: " + totalTransacoes);
        System.out.println("Total Investido: R$ " + String.format("%.2f", calcularTotalInvestido()));
        System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorTotal()));
        System.out.println("Taxas Pagas: R$ " + String.format("%.2f", calcularTotalTaxas()));
        System.out.println("Rentabilidade: " + String.format("%.2f", calcularRentabilidade()) + "%");
    }
}
