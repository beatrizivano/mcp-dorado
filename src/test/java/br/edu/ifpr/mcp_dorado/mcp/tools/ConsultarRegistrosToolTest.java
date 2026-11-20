package br.edu.ifpr.mcp_dorado.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_dorado.model.GenbankRecord;
import br.edu.ifpr.mcp_dorado.service.Catalog;
import br.edu.ifpr.mcp_dorado.service.RegistrosService;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ConsultarRegistrosToolTest {

    private ConsultarRegistrosTool tool;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void preparar() {
        GenbankRecord record = new GenbankRecord("ABC0001.1", "Cambeva castroi",
                "cambeva castroi", false, List.of(), Set.of("tibagi"), Set.of("COI"),
                Map.of("geo_loc_name", List.of("Brazil: Parana")), 123, 0, 0, null);
        Catalog catalog = new Catalog(Path.of("amostra.gb"), Map.of(record.accession(), record),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        tool = new ConsultarRegistrosTool(new RegistrosService(catalog));
    }

    // verifica conta registros.
    @Test
    void contaRegistros() {
        CallToolResult result = chamar(Map.of("operacao", "contar", "gene", "coi"));
        assertFalse(Boolean.TRUE.equals(result.isError()));
        assertTrue(conteudo(result).contains("\"total\":1"));
    }

    // verifica lista registros.
    @Test
    void listaRegistros() {
        CallToolResult result = chamar(Map.of("operacao", "listar"));
        assertFalse(Boolean.TRUE.equals(result.isError()));
        assertTrue(conteudo(result).contains("ABC0001.1"));
    }

    // verifica agrupa por localidade.
    @Test
    void agrupaPorLocalidade() {
        CallToolResult result = chamar(Map.of("operacao", "agrupar", "agrupar_por", "local"));
        assertFalse(Boolean.TRUE.equals(result.isError()));
        assertTrue(conteudo(result).contains("Brazil: Parana"));
    }

    // verifica rejeita agrupamento sem campo.
    @Test
    void rejeitaAgrupamentoSemCampo() {
        assertTrue(Boolean.TRUE.equals(chamar(Map.of("operacao", "agrupar")).isError()));
    }

    // verifica rejeita agrupar por em contagem.
    @Test
    void rejeitaAgruparPorEmContagem() {
        assertTrue(Boolean.TRUE.equals(chamar(Map.of("operacao", "contar",
                "agrupar_por", "gene")).isError()));
    }

    // verifica chamar.
    private CallToolResult chamar(Map<String, Object> arguments) {
        CallToolRequest request = CallToolRequest.builder("consultar_registros")
                .arguments(arguments)
                .build();
        return tool.run(null, request);
    }

    // verifica conteudo.
    private String conteudo(CallToolResult result) {
        return ((TextContent) result.content().get(0)).text();
    }

    // verifica que o agrupamento exige um campo adicional no esquema.
    @Test
    void declaraAgrupamentoCondicionalNoSchema() {
        List<?> alternatives = (List<?>) tool.criarSchema().get("oneOf");
        assertTrue(alternatives.size() == 3);
        Map<?, ?> contar = (Map<?, ?>) alternatives.get(0);
        Map<?, ?> agrupar = (Map<?, ?>) alternatives.get(2);
        Map<?, ?> contarProperties = (Map<?, ?>) contar.get("properties");
        Map<?, ?> agruparProperties = (Map<?, ?>) agrupar.get("properties");
        assertFalse(contarProperties.containsKey("agrupar_por"));
        assertTrue(agruparProperties.containsKey("agrupar_por"));
    }
}
