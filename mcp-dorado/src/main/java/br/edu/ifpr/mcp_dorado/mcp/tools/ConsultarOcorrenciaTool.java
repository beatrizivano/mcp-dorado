package br.edu.ifpr.mcp_dorado.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_dorado.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_dorado.service.OcorrenciaService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** consulta ocorrencia de especies nas bacias, sem inferir localidade de coleta. */
public final class ConsultarOcorrenciaTool implements McpToolDefinition {

    private final OcorrenciaService service;
    private final JsonMapper mapper;

    // recebe o servico responsavel pelas ocorrencias.
    public ConsultarOcorrenciaTool(OcorrenciaService service) {
        this.service = service;
        this.mapper = new JsonMapper();
    }

    @Override
    // anuncia a ferramenta e seu esquema para o servidor mcp.
    public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
        Map<String, Object> schema = criarSchema();

        Tool tool = Tool.builder("consultar_ocorrencia", schema)
                .title("Consultar ocorrencia")
                .description("Tipo: consulta de ocorrencia catalogada por especie ou rio. "
                        + "Operacoes: especie (exemplo: Cambeva castroi); rio (exemplo: tibagi); "
                        + "intersecao (exemplo: ivai, tibagi). "
                        + "O rio de ocorrencia nao representa a localidade de coleta.")
                .build();

        SyncToolSpecification specification = SyncToolSpecification.builder()
                .tool(tool)
                .callHandler(this::run)
                .build();

        mcp.tools(specification);
    }

    @Override
    // valida a operacao e executa a consulta de ocorrencia.
    public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
        if (request == null || request.arguments() == null) {
            return erro("Informe a operacao: especie, rio ou intersecao.");
        }

        Map<String, Object> argumentos = request.arguments();
        Object operacaoValor = argumentos.get("operacao");

        if (!(operacaoValor instanceof String)) {
            return erro("O parametro operacao deve ser texto.");
        }

        String operacao = ((String) operacaoValor).strip().toLowerCase();
        Map<String, Object> resultado;

        if ("especie".equals(operacao)) {
            String especie = textoObrigatorio(argumentos.get("especie"));

            if (especie == null) {
                return erro("Informe o parametro especie como texto nao vazio.");
            }

            resultado = service.consultarEspecie(especie);
        } else if ("rio".equals(operacao)) {
            String rio = textoObrigatorio(argumentos.get("rio"));

            if (rio == null || !OcorrenciaService.rioValido(rio)) {
                return erro("Informe um rio valido: iguacu, ivai, piquiri ou tibagi.");
            }

            resultado = service.consultarRio(rio);
        } else if ("intersecao".equals(operacao)) {
            Object listaRios = argumentos.get("rios");

            if (!(listaRios instanceof List<?> valores)) {
                return erro("Informe rios como uma lista de dois ou mais rios.");
            }

            List<String> rios = new ArrayList<>();

            for (Object valor : valores) {
                if (!(valor instanceof String rio) || !OcorrenciaService.rioValido(rio)) {
                    return erro("A lista rios contem um rio invalido.");
                }

                String normalizado = OcorrenciaService.normalizarRio(rio);

                if (!rios.contains(normalizado)) {
                    rios.add(normalizado);
                }
            }

            if (rios.size() < 2) {
                return erro("Informe pelo menos dois rios diferentes para a intersecao.");
            }

            resultado = service.compararRios(rios);
        } else {
            return erro("Operacao invalida. Use especie, rio ou intersecao.");
        }

        try {
            return CallToolResult.builder()
                    .addTextContent(mapper.writeValueAsString(resultado))
                    .build();
        } catch (Exception exception) {
            return erro("Nao foi possivel preparar a consulta de ocorrencia.");
        }
    }

    // descreve um argumento no esquema json.
    private Map<String, Object> propriedade(String descricao, String tipo) {
        Map<String, Object> propriedade = new LinkedHashMap<>();
        propriedade.put("type", tipo);
        propriedade.put("description", descricao);
        return propriedade;
    }

    // recupera e valida um argumento textual obrigatorio.
    private String textoObrigatorio(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }

        String resultado = texto.strip();

        if (resultado.isEmpty()) {
            return null;
        }

        return resultado;
    }

    // monta uma resposta mcp de erro.
    private CallToolResult erro(String mensagem) {
        return CallToolResult.builder()
                .isError(true)
                .addTextContent(mensagem)
                .build();
    }
    
    Map<String, Object> criarSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");

        schema.put("oneOf", List.of(
                schemaOperacao("especie", "especie",
                        "Nome da especie. Exemplo: Cambeva castroi", "string"),
                schemaOperacao("rio", "rio",
                        "Rio de ocorrencia. Exemplo: tibagi", "string"),
                schemaOperacao("intersecao", "rios",
                        "Rios de ocorrencia. Exemplo: ivai, tibagi", "array")
        ));

        return schema;
    }

    // monta o esquema de uma operacao especifica.
    private Map<String, Object> schemaOperacao(
            String operacao,
            String parametro,
            String descricao,
            String tipo) {

        Map<String, Object> seletor = propriedade(
                "Tipo de consulta.", "string");

        seletor.put("const", operacao);

        Map<String, Object> valor = propriedade(descricao, tipo);

        if ("array".equals(tipo)) {
            valor.put("items", Map.of("type", "string"));
            valor.put("minItems", 2);
            valor.put("uniqueItems", true);
        }

        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("operacao", seletor);
        properties.put(parametro, valor);

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("operacao", parametro));
        schema.put("additionalProperties", false);

        return schema;
    }
    
    
}
