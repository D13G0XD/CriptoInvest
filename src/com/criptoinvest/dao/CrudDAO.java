package com.criptoinvest.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato das operacoes basicas (CRUD) que todo DAO do CriptoInvest oferece.
 *
 * Convencoes comuns a todas as implementacoes:
 *   - id 0 no objeto = ainda nao persistido; a PK vem sempre da sequence da tabela;
 *   - inserir recusa objeto com id > 0 (IllegalStateException): o caminho dele e alterar;
 *   - cada metodo abre a propria conexao e nao engole SQLException.
 *
 * @param <T> entidade do pacote model mapeada pelo DAO
 */
public interface CrudDAO<T> {

    /**
     * Grava a entidade. O id e obtido da sequence e so e atribuido ao objeto
     * depois que o INSERT foi executado com sucesso.
     *
     * @return o id gerado pela sequence
     */
    int inserir(T entidade) throws SQLException;

    /**
     * Atualiza a entidade identificada pelo seu id.
     *
     * @return quantidade de linhas alteradas (0 = id inexistente)
     */
    int alterar(T entidade) throws SQLException;

    /**
     * Exclui a entidade pelo id.
     *
     * @return quantidade de linhas excluidas da tabela principal (0 = id inexistente)
     */
    int excluir(int id) throws SQLException;

    /** Busca pela chave primaria. Retorna null quando nao existe. */
    T buscarPorId(int id) throws SQLException;

    /** Lista todas as linhas da tabela em um ArrayList, ordenadas pela chave primaria. */
    List<T> listarTodos() throws SQLException;
}
