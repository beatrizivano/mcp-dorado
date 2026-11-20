
public class ProcessTest {
	   public static void main(String[] args)
			         throws Exception {
		   /*
		   system.err.println(
					      "console charset: "
					            + (system.console() == null
					                  ? "null"
					                  : system.console().charset())
					);

					system.err.println(
					      "stderr.encoding: "
					            + system.getproperty("stderr.encoding")
					);

					system.err.println(
					      "native.encoding: "
					            + system.getproperty("native.encoding")
					);

					system.err.println(
					      "file.encoding: "
					            + system.getproperty("file.encoding")
					);
					system.err.println(
							      "teste utf-8: á é í ó ú ç ã õ â ê ó"
							);
	*/

			      Process process =
			            new ProcessBuilder(
			                  "java",
			                  "-jar",
			                  "mcp-dorado_server.jar"
			            )
			            .inheritIO()
			            .start();

			      System.out.println(
			            "PID: " + process.pid()
			      );

			      Thread.sleep(5000);

			      System.out.println(
			            "Fechando 'stdin' do processo..."
			      );

			      process.getOutputStream().close();

			      int exitCode =
			            process.waitFor();

			      System.out.println(
			            "Exit code: " + exitCode
			      );
			   }
}
