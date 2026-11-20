package br.edu.ifpr.mcp_dorado.mcp.utils;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/** exibe os dados de um accession em um formato textual inspirado no genbank. */
public final class RegistroFormatter {

    private final JsonMapper mapper = new JsonMapper();

    // seleciona uma apresentacao conforme o resultado da ferramenta.
    public String format(String toolName, String json) {
        try {
            Map<?, ?> response = mapper.readValue(json, Map.class);

            if ("consultar_accession".equals(toolName) && response.containsKey("accession")) {
                return formatRecord(response);
            }

            if ("consultar_registros".equals(toolName)
                    && "listar".equals(response.get("operacao"))) {
                return formatList(response);
            }

            return formatSummary(response);
        } catch (Exception exception) {
            // textos de erro e respostas nao json permanecem intactos.
            return json;
        }
    }

    // organiza os argumentos enviados pelo usuario.
    public String formatArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "Nenhum parametro necessario.";
        }

        StringBuilder text = new StringBuilder();

        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            text.append(label(entry.getKey())).append(": ")
                    .append(displayValue(entry.getValue())).append("\n");
        }

        return text.toString().trim();
    }

    // apresenta objetos json em secoes legiveis.
    public String formatSummary(Map<?, ?> response) {
        StringBuilder text = new StringBuilder();

        for (Map.Entry<?, ?> entry : response.entrySet()) {
            String key = String.valueOf(entry.getKey());

            // parametros ja sao mostrados antes do resultado, no cliente.
            if ("operacao".equals(key) || "filtros".equals(key)
                    || "consulta".equals(key) || "agrupar_por".equals(key)
                    || "rio".equals(key) || "rios".equals(key)) {
                continue;
            }

            appendField(text, key, entry.getValue(), 0);
        }

        if (text.isEmpty()) {
            return "Nenhum resultado encontrado.";
        }

        return text.toString().trim();
    }

    // acrescenta um campo formatado ao texto.
    private void appendField(StringBuilder text, String key, Object value, int level) {
        String indent = "  ".repeat(level);
        String title = label(key);

        if (value instanceof Map<?, ?> map) {
            text.append(indent).append(title).append(":\n");

            if (map.isEmpty()) {
                text.append(indent).append("  Nenhum resultado.\n");
            }

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                appendField(text, String.valueOf(entry.getKey()), entry.getValue(), level + 1);
            }
        } else if (value instanceof List<?> list) {
            text.append(indent).append(title).append(":");

            if (list.isEmpty()) {
                text.append(" nenhum\n");
            } else if (list.stream().allMatch(item -> !(item instanceof Map<?, ?>)
                    && !(item instanceof List<?>))) {
                text.append(" ").append(join(list)).append("\n");
            } else {
                text.append("\n");

                for (Object item : list) {
                    if (item instanceof Map<?, ?> entry) {
                        for (Map.Entry<?, ?> field : entry.entrySet()) {
                            appendField(text, String.valueOf(field.getKey()), field.getValue(), level + 1);
                        }
                        text.append("\n");
                    } else {
                        text.append(indent).append("  - ").append(displayValue(item)).append("\n");
                    }
                }
            }
        } else {
            text.append(indent).append(title).append(": ")
                    .append(displayValue(value)).append("\n");
        }
    }

    // converte um valor de resposta em texto.
    private String displayValue(Object value) {
        if (value instanceof List<?>) {
            return join(value);
        }

        return value(value);
    }

    // torna o nome de um campo mais legivel.
    private String label(String name) {
        String[] parts = name.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return result.toString();
    }

    // monta a visualizacao de um registro estilo genbank.
    public String formatRecord(Map<?, ?> record) {
        StringBuilder text = new StringBuilder();
        String accession = value(record.get("accession"));
        String organism = value(record.get("organism"));

        text.append("LOCUS       ").append(accession)
                .append("  ").append(value(record.get("sequence_length"))).append(" bp\n");
        text.append("ACCESSION   ").append(accession).append("\n");
        text.append("ORGANISM    ").append(organism).append("\n");
        text.append("SPECIES     ").append(value(record.get("normalized_species"))).append("\n");

        if (Boolean.TRUE.equals(record.get("taxonomically_ambiguous"))) {
            text.append("TAXONOMY    Nome ambiguo; candidatos: ")
                    .append(join(record.get("taxonomy_candidates"))).append("\n");
        }

        text.append("GENES       ").append(join(record.get("genes"))).append("\n");
        text.append("OCCURRENCE  ").append(join(record.get("rivers"))).append("\n");
        text.append("LOCALITY    ").append(value(record.get("geo_loc_name"))).append("\n");

        Object coords = record.get("coordinates");

        if (coords instanceof Map<?, ?> coordinates) {
            text.append("COORDINATES ")
                    .append(value(coordinates.get("latitude"))).append(", ")
                    .append(value(coordinates.get("longitude"))).append("\n");
        } else {
            text.append("COORDINATES nao informado\n");
        }

        text.append("FEATURES    Qualificadores informados no GenBank\n");

        if (record.get("qualifiers") instanceof Map<?, ?> qualifiers) {
            for (Map.Entry<?, ?> entry : qualifiers.entrySet()) {
                text.append("            /").append(entry.getKey()).append("=")
                        .append(join(entry.getValue())).append("\n");
            }
        }

        text.append("//");
        return text.toString();
    }

    // organiza a apresentacao de uma lista de registros.
    public String formatList(Map<?, ?> response) {
        StringBuilder text = new StringBuilder();
        text.append("REGISTROS GENBANK (AMOSTRA)\n");
        text.append("TOTAL       ").append(value(response.get("total"))).append("\n");
        text.append("EXIBIDOS    ").append(value(response.get("exibidos"))).append("\n");
        text.append("----------------------------------------\n");

        if (response.get("registros") instanceof List<?> records) {
            for (Object recordValue : records) {
                if (!(recordValue instanceof Map<?, ?> record)) {
                    continue;
                }

                text.append("ACCESSION   ").append(value(record.get("accession"))).append("\n");
                text.append("ORGANISM    ").append(value(record.get("especie"))).append("\n");
                text.append("GENES       ").append(join(record.get("genes"))).append("\n");
                text.append("LOCALITY    ").append(value(record.get("localidade"))).append("\n");
                text.append("//\n");
            }
        }

        if (Boolean.TRUE.equals(response.get("truncado"))) {
            text.append("OBSERVACAO  Lista limitada aos primeiros registros.\n");
        }

        return text.toString().trim();
    }

    // junta os valores de uma colecao para exibicao.
    private String join(Object value) {
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                return "nao informado";
            }

            StringBuilder text = new StringBuilder();

            for (Object item : list) {
                if (!text.isEmpty()) {
                    text.append(", ");
                }

                text.append(value(item));
            }

            return text.toString();
        }

        return value(value);
    }

    // obtem um campo de um objeto json.
    private String value(Object object) {
        if (object == null) {
            return "nao informado";
        }

        String value = String.valueOf(object);

        if (value.isBlank()) {
            return "nao informado";
        }

        return value;
    }
}
