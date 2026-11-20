package br.edu.ifpr.mcp_dorado.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import br.edu.ifpr.mcp_dorado.model.Coordinates;

// faz o parsing de coordenadas decimais com indicadores n/s/e/w do qualificador lat_lon do registro do genbank
public final class CoordinateParser {
    private static final Pattern PAIR = Pattern.compile(
        "^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*[°º]?\\s*([NS])\\s*[,; ]+\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*[°º]?\\s*([EW])\\s*$", Pattern.CASE_INSENSITIVE);
    // impede a criacao de instancias do utilitario.
    private CoordinateParser() {}
    
    // converte coordenadas textuais em latitude e longitude decimais.
    public static Optional<Coordinates> parse(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        
        Matcher match = PAIR.matcher(text);
        
        if (!match.matches()) return Optional.empty();
        
        try {
            double lat = Math.abs(Double.parseDouble(match.group(1)));
            double lon = Math.abs(Double.parseDouble(match.group(3)));
            
            if (lat > 90 || lon > 180) {
            		return Optional.empty();
            }
           
            if (match.group(2).equalsIgnoreCase("S")) {
            		lat = -lat;
            }
            
            if (match.group(4).equalsIgnoreCase("W")) {
            		lon = -lon;
            }
            
            return Optional.of(new Coordinates(lat, lon));
        
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
