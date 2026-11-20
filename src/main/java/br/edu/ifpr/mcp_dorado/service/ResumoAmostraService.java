package br.edu.ifpr.mcp_dorado.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import br.edu.ifpr.mcp_dorado.model.GenbankRecord;

/** calcula as estatisticas da amostra a partir dos registros ja indexados. */
public final class ResumoAmostraService {

    private static final int LIMITE_LOCALIDADES = 10;

    private static final List<String> QUALIFICADORES = List.of(
            "collection_date",
            "isolate",
            "db_xref",
            "geo_loc_name",
            "lat_lon",
            "specimen_voucher"
    );

    private final Catalog catalog;

    // recebe o catalogo que sera resumido.
    public ResumoAmostraService(Catalog catalog) {
        this.catalog = catalog;
    }

    // calcula as estatisticas gerais dos registros indexados.
    public Map<String, Object> resumir() {
        Map<String, Integer> genes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> rios = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> localidades = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Integer> qualificadores = new LinkedHashMap<>();
        Set<String> especies = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (String nome : QUALIFICADORES) {
            qualificadores.put(nome, 0);
        }

        int comLocalidade = 0;
        int comCoordenadas = 0;
        int ambiguos = 0;
        int semGenes = 0;
        int semRios = 0;

        for (GenbankRecord registro : catalog.accessions().values()) {
            especies.add(registro.normalizedSpecies());

            if (registro.taxonomicallyAmbiguous()) {
                ambiguos++;
            }

            if (registro.genes().isEmpty()) {
                semGenes++;
            }

            if (registro.rivers().isEmpty()) {
                semRios++;
            }

            for (String gene : registro.genes()) {
                incrementar(genes, gene);
            }

            // rio de ocorrencia da especie, nao localidade da coleta.
            for (String rio : registro.rivers()) {
                incrementar(rios, rio);
            }

            String localidade = registro.geoLocName();

            if (localidade != null && !localidade.isBlank()) {
                comLocalidade++;
                incrementar(localidades, localidade.strip());
            }

            if (registro.coordinates() != null) {
                comCoordenadas++;
            }

            for (String nome : QUALIFICADORES) {
                List<String> valores = registro.qualifiers().get(nome);

                if (possuiValor(valores)) {
                    incrementar(qualificadores, nome);
                }
            }
        }

        int total = catalog.accessions().size();
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("total_registros", total);
        resposta.put("total_especies", especies.size());
        resposta.put("registros_taxonomia_ambigua", ambiguos);
        resposta.put("registros_sem_gene_anotado", semGenes);
        resposta.put("registros_sem_rio_de_ocorrencia", semRios);
        resposta.put("registros_por_gene", genes);
        resposta.put("registros_por_rio_de_ocorrencia", rios);

        Map<String, Object> localidade = new LinkedHashMap<>();
        localidade.put("com_geo_loc_name", comLocalidade);
        localidade.put("sem_geo_loc_name", total - comLocalidade);
        localidade.put("com_coordenadas_convertidas", comCoordenadas);
        localidade.put("sem_coordenadas_convertidas", total - comCoordenadas);
        localidade.put("total_localidades_distintas", localidades.size());
        localidade.put("localidades_mais_frequentes", maisFrequentes(localidades));
        resposta.put("localidade_dos_registros", localidade);

        resposta.put("registros_com_qualificador", qualificadores);
        resposta.put("observacoes", List.of(
                "O resumo considera somente os accessions presentes na amostra GenBank.",
                "Rios representam ocorrencia catalogada da especie, nao localidade de coleta.",
                "Um accession pode aparecer em mais de um gene e em mais de um rio.",
                "Coordenadas convertidas dependem do formato de lat_lon reconhecido pelo parser."
        ));

        return resposta;
    }

    // verifica se um qualificador possui conteudo.
    private boolean possuiValor(List<String> valores) {
        if (valores == null) {
            return false;
        }

        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return true;
            }
        }

        return false;
    }

    // acumula a contagem de uma categoria.
    private void incrementar(Map<String, Integer> contagens, String nome) {
        contagens.put(nome, contagens.getOrDefault(nome, 0) + 1);
    }

    // seleciona as localidades com maior quantidade de registros.
    private Map<String, Integer> maisFrequentes(Map<String, Integer> localidades) {
        List<Map.Entry<String, Integer>> ordenadas = new ArrayList<>(localidades.entrySet());
        ordenadas.sort((primeira, segunda) -> {
            int diferenca = Integer.compare(segunda.getValue(), primeira.getValue());

            if (diferenca != 0) {
                return diferenca;
            }

            return primeira.getKey().compareToIgnoreCase(segunda.getKey());
        });

        Map<String, Integer> resultado = new LinkedHashMap<>();
        int limite = Math.min(LIMITE_LOCALIDADES, ordenadas.size());

        for (int i = 0; i < limite; i++) {
            Map.Entry<String, Integer> entrada = ordenadas.get(i);
            resultado.put(entrada.getKey(), entrada.getValue());
        }

        return resultado;
    }
}
