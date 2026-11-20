package br.edu.ifpr.mcp_dorado.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_dorado.mcp.utils.RegistroFormatter;

class RegistroFormatterTest {

    // verifica exibe campos no estilo genbank.
    @Test
    void exibeCamposNoEstiloGenbank() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("accession", "CM184936.1");
        record.put("organism", "Astyanax lacustris");
        record.put("normalized_species", "Astyanax lacustris");
        record.put("genes", List.of("12S", "COI"));
        record.put("rivers", List.of("ivai", "tibagi"));
        record.put("sequence_length", 1500);
        record.put("geo_loc_name", "Brazil: Parana");
        record.put("coordinates", Map.of("latitude", -23.5, "longitude", -51.5));
        record.put("qualifiers", Map.of("collection_date", List.of("2022")));

        String formatted = new RegistroFormatter().formatRecord(record);

        assertTrue(formatted.contains("LOCUS       CM184936.1  1500 bp"));
        assertTrue(formatted.contains("ORGANISM    Astyanax lacustris"));
        assertTrue(formatted.contains("GENES       12S, COI"));
        assertTrue(formatted.contains("COORDINATES -23.5, -51.5"));
        assertTrue(formatted.contains("/collection_date=2022"));
        assertTrue(formatted.endsWith("//"));
    }


    // verifica formata lista de registros.
    @Test
    void formataListaDeRegistros() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("operacao", "listar");
        response.put("total", 1);
        response.put("exibidos", 1);
        response.put("truncado", false);
        response.put("registros", List.of(Map.of(
                "accession", "AB123.1", "especie", "Cambeva castroi",
                "genes", List.of("COI"), "localidade", "Brazil: Parana")));

        String formatted = new RegistroFormatter().formatList(response);

        assertTrue(formatted.contains("REGISTROS GENBANK"));
        assertTrue(formatted.contains("ACCESSION   AB123.1"));
        assertTrue(formatted.contains("ORGANISM    Cambeva castroi"));
    }

    // verifica separa argumentos eformata agrupamentos.
    @Test
    void separaArgumentosEFormataAgrupamentos() {
        RegistroFormatter formatter = new RegistroFormatter();
        String arguments = formatter.formatArguments(Map.of("operacao", "agrupar", "agrupar_por", "gene"));
        assertTrue(arguments.contains("Operacao: agrupar"));
        String response = formatter.format("consultar_registros",
                "{\"operacao\":\"agrupar\",\"grupos\":{\"COI\":10,\"12S\":4}}");
        assertTrue(response.contains("Grupos:"));
        assertTrue(response.contains("COI: 10"));
        assertTrue(!response.contains("{\""));
    }

}
