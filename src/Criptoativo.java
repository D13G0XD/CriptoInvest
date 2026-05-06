public class Criptoativo {
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public double getPrecoAtual() {
        return precoAtual;
    }

    public void setPrecoAtual(double precoAtual) {
        this.precoAtual = precoAtual;
    }

    public double getVariacao24h() {
        return variacao24h;
    }

    public void setVariacao24h(double variacao24h) {
        this.variacao24h = variacao24h;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getIdCripto() {
        return idCripto;
    }

    public void setIdCripto(int idCripto) {
        this.idCripto = idCripto;
    }

    int idCripto;
    String nome;
    String sigla;
    double precoAtual;
    double variacao24h;
    String categoria;

    public Criptoativo(int id, String nome, String sigla, double precoAtual, String categoria) {
        this.idCripto = id;
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
