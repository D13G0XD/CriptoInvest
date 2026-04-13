public class Alerta {
    int idAlerta;
    Criptoativo criptoativo;
    double limiteVariacao;
    boolean ativado;

    public Alerta(int id, Criptoativo criptoativo, double limiteVariacao) {
        this.idAlerta = id;
        this.criptoativo = criptoativo;
        this.limiteVariacao = limiteVariacao;
        this.ativado = true;
    }

    public boolean verificarDisparo() {
        if (!ativado) return false;
        double variacao = criptoativo.variacao24h;
        if (variacao >= limiteVariacao || variacao <= -limiteVariacao) {
            return true;
        }
        return false;
    }

    public String mensagemAlerta() {
        if (!verificarDisparo()) return "Nenhum alerta no momento.";
        String direcao = criptoativo.variacao24h > 0 ? "subiu" : "caiu";
        return "ALERTA: " + criptoativo.sigla + " " + direcao + " " +
               String.format("%.2f", Math.abs(criptoativo.variacao24h)) +
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
        System.out.println("Cripto: " + criptoativo.sigla);
        System.out.println("Limite: " + limiteVariacao + "%");
        System.out.println("Ativado: " + ativado);
        System.out.println("Status: " + mensagemAlerta());
    }
}
