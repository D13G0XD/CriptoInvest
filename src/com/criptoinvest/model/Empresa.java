package com.criptoinvest.model;

/**
 * Pessoa Juridica que opera no sistema.
 * 1:1 obrigatorio com CarteiraPJ; N:1 obrigatorio com Usuario (empresa pertence a um dono).
 */
public class Empresa {

    private int idEmpresa;           // PK - 0 enquanto nao persistida (seq_empresa)
    private String nome;             // razao social
    private String cnpj;
    private Usuario dono;            // FK obrigatoria
    private CarteiraPJ carteira;     // FK 1:1 obrigatoria -> carteira_pj

    public Empresa(int id, String razaoSocial, String cnpj, Usuario dono) {
        this(id, razaoSocial, cnpj, "SIMPLES", dono);
    }

    public Empresa(int id, String razaoSocial, String cnpj, String regimeTributario, Usuario dono) {
        if (dono == null) {
            throw new IllegalArgumentException("Empresa exige um Usuario dono.");
        }
        this.idEmpresa = id;
        this.nome = razaoSocial;
        this.cnpj = cnpj;
        this.dono = dono;
        this.carteira = new CarteiraPJ("Carteira PJ - " + razaoSocial, Valores.ZERO_DINHEIRO, regimeTributario);
    }

    /**
     * Reconstrucao de uma empresa ja gravada, com o dono e a carteira PJ lidos do
     * banco. Uso da camada de persistencia: apenas atribui os valores, sem regra
     * de negocio nem avisos.
     *
     * Observacao: o construtor (int, String, String, String, Usuario) tambem tem
     * cinco parametros, entao a chamada com null nas duas ultimas posicoes e
     * ambigua para o compilador - e invalida de qualquer forma, porque dono e
     * carteira sao obrigatorios.
     *
     * @throws IllegalArgumentException quando o dono ou a carteira sao nulos
     */
    public Empresa(int idEmpresa, String nome, String cnpj, Usuario dono, CarteiraPJ carteira) {
        if (dono == null) {
            throw new IllegalArgumentException("Empresa exige um Usuario dono.");
        }
        if (carteira == null) {
            throw new IllegalArgumentException("Empresa exige uma CarteiraPJ.");
        }
        this.idEmpresa = idEmpresa;
        this.nome = nome;
        this.cnpj = cnpj;
        this.dono = dono;
        this.carteira = carteira;
    }

    public int getIdEmpresa() { return idEmpresa; }

    /** A PK so e conhecida depois do INSERT (seq_empresa); 0 = nao persistida. */
    public void setIdEmpresa(int idEmpresa) { this.idEmpresa = idEmpresa; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public Usuario getDono() { return dono; }
    public void setDono(Usuario dono) {
        if (dono == null) {
            System.out.println("Erro: dono nao pode ser nulo.");
            return;
        }
        this.dono = dono;
    }
    public CarteiraPJ getCarteira() { return carteira; }

    public void exibirDados() {
        System.out.println("=== Empresa (PJ) ===");
        System.out.println("Razao Social: " + nome);
        System.out.println("CNPJ: " + cnpj);
        System.out.println("Regime Tributario: " + carteira.getRegimeTributario());
        System.out.println("Dono: " + dono.getNome());
        System.out.println("--- Carteira da Empresa ---");
        carteira.exibirResumo();
    }
}
