public class Empresa {

    int id;
    String razaoSocial;
    String cnpj;
    Carteira carteira;

    public Empresa(int id, String razaoSocial, String cnpj) {
        this.id = id;
        this.razaoSocial = razaoSocial;
        this.cnpj = cnpj;
        this.carteira = new Carteira(id, "Carteira - " + razaoSocial);
    }

    public void exibirDados() {
        System.out.println("=== Empresa ===");
        System.out.println("Razao Social: " + razaoSocial);
        System.out.println("CNPJ: " + cnpj);
        System.out.println("--- Carteira da Empresa ---");
        carteira.exibirResumo();
    }
}
