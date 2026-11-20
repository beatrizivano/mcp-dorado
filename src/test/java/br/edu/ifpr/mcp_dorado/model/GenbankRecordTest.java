package br.edu.ifpr.mcp_dorado.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenbankRecordTest {

    // verifica a leitura da localidade registrada nos qualificadores.
    @Test
    void recuperaLocalidadeDoGenbank() {
        GenbankRecord record = criarRegistro(Map.of("geo_loc_name", List.of("Brazil: Parana")));
        assertEquals("Brazil: Parana", record.geoLocName());
    }

    // verifica que as listas internas nao podem ser modificadas apos a criacao.
    @Test
    void protegeListaDeQualificadores() {
        List<String> values = new ArrayList<>(List.of("Brazil"));
        Map<String, List<String>> qualifiers = new LinkedHashMap<>();
        qualifiers.put("geo_loc_name", values);
        GenbankRecord record = criarRegistro(qualifiers);
        values.add("Other");
        assertEquals(List.of("Brazil"), record.qualifiers().get("geo_loc_name"));
        assertThrows(UnsupportedOperationException.class,
                () -> record.qualifiers().get("geo_loc_name").add("Other"));
    }

    // cria um registro minimo para os cenarios do teste.
    private GenbankRecord criarRegistro(Map<String, List<String>> qualifiers) {
        return new GenbankRecord("ABC123.1", "Cambeva castroi", "cambeva castroi", false,
                List.of(), Set.of(), Set.of(), qualifiers, 4L, 0L, 4L, null);
    }
}
