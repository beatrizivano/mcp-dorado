package br.edu.ifpr.mcp_dorado.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;
import br.edu.ifpr.mcp_dorado.mcp.utils.RegistroFormatter;

/** cliente interativo que descobre e executa as ferramentas anunciadas pelo servidor mcp. */
public final class McpDoradoClient {

    private static final Logger LOG = LoggerFactory.getLogger(McpDoradoClient.class);
    private final McpSyncClient client;
    private final Scanner scanner;
    private final RegistroFormatter formatter = new RegistroFormatter();

    // cria o cliente conectado ao transporte do servidor.
    public McpDoradoClient(Path serverJar, Scanner scanner) {
        this.scanner = scanner;
        this.client = createClient(serverJar);
    }

    // configura o processo java e o transporte stdio.
    private McpSyncClient createClient(Path serverJar) {
        if (!Files.isRegularFile(serverJar)) {
            throw new IllegalArgumentException("JAR do servidor nao encontrado: " + serverJar);
        }

        String jarPath = serverJar.toAbsolutePath().normalize().toString();
        LOG.info("Servidor MCP: {}", jarPath);

        ServerParameters parameters = ServerParameters.builder("java")
                .args("-jar", jarPath)
                .build();

        JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(new JsonMapper());
        StdioClientTransport transport = new StdioClientTransport(parameters, mapper);
        transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] {}", line));

        return McpClient.sync(transport)
                .loggingConsumer(this::onLoggingMessage)
                .build();
    }

    // registra as notificacoes de log enviadas pelo servidor.
    private void onLoggingMessage(LoggingMessageNotification notification) {
        LOG.info("[NOTIFICACAO MCP] {}: {}", notification.level(), notification.data());
    }

    // inicia a conexao e o menu interativo de ferramentas.
    public void execute() {
        client.initialize();
        client.setLoggingLevel(LoggingLevel.INFO);
        List<Tool> tools = client.listTools().tools();
        ToolArgumentsReader reader = new ToolArgumentsReader(scanner, System.out);

        while (true) {
            showMenu(tools);
            System.out.print("Escolha: ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String command = scanner.nextLine().trim();

            if ("sair".equalsIgnoreCase(command)) {
                break;
            }

            if ("listar".equalsIgnoreCase(command)) {
                tools = client.listTools().tools();
                continue;
            }

            Tool selected = selectTool(tools, command);

            if (selected == null) {
                System.out.println("Ferramenta nao encontrada. Informe um numero ou nome da lista.");
                continue;
            }

            System.out.println();
            System.out.println("Ferramenta: " + selected.name());

            try {
                Map<String, Object> arguments = reader.read(selected);
                System.out.println();
                System.out.println("========== PARAMETROS DA CONSULTA ==========");
                System.out.println(formatter.formatArguments(arguments));

                CallToolRequest request = CallToolRequest.builder(selected.name())
                        .arguments(arguments)
                        .build();
                CallToolResult result = client.callTool(request);
                showResult(selected.name(), result);
            } catch (IllegalStateException exception) {
                System.out.println(exception.getMessage());
                break;
            } catch (Exception exception) {
                LOG.error("Falha ao executar {}", selected.name(), exception);
                System.out.println("Nao foi possivel executar a ferramenta.");
            }
        }
    }

    // exibe as ferramentas descobertas no servidor.
    private void showMenu(List<Tool> tools) {
        System.out.println();
        System.out.println("========== MCP DORADO ==========");

        for (int index = 0; index < tools.size(); index++) {
            Tool tool = tools.get(index);
            System.out.println((index + 1) + " - " + tool.name());

        }

        System.out.println("listar - atualizar ferramentas");
        System.out.println("sair   - encerrar cliente");
    }

    // localiza uma ferramenta pelo numero ou pelo nome.
    static Tool selectTool(List<Tool> tools, String command) {
        try {
            int index = Integer.parseInt(command) - 1;

            if (index >= 0 && index < tools.size()) {
                return tools.get(index);
            }
        } catch (NumberFormatException exception) {
            // se nao for numero, procurar pelo nome da ferramenta.
        }

        for (Tool tool : tools) {
            if (tool.name().equalsIgnoreCase(command)) {
                return tool;
            }
        }

        return null;
    }

    // mostra os parametros da consulta e formata a resposta.
    private void showResult(String toolName, CallToolResult result) {
        System.out.println();
        System.out.println("========== RESULTADOS ==========");
        if (Boolean.TRUE.equals(result.isError())) {
            System.out.println("Erro na ferramenta:");
        }

        for (Object content : result.content()) {
            if (content instanceof TextContent text) {
            		System.out.println(formatter.format(toolName, text.text()));
            } else {
                System.out.println(content);
            }
        }

        if (result.structuredContent() != null && result.content().isEmpty()) {
            System.out.println("Dados estruturados:");
            System.out.println(result.structuredContent());
        }
    }

    // encerra a sessao e libera os recursos do cliente.
    public void close() {
        client.closeGracefully();
    }

    // identifica o caminho do jar informado na inicializacao.
    static Path resolveServerPath(String[] args) {
        if (args.length > 0) {
            if (args.length != 2 || !"--server".equals(args[0])) {
                throw new IllegalArgumentException("Uso: --server <caminho-do-jar>");
            }

            return Path.of(args[1]);
        }

        Path[] candidates = {
                Path.of("server", "mcp-dorado_server.jar"),
                Path.of("..", "server", "mcp-dorado_server.jar"),
                Path.of("..", "..", "server", "mcp-dorado_server.jar")
        };

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException("Servidor nao encontrado. Informe --server <caminho-do-jar>.");
    }

    // inicia a aplicacao cliente.
    public static void main(String[] args) {
        try {
            Path serverJar = resolveServerPath(args);
            McpDoradoClient client = new McpDoradoClient(serverJar, new Scanner(System.in));

            try {
                client.execute();
            } finally {
                client.close();
            }
        } catch (Exception exception) {
            LOG.error("Erro no cliente MCP", exception);
        }
    }
}
