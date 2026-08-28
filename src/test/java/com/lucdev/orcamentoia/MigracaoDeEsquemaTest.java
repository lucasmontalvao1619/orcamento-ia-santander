package com.lucdev.orcamentoia;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

// Este teste existe por causa de uma falha real: ao ganhar o campo semSalario,
// a aplicacao gerava "add column sem_salario boolean not null" e quebrava em
// toda instalacao que ja tivesse uma linha de configuracao — as linhas
// existentes ficariam com NULL numa coluna que nao aceita NULL.
//
// A suite normal nao pegava isso porque roda com banco novo a cada execucao. O
// bug so aparece em quem ATUALIZA, que e justamente quem ja usava o app.
class MigracaoDeEsquemaTest {

    // Reproduz a operacao que o Hibernate emite ao encontrar a tabela antiga.
    // Se a coluna for declarada sem default, o H2 recusa com
    // JdbcSQLIntegrityConstraintViolationException.
    @Test
    void adicionarSemSalarioNaoQuebraBancoQueJaTemDados() throws Exception {
        String url = "jdbc:h2:mem:migracao-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

        try (Connection conexao = DriverManager.getConnection(url, "sa", "");
             Statement sql = conexao.createStatement()) {

            // Esquema anterior ao recurso, com a linha de quem ja usava o app.
            sql.execute("""
                    create table configuracao (
                        id bigint primary key,
                        salario numeric(19,2),
                        transacao_salario_id bigint,
                        dia_recebimento integer
                    )
                    """);
            sql.execute("insert into configuracao values (1, 3000.00, 7, 15)");

            // A mesma DDL que o Hibernate gera com a coluna declarada com
            // default — sem o default, esta linha lanca excecao.
            sql.execute("alter table if exists configuracao "
                    + "add column sem_salario boolean default false not null");

            try (ResultSet r = sql.executeQuery("select sem_salario, salario from configuracao where id = 1")) {
                assertThat(r.next()).isTrue();
                // A linha antiga precisa sobreviver com o valor correto: quem
                // tinha salario continua tendo, e nao vira "sem salario".
                assertThat(r.getBoolean("sem_salario")).isFalse();
                assertThat(r.getBigDecimal("salario")).isEqualByComparingTo("3000.00");
            }
        }
    }
}
