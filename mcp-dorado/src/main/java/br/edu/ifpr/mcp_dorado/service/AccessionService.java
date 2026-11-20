package br.edu.ifpr.mcp_dorado.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpr.mcp_dorado.model.Coordinates;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;
import br.edu.ifpr.mcp_dorado.repository.GenbankRepository;

/** consulta um accession do indice e organiza seus dados para a ferramenta mcp. */
public final class AccessionService {

    private final GenbankRepository repository;

    // recebe o repositorio usado para obter registros.
    public AccessionService(GenbankRepository repository) {
        this.repository = repository;
    }

    // monta os metadados da consulta de um accession.
    public Optional<Map<String, Object>> consultar(String accession) {
        if (accession == null || accession.isBlank()) {
            return Optional.empty();
        }

        Optional<GenbankRecord> found = repository.find(accession.strip());

        if (found.isEmpty()) {
            return Optional.empty();
        }

        GenbankRecord record = found.get();
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("accession", record.accession());
        result.put("organism", record.organism());
        result.put("normalized_species", record.normalizedSpecies());
        result.put("taxonomically_ambiguous", record.taxonomicallyAmbiguous());
        result.put("taxonomy_candidates", sorted(record.taxonomyCandidates()));
        result.put("rivers", sorted(new ArrayList<>(record.rivers())));
        result.put("genes", sorted(new ArrayList<>(record.genes())));
        result.put("sequence_length", record.sequenceLength());
        result.put("geo_loc_name", record.geoLocName());
        result.put("qualifiers", record.qualifiers());

        Coordinates coordinates = record.coordinates();

        if (coordinates == null) {
            result.put("coordinates", null);
        } else {
            Map<String, Double> coordinateValues = new LinkedHashMap<>();
            coordinateValues.put("latitude", coordinates.latitude());
            coordinateValues.put("longitude", coordinates.longitude());
            result.put("coordinates", coordinateValues);
        }

        return Optional.of(result);
    }

    // ordena os valores para apresentar uma resposta estavel.
    private List<String> sorted(List<String> values) {
        List<String> result = new ArrayList<>(values);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }
}
