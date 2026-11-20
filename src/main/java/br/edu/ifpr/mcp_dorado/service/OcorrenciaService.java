package br.edu.ifpr.mcp_dorado.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** consulta a ocorrencia de especies nas listas das quatro bacias. */
public final class OcorrenciaService {

    private static final List<String> RIOS = List.of("iguacu", "ivai", "piquiri", "tibagi");

    private final Map<String, Object> especies;
    private final Map<String, Object> nomesAmbiguos;
    private final Map<String, String> nomesAlternativos;

    // recebe os indices de ocorrencia do catalogo.
    public OcorrenciaService(Catalog catalog) {
        this.especies = obterMapa(catalog.occurrenceContext().get("species"));
        this.nomesAmbiguos = obterMapa(catalog.occurrenceContext().get("ambiguous_names"));
        this.nomesAlternativos = new LinkedHashMap<>();

        for (Map.Entry<String, Object> item : especies.entrySet()) {
            String nomeValido = item.getKey();
            Map<String, Object> dados = obterMapa(item.getValue());

            nomesAlternativos.put(normalizar(nomeValido), nomeValido);

            for (String nome : obterLista(dados.get("query_names"))) {
                nomesAlternativos.putIfAbsent(normalizar(nome), nomeValido);
            }
        }
    }

    // consulta os rios de ocorrencia de uma especie.
    public Map<String, Object> consultarEspecie(String nome) {
        String consulta = normalizar(nome);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("operacao", "especie");
        resposta.put("consulta", nome);

        String nomeValido = nomesAlternativos.get(consulta);
        String nomeAmbiguo = consulta;

        if (!nomesAmbiguos.containsKey(nomeAmbiguo) && nomeValido != null) {
            nomeAmbiguo = nomeValido;
        }

        if (nomesAmbiguos.containsKey(nomeAmbiguo)) {
            Map<String, Object> dados = obterMapa(nomesAmbiguos.get(nomeAmbiguo));
            resposta.put("encontrada", true);
            resposta.put("ambigua", true);
            resposta.put("nome", nomeAmbiguo);
            resposta.put("rios", ordenar(obterLista(dados.get("rivers"))));
            resposta.put("candidatos", ordenar(obterLista(dados.get("candidates"))));
            resposta.put("observacao", "A ocorrencia pertence ao nome consultado. "
                    + "Nao e possivel atribui-la automaticamente a um candidato taxonomico.");
            return resposta;
        }

        if (nomeValido == null) {
            resposta.put("encontrada", false);
            resposta.put("ambigua", false);
            resposta.put("rios", List.of());
            return resposta;
        }

        Map<String, Object> dados = obterMapa(especies.get(nomeValido));
        resposta.put("encontrada", true);
        resposta.put("ambigua", false);
        resposta.put("nome", nomeValido);
        resposta.put("rios", ordenar(obterLista(dados.get("rivers"))));
        resposta.put("nomes_consultados", ordenar(obterLista(dados.get("query_names"))));
        return resposta;
    }

    // lista especies associadas ao rio consultado.
    public Map<String, Object> consultarRio(String rio) {
        String rioNormalizado = normalizarRio(rio);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("operacao", "rio");
        resposta.put("rio", rioNormalizado);
        adicionarEspecies(resposta, especiesDoRio(rioNormalizado), ambiguosDoRio(rioNormalizado));
        return resposta;
    }

    // encontra especies compartilhadas entre os rios informados.
    public Map<String, Object> compararRios(List<String> rios) {
        List<String> riosNormalizados = new ArrayList<>();

        for (String rio : rios) {
            String normalizado = normalizarRio(rio);

            if (!riosNormalizados.contains(normalizado)) {
                riosNormalizados.add(normalizado);
            }
        }

        Collections.sort(riosNormalizados);
        Set<String> intersecao = null;
        Set<String> ambiguos = null;

        for (String rio : riosNormalizados) {
            Set<String> especiesDoRio = especiesDoRio(rio);
            Set<String> ambiguosDoRio = ambiguosDoRio(rio);

            if (intersecao == null) {
                intersecao = new LinkedHashSet<>(especiesDoRio);
                ambiguos = new LinkedHashSet<>(ambiguosDoRio);
            } else {
                intersecao.retainAll(especiesDoRio);
                ambiguos.retainAll(ambiguosDoRio);
            }
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("operacao", "intersecao");
        resposta.put("rios", riosNormalizados);
        adicionarEspecies(resposta, intersecao, ambiguos);
        return resposta;
    }

    // padroniza grafias alternativas de nomes de rios.
    public static String normalizarRio(String rio) {
        String normalizado = normalizar(rio);

        if (normalizado.equals("iguassu")) {
            return "iguacu";
        }

        if (normalizado.equals("tibaji")) {
            return "tibagi";
        }

        return normalizado;
    }

    // verifica se o rio existe no contexto disponivel.
    public static boolean rioValido(String rio) {
        return RIOS.contains(normalizarRio(rio));
    }

    // recupera as especies confirmadas de um rio.
    private Set<String> especiesDoRio(String rio) {
        Set<String> resultado = new LinkedHashSet<>();

        for (Map.Entry<String, Object> item : especies.entrySet()) {
            Map<String, Object> dados = obterMapa(item.getValue());

            if (obterLista(dados.get("rivers")).contains(rio)) {
                resultado.add(item.getKey());
            }
        }

        return resultado;
    }

    // recupera nomes taxonomicos ambiguos relacionados ao rio.
    private Set<String> ambiguosDoRio(String rio) {
        Set<String> resultado = new LinkedHashSet<>();

        for (Map.Entry<String, Object> item : nomesAmbiguos.entrySet()) {
            Map<String, Object> dados = obterMapa(item.getValue());

            if (obterLista(dados.get("rivers")).contains(rio)) {
                resultado.add(item.getKey());
            }
        }

        return resultado;
    }

    // reune as especies no resultado da consulta.
    private void adicionarEspecies(Map<String, Object> resposta, Set<String> nomes, Set<String> ambiguos) {
        List<String> nomesOrdenados = ordenar(nomes);
        List<String> ambiguosOrdenados = ordenar(ambiguos);
        List<Map<String, Object>> detalhesAmbiguos = new ArrayList<>();

        for (String nome : ambiguosOrdenados) {
            Map<String, Object> dados = obterMapa(nomesAmbiguos.get(nome));
            Map<String, Object> detalhe = new LinkedHashMap<>();
            detalhe.put("nome", nome);
            detalhe.put("candidatos", ordenar(obterLista(dados.get("candidates"))));
            detalhesAmbiguos.add(detalhe);
        }

        resposta.put("total_especies", nomesOrdenados.size());
        resposta.put("especies", nomesOrdenados);
        resposta.put("total_nomes_ambiguos", detalhesAmbiguos.size());
        resposta.put("nomes_ambiguos", detalhesAmbiguos);
    }

    // organiza os valores em ordem alfabetica.
    private static List<String> ordenar(Iterable<String> nomes) {
        List<String> resultado = new ArrayList<>();

        for (String nome : nomes) {
            resultado.add(nome);
        }

        resultado.sort(Comparator.naturalOrder());
        return resultado;
    }

    // padroniza textos para comparacoes.
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    // extrai um mapa do contexto carregado.
    private static Map<String, Object> obterMapa(Object valor) {
        if (!(valor instanceof Map<?, ?> mapa)) {
            return Map.of();
        }

        Map<String, Object> resultado = new LinkedHashMap<>();

        for (Map.Entry<?, ?> item : mapa.entrySet()) {
            resultado.put(String.valueOf(item.getKey()), item.getValue());
        }

        return resultado;
    }

    // extrai uma lista do contexto carregado.
    private static List<String> obterLista(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }

        List<String> resultado = new ArrayList<>();

        for (Object item : lista) {
            resultado.add(String.valueOf(item));
        }

        return resultado;
    }
}
