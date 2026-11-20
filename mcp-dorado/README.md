# MCP Dorado

Projeto acadêmico em Java que implementa um servidor e um cliente MCP (*Model Context Protocol*) com transporte STDIO. Foi desenvolvido a partir do trabalho [Projeto Bacias do Paraná](https://github.com/beatrizivano/projeto-bacias-parana), que reúne consultas de dados de peixes de quatro bacias hidrográficas paranaenses: Iguaçu, Ivaí, Piquiri e Tibagi.

O MCP trabalha com uma **amostra reduzida de registros do GenBank**, armazenada em `data/amostra.gb`, e não com a base completa do trabalho original. O servidor indexa essa amostra em memória ao iniciar. Os arquivos JSON em `src/main/resources/context/` complementam os registros com informações de taxonomia e ocorrência de espécies nos rios. A ocorrência catalogada de uma espécie **não equivale à localidade de coleta** de um registro GenBank.

A explicação sobre o GenBank e a obtenção dos dados ficará no repositório do trabalho original.

## Ferramentas

| Tool | Função | Exemplo de argumentos JSON |
|---|---|---|
| `consultar_accession` | Consulta os metadados de um registro pelo accession. | `{"accession":"ACCESSION_DA_AMOSTRA"}` |
| `consultar_registros` | Conta, lista (até 50) ou agrupa registros, com filtros opcionais por espécie, gene e localidade. | `{"operacao":"agrupar","agrupar_por":"gene"}` |
| `consultar_ocorrencia` | Consulta espécies por rio, rios por espécie ou a interseção entre rios. | `{"operacao":"intersecao","rios":["ivai","tibagi"]}` |
| `resumir_amostra` | Retorna estatísticas gerais dos registros da amostra. | `{}` |

Em `consultar_registros`, a operação deve ser `contar`, `listar` ou `agrupar`. O campo `agrupar_por` é usado apenas em `agrupar` e aceita `especie`, `gene` ou `local`. Os filtros `especie`, `gene` e `local` podem ser combinados.

Em `consultar_ocorrencia`, use `operacao: especie` com o campo `especie`, `operacao: rio` com o campo `rio`, ou `operacao: intersecao` com a lista `rios` (dois ou mais rios diferentes).

## Requisitos

- JDK 25 e Maven para compilar e executar o cliente Java.
- Docker Desktop para testar o servidor no MCP Inspector via contêiner.

## Compilar e executar o cliente Java

Na raiz do projeto, execute os comandos nesta ordem:

```bash
mvn clean verify -Pserver
mvn package -Pclient
```

O primeiro comando gera o servidor em `server/` e executa os testes unitários e de integração. O segundo gera o cliente em `target/dist-client/`. **Não use `clean` no segundo comando**, pois ele removeria os artefatos gerados anteriormente.

Para iniciar o cliente a partir da raiz do projeto:

```bash
java -jar target/dist-client/mcp-dorado_client.jar --server server/mcp-dorado_server.jar
```

O cliente inicia o servidor automaticamente, lista as quatro ferramentas e solicita os argumentos necessários. É possível selecionar uma ferramenta por número ou nome. Digite `listar` para atualizar a lista ou `sair` para encerrar.

## Testar com MCP Inspector

O Inspector é outro cliente MCP; ele se conecta diretamente ao **servidor**, sem usar `McpDoradoClient`.

1. Gere o servidor com `mvn package -Pserver`, caso ainda não esteja compilado.
2. Com o Docker Desktop em execução, inicie na raiz do projeto:

   ```bash
   docker compose up -d --build
   ```

3. Consulte a URL com token nos logs: `docker compose logs -f`.
4. Abra o Inspector, acesse **Tools** e confirme que as quatro ferramentas aparecem.
5. Execute as tools com os argumentos JSON da tabela acima. Para testar `consultar_registros` com `contar`, por exemplo, use `{"operacao":"contar","gene":"COI"}`.

A configuração Docker deve disponibilizar `server/mcp-dorado_server.jar` e a pasta `server/data/` dentro da imagem ou do contêiner. Se o Dockerfile utilizar `COPY server/ ...`, reconstrua a imagem após gerar um novo JAR. Para encerrar o Inspector:

```bash
docker compose down
```

Se o Maven não conseguir remover arquivos de `server/`, encerre previamente o cliente Java e o Inspector: processos ativos podem manter bibliotecas JAR abertas no Windows.
