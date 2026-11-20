package br.edu.ifpr.mcp_dorado.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;
import br.edu.ifpr.mcp_dorado.parser.CoordinateParser;
import br.edu.ifpr.mcp_dorado.parser.GenbankParser;
import tools.jackson.databind.json.JsonMapper;

// cria um catálogo na memória antes da conexão com o client
public final class DataInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);
    private final JsonMapper mapper = new JsonMapper();

    // le a amostra e cria os indices utilizados pelas consultas.
    public Catalog initialize(Path sample) throws IOException {
        Map<String,Object> occurrence = readContext("occurrence.json");
        Map<String,Object> taxonomy = readContext("taxonomy.json");
        Map<String,Object> locations = readContext("locations.json");
        Map<String,Object> species = map(occurrence.get("species"));
        Map<String,Object> ambiguous = map(occurrence.get("ambiguous_names"));
        Map<String,GenbankRecord> records = new LinkedHashMap<>();
        Map<String,Set<String>> bySpecies = new LinkedHashMap<>();
        Map<String,Set<String>> byGene = new LinkedHashMap<>();
        Map<String,Set<String>> byRiver = new LinkedHashMap<>();
        Map<String,Set<String>> byLocality = new LinkedHashMap<>();
        
        if (!Files.isRegularFile(sample)) {
        		throw new IOException("GenBank sample not found: " + sample);
        }
        
        long started = System.nanoTime();
        
        try {
            new GenbankParser().scan(sample, raw -> {
                
            		if (records.containsKey(raw.accession())) {
                    throw new IllegalStateException("Duplicate accession: " + raw.accession());
                }
                
                String original = normalize(raw.organism());
                Map<String,Object> taxon = map(taxonomy.get(original));
                boolean taxAmbiguous = Boolean.TRUE.equals(taxon.get("ambiguous")) || Integer.valueOf(1).equals(taxon.get("ambiguous"));
                String validName = taxAmbiguous ? original : normalize((String) taxon.getOrDefault("valid_name", original));
                Map<String,Object> unresolved = map(ambiguous.get(original));
                
                if (unresolved.isEmpty()) {
                		unresolved = map(ambiguous.get(validName));
                }
                
                taxAmbiguous = taxAmbiguous || !unresolved.isEmpty();
                List<String> candidates = strings(unresolved.get("candidates"));
                
                if (candidates.isEmpty() && taxAmbiguous) {
                		candidates = new ArrayList<>(map(taxon.get("candidates")).keySet());
                }
                    
                Map<String,Object> occurrenceEntry = map(species.get(validName));
                
                // para nomes de espécie ambíguos não usa nomes de candidatos
                Set<String> rivers = new LinkedHashSet<>(strings((taxAmbiguous ? unresolved : occurrenceEntry).get("rivers")));
                String latLon = raw.qualifiers().getOrDefault("lat_lon", List.of()).stream().findFirst().orElse(null);
                
                GenbankRecord record = new GenbankRecord(
                		raw.accession(),
                		raw.organism(),
                		validName,
                    taxAmbiguous,
                    candidates,
                    rivers,
                    raw.genes(),
                    raw.qualifiers(),
                    raw.sequenceLength(),
                    raw.offset(),
                    raw.byteLength(),
                    CoordinateParser.parse(latLon).orElse(null)
                );
                
                records.put(record.accession(), record);
                add(bySpecies, validName, record.accession());
                record.genes().forEach(g -> add(byGene, g, record.accession()));
                record.rivers().forEach(r -> add(byRiver, r, record.accession()));
                
                if (record.geoLocName() != null) {
                		add(byLocality, normalize(record.geoLocName()), record.accession());
                }                    
            });
            
        } catch (IllegalStateException ex) {
            throw new IOException("Failed indexing GenBank: " + ex.getMessage(), ex);
        }
        
        if (records.isEmpty()) {
        		throw new IOException("GenBank sample contains no records");
        }
        
        Catalog catalog = new Catalog(
        		sample, 
        		Map.copyOf(records), 
        		freeze(bySpecies),
            freeze(byGene), 
            freeze(byRiver), 
            freeze(byLocality), 
            occurrence, 
            taxonomy, 
            locations
        );
        
        LOG.info("Indexed {} accessions, {} species, {} genes, {} locality values in {} ms",
            records.size(), 
            bySpecies.size(), 
            byGene.size(), 
            byLocality.size(),
            (System.nanoTime() - started) / 1_000_000
        );
        
        return catalog;
    }
    
    // carrega um arquivo json de contexto do projeto.
    private Map<String,Object> readContext(String filename) throws IOException {
        try (InputStream in = DataInitializer.class.getResourceAsStream("/context/" + filename)) {
            if (in == null) {
            		throw new IOException("Missing static context: " + filename);
            }
            
            @SuppressWarnings("unchecked")
            Map<String,Object> result = mapper.readValue(in, Map.class);
            
            return Collections.unmodifiableMap(result);
            
        } catch (RuntimeException ex) {
            throw new IOException("Invalid context JSON " + filename, ex);
        }
    }
    
    // padroniza nomes para busca e comparacao.
    private static String normalize(String name) {
        if (name == null) {
        		return "";
        }
        
        return Normalizer
        		.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .strip();
    }
    
    @SuppressWarnings("unchecked")
    // converte um valor generico em mapa de contexto.
    private static Map<String,Object> map(Object value) {
        return value instanceof Map<?,?> ? (Map<String,Object>)value : Map.of();
    }
    
    // extrai uma lista de textos do contexto.
    private static List<String> strings(Object value) {
        if (!(value instanceof Collection<?> values)) {
        		return List.of();
        }
        
        return values.stream().map(String::valueOf).toList();
    }
    
    // associa um accession a uma chave do indice.
    private static void add(Map<String,Set<String>> m, String key, String accession) {
        m.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(accession);
    }
    
    // cria uma copia imutavel dos indices construidos.
    private static Map<String,Set<String>> freeze(Map<String,Set<String>> source) {
        Map<String,Set<String>> result = new LinkedHashMap<>();
        source.forEach((k,v) -> result.put(k,Set.copyOf(v)));
        return Map.copyOf(result);
    }
}
