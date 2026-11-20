package br.edu.ifpr.mcp_dorado.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// coleta metadados do registro do genbank
public final class GenbankParser {
    // guarda os dados brutos extraidos de uma entrada.
    public record RawEntry(String accession, String organism, long sequenceLength,
                           long offset, long byteLength, Set<String> genes,
                           Map<String, List<String>> qualifiers) {}

    private static final Pattern LOCUS = Pattern.compile("^LOCUS\\s+\\S+\\s+(\\d+)\\s+bp\\b.*");
    private static final Pattern FIELD = Pattern.compile("^\\s*/([A-Za-z_]+)(?:=(.*))?$");
    private static final Pattern PRODUCT_12S = Pattern.compile("(?i)\\b12S\\s+(?:ribosomal\\s+RNA|rRNA)\\b");
    private static final Pattern PRODUCT_16S = Pattern.compile("(?i)\\b16S\\s+(?:ribosomal\\s+RNA|rRNA)\\b");
    private static final Set<String> REQUIRED = Set.of(
        "collection_date", "isolate", "db_xref", "geo_loc_name", "lat_lon", "specimen_voucher");

    // percorre o arquivo e entrega cada registro encontrado.
    public void scan(Path file, Consumer<RawEntry> consumer) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            
        		List<String> lines = null;
            long start = -1;
            String line;
            
            while (true) {
                long position = raf.getFilePointer();
                line = raf.readLine();
                
                if (line == null) {
                		break;
                }
                
                if (line.startsWith("LOCUS ")) {
                    if (lines != null) throw new IOException("Entry without // at byte " + start);
                    
                    start = position;
                    lines = new ArrayList<>();
                }
                
                if (lines == null) {
                    if (!line.isBlank()) throw new IOException("Text outside GenBank entry");
                    continue;
                }
                
                lines.add(line);
                
                if (line.equals("//")) {
                    long end = raf.getFilePointer();
                    consumer.accept(parseEntry(lines, start, end - start));
                    lines = null;
                    start = -1;
                }
            }
            
            if (lines != null) {
            		throw new IOException("Last entry lacks terminator //");
            }
        }
    }

    // extrai os campos e qualificadores de uma entrada genbank.
    private RawEntry parseEntry(List<String> lines, long offset, long length) throws IOException {
        
    		String accession = null, organism = null;
        long sequenceLength = -1;
        boolean features = false, sourceFeature = false;
        String activeKey = null;
        StringBuilder activeValue = new StringBuilder();
        Map<String, List<String>> source = new LinkedHashMap<>();
        Map<String, List<String>> other = new LinkedHashMap<>();
        Set<String> genes = new LinkedHashSet<>();
        
        for (String line : lines) {
            if (line.startsWith("LOCUS ")) {
                Matcher m = LOCUS.matcher(line);
                
                if (m.matches()) {
                		sequenceLength = Long.parseLong(m.group(1));
                }
            
            } else if (line.startsWith("VERSION ")) {
            		accession = line.substring(12).trim().split("\\s+")[0];
            
            } else if (line.startsWith("  ORGANISM  ")) {
            		organism = line.substring(12).trim();
            }
            
            if (line.startsWith("FEATURES ")) { 
            		features = true; 
            		continue; 
            	}
            
            if (features && (line.startsWith("ORIGIN") || line.startsWith("CONTIG") || line.equals("//"))) {
                if (activeKey != null) add(activeKey, activeValue.toString(), sourceFeature, source, other, genes);
                activeKey = null; features = false;
            }	
            
            if (!features) {
            		continue;
            }
            
            if (line.length() > 5 && line.startsWith("     ") && line.charAt(5) != ' ' && line.charAt(5) != '/') {
                if (activeKey != null) {
                		add(activeKey, activeValue.toString(), sourceFeature, source, other, genes);
                }
                
                activeKey = null;
                sourceFeature = line.substring(5).stripLeading().startsWith("source ");
                continue;
            }
            
            Matcher m = FIELD.matcher(line);
            
            if (m.matches() && line.startsWith("                     /")) {
                if (activeKey != null) {
                		add(activeKey, activeValue.toString(), sourceFeature, source, other, genes);
                }
                
                activeKey = m.group(1);
                activeValue = new StringBuilder(m.group(2) == null ? "" : m.group(2).trim());
            
            } else if (activeKey != null && line.startsWith("                     ")) {
                String continuation = line.strip();
                if (!continuation.isEmpty()) {
                		activeValue.append(' ').append(continuation);
                }
            }
        }
        
        if (accession == null || organism == null || sequenceLength < 0) {
            throw new IOException("Invalid entry at offset " + offset + ": missing VERSION, ORGANISM or LOCUS bp");
        }
        
        Map<String, List<String>> selected = new LinkedHashMap<>();
        
        for (String k : REQUIRED) {
            List<String> vals = source.getOrDefault(k, other.getOrDefault(k, List.of()));
            
            if (!vals.isEmpty()) {
            		selected.put(k, List.copyOf(vals));
            }
        }
        
        return new RawEntry(accession, organism, sequenceLength, offset, length, Set.copyOf(genes), Map.copyOf(selected));
    }
    
    // adiciona um qualificador a lista de valores.
    private void add(String key, String raw, boolean sourceFeature,
                     Map<String,List<String>> source, Map<String,List<String>> other,
                     Set<String> genes) {
        String value = raw.strip();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
        		value = value.substring(1, value.length() - 1);
        }
        
        if (REQUIRED.contains(key)) {
            (sourceFeature ? source : other).computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        
        if (key.equals("gene")) {
            String normalized = value.toUpperCase(Locale.ROOT).replace("-", "");
            
            if (normalized.matches("12S|RNR1|MT-RNR1")) {
            		genes.add("12S");
            }
            if (normalized.matches("16S|RNR2|MT-RNR2")) {
            		genes.add("16S");
            }
            if (normalized.matches("COI|CO1|COX1|MTCO1|MT-CO1")) {
            		genes.add("COI");
            }
        }
        
        if (key.equals("product")) {
            
        		if (PRODUCT_12S.matcher(value).find()) {
        			genes.add("12S");
        		}
        		
            if (PRODUCT_16S.matcher(value).find()) {
            		genes.add("16S");
            }
            
            String v = value.toLowerCase(Locale.ROOT);
            
            if (v.contains("cytochrome c oxidase subunit i") || v.contains("cytochrome oxidase subunit 1")) {
            		genes.add("COI");
            }
        }
    }
}
