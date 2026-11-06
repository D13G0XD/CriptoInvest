package com.criptoinvest.view;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;

/**
 * Traduz as SQLException do Oracle em mensagens que o usuario entende.
 *
 * Os menus validam os dados ANTES de gravar (tamanhos, faixas, chaves unicas,
 * dependencias), entao estes erros so aparecem quando o banco recusa algo que
 * escapou da validacao - por exemplo, um registro alterado por outra sessao
 * entre a conferencia e a gravacao. A mensagem traz a causa provavel, o codigo
 * ORA e o texto do driver, para facilitar o diagnostico.
 */
public final class ErrosBanco {

    /** ORA-00001: chave unica (UK) ou primaria (PK) repetida. */
    public static final int CHAVE_UNICA = 1;

    /** ORA-02292: o registro e referenciado por uma FK de outra tabela. */
    public static final int REGISTRO_EM_USO = 2292;

    /** Classe utilitaria: nao deve ser instanciada. */
    private ErrosBanco() {
    }

    /** true quando o banco recusou um DELETE porque outra tabela ainda aponta para o registro (ORA-02292). */
    public static boolean registroEmUso(SQLException e) {
        return e.getErrorCode() == REGISTRO_EM_USO;
    }

    /** Mensagem amigavel: causa provavel + codigo ORA + mensagem original do driver. */
    public static String explicar(SQLException e) {
        return causa(e) + " [ORA-" + String.format("%05d", e.getErrorCode()) + "] " + primeiraLinha(e.getMessage());
    }

    private static String causa(SQLException e) {
        switch (e.getErrorCode()) {
            case CHAVE_UNICA:
                return "Ja existe um registro com esse valor (CPF, e-mail, CNPJ, sigla ou par repetido).";
            case REGISTRO_EM_USO:
                return "O registro ainda e usado por outros cadastros: exclua-os antes.";
            case 2291:
                return "O registro relacionado nao existe mais (pode ter sido excluido).";
            case 2290:
                return "Valor fora do permitido pela regra da coluna (CHECK).";
            case 1400:
                return "Campo obrigatorio sem valor.";
            case 1438:
                return "Numero maior que o permitido pela coluna.";
            case 12899:
                return "Texto maior que o permitido pela coluna.";
            case 942:
            case 2289:
                return "Tabela ou sequence inexistente: execute o script sql/criptoinvest_ddl.sql.";
            case 1017:
                return "Usuario ou senha do banco invalidos: confira o arquivo .env.";
            case 28000:
                return "Conta do banco bloqueada: procure o suporte da FIAP.";
            default:
                if (e instanceof SQLRecoverableException) {
                    return "Falha de comunicacao com o banco: confira a rede e tente novamente.";
                }
                return "O banco recusou a operacao.";
        }
    }

    private static String primeiraLinha(String texto) {
        if (texto == null) {
            return "";
        }
        int quebra = texto.indexOf('\n');
        return (quebra < 0 ? texto : texto.substring(0, quebra)).trim();
    }
}
