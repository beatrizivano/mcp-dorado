package br.edu.ifpr.mcp_dorado.service;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;

// índice compartilhado por todas as tools
public record Catalog(
    Path sample,
    Map<String,GenbankRecord> accessions,
    Map<String,Set<String>> accessionsBySpecies,
    Map<String,Set<String>> accessionsByGene,
    Map<String,Set<String>> accessionsByRiver,
    Map<String,Set<String>> accessionsByLocality,
    Map<String,Object> occurrenceContext,
    Map<String,Object> taxonomyContext,
    Map<String,Object> locationContext
) {}
