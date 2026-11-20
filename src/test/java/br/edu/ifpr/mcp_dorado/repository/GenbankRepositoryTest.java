package br.edu.ifpr.mcp_dorado.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;
import br.edu.ifpr.mcp_dorado.service.Catalog;

class GenbankRepositoryTest {

    @TempDir
    Path tempDirectory;

    // verifica a busca por identificador nos metadados indexados.
    @Test
    void encontraRegistroPorAccession() throws Exception {
        GenbankRepository repository = criarRepositorio();
        assertTrue(repository.find("ABC123.1").isPresent());
        assertTrue(repository.find("UNKNOWN").isEmpty());
    }

    // verifica a leitura do trecho original pela posicao em bytes.
    @Test
    void recuperaEntradaOriginal() throws Exception {
        GenbankRepository repository = criarRepositorio();
        assertEquals("LOCUS ABC123\n//\n", repository.originalEntry("ABC123.1").orElseThrow());
        assertTrue(repository.originalEntry("UNKNOWN").isEmpty());
    }

    // prepara uma amostra curta e seu indice para os testes.
    private GenbankRepository criarRepositorio() throws Exception {
        String entry = "LOCUS ABC123\n//\n";
        Path sample = tempDirectory.resolve("sample.gb");
        Files.writeString(sample, entry);
        GenbankRecord record = new GenbankRecord("ABC123.1", "Cambeva castroi",
                "cambeva castroi", false, List.of(), Set.of(), Set.of(), Map.of(),
                4L, 0L, entry.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, null);
        Catalog catalog = new Catalog(sample, Map.of(record.accession(), record), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        return new GenbankRepository(catalog);
    }
}
