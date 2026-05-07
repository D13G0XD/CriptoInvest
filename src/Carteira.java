public class Carteira {

    private int idCarteira;
    private String descricao;
    private Transacao[] transacoes;
    private int totalTransacoes;

    public Carteira(int id, String descricao) {
        this.idCarteira = id;
        this.descricao = descricao;
        this.transacoes = new Transacao[100];
        this.totalTransacoes = 0;
    }

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

    public int getTotalTransacoes() {
        return totalTransacoes;
    }

    public void setTotalTransacoes(int totalTransacoes) {
        this.totalTransacoes = totalTransacoes;
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
            if (transacoes[i].getCriptoativo().getSigla().equals(sigla)) {

                if (transacoes[i].getTipo().equals("COMPRA")) {
                    saldo += transacoes[i].getQuantidade();

                } else if (transacoes[i].getTipo().equals("VENDA")) {
                    saldo -= transacoes[i].getQuantidade();
                }
            }
        }

        return saldo;
    }

    public double calcularValorTotal() {
        double total = 0;

        for (int i = 0; i < totalTransacoes; i++) {

            if (transacoes[i].getTipo().equals("COMPRA")) {

                String sigla = transacoes[i].getCriptoativo().getSigla();

                double saldoCripto = calcularSaldoCripto(sigla);

                if (saldoCripto > 0) {
                    total += saldoCripto * transacoes[i].getCriptoativo().getPrecoAtual();
                }
            }
        }

        return total;
    }

    public double calcularTotalInvestido() {
        double investido = 0;

        for (int i = 0; i < totalTransacoes; i++) {

            if (transacoes[i].getTipo().equals("COMPRA")) {
                investido += transacoes[i].calcularValorComTaxa();
            }
        }

        return investido;
    }

    public double calcularTotalTaxas() {
        double taxas = 0;

        for (int i = 0; i < totalTransacoes; i++) {
            taxas += transacoes[i].getTaxa();
        }

        return taxas;
    }

    public double calcularRentabilidade() {
        double investido = calcularTotalInvestido();

        if (investido == 0) {
            return 0;
        }

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