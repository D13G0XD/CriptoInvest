package com.criptoinvest.model;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Carteira e Criptoativo.
 * Uma Carteira pode possuir varios Criptoativos, e um Criptoativo pode estar em varias Carteiras.
 * Posicao representa o saldo agregado (snapshot) de um criptoativo dentro de uma carteira.
 *
 * Cardinalidade: Carteira (1) ----- (N) Posicao (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Carteira POSSUI Criptoativo" (com quantidade e preco medio)
 * Obrigatoriedade: Posicao exige Carteira e Criptoativo (ambas FKs obrigatorias).
 */
public class Posicao {

    private int idPosicao;            // PK
    private Carteira carteira;        // FK -> Carteira (idCarteira) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private double quantidadeAtual;
    private double precoMedioCompra;
    private String dataPrimeiraAquisicao;
    private String dataUltimaAtualizacao;

    public Posicao(int idPosicao, Carteira carteira, Criptoativo criptoativo,
                   double quantidadeInicial, double precoInicial, String dataAquisicao) {
        this.idPosicao = idPosicao;
        this.carteira = carteira;
        this.criptoativo = criptoativo;
        this.quantidadeAtual = quantidadeInicial;
        this.precoMedioCompra = precoInicial;
        this.dataPrimeiraAquisicao = dataAquisicao;
        this.dataUltimaAtualizacao = dataAquisicao;
    }

    public int getIdPosicao() { return idPosicao; }
    public Carteira getCarteira() { return carteira; }
    public Criptoativo getCriptoativo() { return criptoativo; }
    public double getQuantidadeAtual() { return quantidadeAtual; }
    public double getPrecoMedioCompra() { return precoMedioCompra; }
    public String getDataPrimeiraAquisicao() { return dataPrimeiraAquisicao; }
    public String getDataUltimaAtualizacao() { return dataUltimaAtualizacao; }

    public void setQuantidadeAtual(double quantidadeAtual) {
        // ck_posicao_qtde CHECK (quantidade_atual >= 0)
        if (quantidadeAtual < 0) {
            System.out.println("Erro: quantidade_atual nao pode ser negativa.");
            return;
        }
        this.quantidadeAtual = quantidadeAtual;
    }
    public void setPrecoMedioCompra(double precoMedioCompra) {
        // ck_posicao_preco CHECK (preco_medio_compra >= 0)
        if (precoMedioCompra < 0) {
            System.out.println("Erro: preco_medio_compra nao pode ser negativo.");
            return;
        }
        this.precoMedioCompra = precoMedioCompra;
    }
    public void setDataUltimaAtualizacao(String data) { this.dataUltimaAtualizacao = data; }

    public void aplicarCompra(double quantidade, double precoUnitario, String data) {
        double valorAtual = quantidadeAtual * precoMedioCompra;
        double valorCompra = quantidade * precoUnitario;
        this.quantidadeAtual += quantidade;
        this.precoMedioCompra = (valorAtual + valorCompra) / quantidadeAtual;
        this.dataUltimaAtualizacao = data;
    }

    public void aplicarVenda(double quantidade, String data) {
        if (quantidade > quantidadeAtual) {
            System.out.println("Erro: quantidade superior ao saldo da posicao.");
            return;
        }
        this.quantidadeAtual -= quantidade;
        this.dataUltimaAtualizacao = data;
    }

    public double calcularValorAtual() {
        return quantidadeAtual * criptoativo.getPrecoAtual();
    }

    public double calcularLucroNaoRealizado() {
        return calcularValorAtual() - (quantidadeAtual * precoMedioCompra);
    }

    public void exibirDados() {
        System.out.println("=== Posicao ===");
        System.out.println("Carteira: " + carteira.getDescricao());
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Quantidade: " + quantidadeAtual);
        System.out.println("Preco Medio: R$ " + String.format("%.2f", precoMedioCompra));
        System.out.println("Valor Atual: R$ " + String.format("%.2f", calcularValorAtual()));
        System.out.println("Lucro nao realizado: R$ " + String.format("%.2f", calcularLucroNaoRealizado()));
        System.out.println("Primeira Aquisicao: " + dataPrimeiraAquisicao);
    }
}
