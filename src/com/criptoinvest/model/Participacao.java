package com.criptoinvest.model;

/**
 * Entidade associativa que resolve o relacionamento N:N entre Usuario e Empresa.
 * Um Usuario pode ser socio de varias Empresas, e uma Empresa pode ter varios Usuarios (socios).
 *
 * Cardinalidade: Usuario (1) ----- (N) Participacao (N) ----- (1) Empresa
 * Verbo do relacionamento: "Usuario PARTICIPA DE Empresa"
 * Obrigatoriedade: Participacao exige Usuario e Empresa (ambas FKs obrigatorias).
 *
 * Chave primaria composta: (idUsuario, idEmpresa) — garante unicidade da participacao.
 */
public class Participacao {

    // PK composta: idUsuario + idEmpresa
    private Usuario usuario;     // FK -> Usuario (idUsuario) - parte da PK composta - obrigatoria
    private Empresa empresa;     // FK -> Empresa (idEmpresa) - parte da PK composta - obrigatoria

    private double percentualParticipacao;
    private String cargo;
    private String dataEntrada;
    private boolean ativo;

    public Participacao(Usuario usuario, Empresa empresa,
                        double percentualParticipacao, String cargo, String dataEntrada) {
        this.usuario = usuario;
        this.empresa = empresa;
        this.percentualParticipacao = percentualParticipacao;
        this.cargo = cargo;
        this.dataEntrada = dataEntrada;
        this.ativo = true;
    }

    public Usuario getUsuario() { return usuario; }
    public Empresa getEmpresa() { return empresa; }
    public double getPercentualParticipacao() { return percentualParticipacao; }
    public String getCargo() { return cargo; }
    public String getDataEntrada() { return dataEntrada; }
    public boolean isAtivo() { return ativo; }

    public void setPercentualParticipacao(double percentualParticipacao) {
        if (percentualParticipacao < 0 || percentualParticipacao > 100) {
            System.out.println("Erro: percentual deve estar entre 0 e 100.");
            return;
        }
        this.percentualParticipacao = percentualParticipacao;
    }

    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String chavePrimariaComposta() {
        return usuario.getId() + "-" + empresa.getId();
    }

    public void exibirDados() {
        System.out.println("=== Participacao ===");
        System.out.println("PK Composta (idUsuario-idEmpresa): " + chavePrimariaComposta());
        System.out.println("Socio: " + usuario.getNome());
        System.out.println("Empresa: " + empresa.getNome());
        System.out.println("Cargo: " + cargo);
        System.out.println("Percentual: " + percentualParticipacao + "%");
        System.out.println("Desde: " + dataEntrada);
        System.out.println("Ativo: " + ativo);
    }
}
