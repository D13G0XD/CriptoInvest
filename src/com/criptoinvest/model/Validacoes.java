package com.criptoinvest.model;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validacoes e normalizacoes dos dados informados pelo usuario.
 *
 * Os documentos sao gravados sempre no mesmo formato (CPF 000.000.000-00 e
 * CNPJ 00.000.000/0000-00), porque as chaves unicas uk_usuario_cpf e
 * uk_empresa_cnpj comparam o texto exatamente: sem a normalizacao, o mesmo
 * CPF digitado com e sem pontuacao entraria duas vezes no banco.
 *
 * Os tamanhos sao contados em BYTES UTF-8, como o Oracle conta o VARCHAR2
 * (semantica padrao): um nome com acentos ocupa mais bytes do que caracteres.
 */
public final class Validacoes {

    /** usuario.cpf VARCHAR2(14): 000.000.000-00. */
    public static final int TAMANHO_CPF = 11;

    /** empresa.cnpj VARCHAR2(18): 00.000.000/0000-00. */
    public static final int TAMANHO_CNPJ = 14;

    /** Formato pratico de e-mail: usuario@dominio.tld, sem espacos. */
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /** Nome de pessoa: letras (com acento), espacos, apostrofo, hifen e ponto. */
    private static final Pattern NOME_PESSOA = Pattern.compile("^[\\p{L} .'-]+$");

    /** Sigla de criptoativo: letras e numeros, sem espacos. */
    private static final Pattern SIGLA = Pattern.compile("^[A-Z0-9]+$");

    /**
     * CNPJ alfanumerico (Receita Federal, a partir de julho de 2026): 12
     * posicoes com letras maiusculas ou numeros e 2 digitos verificadores.
     * O CNPJ so numerico continua valido, porque e um caso particular.
     */
    private static final Pattern CNPJ = Pattern.compile("^[A-Z0-9]{12}[0-9]{2}$");

    /** Classe utilitaria: nao deve ser instanciada. */
    private Validacoes() {
    }

    // ------------------------------------------------------------------------
    // Texto
    // ------------------------------------------------------------------------

    /** Tamanho do texto em bytes UTF-8 (como o Oracle mede o VARCHAR2). null = 0. */
    public static int bytes(String texto) {
        return texto == null ? 0 : texto.getBytes(StandardCharsets.UTF_8).length;
    }

    /** true quando o texto cabe numa coluna VARCHAR2 com o tamanho informado. */
    public static boolean cabe(String texto, int maximoBytes) {
        return bytes(texto) <= maximoBytes;
    }

    /** Mantem apenas os digitos do texto (remove pontos, barras, tracos e espacos). */
    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9]", "");
    }

    /** true quando o nome de pessoa so tem letras, espacos e pontuacao de nomes, com ao menos 2 letras. */
    public static boolean nomePessoaValido(String nome) {
        return nome != null && NOME_PESSOA.matcher(nome).matches() && nome.replaceAll("[^\\p{L}]", "").length() >= 2;
    }

    /** true quando a sigla (ja normalizada por Criptoativo.normalizarSigla) so tem letras e numeros. */
    public static boolean siglaValida(String sigla) {
        return sigla != null && SIGLA.matcher(sigla).matches();
    }

    // ------------------------------------------------------------------------
    // E-mail
    // ------------------------------------------------------------------------

    /** E-mail sem espacos nas pontas e em minusculas (uk_usuario_email diferencia caixa). null continua null. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    // ------------------------------------------------------------------------
    // CPF
    // ------------------------------------------------------------------------

    /**
     * true quando o CPF (com ou sem pontuacao) tem 11 digitos, nao e uma
     * sequencia repetida (000.000.000-00, 111.111.111-11...) e os dois digitos
     * verificadores conferem.
     */
    public static boolean cpfValido(String cpf) {
        String digitos = somenteDigitos(cpf);
        if (digitos.length() != TAMANHO_CPF || repetido(digitos)) {
            return false;
        }
        int dv1 = digitoCpf(digitos, 9);
        int dv2 = digitoCpf(digitos, 10);
        return dv1 == valor(digitos.charAt(9)) && dv2 == valor(digitos.charAt(10));
    }

    /**
     * Formata o CPF como 000.000.000-00. Texto que nao tem 11 digitos volta
     * sem alteracao (a validacao fica com cpfValido).
     */
    public static String formatarCpf(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != TAMANHO_CPF) {
            return cpf == null ? null : cpf.trim();
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Digito verificador do CPF calculado sobre as primeiras 'quantidade' posicoes. */
    private static int digitoCpf(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += valor(digitos.charAt(i)) * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // ------------------------------------------------------------------------
    // CNPJ
    // ------------------------------------------------------------------------

    /** CNPJ sem pontuacao e em maiusculas (aceita o formato alfanumerico). */
    public static String normalizarCnpj(String cnpj) {
        return cnpj == null ? "" : cnpj.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * true quando o CNPJ (com ou sem pontuacao, numerico ou alfanumerico) tem
     * 14 posicoes, nao e uma sequencia repetida e os dois digitos verificadores
     * conferem. No calculo, cada caractere vale o seu codigo ASCII menos 48
     * (0-9 valem 0-9 e A-Z valem 17-42), regra da Receita Federal que mantem
     * o resultado do CNPJ numerico.
     */
    public static boolean cnpjValido(String cnpj) {
        String c = normalizarCnpj(cnpj);
        if (!CNPJ.matcher(c).matches() || repetido(c)) {
            return false;
        }
        int dv1 = digitoCnpj(c, 12);
        int dv2 = digitoCnpj(c, 13);
        return dv1 == valor(c.charAt(12)) && dv2 == valor(c.charAt(13));
    }

    /**
     * Formata o CNPJ como 00.000.000/0000-00. Texto que nao tem 14 posicoes
     * volta sem alteracao (a validacao fica com cnpjValido).
     */
    public static String formatarCnpj(String cnpj) {
        String c = normalizarCnpj(cnpj);
        if (c.length() != TAMANHO_CNPJ) {
            return cnpj == null ? null : cnpj.trim();
        }
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "/"
                + c.substring(8, 12) + "-" + c.substring(12);
    }

    /** Digito verificador do CNPJ sobre as primeiras 'quantidade' posicoes (pesos 2 a 9, da direita para a esquerda). */
    private static int digitoCnpj(String c, int quantidade) {
        int soma = 0;
        int peso = 2;
        for (int i = quantidade - 1; i >= 0; i--) {
            soma += valor(c.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // ------------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------------

    /** Valor do caractere no calculo dos digitos verificadores: codigo ASCII - 48. */
    private static int valor(char caractere) {
        return caractere - '0';
    }

    /** true quando todos os caracteres sao iguais (ex.: 00000000000). */
    private static boolean repetido(String texto) {
        return texto.chars().distinct().count() == 1;
    }
}
