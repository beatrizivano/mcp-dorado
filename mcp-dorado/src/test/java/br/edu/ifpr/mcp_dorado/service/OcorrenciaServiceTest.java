package br.edu.ifpr.mcp_dorado.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcorrenciaServiceTest {

    private OcorrenciaService service;

    // prepara os dados utilizados pelos cenarios de teste.
    @BeforeEach
    void preparar() {
        Map<String, Object> especies = Map.of(
                "cambeva castroi", Map.of("rivers", List.of("iguacu", "tibagi"),
                        "query_names", List.of("cambeva castroi", "trichomycterus castroi")),
                "rhamdia quelen", Map.of("rivers", List.of("iguacu", "ivai", "tibagi"),
                        "query_names", List.of("rhamdia quelen")),
                "acrolebias carvalhoi", Map.of("rivers", List.of("iguacu"),
                        "query_names", List.of("acrolebias carvalhoi"))
        );
        Map<String, Object> ambiguos = Map.of(
                "astyanax lacustris", Map.of("rivers", List.of("iguacu", "tibagi"),
                        "candidates", List.of("astyanax lacustris", "astyanax altiparanae"))
        );
        Map<String, Object> ocorrencia = Map.of("species", especies, "ambiguous_names", ambiguos);
        Catalog catalog = new Catalog(Path.of("amostra.gb"), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), ocorrencia, Map.of(), Map.of());
        service = new OcorrenciaService(catalog);
    }

    // verifica consulta especie pelo nome valido.
    @Test
    void consultaEspeciePeloNomeValido() {
        Map<String, Object> resultado = service.consultarEspecie("Cambeva castroi");
        assertEquals(true, resultado.get("encontrada"));
        assertEquals(false, resultado.get("ambigua"));
        assertEquals(List.of("iguacu", "tibagi"), resultado.get("rios"));
    }

    // verifica consulta especie por sinonimo.
    @Test
    void consultaEspeciePorSinonimo() {
        Map<String, Object> resultado = service.consultarEspecie("Trichomycterus castroi");
        assertEquals("cambeva castroi", resultado.get("nome"));
    }

    // verifica preserva ambiguidade sem escolher candidato.
    @Test
    void preservaAmbiguidadeSemEscolherCandidato() {
        Map<String, Object> resultado = service.consultarEspecie("Astyanax lacustris");
        assertEquals(true, resultado.get("ambigua"));
        assertEquals(List.of("iguacu", "tibagi"), resultado.get("rios"));
        assertTrue(((List<?>) resultado.get("candidatos")).contains("astyanax altiparanae"));
    }

    // verifica consulta rio com grafia alternativa.
    @Test
    void consultaRioComGrafiaAlternativa() {
        Map<String, Object> resultado = service.consultarRio("Iguassu");
        assertEquals("iguacu", resultado.get("rio"));
        assertEquals(3, resultado.get("total_especies"));
        assertEquals(1, resultado.get("total_nomes_ambiguos"));
    }


    // verifica intersecao mostra somente especies compartilhadas.
    @Test
    void intersecaoMostraSomenteEspeciesCompartilhadas() {
        Map<String, Object> resultado = service.compararRios(List.of("iguacu", "tibagi"));
        assertEquals(2, resultado.get("total_especies"));
        assertEquals(List.of("cambeva castroi", "rhamdia quelen"), resultado.get("especies"));
        assertEquals(1, resultado.get("total_nomes_ambiguos"));
    }


    // verifica valida rios ecorrige acentos.
    @Test
    void validaRiosECorrigeAcentos() {
        assertTrue(OcorrenciaService.rioValido("Iguaçu"));
        assertTrue(OcorrenciaService.rioValido("Tibaji"));
        assertFalse(OcorrenciaService.rioValido("Parana"));
    }
}
