package br.edu.ifpr.mcp_dorado.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_dorado.model.GenbankRecord;

class RegistrosServiceTest {

    private RegistrosService service;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void preparar() {
        Map<String, GenbankRecord> registros = new LinkedHashMap<>();
        registros.put("A.1", registro("A.1", "Cambeva castroi", "cambeva castroi",
                Set.of("COI", "12S"), "Brazil: Parana: Tibagi"));
        registros.put("B.1", registro("B.1", "Cambeva castroi", "cambeva castroi",
                Set.of("COI"), null));
        registros.put("C.1", registro("C.1", "Astyanax lacustris", "astyanax lacustris",
                Set.of("16S"), "Brazil: Parana: Ivaí"));

        Catalog catalog = new Catalog(Path.of("amostra.gb"), registros,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        service = new RegistrosService(catalog);
    }

    // verifica conta todos os registros sem filtros.
    @Test
    void contaTodosOsRegistrosSemFiltros() {
        Map<String, Object> resultado = service.consultar("contar", "", "", "", "");
        assertEquals(3, resultado.get("total"));
    }

    // verifica combina filtros de especie gene elocalidade.
    @Test
    void combinaFiltrosDeEspecieGeneELocalidade() {
        Map<String, Object> resultado = service.consultar("contar", " CAMBEVA CASTROI ",
                "coi", "tibagi", "");
        assertEquals(1, resultado.get("total"));
    }

    // verifica nao confunde rio de ocorrencia com localidade.
    @Test
    void naoConfundeRioDeOcorrenciaComLocalidade() {
        Map<String, Object> resultado = service.consultar("contar", "", "", "iguacu", "");
        assertEquals(0, resultado.get("total"));
    }


    // verifica lista os accessions encontrados em ordem.
    @Test
    void listaOsAccessionsEncontradosEmOrdem() {
        Map<String, Object> resultado = service.consultar("listar", "cambeva castroi", "", "", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> registros = (List<Map<String, Object>>) resultado.get("registros");

        assertEquals(2, resultado.get("total"));
        assertEquals("A.1", registros.get(0).get("accession"));
        assertEquals("B.1", registros.get(1).get("accession"));
        assertEquals(false, resultado.get("truncado"));
    }

    // verifica agrupa genes contando um registro em cada gene.
    @Test
    void agrupaGenesContandoUmRegistroEmCadaGene() {
        Map<String, Object> resultado = service.consultar("agrupar", "", "", "", "gene");
        @SuppressWarnings("unchecked")
        Map<String, Integer> grupos = (Map<String, Integer>) resultado.get("grupos");

        assertEquals(2, grupos.get("COI"));
        assertEquals(1, grupos.get("12S"));
        assertEquals(1, grupos.get("16S"));
    }

    // verifica limita listagem sem alterar total.
    @Test
    void limitaListagemSemAlterarTotal() {
        Map<String, GenbankRecord> registros = new LinkedHashMap<>();

        for (int i = 0; i < 53; i++) {
            String accession = String.format("ACC%03d.1", i);
            registros.put(accession, registro(accession, "Cambeva castroi", "cambeva castroi",
                    Set.of("COI"), "Brazil: Parana"));
        }

        Catalog catalog = new Catalog(Path.of("amostra.gb"), registros,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        RegistrosService outroServico = new RegistrosService(catalog);
        Map<String, Object> resultado = outroServico.consultar("listar", "", "", "", "");

        assertEquals(53, resultado.get("total"));
        assertEquals(50, resultado.get("exibidos"));
        assertTrue((Boolean) resultado.get("truncado"));
    }


    // verifica registro.
    private GenbankRecord registro(String accession, String organismo, String especie,
            Set<String> genes, String localidade) {
        Map<String, List<String>> qualificadores = new LinkedHashMap<>();

        if (localidade != null) {
            qualificadores.put("geo_loc_name", List.of(localidade));
        }

        return new GenbankRecord(accession, organismo, especie, false, List.of(),
                Set.of("iguacu", "tibagi"), genes, qualificadores, 100, 0, 0, null);
    }
}
