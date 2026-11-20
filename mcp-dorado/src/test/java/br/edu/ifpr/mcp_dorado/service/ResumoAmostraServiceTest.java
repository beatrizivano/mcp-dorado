package br.edu.ifpr.mcp_dorado.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_dorado.model.Coordinates;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;

class ResumoAmostraServiceTest {

    // verifica resume registros especies egenes sem duplicar accessions.
    @Test
    void resumeRegistrosEspeciesEGenesSemDuplicarAccessions() {
        Map<String, Object> resumo = prepararServico().resumir();
        Map<String, Integer> genes = mapaInteiros(resumo.get("registros_por_gene"));

        assertEquals(3, resumo.get("total_registros"));
        assertEquals(2, resumo.get("total_especies"));
        assertEquals(2, genes.get("COI"));
        assertEquals(1, genes.get("12S"));
        assertEquals(1, resumo.get("registros_sem_gene_anotado"));
    }

    // verifica conta rios de ocorrencia independentes da localidade.
    @Test
    void contaRiosDeOcorrenciaIndependentesDaLocalidade() {
        Map<String, Object> resumo = prepararServico().resumir();
        Map<String, Integer> rios = mapaInteiros(resumo.get("registros_por_rio_de_ocorrencia"));

        assertEquals(2, rios.get("tibagi"));
        assertEquals(1, rios.get("ivai"));
        assertEquals(1, resumo.get("registros_sem_rio_de_ocorrencia"));
    }

    // verifica conta localidade ecoordenadas separadamente.
    @Test
    void contaLocalidadeECoordenadasSeparadamente() {
        Map<String, Object> resumo = prepararServico().resumir();
        Map<String, Object> localidade = mapaObjetos(resumo.get("localidade_dos_registros"));

        assertEquals(2, localidade.get("com_geo_loc_name"));
        assertEquals(1, localidade.get("sem_geo_loc_name"));
        assertEquals(1, localidade.get("com_coordenadas_convertidas"));
        assertEquals(2, localidade.get("sem_coordenadas_convertidas"));
        assertEquals(2, localidade.get("total_localidades_distintas"));
    }

    // verifica conta qualificadores preenchidos sem duplicar valores.
    @Test
    void contaQualificadoresPreenchidosSemDuplicarValores() {
        Map<String, Integer> qualificadores = mapaInteiros(
                prepararServico().resumir().get("registros_com_qualificador"));

        assertEquals(2, qualificadores.get("geo_loc_name"));
        assertEquals(1, qualificadores.get("lat_lon"));
        assertEquals(1, qualificadores.get("collection_date"));
        assertEquals(0, qualificadores.get("isolate"));
        assertEquals(0, qualificadores.get("db_xref"));
        assertEquals(0, qualificadores.get("specimen_voucher"));
    }

    // verifica preparar servico.
    private ResumoAmostraService prepararServico() {
        Map<String, GenbankRecord> registros = new LinkedHashMap<>();
        registros.put("A.1", registro("A.1", "cambeva castroi", false,
                Set.of("COI", "12S"), Set.of("tibagi", "ivai"),
                Map.of("geo_loc_name", List.of("Brazil: Parana: Tibagi"),
                        "lat_lon", List.of("23 S 51 W"),
                        "collection_date", List.of("2020", "2021")),
                new Coordinates(-23, -51)));
        registros.put("B.1", registro("B.1", "cambeva castroi", false,
                Set.of("COI"), Set.of("tibagi"),
                Map.of("geo_loc_name", List.of("Brazil: Parana")), null));
        registros.put("C.1", registro("C.1", "especie incerta", true,
                Set.of(), Set.of(), Map.of("isolate", List.of(" ")), null));

        return new ResumoAmostraService(catalog(registros));
    }

    // verifica catalog.
    private Catalog catalog(Map<String, GenbankRecord> registros) {
        return new Catalog(Path.of("amostra.gb"), registros, Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    // verifica registro.
    private GenbankRecord registro(String accession, String especie, boolean ambigua,
            Set<String> genes, Set<String> rios, Map<String, List<String>> qualificadores,
            Coordinates coordenadas) {
        return new GenbankRecord(accession, especie, especie, ambigua, List.of(),
                rios, genes, qualificadores, 100, 0, 0, coordenadas);
    }

    @SuppressWarnings("unchecked")
    // verifica mapa inteiros.
    private Map<String, Integer> mapaInteiros(Object objeto) {
        return (Map<String, Integer>) objeto;
    }

    @SuppressWarnings("unchecked")
    // verifica mapa objetos.
    private Map<String, Object> mapaObjetos(Object objeto) {
        return (Map<String, Object>) objeto;
    }
}
