package br.edu.ifpr.mcp_dorado.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_dorado.repository.GenbankRepository;

class AccessionServiceTest {

    @TempDir
    Path tempDirectory;

    private AccessionService service;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void prepare() throws Exception {
        Path sample = tempDirectory.resolve("sample.gb");
        Files.writeString(sample, "LOCUS       TEST         4 bp    DNA     linear   PLN 01-JAN-2025\n"
                + "VERSION     SERVICE0001.1\n"
                + "  ORGANISM  Cambeva castroi\n"
                + "FEATURES             Location/Qualifiers\n"
                + "     source          1..4\n"
                + "                     /geo_loc_name=\"Brazil: Parana\"\n"
                + "ORIGIN\n"
                + "        1 atgc\n"
                + "//\n");

        Catalog catalog = new DataInitializer().initialize(sample);
        service = new AccessionService(new GenbankRepository(catalog));
    }

    // verifica returns the expected fields.
    @Test
    void returnsTheExpectedFields() {
        Map<String, Object> result = service.consultar("SERVICE0001.1").orElseThrow();

        assertEquals("SERVICE0001.1", result.get("accession"));
        assertEquals("Cambeva castroi", result.get("organism"));
        assertEquals(4L, result.get("sequence_length"));
        assertEquals("Brazil: Parana", result.get("geo_loc_name"));
        assertTrue(result.containsKey("genes"));
        assertTrue(result.containsKey("rivers"));
        assertTrue(result.containsKey("qualifiers"));
        assertTrue(result.containsKey("coordinates"));
    }


    // verifica trims the search argument.
    @Test
    void trimsTheSearchArgument() {
        assertTrue(service.consultar("  SERVICE0001.1 ").isPresent());
    }


    // verifica returns empty for null or blank accession.
    @Test
    void returnsEmptyForNullOrBlankAccession() {
        assertEquals(Optional.empty(), service.consultar(null));
        assertEquals(Optional.empty(), service.consultar(" "));
    }
}
