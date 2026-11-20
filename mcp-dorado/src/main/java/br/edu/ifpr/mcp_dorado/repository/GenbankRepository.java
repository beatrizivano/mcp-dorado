package br.edu.ifpr.mcp_dorado.repository;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import br.edu.ifpr.mcp_dorado.model.GenbankRecord;
import br.edu.ifpr.mcp_dorado.service.Catalog;

// faz o fetch em registro único por byte range, sem olhar toda a amostra novamente
public final class GenbankRepository {
    private final Catalog catalog;
    // recebe o catalogo usado para consultar os registros.
    public GenbankRepository(Catalog catalog) { 
    		this.catalog = catalog; 
    	}
    
    // busca os metadados de um registro pelo accession.
    public Optional<GenbankRecord> find(String accession) {
        return Optional.ofNullable(catalog.accessions().get(accession));
    }
    
    // le o trecho original do arquivo usando a posicao em bytes.
    public Optional<String> originalEntry(String accession) throws IOException {
        GenbankRecord record = catalog.accessions().get(accession);
        
        if (record == null) {
        		return Optional.empty();
        }
        
        if (record.byteLength() > Integer.MAX_VALUE) {
        		throw new IOException("Entry too large to read");
        }
        
        byte[] bytes = new byte[(int)record.byteLength()];
        
        try (RandomAccessFile raf = new RandomAccessFile(catalog.sample().toFile(), "r")) {
            raf.seek(record.offset());
            raf.readFully(bytes);
        }
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }
}
