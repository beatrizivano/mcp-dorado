package br.edu.ifpr.mcp_dorado.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.Tool;

class ToolArgumentsReaderTest {

    // verifica converte lista de rios.
    @Test
    void converteListaDeRios() {
        assertEquals(List.of("ivai", "tibagi"), ToolArgumentsReader.parseArray("ivai, tibagi"));
        assertEquals(List.of("ivai", "tibagi"), ToolArgumentsReader.parseArray("[\"ivai\", \"tibagi\"]"));
    }


    // verifica converte valores simples.
    @Test
    void converteValoresSimples() {
        assertEquals(3, ToolArgumentsReader.convert("3", "integer"));
        assertEquals(2.5, ToolArgumentsReader.convert("2.5", "number"));
        assertEquals(true, ToolArgumentsReader.convert("sim", "boolean"));
        assertEquals("COI", ToolArgumentsReader.convert("COI", "string"));
    }


    // verifica le parametros do esquema.
    @Test
    void leParametrosDoEsquema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("operacao", Map.of("type", "string"));
        properties.put("rios", Map.of("type", "array"));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("operacao"));
        Tool tool = Tool.builder("consultar_ocorrencia", schema).build();
        Scanner scanner = new Scanner("intersecao\nivaí, tibagi\n");
        ToolArgumentsReader reader = new ToolArgumentsReader(scanner,
                new PrintStream(new ByteArrayOutputStream()));

        Map<String, Object> arguments = reader.read(tool);

        assertEquals("intersecao", arguments.get("operacao"));
        assertEquals(List.of("ivaí", "tibagi"), arguments.get("rios"));
    }

    // verifica deixa campo opcional ausente.
    @Test
    void deixaCampoOpcionalAusente() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("gene", Map.of("type", "string")));
        Tool tool = Tool.builder("consultar_registros", schema).build();
        ToolArgumentsReader reader = new ToolArgumentsReader(new Scanner("\n"),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(Map.of(), reader.read(tool));
    }

    // verifica que uma escolha condicional solicita apenas o rio.
    @Test
    void solicitaSomenteOParametroDaOperacao() {
        Map<String, Object> ramoRio = Map.of(
                "type", "object", "properties", Map.of(
                        "operacao", Map.of("type", "string", "const", "rio"),
                        "rio", Map.of("type", "string")),
                "required", List.of("operacao", "rio"));
        Map<String, Object> ramoEspecie = Map.of(
                "type", "object", "properties", Map.of(
                        "operacao", Map.of("type", "string", "const", "especie"),
                        "especie", Map.of("type", "string")),
                "required", List.of("operacao", "especie"));
        Tool tool = Tool.builder("consultar_ocorrencia",
                Map.of("type", "object", "oneOf", List.of(ramoRio, ramoEspecie))).build();
        ToolArgumentsReader reader = new ToolArgumentsReader(new Scanner("1\ntibagi\n"),
                new PrintStream(new ByteArrayOutputStream()));
        assertEquals(Map.of("operacao", "rio", "rio", "tibagi"), reader.read(tool));
    }

    // verifica que uma lista de rios e convertida para o tipo esperado.
    @Test
    void interpretaListaDaOperacaoCondicional() {
        Map<String, Object> ramo = Map.of(
                "type", "object", "properties", Map.of(
                        "operacao", Map.of("type", "string", "const", "intersecao"),
                        "rios", Map.of("type", "array", "minItems", 2)),
                "required", List.of("operacao", "rios"));
        Tool tool = Tool.builder("consultar_ocorrencia",
                Map.of("type", "object", "oneOf", List.of(ramo))).build();
        ToolArgumentsReader reader = new ToolArgumentsReader(new Scanner("1\nivai, tibagi\n"),
                new PrintStream(new ByteArrayOutputStream()));
        assertEquals(List.of("ivai", "tibagi"), reader.read(tool).get("rios"));
    }
}
