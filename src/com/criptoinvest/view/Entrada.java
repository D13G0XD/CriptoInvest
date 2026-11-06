package com.criptoinvest.view;

import com.criptoinvest.model.Criptoativo;
import com.criptoinvest.model.Validacoes;

import java.io.Console;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Leitura do teclado COM VALIDACAO: cada metodo so devolve um valor que o
 * banco aceita, repetindo a pergunta (com o motivo) ate o usuario acertar.
 *
 * Os limites vem do DDL: tamanho dos VARCHAR2 em bytes, precisao e escala dos
 * NUMBER (dinheiro NUMBER(15,2), cripto NUMBER(18,8), percentual NUMBER(10,4))
 * e as regras de CHECK. Assim os erros ORA-12899, ORA-01438 e ORA-02290 nao
 * chegam a acontecer.
 *
 * Convencoes de todos os campos:
 *   - CANCELAR (em qualquer caixa) desiste da operacao (OperacaoCancelada);
 *   - quando o rotulo mostra um valor entre colchetes, ENTER mantem esse valor
 *     (o atual, na alteracao, ou o sugerido, no cadastro);
 *   - nos campos opcionais da alteracao, "-" apaga o valor.
 */
public class Entrada {

    public static final String CANCELAR = "CANCELAR";
    private static final String APAGAR = "-";

    /** Datas no padrao brasileiro, conferidas de verdade (31/02 e recusado). */
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Nenhuma operacao com criptoativos e anterior ao bloco genese do Bitcoin. */
    private static final LocalDate DATA_MINIMA = LocalDate.of(2009, 1, 3);

    /**
     * Numero com virgula decimal, sem separador de milhar nem notacao cientifica.
     * O ponto e recusado: no padrao brasileiro (o mesmo dos valores exibidos)
     * ele separa milhares, entao "350.000" lido como decimal viraria 350.
     */
    private static final Pattern NUMERO = Pattern.compile("^-?(\\d+(,\\d*)?|,\\d+)$");

    /** Senha: 6 ou mais caracteres, sem espacos, com pelo menos uma letra e um numero. */
    private static final Pattern SENHA = Pattern.compile("^(?=.*\\p{L})(?=.*\\d)\\S{6,}$");

    private final Scanner scanner;

    public Entrada(InputStream entrada) {
        this.scanner = new Scanner(entrada, charsetDoConsole());
    }

    /**
     * O console do Windows pode usar uma codificacao diferente da padrao do
     * Java: lendo com a do console, nomes acentuados chegam corretos e o
     * tamanho em bytes conferido aqui e o mesmo que o Oracle vai contar.
     */
    private static Charset charsetDoConsole() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }

    // ------------------------------------------------------------------------
    // Leitura basica
    // ------------------------------------------------------------------------

    /**
     * Le uma linha sem os espacos das pontas. No fim da entrada (Ctrl+Z / Ctrl+D)
     * o Scanner lanca NoSuchElementException, que a Main trata encerrando o programa.
     */
    private String ler(String rotulo) {
        System.out.print(rotulo);
        return scanner.nextLine().trim();
    }

    /** Le um campo de uma operacao: CANCELAR desiste da operacao inteira. */
    public String lerCampo(String rotulo) {
        String linha = ler(rotulo);
        if (linha.equalsIgnoreCase(CANCELAR)) {
            throw new OperacaoCancelada();
        }
        return linha;
    }

    /** Opcao de menu entre minimo e maximo. */
    public int lerOpcao(int minimo, int maximo) {
        while (true) {
            String linha = ler("Opcao: ");
            Integer opcao = inteiro(linha);
            if (opcao != null && opcao >= minimo && opcao <= maximo) {
                return opcao;
            }
            Tela.erro("Opcao invalida: digite um numero de " + minimo + " a " + maximo + ".");
        }
    }

    /**
     * Mostra as opcoes numeradas a partir de 1 e devolve o INDICE escolhido
     * (0 = primeira). Com atual >= 0, ENTER mantem essa opcao.
     */
    public int lerEscolha(String rotulo, List<String> opcoes, int atual) {
        System.out.println(rotulo + ":");
        for (int i = 0; i < opcoes.size(); i++) {
            System.out.println("  " + (i + 1) + " - " + opcoes.get(i));
        }
        while (true) {
            String linha = lerCampo("Escolha" + (atual >= 0 ? " [" + (atual + 1) + "]" : "") + ": ");
            if (linha.isEmpty() && atual >= 0) {
                return atual;
            }
            Integer escolha = inteiro(linha);
            if (escolha != null && escolha >= 1 && escolha <= opcoes.size()) {
                return escolha - 1;
            }
            Tela.erro("Escolha um numero de 1 a " + opcoes.size() + ".");
        }
    }

    /** S/N. Com atual != null, ENTER mantem o valor atual. */
    public boolean lerSimNao(String rotulo, Boolean atual) {
        while (true) {
            String linha = lerCampo(rotulo + " (S/N)" + (atual == null ? "" : " [" + (atual ? "S" : "N") + "]") + ": ");
            if (linha.isEmpty() && atual != null) {
                return atual;
            }
            if (linha.equalsIgnoreCase("S") || linha.equalsIgnoreCase("SIM")) {
                return true;
            }
            if (linha.equalsIgnoreCase("N") || linha.equalsIgnoreCase("NAO")) {
                return false;
            }
            Tela.erro("Responda S (sim) ou N (nao).");
        }
    }

    /** Pergunta de confirmacao, sem resposta padrao: o usuario precisa digitar S ou N. */
    public boolean confirmar(String pergunta) {
        return lerSimNao(pergunta, null);
    }

    public void pausar() {
        ler("Pressione ENTER para continuar...");
    }

    /** Converte o texto em inteiro positivo (id, opcao); null quando nao e um. */
    public static Integer inteiro(String texto) {
        if (texto == null || !texto.matches("\\d{1,9}")) {
            return null;
        }
        return Integer.valueOf(texto);
    }

    // ------------------------------------------------------------------------
    // Texto
    // ------------------------------------------------------------------------

    /** Texto obrigatorio de ate maximoBytes bytes (espacos repetidos viram um). atual != null: ENTER mantem. */
    public String lerTexto(String rotulo, int maximoBytes, String atual) {
        return lerValidado(rotulo, atual, true, maximoBytes, Entrada::espacosSimples,
                texto -> texto.chars().anyMatch(Character::isLetterOrDigit), "Informe ao menos uma letra ou numero.");
    }

    /** Texto opcional no cadastro: ENTER deixa em branco (gravado como NULL). */
    public String lerTextoOpcional(String rotulo, int maximoBytes) {
        while (true) {
            String linha = espacosSimples(lerCampo(rotulo + " (opcional): "));
            if (linha.isEmpty()) {
                return null;
            }
            if (cabe(linha, maximoBytes)) {
                return linha;
            }
        }
    }

    /** Texto opcional na alteracao: ENTER mantem o atual e "-" apaga. */
    public String lerTextoOpcional(String rotulo, int maximoBytes, String atual) {
        while (true) {
            String linha = espacosSimples(lerCampo(rotulo + " [" + (atual == null ? "vazio" : atual) + "]"
                    + " (- apaga): "));
            if (linha.isEmpty()) {
                return atual;
            }
            if (linha.equals(APAGAR)) {
                return null;
            }
            if (cabe(linha, maximoBytes)) {
                return linha;
            }
        }
    }

    /** Nome de pessoa: letras, espacos e pontuacao de nomes, ate 150 bytes (usuario.nome). */
    public String lerNomePessoa(String rotulo, String atual) {
        return lerValidado(rotulo, atual, true, 150, Entrada::espacosSimples, Validacoes::nomePessoaValido,
                "Use apenas letras e espacos, com pelo menos 2 letras.");
    }

    /** E-mail em minusculas, ate 150 bytes (usuario.email). */
    public String lerEmail(String atual) {
        return lerValidado("E-mail", atual, true, 150, Validacoes::normalizarEmail, Validacoes::emailValido,
                "E-mail invalido. Exemplo: nome@dominio.com");
    }

    /** Senha de 6 a 100 caracteres com letras e numeros; o valor atual nunca e exibido. */
    public String lerSenha(String atual) {
        return lerValidado("Senha (min. 6 caracteres, com letras e numeros)", atual, false, 100,
                UnaryOperator.identity(), texto -> SENHA.matcher(texto).matches(),
                "A senha precisa de 6 ou mais caracteres, sem espacos, com letras e numeros.");
    }

    /** CPF com digitos verificadores validos, devolvido no formato 000.000.000-00 (usuario.cpf). */
    public String lerCpf(String atual) {
        return lerValidado("CPF (com ou sem pontuacao)", atual, true, 14, Validacoes::formatarCpf,
                Validacoes::cpfValido, "CPF invalido: confira os 11 digitos.");
    }

    /** CNPJ (numerico ou alfanumerico) valido, no formato 00.000.000/0000-00 (empresa.cnpj). */
    public String lerCnpj(String atual) {
        return lerValidado("CNPJ (com ou sem pontuacao)", atual, true, 18, Validacoes::formatarCnpj,
                Validacoes::cnpjValido, "CNPJ invalido: confira os 14 caracteres e os digitos verificadores.");
    }

    /** Sigla em maiusculas, so letras e numeros, ate 10 caracteres (criptoativo.sigla). */
    public String lerSigla(String atual) {
        return lerValidado("Sigla (ex.: BTC)", atual, true, 10, Criptoativo::normalizarSigla,
                Validacoes::siglaValida, "A sigla deve ter so letras e numeros, sem espacos.");
    }

    /**
     * Base dos campos de texto obrigatorios: le, normaliza, confere o tamanho em
     * bytes e a regra; repete ate valer. atual != null: ENTER mantem o atual.
     *
     * @param mostrarAtual false para nao exibir o valor atual (senha)
     */
    private String lerValidado(String rotulo, String atual, boolean mostrarAtual, int maximoBytes,
                               UnaryOperator<String> normalizar, Predicate<String> regra, String mensagemErro) {
        String sufixo = atual == null ? "" : " [" + (mostrarAtual ? atual : "******") + "]";
        while (true) {
            String linha = lerCampo(rotulo + sufixo + ": ");
            if (linha.isEmpty()) {
                if (atual != null) {
                    return atual;
                }
                Tela.erro("Campo obrigatorio.");
                continue;
            }
            String valor = normalizar.apply(linha);
            if (!cabe(valor, maximoBytes)) {
                continue;
            }
            if (!regra.test(valor)) {
                Tela.erro(mensagemErro);
                continue;
            }
            return valor;
        }
    }

    /** Confere o tamanho em bytes (como o Oracle conta) e explica quando nao cabe. */
    private static boolean cabe(String texto, int maximoBytes) {
        if (Validacoes.cabe(texto, maximoBytes)) {
            return true;
        }
        Tela.erro("Texto muito longo: maximo de " + maximoBytes + " bytes (letras acentuadas ocupam 2), "
                + "informado " + Validacoes.bytes(texto) + ".");
        return false;
    }

    private static String espacosSimples(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    // ------------------------------------------------------------------------
    // Numeros
    // ------------------------------------------------------------------------

    /** Dinheiro em reais, NUMBER(15,2): ate 13 digitos inteiros e 2 casas, nunca negativo. */
    public BigDecimal lerReais(String rotulo, boolean aceitaZero, BigDecimal atual) {
        return lerNumero(rotulo, atual, false, Tela::reais, 2, 13, BigDecimal.ZERO, aceitaZero);
    }

    /** Quantidade ou preco de criptoativo, NUMBER(18,8): ate 10 digitos inteiros e 8 casas, nunca negativo. */
    public BigDecimal lerCripto(String rotulo, boolean aceitaZero, BigDecimal atual) {
        return lerNumero(rotulo, atual, false, Tela::cripto, 8, 10, BigDecimal.ZERO, aceitaZero);
    }

    /** Percentual positivo, NUMBER(10,4) com CHECK > 0 (alerta.limite_variacao). */
    public BigDecimal lerPercentualPositivo(String rotulo, BigDecimal atual) {
        return lerNumero(rotulo, atual, false, Tela::percentual, 4, 6, BigDecimal.ZERO, false);
    }

    /** Variacao percentual, NUMBER(10,4), positiva ou negativa. opcional: ENTER devolve null. */
    public BigDecimal lerVariacao(String rotulo, BigDecimal atual, boolean opcional) {
        return lerNumero(rotulo, atual, opcional, Tela::variacao, 4, 6, null, true);
    }

    /**
     * Base dos campos numericos. Aceita virgula decimal (sem separador de
     * milhar), confere casas decimais e digitos inteiros contra a coluna e o
     * minimo (zero) quando informado.
     *
     * @param opcional quando atual e null: ENTER devolve null em vez de exigir o valor
     * @param minimo   menor valor aceito (null = sem limite inferior)
     */
    private BigDecimal lerNumero(String rotulo, BigDecimal atual, boolean opcional, Function<BigDecimal, String> formatar,
                                 int casas, int digitosInteiros, BigDecimal minimo, boolean aceitaMinimo) {
        BigDecimal teto = BigDecimal.TEN.pow(digitosInteiros);
        String sufixo = atual != null ? " [" + formatar.apply(atual) + "]" : opcional ? " (opcional)" : "";
        while (true) {
            String linha = lerCampo(rotulo + sufixo + ": ");
            if (linha.isEmpty()) {
                if (atual != null || opcional) {
                    return atual;
                }
                Tela.erro("Campo obrigatorio.");
                continue;
            }
            if (!NUMERO.matcher(linha).matches()) {
                Tela.erro(linha.contains(".")
                        ? "Use virgula para os decimais (ex.: 0,5) e nao use ponto de milhar (ex.: 1500,75)."
                        : "Numero invalido: use so digitos e virgula para os decimais (ex.: 1500,75).");
                continue;
            }
            BigDecimal valor = new BigDecimal(linha.replace(',', '.'));
            if (valor.stripTrailingZeros().scale() > casas) {
                Tela.erro("Use no maximo " + casas + " casas decimais.");
                continue;
            }
            if (valor.abs().compareTo(teto) >= 0) {
                Tela.erro("Valor acima do maximo permitido ("
                        + formatar.apply(teto.subtract(BigDecimal.ONE.movePointLeft(casas))) + ").");
                continue;
            }
            if (minimo != null) {
                int comparacao = valor.compareTo(minimo);
                if (comparacao < 0 || (comparacao == 0 && !aceitaMinimo)) {
                    Tela.erro(aceitaMinimo ? "O valor nao pode ser negativo." : "O valor deve ser maior que zero.");
                    continue;
                }
            }
            return valor.setScale(casas, RoundingMode.UNNECESSARY);
        }
    }

    // ------------------------------------------------------------------------
    // Datas
    // ------------------------------------------------------------------------

    /**
     * Data dd/mm/aaaa, entre 03/01/2009 (bloco genese do Bitcoin) e hoje: as
     * datas do sistema registram fatos ja ocorridos. atual != null: ENTER mantem.
     */
    public LocalDate lerData(String rotulo, LocalDate atual) {
        String sufixo = atual == null ? "" : " [" + Tela.data(atual) + "]";
        while (true) {
            String linha = lerCampo(rotulo + " (dd/mm/aaaa)" + sufixo + ": ");
            if (linha.isEmpty()) {
                if (atual != null) {
                    return atual;
                }
                Tela.erro("Campo obrigatorio.");
                continue;
            }
            LocalDate data;
            try {
                data = LocalDate.parse(linha, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                Tela.erro("Data invalida: use dd/mm/aaaa com um dia que exista (ex.: 05/11/2026).");
                continue;
            }
            if (data.isAfter(LocalDate.now())) {
                Tela.erro("A data nao pode ser futura.");
                continue;
            }
            if (data.isBefore(DATA_MINIMA)) {
                Tela.erro("A data nao pode ser anterior a " + Tela.data(DATA_MINIMA) + " (inicio do Bitcoin).");
                continue;
            }
            return data;
        }
    }
}
