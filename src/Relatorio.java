public class Relatorio {
    int id;
    Carteira carteira;
    String dataGeracao;
    double valorTotalCarteira;
    double totalInvestido;
    double totalTaxas;
    double rentabilidadePercentual;

    public Relatorio(int id, Carteira carteira, String dataGeracao) {
        this.id = id;
        this.carteira = carteira;
        this.dataGeracao = dataGeracao;
        this.valorTotalCarteira = carteira.calcularValorTotal();
        this.totalInvestido = carteira.calcularTotalInvestido();
        this.totalTaxas = carteira.calcularTotalTaxas();
        this.rentabilidadePercentual = carteira.calcularRentabilidade();
    }

    public double calcularLucroLiquido() {
        return valorTotalCarteira - totalInvestido;
    }

    public String gerarResumo() {
        return "=== Relatorio de Desempenho ===\n" +
               "Data: " + dataGeracao + "\n" +
               "Carteira: " + carteira.descricao + "\n" +
               "Total Investido: R$ " + String.format("%.2f", totalInvestido) + "\n" +
               "Valor Atual: R$ " + String.format("%.2f", valorTotalCarteira) + "\n" +
               "Taxas Pagas: R$ " + String.format("%.2f", totalTaxas) + "\n" +
               "Lucro Liquido: R$ " + String.format("%.2f", calcularLucroLiquido()) + "\n" +
               "Rentabilidade: " + String.format("%.2f", rentabilidadePercentual) + "%\n";
    }

    public void exibirRelatorio() {
        System.out.println(gerarResumo());
    }
}
