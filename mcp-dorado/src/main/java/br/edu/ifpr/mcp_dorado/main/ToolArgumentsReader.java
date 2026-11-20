package br.edu.ifpr.mcp_dorado.main;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.modelcontextprotocol.spec.McpSchema.Tool;

/** le argumentos conforme o esquema json anunciado pela ferramenta mcp. */
public final class ToolArgumentsReader {

    private final Scanner scanner;
    private final PrintStream output;

    // recebe os canais usados para leitura e exibicao.
    public ToolArgumentsReader(Scanner scanner, PrintStream output) {
        this.scanner = scanner;
        this.output = output;
    }

    // le os argumentos conforme o esquema da ferramenta.
    public Map<String, Object> read(Tool tool) {
        Map<String, Object> schema = tool.inputSchema();

        if (schema == null) {
            return new LinkedHashMap<>();
        }

        // oneof declara alternativas. o usuario escolhe primeiro a operacao;
        // depois o cliente pergunta somente pelos campos daquela alternativa.
        if (schema.get("oneOf") instanceof List<?> alternatives && !alternatives.isEmpty()) {
            return readAlternative(alternatives);
        }

        return readProperties(schema, new LinkedHashMap<>());
    }

    // identifica a operacao e le apenas os campos correspondentes.
    private Map<String, Object> readAlternative(List<?> alternatives) {
        List<String> operations = new ArrayList<>();
        Map<String, Map<?, ?>> schemas = new LinkedHashMap<>();

        for (Object alternative : alternatives) {
            if (!(alternative instanceof Map<?, ?> schema)) {
                throw new IllegalArgumentException("Alternativa do esquema MCP invalida.");
            }

            Object propertiesValue = schema.get("properties");

            if (!(propertiesValue instanceof Map<?, ?> properties)) {
                throw new IllegalArgumentException("Alternativa sem propriedades.");
            }

            Object selectorValue = properties.get("operacao");

            if (!(selectorValue instanceof Map<?, ?> selector)) {
                throw new IllegalArgumentException("Alternativa sem discriminador operacao.");
            }

            Object fixedValue = selector.get("const");

            if (!(fixedValue instanceof String operation)) {
                throw new IllegalArgumentException("Alternativa sem valor fixo da operacao.");
            }

            operations.add(operation);
            schemas.put(operation, schema);
        }

        output.println();
        output.println("Escolha a operacao:");

        for (int index = 0; index < operations.size(); index++) {
            output.println((index + 1) + " - " + operations.get(index));
        }

        String selectedOperation = readOperation(operations);
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("operacao", selectedOperation);
        Map<?, ?> selectedSchema = schemas.get(selectedOperation);

        return readProperties(selectedSchema, arguments);
    }

    // valida a escolha de uma operacao pelo usuario
    private String readOperation(List<String> operations) {
        while (true) {
            output.print("Opcao: ");

            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Entrada encerrada durante a leitura da operacao.");
            }

            String input = scanner.nextLine().trim();

            try {
                int index = Integer.parseInt(input) - 1;

                if (index >= 0 && index < operations.size()) {
                    return operations.get(index);
                }
            } catch (NumberFormatException exception) {
                // o usuario tambem pode digitar o nome da operacao.
            }

            for (String operation : operations) {
                if (operation.equalsIgnoreCase(input)) {
                    return operation;
                }
            }

            output.println("Operacao invalida. Escolha um numero ou nome da lista.");
        }
    }

    // percorre os parametros definidos no esquema selecionado.
    private Map<String, Object> readProperties(Map<?, ?> schema, Map<String, Object> arguments) {
        Object propertiesValue = schema.get("properties");

        if (!(propertiesValue instanceof Map<?, ?> properties)) {
            return arguments;
        }

        List<?> required = List.of();

        if (schema.get("required") instanceof List<?> requiredProperties) {
            required = requiredProperties;
        }

        if (!properties.isEmpty()) {
            output.println();
            output.println("Parametros (Enter para ignorar os opcionais):");
        }

        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> property)) {
                continue;
            }

            String name = String.valueOf(entry.getKey());

            if (arguments.containsKey(name)) {
                continue;
            }

            String type = getText(property, "type", "string");
            String description = getText(property, "description", "");
            boolean isRequired = required.contains(name);
            Object value = readValue(name, type, description, isRequired, property);

            if (value != null) {
                arguments.put(name, value);
            }
        }

        return arguments;
    }

    // solicita e valida o valor de um parametro
    private Object readValue(String name, String type, String description,
            boolean required, Map<?, ?> property) {
        while (true) {
            String status = required ? "obrigatorio" : "opcional";
            output.println();
            output.println(name + " (" + describeType(type) + ", " + status + ")");

            if (!description.isBlank()) {
                String example = extractExample(description);

                if (!example.isBlank()) {
                    output.println("Exemplo: " + example);
                } else {
                    output.println(description);
                }
            }

            if ("array".equals(type)) {
                output.println("Separe os valores por virgula.");
            }

            if (property.get("enum") instanceof List<?> options && !options.isEmpty()) {
                output.println("Opcoes: " + String.join(", ", options.stream()
                        .map(String::valueOf).toList()));
            }

            output.print(name + ": ");

            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Entrada encerrada durante a leitura dos parametros.");
            }

            String text = scanner.nextLine().trim();

            if (text.isBlank()) {
                if (!required) {
                    return null;
                }

                output.println("Este parametro e obrigatorio.");
                continue;
            }

            try {
                Object value = convert(text, type);
                validateOptions(value, property);
                validateArray(value, property);
                return value;
            } catch (IllegalArgumentException exception) {
                output.println("Valor invalido: " + exception.getMessage());
            }
        }
    }

    // obtém um exemplo curto para preencher parametro
    private static String extractExample(String description) {
        int position = description.indexOf("Exemplo:");

        if (position < 0) {
            return "";
        }

        String example = description.substring(position + "Exemplo:".length()).trim();

        if (example.endsWith(".")) {
            example = example.substring(0, example.length() - 1);
        }

        return example;
    }

    // traduz o tipo do esquema
    private static String describeType(String type) {
        return switch (type) {
            case "string" -> "texto";
            case "integer" -> "numero inteiro";
            case "number" -> "numero";
            case "boolean" -> "sim/nao";
            case "array" -> "lista";
            default -> type;
        };
    }

    // converte o texto para o tipo requerido pela ferramenta
    static Object convert(String text, String type) {
        try {
            switch (type) {
                case "string":
                    return text;
                case "integer":
                    return Integer.valueOf(text);
                case "number":
                    return Double.valueOf(text);
                case "boolean":
                    return parseBoolean(text);
                case "array":
                    return parseArray(text);
                default:
                    throw new IllegalArgumentException("Tipo nao suportado: " + type);
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um numero valido.");
        }
    }

    // separa uma entrada textual em elementos de uma lista
    static List<String> parseArray(String text) {
        String content = text.trim();

        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1);
        }

        List<String> values = new ArrayList<>();

        for (String part : content.split(",", -1)) {
            String value = part.trim();

            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }

            if (value.isBlank()) {
                throw new IllegalArgumentException("A lista nao pode conter valores vazios.");
            }

            values.add(value);
        }

        return values;
    }

    // interpreta respostas boolean
    private static boolean parseBoolean(String text) {
        if ("true".equalsIgnoreCase(text) || "sim".equalsIgnoreCase(text)) {
            return true;
        }

        if ("false".equalsIgnoreCase(text) || "nao".equalsIgnoreCase(text)
                || "não".equalsIgnoreCase(text)) {
            return false;
        }

        throw new IllegalArgumentException("Informe sim/nao ou true/false.");
    }

    // confere se o valor esta entre opcoes permitidas
    private static void validateOptions(Object value, Map<?, ?> property) {
        if (property.get("enum") instanceof List<?> options && !options.contains(value)) {
            throw new IllegalArgumentException("Use uma das opcoes: " + options);
        }
    }

    // confere o tamanho e unicidade dos itens da lista
    private static void validateArray(Object value, Map<?, ?> property) {
        if (!(value instanceof List<?> values)) {
            return;
        }

        Object minItems = property.get("minItems");

        if (minItems instanceof Number minimum && values.size() < minimum.intValue()) {
            throw new IllegalArgumentException("Informe pelo menos " + minimum.intValue() + " valores.");
        }

        if (Boolean.TRUE.equals(property.get("uniqueItems"))
                && values.stream().distinct().count() != values.size()) {
            throw new IllegalArgumentException("Nao repita valores na lista.");
        }
    }

    // recupera um campo textual ou valor padrao
    private static String getText(Map<?, ?> property, String name, String defaultValue) {
        Object value = property.get(name);

        if (value == null) {
            return defaultValue;
        }

        return String.valueOf(value);
    }
}
