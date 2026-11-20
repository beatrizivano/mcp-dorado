package br.edu.ifpr.mcp_dorado.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpr.mcp_dorado.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_dorado.service.AccessionService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** ferramenta mcp para consultar um unico accession da amostra. */
public final class ConsultarAccessionTool implements McpToolDefinition {

    private final AccessionService service;
    private final JsonMapper mapper;

    // recebe o servico que consulta accessions.
    public ConsultarAccessionTool(AccessionService service) {
        this.service = service;
        this.mapper = new JsonMapper();
    }

    @Override
    // registra a ferramenta e seu parametro de entrada.
    public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
        Map<String, Object> accessionProperty = new LinkedHashMap<>();
        accessionProperty.put("type", "string");
        accessionProperty.put("description", "Identificador do registro GenBank, com versao. Ex.: CM184936.1");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("accession", accessionProperty);

        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", List.of("accession"));
        inputSchema.put("additionalProperties", false);

        Tool tool = Tool.builder("consultar_accession", inputSchema)
                .title("Consultar accession")
                .description("Tipo: consulta de um registro GenBank pelo identificador accession. "
                        + "Entrada: accession (texto). Exemplo: CM184936.1. Retorna organismo, "
                        + "revisao taxonomica, genes, rios de ocorrencia, localidade, "
                        + "coordenadas, qualificadores e tamanho da sequencia.")
                .build();

        SyncToolSpecification specification = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::run)
                .build();

        mcp.tools(specification);
    }

    @Override
    // valida o accession e solicita os metadados.
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        if (request == null || request.arguments() == null) {
            return error("Informe um accession valido.");
        }

        Object argument = request.arguments().get("accession");

        if (!(argument instanceof String)) {
            return error("O parametro accession deve ser uma string nao vazia.");
        }

        String accession = ((String) argument).strip();

        if (accession.isEmpty()) {
            return error("O parametro accession nao pode estar vazio.");
        }

        Optional<Map<String, Object>> result = service.consultar(accession);

        if (result.isEmpty()) {
            return error("Accession nao encontrado na amostra: " + accession);
        }

        try {
            String json = mapper.writeValueAsString(result.get());
            return CallToolResult.builder()
                    .addTextContent(json)
                    .build();
        } catch (Exception exception) {
            return error("Nao foi possivel preparar a resposta do accession.");
        }
    }

    // retorna um erro ao cliente
    private CallToolResult error(String message) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(message)
                .build();
    }
}
