public class Criptoativo {
    int id;
    String nome;
    String sigla;
    double precoAtual;
    double variacao24h;
    String categoria;

    public Criptoativo(int id, String nome, String sigla, double precoAtual, String categoria) {
        this.id = id;
        this.nome = nome;
        this.sigla = sigla;
        this.precoAtual = precoAtual;
        this.variacao24h = 0;
        this.categoria = categoria;
    }

    public void atualizarPreco(double novoPreco) {
        if (novoPreco < 0) {
            System.out.println("Erro: preco nao pode ser negativo.");
            return;
        }
        this.variacao24h = ((novoPreco - this.precoAtual) / this.precoAtual) * 100;
        this.precoAtual = novoPreco;
    }

    public void exibirDados() {
        System.out.println("=== Criptoativo ===");
        System.out.println("Nome: " + nome + " (" + sigla + ")");
        System.out.println("Categoria: " + categoria);
        System.out.println("Preco Atual: R$ " + precoAtual);
        System.out.println("Variacao 24h: " + String.format("%.2f", variacao24h) + "%");
    }
}
