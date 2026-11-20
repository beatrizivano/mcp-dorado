package br.edu.ifpr.mcp_dorado.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import br.edu.ifpr.mcp_dorado.repository.GenbankRepository;

class DataInitializerTest {
    // verifica indexes provided sample and reads original record.
    @Test void indexesProvidedSampleAndReadsOriginalRecord() throws Exception {
        var catalog = new DataInitializer().initialize(Path.of("data", "amostra.gb"));
        assertEquals(954, catalog.accessions().size());
        var record = catalog.accessions().get("CM184936.1");
        assertNotNull(record);
        assertEquals("Astyanax lacustris", record.organism());
        assertTrue(record.rivers().contains("iguacu"));
        assertEquals(126390825L, record.sequenceLength());
        assertTrue(new GenbankRepository(catalog).originalEntry("CM184936.1")
            .orElseThrow().startsWith("LOCUS"));
    }

    // verifica rejeita arquivo de amostra inexistente.
    @Test
    void rejeitaArquivoDeAmostraInexistente() {
        Path sample = Path.of("arquivo_inexistente.gb");
        assertThrows(java.io.IOException.class, () -> new DataInitializer().initialize(sample));
    }
}
