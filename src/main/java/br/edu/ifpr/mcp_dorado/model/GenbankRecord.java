package br.edu.ifpr.mcp_dorado.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//metadados de registro do genbank
// copia as colecoes recebidas para proteger os metadados do registro.
public record GenbankRecord(
    String accession,
    String organism,
    String normalizedSpecies,
    boolean taxonomicallyAmbiguous,
    List<String> taxonomyCandidates,
    Set<String> rivers,
    Set<String> genes,
    Map<String, List<String>> qualifiers,
    long sequenceLength,
    long offset,
    long byteLength,
    Coordinates coordinates
) {
    public GenbankRecord {
        taxonomyCandidates = List.copyOf(taxonomyCandidates);
        rivers = Set.copyOf(rivers);
        genes = Set.copyOf(genes);
        Map<String, List<String>> copy = new LinkedHashMap<>();

        qualifiers.forEach((name, values) -> {
            copy.put(name, List.copyOf(values));
        });

        qualifiers = Map.copyOf(copy);
    }
    
    // retorna a primeira localidade informada no registro.
    public String geoLocName() {
        return qualifiers.getOrDefault("geo_loc_name", List.of()).stream().findFirst().orElse(null);
    }
}
