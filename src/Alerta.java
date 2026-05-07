public class Alerta {

    private int idAlerta;
    private Criptoativo criptoativo;
    private double limiteVariacao;
    private boolean ativado;

    public Alerta(int id, Criptoativo criptoativo, double limiteVariacao) {
        this.idAlerta = id;
        this.criptoativo = criptoativo;
        this.limiteVariacao = limiteVariacao;
        this.ativado = true;
    }

    public int getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(int idAlerta) {
        this.idAlerta = idAlerta;
    }

    public boolean isAtivado() {
        return ativado;
    }

    public void setAtivado(boolean ativado) {
        this.ativado = ativado;
    }

    public double getLimiteVariacao() {
        return limiteVariacao;
    }

    public void setLimiteVariacao(double limiteVariacao) {
        this.limiteVariacao = limiteVariacao;
    }

    public Criptoativo getCriptoativo() {
        return criptoativo;
    }

    public void setCriptoativo(Criptoativo criptoativo) {
        this.criptoativo = criptoativo;
    }

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

        return "ALERTA: " + criptoativo.getSigla() + " " + direcao + " " +
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
        System.out.println("Cripto: " + criptoativo.getSigla());
        System.out.println("Limite: " + limiteVariacao + "%");
        System.out.println("Ativado: " + ativado);
        System.out.println("Status: " + mensagemAlerta());
    }
}