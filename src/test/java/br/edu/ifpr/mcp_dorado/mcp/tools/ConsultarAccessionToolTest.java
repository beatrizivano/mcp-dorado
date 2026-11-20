package br.edu.ifpr.mcp_dorado.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_dorado.repository.GenbankRepository;
import br.edu.ifpr.mcp_dorado.service.AccessionService;
import br.edu.ifpr.mcp_dorado.service.DataInitializer;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ConsultarAccessionToolTest {

    @TempDir
    Path tempDirectory;

    private ConsultarAccessionTool tool;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void prepare() throws Exception {
        Path sample = tempDirectory.resolve("sample.gb");
        Files.writeString(sample, "LOCUS       TEST         4 bp    DNA     linear   PLN 01-JAN-2025\n"
                + "VERSION     ABC0001.1\n"
                + "  ORGANISM  Cambeva castroi\n"
                + "FEATURES             Location/Qualifiers\n"
                + "     source          1..4\n"
                + "                     /geo_loc_name=\"Brazil: Parana\"\n"
                + "                     /lat_lon=\"23.5 S 51.5 W\"\n"
                + "ORIGIN\n"
                + "        1 atgc\n"
                + "//\n");

        AccessionService service = new AccessionService(
                new GenbankRepository(new DataInitializer().initialize(sample))
        );
        tool = new ConsultarAccessionTool(service);
    }

    // verifica returns metadata for existing accession.
    @Test
    void returnsMetadataForExistingAccession() {
        CallToolResult result = call(Map.of("accession", "ABC0001.1"));
        String text = ((TextContent) result.content().get(0)).text();

        assertFalse(Boolean.TRUE.equals(result.isError()));
        assertTrue(text.contains("\"accession\":\"ABC0001.1\""));
        assertTrue(text.contains("\"organism\":\"Cambeva castroi\""));
        assertTrue(text.contains("\"geo_loc_name\":\"Brazil: Parana\""));
        assertTrue(text.contains("\"sequence_length\":4"));
        assertTrue(text.contains("\"latitude\":-23.5"));
        assertTrue(text.contains("\"longitude\":-51.5"));
    }


    // verifica reports unknown accession.
    @Test
    void reportsUnknownAccession() {
        CallToolResult result = call(Map.of("accession", "NOT_FOUND.1"));

        assertTrue(Boolean.TRUE.equals(result.isError()));
        assertTrue(((TextContent) result.content().get(0)).text().contains("nao encontrado"));
    }


    // verifica rejects non string accession.
    @Test
    void rejectsNonStringAccession() {
        CallToolResult result = call(Map.of("accession", 123));

        assertTrue(Boolean.TRUE.equals(result.isError()));
    }


    // verifica call.
    private CallToolResult call(Map<String, Object> arguments) {
        CallToolRequest request = CallToolRequest.builder("consultar_accession")
                .arguments(arguments)
                .build();

        return tool.run(null, request);
    }
}
