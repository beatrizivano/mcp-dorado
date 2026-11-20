package br.edu.ifpr.mcp_dorado.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenbankParserTest {

    @TempDir
    Path tempDirectory;

    // verifica reads multiple entries and their byte offsets.
    @Test
    void readsMultipleEntriesAndTheirByteOffsets() throws Exception {
        Path file = writeSample(validEntry("ABC123.1") + validEntry("DEF456.1"));
        List<GenbankParser.RawEntry> records = scan(file);

        assertEquals(2, records.size());
        assertEquals("ABC123.1", records.get(0).accession());
        assertEquals("DEF456.1", records.get(1).accession());
        assertEquals(records.get(0).offset() + records.get(0).byteLength(), records.get(1).offset());

        try (RandomAccessFile reader = new RandomAccessFile(file.toFile(), "r")) {
            reader.seek(records.get(1).offset());
            assertTrue(reader.readLine().startsWith("LOCUS"));
        }
    }

    // verifica prioritizes source qualifiers and preserves repeated values.
    @Test
    void prioritizesSourceQualifiersAndPreservesRepeatedValues() throws Exception {
        String entry = validEntry("ABC123.1")
                .replace("ORIGIN\n", "     misc_feature    20..50\n"
                        + "                     /geo_loc_name=\"Other locality\"\nORIGIN\n")
                .replace("                     /geo_loc_name=\"Brazil: Parana\"\n",
                        "                     /geo_loc_name=\"Brazil: Parana\"\n"
                        + "                     /db_xref=\"taxon:123\"\n"
                        + "                     /db_xref=\"BioSample:XYZ\"\n");

        var record = scan(writeSample(entry)).get(0);

        assertEquals(List.of("Brazil: Parana"), record.qualifiers().get("geo_loc_name"));
        assertEquals(List.of("taxon:123", "BioSample:XYZ"), record.qualifiers().get("db_xref"));
        assertEquals(List.of("23.55 S 51.45 W"), record.qualifiers().get("lat_lon"));
        assertFalse(record.qualifiers().containsKey("product"));
    }

    // verifica joins multiline qualifiers and detects genes.
    @Test
    void joinsMultilineQualifiersAndDetectsGenes() throws Exception {
        String entry = validEntry("ABC123.1")
                .replace("                     /geo_loc_name=\"Brazil: Parana\"",
                        "                     /geo_loc_name=\"Brazil:\n"
                        + "                     Parana\"")
                .replace("ORIGIN\n", "     gene            1..100\n"
                        + "                     /gene=\"COX1\"\n"
                        + "     rRNA            5..40\n"
                        + "                     /product=\"16S ribosomal RNA\"\nORIGIN\n");

        var record = scan(writeSample(entry)).get(0);

        assertEquals(List.of("Brazil: Parana"), record.qualifiers().get("geo_loc_name"));
        assertTrue(record.genes().containsAll(List.of("12S", "16S", "COI")));
        assertEquals(150L, record.sequenceLength());
    }

    // verifica accepts contig records without origin.
    @Test
    void acceptsContigRecordsWithoutOrigin() throws Exception {
        String entry = validEntry("ABC123.1")
                .replace("ORIGIN\n        1 atgc\n", "CONTIG      join(ABC123:1..150)\n");

        assertEquals(1, scan(writeSample(entry)).size());
    }

    // verifica rejects entry missing required identification.
    @Test
    void rejectsEntryMissingRequiredIdentification() throws Exception {
        Path file = writeSample(validEntry("ABC123.1").replace("VERSION     ABC123.1\n", ""));

        assertThrows(IOException.class, () -> scan(file));
    }

    // verifica rejects entry without terminator.
    @Test
    void rejectsEntryWithoutTerminator() throws Exception {
        Path file = writeSample(validEntry("ABC123.1").replace("//\n", ""));

        assertThrows(IOException.class, () -> scan(file));
    }

    // prepara os dados auxiliares usados nos testes.
    private Path writeSample(String content) throws IOException {
        Path file = tempDirectory.resolve("sample.gb");
        Files.writeString(file, content);
        return file;
    }

    // prepara os dados auxiliares usados nos testes.
    private List<GenbankParser.RawEntry> scan(Path file) throws IOException {
        List<GenbankParser.RawEntry> records = new ArrayList<>();
        new GenbankParser().scan(file, records::add);
        return records;
    }

    // prepara os dados auxiliares usados nos testes.
    private String validEntry(String accession) {
        return "LOCUS       ABC123       150 bp    DNA     linear   PLN 01-JAN-2025\n"
                + "VERSION     " + accession + "\n"
                + "  ORGANISM  Cambeva castroi\n"
                + "FEATURES             Location/Qualifiers\n"
                + "     source          1..150\n"
                + "                     /geo_loc_name=\"Brazil: Parana\"\n"
                + "                     /lat_lon=\"23.55 S 51.45 W\"\n"
                + "     rRNA            1..100\n"
                + "                     /product=\"12S ribosomal RNA\"\n"
                + "ORIGIN\n"
                + "        1 atgc\n"
                + "//\n";
    }
}
