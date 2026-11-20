package br.edu.ifpr.mcp_dorado.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_dorado.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_dorado.service.RegistrosService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** ferramenta mcp para contagem, listagem e agrupamento de registros genbank. */
public final class ConsultarRegistrosTool implements McpToolDefinition {

    private final RegistrosService service;
    private final JsonMapper mapper;

    // recebe o servico de consulta de registros.
    public ConsultarRegistrosTool(RegistrosService service) {
        this.service = service;
        this.mapper = new JsonMapper();
    }

    @Override
    // anuncia as operacoes e os argumentos da ferramenta.
    public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
        Map<String, Object> schema = criarSchema();

        Tool tool = Tool.builder("consultar_registros", schema)
                .title("Consultar registros")
                .description("Tipo: consulta aos registros GenBank da amostra. "
                        + "Operacoes: contar (total de registros); listar (ate 50 accessions); "
                        + "agrupar (contagem por especie, gene ou local). "
                        + "Filtros opcionais: especie, gene e local. Exemplo: agrupar por gene, "
                        + "filtrando local por Brazil. Local e localidade de coleta, nao rio de ocorrencia.")
                .build();

        SyncToolSpecification specification = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::run)
                .build();

        mcp.tools(specification);
    }

    Map<String, Object> criarSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("oneOf", List.of(
                schemaOperacao("contar", false),
                schemaOperacao("listar", false),
                schemaOperacao("agrupar", true)
        ));
        return schema;
    }

    // monta os campos permitidos para a operacao.
    private Map<String, Object> schemaOperacao(String operacao, boolean agrupamento) {
        Map<String, Object> properties = new LinkedHashMap<>();
        Map<String, Object> seletor = propriedade(
                "Tipo de consulta. Exemplo: " + operacao, null);
        seletor.put("const", operacao);
        properties.put("operacao", seletor);
        properties.put("especie", propriedade(
                "Filtro opcional por especie. Exemplo: Cambeva castroi", null));
        properties.put("gene", propriedade(
                "Filtro opcional por gene anotado no GenBank. Exemplo: COI", null));
        properties.put("local", propriedade(
                "Filtro opcional por geo_loc_name. Exemplo: Brazil", null));

        List<String> required = List.of("operacao");

        if (agrupamento) {
            properties.put("agrupar_por", propriedade(
                    "Tipo de agrupamento. Exemplo: gene (contagem por gene)",
                    List.of("especie", "gene", "local")));
            required = List.of("operacao", "agrupar_por");
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    // valida os argumentos e consulta os registros.
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        if (request == null || request.arguments() == null) {
            return error("Informe a operacao: contar, listar ou agrupar.");
        }

        Map<String, Object> arguments = request.arguments();
        String[] nomes = {"operacao", "especie", "gene", "local", "agrupar_por"};

        for (String nome : nomes) {
            Object valor = arguments.get(nome);

            if (valor != null && !(valor instanceof String)) {
                return error("O parametro " + nome + " deve ser texto.");
            }
        }

        String operacao = texto(arguments, "operacao");
        String especie = texto(arguments, "especie");
        String gene = texto(arguments, "gene");
        String local = texto(arguments, "local");
        String agruparPor = texto(arguments, "agrupar_por");

        if (!List.of("contar", "listar", "agrupar").contains(operacao)) {
            return error("Operacao invalida. Use contar, listar ou agrupar.");
        }

        if ("agrupar".equals(operacao)
                && !List.of("especie", "gene", "local").contains(agruparPor)) {
            return error("Para agrupar, informe agrupar_por: especie, gene ou local.");
        }

        if (!"agrupar".equals(operacao) && !agruparPor.isEmpty()) {
            return error("O parametro agrupar_por so pode ser usado com a operacao agrupar.");
        }

        try {
            Map<String, Object> result = service.consultar(operacao, especie, gene, local, agruparPor);
            return CallToolResult.builder()
                    .addTextContent(mapper.writeValueAsString(result))
                    .build();
        } catch (Exception exception) {
            return error("Nao foi possivel consultar os registros.");
        }
    }

    // descreve um parametro do esquema json.
    private Map<String, Object> propriedade(String descricao, List<String> opcoes) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "string");
        result.put("description", descricao);

        if (opcoes != null) {
            result.put("enum", opcoes);
        }

        return result;
    }

    // extrai um argumento textual quando informado.
    private String texto(Map<String, Object> arguments, String nome) {
        Object valor = arguments.get(nome);

        if (valor == null) {
            return "";
        }

        return ((String) valor).strip();
    }

    // monta uma resposta mcp de erro.
    private CallToolResult error(String mensagem) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(mensagem)
                .build();
    }
}
