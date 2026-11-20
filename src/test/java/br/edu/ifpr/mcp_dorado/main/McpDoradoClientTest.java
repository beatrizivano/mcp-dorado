package br.edu.ifpr.mcp_dorado.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.Tool;

class McpDoradoClientTest {

    // verifica seleciona ferramenta por nome ou numero.
    @Test
    void selecionaFerramentaPorNomeOuNumero() {
        Tool first = Tool.builder("consultar_accession", Map.of("type", "object")).build();
        Tool second = Tool.builder("consultar_ocorrencia", Map.of("type", "object")).build();
        List<Tool> tools = List.of(first, second);

        assertEquals(first, McpDoradoClient.selectTool(tools, "1"));
        assertEquals(second, McpDoradoClient.selectTool(tools, "CONSULTAR_OCORRENCIA"));
    }

    // verifica retorna nulo para ferramenta inexistente.
    @Test
    void retornaNuloParaFerramentaInexistente() {
        assertNull(McpDoradoClient.selectTool(List.of(), "1"));
        assertNull(McpDoradoClient.selectTool(List.of(), "desconhecida"));
    }


    // verifica rejeita parametros de inicializacao invalidos.
    @Test
    void rejeitaParametrosDeInicializacaoInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> McpDoradoClient.resolveServerPath(new String[] {"--server"}));
    }
}
