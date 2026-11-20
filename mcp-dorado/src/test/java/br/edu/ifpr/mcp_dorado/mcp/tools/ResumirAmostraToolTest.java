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
import br.edu.ifpr.mcp_dorado.service.ResumoAmostraService;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ResumirAmostraToolTest {

    private ResumirAmostraTool tool;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void preparar() {
        GenbankRecord registro = new GenbankRecord("ABC.1", "Cambeva castroi",
                "cambeva castroi", false, List.of(), Set.of("tibagi"), Set.of("COI"),
                Map.of("geo_loc_name", List.of("Brazil: Parana")), 100, 0, 0, null);

        Catalog catalog = new Catalog(Path.of("amostra.gb"),
                Map.of(registro.accession(), registro), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of());
        tool = new ResumirAmostraTool(new ResumoAmostraService(catalog));
    }

    // verifica retorna resumo sem parametros.
    @Test
    void retornaResumoSemParametros() {
        CallToolResult resultado = chamar(Map.of());

        assertFalse(Boolean.TRUE.equals(resultado.isError()));
        assertTrue(conteudo(resultado).contains("\"total_registros\":1"));
        assertTrue(conteudo(resultado).contains("\"total_especies\":1"));
    }

    // verifica retorna genes erios no resumo.
    @Test
    void retornaGenesERiosNoResumo() {
        CallToolResult resultado = chamar(Map.of());

        assertTrue(conteudo(resultado).contains("COI"));
        assertTrue(conteudo(resultado).contains("tibagi"));
    }

    // verifica chamar.
    private CallToolResult chamar(Map<String, Object> argumentos) {
        CallToolRequest request = CallToolRequest.builder("resumir_amostra")
                .arguments(argumentos)
                .build();
        return tool.run(null, request);
    }

    // verifica conteudo.
    private String conteudo(CallToolResult resultado) {
        return ((TextContent) resultado.content().get(0)).text();
    }
}
