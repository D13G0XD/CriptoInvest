package com.criptoinvest.model;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Usuario e Criptoativo.
 * Um Usuario pode configurar varios Alertas sobre criptos diferentes, e um Criptoativo
 * pode ser monitorado por varios Usuarios (cada um com seu proprio limite).
 *
 * Cardinalidade: Usuario (1) ----- (N) Alerta (N) ----- (1) Criptoativo
 * Verbo do relacionamento: "Usuario MONITORA Criptoativo (com limite de variacao)"
 * Obrigatoriedade: Alerta exige Usuario e Criptoativo (ambas FKs obrigatorias).
 */
public class Alerta {

    private int idAlerta;             // PK
    private Usuario usuario;          // FK -> Usuario (idUsuario) - obrigatoria
    private Criptoativo criptoativo;  // FK -> Criptoativo (idCripto) - obrigatoria

    private double limiteVariacao;
    private boolean ativado;
    private String dataConfiguracao;

    public Alerta(int id, Usuario usuario, Criptoativo criptoativo,
                  double limiteVariacao, String dataConfiguracao) {
        if (usuario == null || criptoativo == null) {
            throw new IllegalArgumentException("Alerta exige usuario e criptoativo.");
        }
        this.idAlerta = id;
        this.usuario = usuario;
        this.criptoativo = criptoativo;
        if (limiteVariacao <= 0) {
            System.out.println("Aviso: limiteVariacao deve ser positivo, ajustado para 5%.");
            this.limiteVariacao = 5.0;
        } else {
            this.limiteVariacao = limiteVariacao;
        }
        this.dataConfiguracao = dataConfiguracao;
        this.ativado = true;
    }

    public int getIdAlerta() { return idAlerta; }
    public void setIdAlerta(int idAlerta) { this.idAlerta = idAlerta; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Criptoativo getCriptoativo() { return criptoativo; }
    public void setCriptoativo(Criptoativo criptoativo) { this.criptoativo = criptoativo; }

    public boolean isAtivado() { return ativado; }
    public void setAtivado(boolean ativado) { this.ativado = ativado; }

    public double getLimiteVariacao() { return limiteVariacao; }
    public void setLimiteVariacao(double limiteVariacao) {
        if (limiteVariacao <= 0) {
            System.out.println("Erro: limiteVariacao deve ser positivo.");
            return;
        }
        this.limiteVariacao = limiteVariacao;
    }

    public String getDataConfiguracao() { return dataConfiguracao; }
    public void setDataConfiguracao(String dataConfiguracao) { this.dataConfiguracao = dataConfiguracao; }

    public boolean verificarDisparo() {
        if (!ativado) return false;

        double variacao = criptoativo.getVariacao24h();

        if (variacao >= limiteVariacao || variacao <= -limiteVariacao) {
            return true;
        }

        return false;
    }

    public String mensagemAlerta() {
        if (!verificarDisparo()) {
            return "Nenhum alerta no momento.";
        }

        String direcao = criptoativo.getVariacao24h() > 0 ? "subiu" : "caiu";

        return "ALERTA para " + usuario.getNome() + ": " + criptoativo.getSigla() + " " + direcao + " " +
                String.format("%.2f", Math.abs(criptoativo.getVariacao24h())) +
                "% (limite: " + limiteVariacao + "%)";
    }

    public void desativar() {
        this.ativado = false;
    }

    public void ativar() {
        this.ativado = true;
    }

    public void exibirDados() {
        System.out.println("=== Alerta ===");
        System.out.println("Usuario: " + usuario.getNome());
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Limite: " + limiteVariacao + "%");
        System.out.println("Ativado: " + ativado);
        System.out.println("Configurado em: " + dataConfiguracao);
        System.out.println("Status: " + mensagemAlerta());
    }
}