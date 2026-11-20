package br.edu.ifpr.mcp_dorado.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

class McpDoradoServerIT {
   private static final Logger LOG =
                                   LoggerFactory.getLogger(McpDoradoServerIT.class);
   // nome e caminho para o servidor mcp
   private static final String SERVER_PATH = "server/mcp-dorado_server.jar";

   private static McpSyncClient client;

   @BeforeAll
   // verifica set up before class.
   static void setUpBeforeClass() throws Exception {
      // antes que a classe de testes seja carregada, este método providencia
      // alguma inicialização ou alocação de recursos, neste caso, o servidor
      // mcp é executado e o cliente se conecta a ele
      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
                                    serverJar.getAbsolutePath());

      String           jarPath = serverJar.getAbsolutePath();
      ServerParameters params  = ServerParameters.builder("java")
    			                                 .args("-jar",jarPath)
    			                                 .build();
      JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
    			                                              new JsonMapper());
      StdioClientTransport transport  = new StdioClientTransport(params,
    			                                                 jsonMapper);

      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));

      client = McpClient.sync(transport).build();
      client.initialize();
   }

   @AfterAll
   // verifica tear down after class.
   static void tearDownAfterClass() throws Exception {
      // ao terminar todos os testes, todos os métodos nesta classe foram
      // executados, o cliente se desconecta do servidor
      if (client != null)
         client.closeGracefully();
   }

   // verifica deve disponibilizar somente as quatro tools do dorado.
   @Test
   void deveDisponibilizarSomenteAsQuatroToolsDoDorado() {
      ListToolsResult result = client.listTools();
      assertNotNull(result);
      assertNotNull(result.tools());
      assertTrue(result.tools().size() == 4);
      List<String> names = result.tools().stream().map(Tool::name).toList();
      assertTrue(names.containsAll(List.of("consultar_accession", "consultar_registros",
              "consultar_ocorrencia", "resumir_amostra")));
      assertFalse(names.contains("obter_documento"));
   }

   // ==========================================================================
   // testes mcp dorado:

   @Test
   void deveConsultarAccession() {
      CallToolRequest request = CallToolRequest.builder("consultar_accession")
              .arguments(Map.of("accession", "CM184936.1"))
              .build();
      CallToolResult result = client.callTool(request);

      assertNotNull(result);
      assertFalse(Boolean.TRUE.equals(result.isError()));
      assertFalse(result.content().isEmpty());
   }

   // verifica deve retornar erro para accession ausente.
   @Test
   void deveRetornarErroParaAccessionAusente() {
      CallToolRequest request = CallToolRequest.builder("consultar_accession")
              .arguments(Map.of("accession", "NOT_FOUND.1"))
              .build();
      CallToolResult result = client.callTool(request);

      assertTrue(Boolean.TRUE.equals(result.isError()));
   }
   
   // verifica deve consultar registros.
   @Test
   void deveConsultarRegistros() {
       CallToolRequest request = CallToolRequest.builder("consultar_registros")
               .arguments(Map.of(
                       "operacao", "contar",
                       "gene", "COI"
               ))
               .build();

       CallToolResult result = client.callTool(request);

       assertNotNull(result);
       assertFalse(Boolean.TRUE.equals(result.isError()));
       assertFalse(result.content().isEmpty());
   }
   
   // verifica deve consultar ocorrencia.
   @Test
   void deveConsultarOcorrencia() {
       CallToolRequest request = CallToolRequest.builder("consultar_ocorrencia")
               .arguments(Map.of(
                       "operacao", "intersecao",
                       "rios", List.of("ivai", "tibagi")
               ))
               .build();

       CallToolResult result = client.callTool(request);

       assertNotNull(result);
       assertFalse(Boolean.TRUE.equals(result.isError()));
       assertFalse(result.content().isEmpty());
   }
   
   // verifica deve resumir amostra pelo mcp.
   @Test
   void deveResumirAmostraPeloMcp() {
       CallToolRequest request = CallToolRequest.builder("resumir_amostra")
               .arguments(Map.of())
               .build();

       CallToolResult result = client.callTool(request);

       assertNotNull(result);
       assertFalse(Boolean.TRUE.equals(result.isError()));
       assertFalse(result.content().isEmpty());
   }
}
