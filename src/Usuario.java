public class Usuario {

    int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public boolean isAutenticacaoDoisFatores() {
        return autenticacaoDoisFatores;
    }

    public void setAutenticacaoDoisFatores(boolean autenticacaoDoisFatores) {
        this.autenticacaoDoisFatores = autenticacaoDoisFatores;
    }

    public double getSaldoReais() {
        return saldoReais;
    }

    public void setSaldoReais(double saldoReais) {
        this.saldoReais = saldoReais;
    }

    public Carteira[] getCarteiras() {
        return carteiras;
    }

    public void setCarteiras(Carteira[] carteiras) {
        this.carteiras = carteiras;
    }

    public Empresa[] getEmpresas() {
        return empresas;
    }

    public void setEmpresas(Empresa[] empresas) {
        this.empresas = empresas;
    }

    public int getTotalCarteiras() {
        return totalCarteiras;
    }

    public void setTotalCarteiras(int totalCarteiras) {
        this.totalCarteiras = totalCarteiras;
    }

    public int getTotalEmpresas() {
        return totalEmpresas;
    }

    public void setTotalEmpresas(int totalEmpresas) {
        this.totalEmpresas = totalEmpresas;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    String nome;
    String email;
    String senha;
    String cpf;
    boolean autenticacaoDoisFatores;
    double saldoReais;
    Carteira[] carteiras;
    Empresa[] empresas;
    int totalCarteiras;
    int totalEmpresas;

    public Usuario(int id, String nome, String email, String senha, String cpf) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.autenticacaoDoisFatores = false;
        this.saldoReais = 0;
        this.carteiras = new Carteira[10];
        this.empresas = new Empresa[10];
        this.totalCarteiras = 0;
        this.totalEmpresas = 0;
    }

    public void ativar2FA() {
        this.autenticacaoDoisFatores = true;
        System.out.println("2FA ativado para " + nome);
    }

    public void depositar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        this.saldoReais += valor;
        System.out.println("Deposito de R$ " + valor + " realizado. Saldo: R$ " + saldoReais);
    }

    public void sacar(double valor) {
        if (valor <= 0) {
            System.out.println("Erro: valor deve ser positivo.");
            return;
        }
        if (valor > saldoReais) {
            System.out.println("Erro: saldo insuficiente.");
            return;
        }
        this.saldoReais -= valor;
        System.out.println("Saque de R$ " + valor + " realizado. Saldo: R$ " + saldoReais);
    }

    public void adicionarCarteira(Carteira carteira) {
        if (totalCarteiras < 10) {
            carteiras[totalCarteiras] = carteira;
            totalCarteiras++;
        }
    }

    public void adicionarEmpresa(Empresa empresa) {
        if (totalEmpresas < 10) {
            empresas[totalEmpresas] = empresa;
            totalEmpresas++;
        }
    }

    public void exibirDados() {
        System.out.println("=== Usuario ===");
        System.out.println("Nome: " + nome);
        System.out.println("Email: " + email);
        System.out.println("CPF: " + cpf);
        System.out.println("2FA: " + autenticacaoDoisFatores);
        System.out.println("Saldo em Reais: R$ " + saldoReais);
        System.out.println("Carteiras: " + totalCarteiras);
        System.out.println("Empresas: " + totalEmpresas);
    }
}
