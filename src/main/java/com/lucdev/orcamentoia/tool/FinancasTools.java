package com.lucdev.orcamentoia.tool;

import com.lucdev.orcamentoia.model.Configuracao;
import com.lucdev.orcamentoia.model.TipoTransacao;
import com.lucdev.orcamentoia.model.Transacao;
import com.lucdev.orcamentoia.service.ConfiguracaoService;
import com.lucdev.orcamentoia.service.TransacaoService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
public class FinancasTools {

    // O texto destas ferramentas volta para o usuario final, entao o locale e
    // fixo: sem isso o valor sairia formatado conforme a maquina que roda a app.
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private final TransacaoService transacaoService;
    private final ConfiguracaoService configuracaoService;

    public FinancasTools(TransacaoService transacaoService, ConfiguracaoService configuracaoService) {
        this.transacaoService = transacaoService;
        this.configuracaoService = configuracaoService;
    }

    // Sem esta ferramenta, "meu salario e 3000" caia em registrarTransacao e
    // virava mais uma receita, somando em cima do salario que ja existia.
    // Aqui o salario e substituido, nao acrescentado.
    @Tool(description = "Define ou altera o salario mensal do usuario. Use quando ele disser quanto "
            + "ganha, quanto recebe por mes, ou pedir para estabelecer, configurar ou corrigir o "
            + "salario. Substitui o salario anterior em vez de somar.")
    public String definirSalario(
            @ToolParam(description = "Valor do salario mensal em reais, sempre positivo") BigDecimal salario,
            @ToolParam(required = false, description = "Dia do mes em que o salario cai, de 1 a 31. "
                    + "Deixe vazio se o usuario nao disser") Integer diaDoRecebimento) {

        Configuracao configuracao = configuracaoService.definirSalario(salario, diaDoRecebimento);
        String quando = configuracao.getDiaRecebimento() == null
                ? ""
                : String.format(BR, ", recebido todo dia %d", configuracao.getDiaRecebimento());
        return String.format(BR, "Salario definido em R$ %.2f%s.", configuracao.getSalario(), quando);
    }

    @Tool(description = "Declara que o usuario NAO tem salario fixo. Use quando ele disser que e "
            + "autonomo, freelancer, que vive de renda variavel, que esta desempregado ou que nao "
            + "tem salario. O orcamento continua zerado e cada entrada e registrada conforme ele "
            + "recebe. Apaga a receita de salario anterior, se existir.")
    public String declararQueNaoTenhoSalario() {
        configuracaoService.declararQueNaoTemSalario();
        return "Certo, sem salario fixo. Registre cada entrada de dinheiro conforme receber, "
                + "com comandos como 'recebi 500 de freela'.";
    }

    @Tool(description = "Consulta o salario mensal configurado pelo usuario.")
    public String consultarSalario() {
        Configuracao configuracao = configuracaoService.obter();
        // Quem declarou nao ter salario fixo nao esta "sem configurar": dizer
        // isso mandaria o usuario configurar algo que ele decidiu nao ter.
        if (configuracao.isSemSalario()) {
            return "Voce indicou que nao tem salario fixo. As entradas de dinheiro sao "
                    + "registradas uma a uma, conforme voce recebe.";
        }
        if (!configuracao.isConfigurado()) {
            return "Nenhum salario foi configurado ainda.";
        }
        String quando = configuracao.getDiaRecebimento() == null
                ? ""
                : String.format(BR, ", recebido todo dia %d", configuracao.getDiaRecebimento());
        return String.format(BR, "O salario configurado e de R$ %.2f%s.", configuracao.getSalario(), quando);
    }

    @Tool(description = "Registra uma nova transacao financeira (receita ou despesa) no orcamento do usuario.")
    public String registrarTransacao(
            @ToolParam(description = "Descricao curta da transacao, ex: 'almoco no restaurante'") String descricao,
            @ToolParam(description = "Valor da transacao em reais, sempre positivo") BigDecimal valor,
            @ToolParam(description = "Categoria da transacao, ex: 'alimentacao', 'transporte', 'salario'") String categoria,
            @ToolParam(description = "Tipo da transacao: RECEITA para entradas de dinheiro, DESPESA para saidas") TipoTransacao tipo) {

        Transacao transacao = transacaoService.registrar(descricao, valor, categoria, tipo);
        return String.format(BR, "Transacao registrada com sucesso. ID %d, %s de R$ %.2f na categoria %s.",
                transacao.getId(), transacao.getTipo(), transacao.getValor(), transacao.getCategoria());
    }

    @Tool(description = "Consulta o saldo atual do orcamento, calculado como a soma das receitas menos a soma das despesas.")
    public String consultarSaldo() {
        BigDecimal saldo = transacaoService.calcularSaldo();
        return String.format(BR, "O saldo atual do orcamento e de R$ %.2f.", saldo);
    }

    @Tool(description = "Consulta o total gasto em uma categoria especifica de despesas.")
    public String consultarGastoPorCategoria(
            @ToolParam(description = "Nome da categoria a consultar, ex: 'alimentacao'") String categoria) {

        BigDecimal total = transacaoService.listarPorCategoria(categoria).stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format(BR, "O total de despesas na categoria %s e de R$ %.2f.", categoria, total);
    }

    @Tool(description = "Corrige uma transacao ja registrada. Use quando o usuario disser que errou, "
            + "quiser mudar o valor, a descricao, a categoria ou o tipo de um lancamento, ou disser algo "
            + "como 'na verdade foram 60'. Informe apenas os campos que mudam; os demais ficam como estao. "
            + "Se nao souber o id, chame listarTransacoes antes.")
    public String atualizarTransacao(
            @ToolParam(description = "Id da transacao a corrigir") Long id,
            @ToolParam(required = false, description = "Novo valor em reais, se mudou") BigDecimal valor,
            @ToolParam(required = false, description = "Nova descricao, se mudou") String descricao,
            @ToolParam(required = false, description = "Nova categoria, se mudou") String categoria,
            @ToolParam(required = false, description = "Novo tipo: RECEITA ou DESPESA, se mudou") TipoTransacao tipo) {

        Transacao transacao = transacaoService.atualizar(id, descricao, valor, categoria, tipo);
        return String.format(BR, "Transacao %d atualizada: %s de R$ %.2f na categoria %s (%s).",
                transacao.getId(), transacao.getTipo(), transacao.getValor(),
                transacao.getCategoria(), transacao.getDescricao());
    }

    @Tool(description = "Apaga uma transacao do orcamento pelo id. Use quando o usuario pedir para "
            + "apagar, remover, excluir ou desfazer um lancamento. Se voce nao souber o id, chame "
            + "listarTransacoes antes: a listagem mostra o id de cada uma entre colchetes.")
    public String apagarTransacao(
            @ToolParam(description = "Id da transacao a apagar, como aparece entre colchetes na listagem") Long id) {

        Transacao transacao = transacaoService.apagar(id);
        return String.format(BR, "Transacao apagada: %s de R$ %.2f na categoria %s.",
                transacao.getTipo(), transacao.getValor(), transacao.getCategoria());
    }

    @Tool(description = "Lista as ultimas transacoes registradas no orcamento, com o id de cada uma.")
    public String listarTransacoes() {
        List<Transacao> transacoes = transacaoService.listarTodas();
        if (transacoes.isEmpty()) {
            return "Nenhuma transacao registrada ate o momento.";
        }
        StringBuilder resultado = new StringBuilder("Transacoes registradas:\n");
        for (Transacao t : transacoes) {
            resultado.append(String.format(BR, "- [%d] %s | %s | R$ %.2f | %s%n",
                    t.getId(), t.getTipo(), t.getDescricao(), t.getValor(), t.getCategoria()));
        }
        return resultado.toString();
    }
}
