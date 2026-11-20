package br.edu.ifpr.mcp_dorado.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_dorado.service.Catalog;
import br.edu.ifpr.mcp_dorado.service.OcorrenciaService;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ConsultarOcorrenciaToolTest {

    private ConsultarOcorrenciaTool tool;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void preparar() {
        Map<String, Object> especies = Map.of(
                "cambeva castroi", Map.of("rivers", List.of("iguacu", "tibagi"),
                        "query_names", List.of("cambeva castroi", "trichomycterus castroi"))
        );
        Map<String, Object> ambiguos = Map.of(
                "astyanax lacustris", Map.of("rivers", List.of("iguacu", "tibagi"),
                        "candidates", List.of("astyanax lacustris", "astyanax altiparanae"))
        );
        Map<String, Object> ocorrencia = Map.of("species", especies, "ambiguous_names", ambiguos);
        Catalog catalog = new Catalog(Path.of("amostra.gb"), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), ocorrencia, Map.of(), Map.of());
        tool = new ConsultarOcorrenciaTool(new OcorrenciaService(catalog));
    }

    // verifica consulta especie.
    @Test
    void consultaEspecie() {
        CallToolResult resultado = chamar(Map.of("operacao", "especie", "especie", "cambeva castroi"));
        assertFalse(Boolean.TRUE.equals(resultado.isError()));
        assertTrue(conteudo(resultado).contains("\"iguacu\""));
    }

    // verifica consulta rio.
    @Test
    void consultaRio() {
        CallToolResult resultado = chamar(Map.of("operacao", "rio", "rio", "Tibaji"));
        assertFalse(Boolean.TRUE.equals(resultado.isError()));
        assertTrue(conteudo(resultado).contains("\"rio\":\"tibagi\""));
    }

    // verifica consulta intersecao.
    @Test
    void consultaIntersecao() {
        CallToolResult resultado = chamar(Map.of("operacao", "intersecao", "rios", List.of("iguacu", "tibagi")));
        assertFalse(Boolean.TRUE.equals(resultado.isError()));
        assertTrue(conteudo(resultado).contains("cambeva castroi"));
    }

    // verifica consulta ambigua mostra candidatos.
    @Test
    void consultaAmbiguaMostraCandidatos() {
        CallToolResult resultado = chamar(Map.of("operacao", "especie", "especie", "astyanax lacustris"));
        assertFalse(Boolean.TRUE.equals(resultado.isError()));
        assertTrue(conteudo(resultado).contains("astyanax altiparanae"));
    }

    // verifica rejeita lista ausente.
    @Test
    void rejeitaListaAusente() {
        assertTrue(Boolean.TRUE.equals(chamar(Map.of("operacao", "intersecao")).isError()));
    }

    // verifica chamar.
    private CallToolResult chamar(Map<String, Object> argumentos) {
        CallToolRequest request = CallToolRequest.builder("consultar_ocorrencia")
                .arguments(argumentos)
                .build();
        return tool.run(null, request);
    }

    // verifica conteudo.
    private String conteudo(CallToolResult resultado) {
        return ((TextContent) resultado.content().get(0)).text();
    }

    // verifica que cada operacao anuncia somente os seus parametros.
    @Test
    void declaraOperacoesSeparadasNoSchema() {
        List<?> alternatives = (List<?>) tool.criarSchema().get("oneOf");
        assertTrue(alternatives.size() == 3);
        String[] keys = {"especie", "rio", "rios"};
        for (int index = 0; index < alternatives.size(); index++) {
            Map<?, ?> branch = (Map<?, ?>) alternatives.get(index);
            Map<?, ?> properties = (Map<?, ?>) branch.get("properties");
            assertTrue(properties.containsKey(keys[index]));
            assertTrue(properties.size() == 2);
        }
    }
}
