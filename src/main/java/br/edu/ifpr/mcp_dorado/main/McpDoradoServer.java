package br.edu.ifpr.mcp_dorado.main;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.nio.file.Files;

import br.edu.ifpr.mcp_dorado.service.Catalog;
import br.edu.ifpr.mcp_dorado.service.DataInitializer;
import br.edu.ifpr.mcp_dorado.service.AccessionService;
import br.edu.ifpr.mcp_dorado.repository.GenbankRepository;
import br.edu.ifpr.mcp_dorado.mcp.tools.ConsultarAccessionTool;
import br.edu.ifpr.mcp_dorado.service.RegistrosService;
import br.edu.ifpr.mcp_dorado.mcp.tools.ConsultarRegistrosTool;
import br.edu.ifpr.mcp_dorado.service.OcorrenciaService;
import br.edu.ifpr.mcp_dorado.mcp.tools.ConsultarOcorrenciaTool;
import br.edu.ifpr.mcp_dorado.mcp.tools.ResumirAmostraTool;
import br.edu.ifpr.mcp_dorado.service.ResumoAmostraService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_dorado.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

public class McpDoradoServer {
   private static final Logger LOG =
                            LoggerFactory.getLogger(McpDoradoServer.class);

   private McpSyncServer server;
   private final Catalog catalog;

   // registra as quatro ferramentas de consulta no servidor.
   private void registerTools(
                          SyncSpecification<SingleSessionSyncSpecification> mcp)
                throws Exception {
      // cria a lista de ferramentas disponíveis no servidor...
      AccessionService accessionService = new AccessionService(new GenbankRepository(catalog));
      RegistrosService registrosService = new RegistrosService(catalog);
      OcorrenciaService ocorrenciaService = new OcorrenciaService(catalog);
      ResumoAmostraService resumoAmostraService = new ResumoAmostraService(catalog);
      
      List<McpToolDefinition> tools = List.of(
              new ConsultarAccessionTool(accessionService),
              new ConsultarRegistrosTool(registrosService),
              new ConsultarOcorrenciaTool(ocorrenciaService),
              new ResumirAmostraTool(resumoAmostraService)
      );
      
      // ...para cada ferramenta, executa seu registro para posterior execução
      for (McpToolDefinition tool : tools)
         if (tool != null)
            tool.register(mcp);
   }

   // configura o transporte stdio e as capacidades anunciadas.
   private SyncSpecification<SingleSessionSyncSpecification> configMcpServer() {
      // cria o objeto para comunicação entre servidor e cliente
      JacksonMcpJsonMapper         jsonMapper        =
                                     new JacksonMcpJsonMapper(new JsonMapper());
      // estabelece que a comunicação usará o transporte stdio
      StdioServerTransportProvider transportProvider =
                                   new StdioServerTransportProvider(jsonMapper);
      // identifica quais "capacidades" o servidor disponibiliza, neste caso,
      // ferramentas e 'logging'
      /* capacidades, o que o servidor disponibiliza, do servidor
      completions  presente se o servidor suporta sugestões para auto completar
                   argumentos
      experimental capacidades não padronizadas que o servidor suporta!!!
      logging      presente se o servidor suporta envio de mensagen ('logging')
                   ao cliente
      prompts      presente se o servidor oferece modelos ('templates') de
                   promptspresent if the server offers any prompt templates
      resources    presente se o servidor oferece recursos que podem ser lidos
      tools        presente se o servidor oferece ferramentas
       */
      ServerCapabilities capabilities    = ServerCapabilities.builder()
                                                             .tools(true)
                                                             .logging()
                                                             .build();
      // identificação do servidor
      Implementation      implementation = Implementation.builder("MCP Server",
    			                                                  "1.0.0")
                                                         .build(); 

      // configura o servidor com transporte stdio
      return McpServer.sync(transportProvider)
                      .serverInfo(implementation)
                      .capabilities(capabilities);
   }

   // localiza o arquivo de amostra conforme o ambiente de execucao.
   private static Path findSample() throws URISyntaxException {
      String configured = System.getProperty("mcp.sample.path");
      if (configured != null && !configured.isBlank()) return Path.of(configured);
      Path local = Path.of("data", "amostra.gb");
      if (Files.isRegularFile(local)) return local;
      Path application = Path.of(McpDoradoServer.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).toAbsolutePath();
      Path packaged = application.getParent().resolve("data").resolve("amostra.gb");
      return packaged;
   }

   // indexa a amostra e prepara o servidor mcp.
   public McpDoradoServer() throws Exception {
      // load and index the sample before accepting mcp requests.
      Path sample = findSample();
      this.catalog = new DataInitializer().initialize(sample);
      // configura/inicializa o servidor
      SyncSpecification<SingleSessionSyncSpecification> srv = configMcpServer();
      
      // registra as ferramentas disponíveis neste servidor
      registerTools(srv);
      // cria o servidor
      this.server = srv.build();

      LOG.info("MCP Dorado iniciado via STDIO...");
   }

   // inicia o servidor e aguarda seu encerramento.
   public static void main(String[] args) throws Exception {
      McpDoradoServer application = new McpDoradoServer();
      CountDownLatch shutdown = new CountDownLatch(1);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         shutdown.countDown();
         application.server.closeGracefully();
      }, "mcp-dorado-shutdown"));

      shutdown.await();
   }
}
