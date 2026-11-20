package br.edu.ifpr.mcp_dorado.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoordinateParserTest {

    // verifica converts southern and western coordinates.
    @Test
    void convertsSouthernAndWesternCoordinates() {
        var coordinates = CoordinateParser.parse("23.55 S 51.45 W").orElseThrow();

        assertEquals(-23.55, coordinates.latitude());
        assertEquals(-51.45, coordinates.longitude());
    }

    // verifica converts northern and eastern coordinates with separators.
    @Test
    void convertsNorthernAndEasternCoordinatesWithSeparators() {
        var coordinates = CoordinateParser.parse("12.5 N, 45.25 E").orElseThrow();

        assertEquals(12.5, coordinates.latitude());
        assertEquals(45.25, coordinates.longitude());
    }


    // verifica rejects out of range coordinates.
    @Test
    void rejectsOutOfRangeCoordinates() {
        assertTrue(CoordinateParser.parse("90.1 N 51 W").isEmpty());
        assertTrue(CoordinateParser.parse("23 S 180.1 E").isEmpty());
    }

}
