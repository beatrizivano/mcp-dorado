package br.edu.ifpr.mcp_dorado.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import br.edu.ifpr.mcp_dorado.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_dorado.service.ResumoAmostraService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** disponibiliza um resumo da amostra genbank por meio do mcp. */
public final class ResumirAmostraTool implements McpToolDefinition {

    private final ResumoAmostraService service;
    private final JsonMapper mapper;

    // recebe o servico de estatisticas da amostra.
    public ResumirAmostraTool(ResumoAmostraService service) {
        this.service = service;
        this.mapper = new JsonMapper();
    }

    @Override
    // registra a ferramenta sem parametros obrigatorios.
    public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        schema.put("additionalProperties", false);

        Tool tool = Tool.builder("resumir_amostra", schema)
                .title("Resumir amostra")
                .description("Tipo: resumo estatistico da amostra GenBank. Entrada: nenhum parametro. "
                        + "Retorna total de accessions, "
                        + "especies, genes, ocorrencia catalogada por rio, disponibilidade de "
                        + "localidades e coordenadas e cobertura dos seis qualificadores. "
                        + "Nao recebe parametros.")
                .build();

        SyncToolSpecification specification = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::run)
                .build();

        mcp.tools(specification);
    }

    @Override
    // devolve as estatisticas resumidas pelo servico.
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        if (request != null && request.arguments() != null && !request.arguments().isEmpty()) {
            return error("A ferramenta resumir_amostra nao recebe parametros.");
        }

        try {
            String json = mapper.writeValueAsString(service.resumir());
            return CallToolResult.builder()
                    .addTextContent(json)
                    .build();
        } catch (Exception exception) {
            return error("Nao foi possivel gerar o resumo da amostra.");
        }
    }

    // retorna uma resposta mcp de erro.
    private CallToolResult error(String mensagem) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(mensagem)
                .build();
    }
}
