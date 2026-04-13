public class Transacao {

    int id;
    String tipo;
    Criptoativo criptoativo;
    double quantidade;
    double precoUnitario;
    double taxa;
    String dataOperacao;

    public Transacao(int id, String tipo, Criptoativo criptoativo,
                     double quantidade, String dataOperacao) {
        this.id = id;
        this.criptoativo = criptoativo;
        this.quantidade = quantidade;
        this.precoUnitario = criptoativo.precoAtual;
        this.dataOperacao = dataOperacao;

        if (tipo.equals("COMPRA") || tipo.equals("VENDA") || tipo.equals("CONVERSAO")) {
            this.tipo = tipo;
        } else {
            System.out.println("Aviso: tipo invalido, definido como COMPRA.");
            this.tipo = "COMPRA";
        }

        this.taxa = calcularValorBruto() * 0.001;
    }

    public double calcularValorBruto() {
        return quantidade * precoUnitario;
    }

    public double calcularValorComTaxa() {
        if (tipo.equals("COMPRA")) {
            return calcularValorBruto() + taxa;
        }
        return calcularValorBruto() - taxa;
    }

    public double calcularValorAtual() {
        return quantidade * criptoativo.precoAtual;
    }

    public double calcularLucro() {
        if (tipo.equals("COMPRA")) {
            return calcularValorAtual() - calcularValorComTaxa();
        }
        return 0;
    }

    public void exibirDados() {
        System.out.println("=== Transacao ===");
        System.out.println("Tipo: " + tipo);
        System.out.println("Cripto: " + criptoativo.sigla);
        System.out.println("Quantidade: " + quantidade);
        System.out.println("Preco Unitario: R$ " + precoUnitario);
        System.out.println("Valor Bruto: R$ " + String.format("%.2f", calcularValorBruto()));
        System.out.println("Taxa (0.1%): R$ " + String.format("%.2f", taxa));
        System.out.println("Valor Liquido: R$ " + String.format("%.2f", calcularValorComTaxa()));
        System.out.println("Data: " + dataOperacao);
        if (tipo.equals("COMPRA")) {
            System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorAtual()));
            System.out.println("Lucro: R$ " + String.format("%.2f", calcularLucro()));
        }
    }
}
