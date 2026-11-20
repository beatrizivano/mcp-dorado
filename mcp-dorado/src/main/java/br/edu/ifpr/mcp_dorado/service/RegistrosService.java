package br.edu.ifpr.mcp_dorado.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.ifpr.mcp_dorado.model.GenbankRecord;

/** filtra e resume os registros genbank que ja foram indexados na inicializacao. */
public final class RegistrosService {

    private static final int MAX_LIST_RESULTS = 50;
    private final Catalog catalog;

    // recebe os indices usados para filtrar registros.
    public RegistrosService(Catalog catalog) {
        this.catalog = catalog;
    }

    // executa contagem, listagem ou agrupamento dos registros.
    public Map<String, Object> consultar(String operacao, String especie,
            String gene, String local, String agruparPor) {

        List<GenbankRecord> registros = filtrar(especie, gene, local);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("operacao", operacao);
        resposta.put("filtros", filtros(especie, gene, local));
        resposta.put("total", registros.size());

        if ("contar".equals(operacao)) {
            return resposta;
        }

        if ("listar".equals(operacao)) {
            List<Map<String, Object>> itens = new ArrayList<>();
            int limite = Math.min(registros.size(), MAX_LIST_RESULTS);

            for (int i = 0; i < limite; i++) {
                GenbankRecord registro = registros.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("accession", registro.accession());
                item.put("especie", registro.normalizedSpecies());
                item.put("genes", ordenar(new ArrayList<>(registro.genes())));
                item.put("localidade", registro.geoLocName());
                itens.add(item);
            }

            resposta.put("registros", itens);
            resposta.put("limite", MAX_LIST_RESULTS);
            resposta.put("exibidos", itens.size());
            resposta.put("truncado", registros.size() > MAX_LIST_RESULTS);
            return resposta;
        }

        Map<String, Integer> contagens = new LinkedHashMap<>();

        for (GenbankRecord registro : registros) {
            if ("gene".equals(agruparPor)) {
                for (String nomeGene : registro.genes()) {
                    adicionar(contagens, nomeGene);
                }
            } else if ("especie".equals(agruparPor)) {
                adicionar(contagens, registro.normalizedSpecies());
            } else {
                String localidade = registro.geoLocName();

                if (localidade == null || localidade.isBlank()) {
                    localidade = "nao_informada";
                }

                adicionar(contagens, localidade);
            }
        }

        List<String> chaves = ordenar(new ArrayList<>(contagens.keySet()));
        Map<String, Integer> grupos = new LinkedHashMap<>();

        for (String chave : chaves) {
            grupos.put(chave, contagens.get(chave));
        }

        resposta.put("agrupar_por", agruparPor);
        resposta.put("grupos", grupos);
        return resposta;
    }

    // combina os filtros para selecionar os accessions.
    private List<GenbankRecord> filtrar(String especie, String gene, String local) {
        List<GenbankRecord> resultado = new ArrayList<>();
        String especieNormalizada = normalizar(especie);
        String geneNormalizado = normalizar(gene);
        String localNormalizado = normalizar(local);

        for (GenbankRecord registro : catalog.accessions().values()) {
            if (!especieNormalizada.isEmpty()
                    && !especieNormalizada.equals(normalizar(registro.normalizedSpecies()))
                    && !especieNormalizada.equals(normalizar(registro.organism()))) {
                continue;
            }

            if (!geneNormalizado.isEmpty() && !possuiGene(registro, geneNormalizado)) {
                continue;
            }

            // localidade informada no genbank, nao rios de ocorrencia da especie.
            if (!localNormalizado.isEmpty()
                    && !normalizar(registro.geoLocName()).contains(localNormalizado)) {
                continue;
            }

            resultado.add(registro);
        }

        resultado.sort(Comparator.comparing(GenbankRecord::accession));
        return resultado;
    }

    // verifica a presenca de um gene no registro.
    private boolean possuiGene(GenbankRecord registro, String gene) {
        for (String valor : registro.genes()) {
            if (gene.equals(normalizar(valor))) {
                return true;
            }
        }

        return false;
    }

    // normaliza os campos de filtro informados.
    private Map<String, String> filtros(String especie, String gene, String local) {
        Map<String, String> resultado = new LinkedHashMap<>();

        if (especie != null && !especie.isBlank()) {
            resultado.put("especie", especie.strip());
        }

        if (gene != null && !gene.isBlank()) {
            resultado.put("gene", gene.strip());
        }

        if (local != null && !local.isBlank()) {
            resultado.put("local", local.strip());
        }

        return resultado;
    }

    // acrescenta identificadores ao conjunto de resultados.
    private void adicionar(Map<String, Integer> contagens, String chave) {
        contagens.put(chave, contagens.getOrDefault(chave, 0) + 1);
    }

    // organiza os registros ou grupos em ordem estavel.
    private List<String> ordenar(List<String> valores) {
        valores.sort(String.CASE_INSENSITIVE_ORDER);
        return valores;
    }

    // padroniza um texto usado na busca.
    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
