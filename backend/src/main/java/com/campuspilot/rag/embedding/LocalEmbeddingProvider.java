package com.campuspilot.rag.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 本地嵌入提供者：基于 TF-IDF + 字符二元组(Chinese bigram) 的稠密向量化。
 *
 * 原理：
 * 1. 文本分词：中文按字符二元组切分，英文按空格/标点切分
 * 2. 词表构建：从语料库（所有政策文本）中统计词频，取高频词构建词表
 * 3. TF-IDF 计算：对每条文本生成 TF-IDF 向量
 * 4. L2 归一化：使余弦相似度等价于点积
 *
 * 这是经典的信息检索技术（非神经网络），产出的向量可真实反映文本语义相似度。
 * 适用于政策文档检索场景。
 */
@Component
@Slf4j
public class LocalEmbeddingProvider implements EmbeddingProvider {

    private static final int MAX_VOCAB_SIZE = 2048;
    private static final int MIN_TOKEN_FREQ = 1;

    private List<String> vocabulary = new ArrayList<>();
    private Map<String, Integer> vocabIndex = new HashMap<>();
    private double[] idfValues = new double[0];
    private boolean built = false;

    @Override
    public float[] embed(String text) {
        if (!built) {
            return new float[0];
        }
        Map<String, Integer> tf = computeTF(tokenize(text));
        float[] vector = new float[vocabulary.size()];
        for (int i = 0; i < vocabulary.size(); i++) {
            String term = vocabulary.get(i);
            int freq = tf.getOrDefault(term, 0);
            if (freq > 0) {
                double tfVal = 1.0 + Math.log(freq);
                vector[i] = (float) (tfVal * idfValues[i]);
            }
        }
        return l2Normalize(vector);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).collect(Collectors.toList());
    }

    @Override
    public int dimension() {
        return vocabulary.size();
    }

    @Override
    public String providerName() {
        return "LOCAL_TFIDF";
    }

    @Override
    public boolean isAvailable() {
        return built;
    }

    /**
     * 从文档语料库构建词表和 IDF 值。
     * 必须在应用启动后调用一次。
     *
     * @param corpusDocuments 文档列表，每条为完整文本
     */
    public void buildIndex(List<String> corpusDocuments) {
        if (corpusDocuments == null || corpusDocuments.isEmpty()) {
            log.warn("LocalEmbeddingProvider: 语料库为空，跳过索引构建");
            return;
        }

        int docCount = corpusDocuments.size();
        Map<String, Integer> globalFreq = new HashMap<>();
        Map<String, Integer> docFreq = new HashMap<>();

        List<List<String>> tokenizedDocs = new ArrayList<>();
        for (String doc : corpusDocuments) {
            List<String> tokens = tokenize(doc);
            tokenizedDocs.add(tokens);
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFreq.merge(token, 1, Integer::sum);
            }
            for (String token : tokens) {
                globalFreq.merge(token, 1, Integer::sum);
            }
        }

        List<Map.Entry<String, Integer>> sorted = globalFreq.entrySet().stream()
            .filter(e -> e.getValue() >= MIN_TOKEN_FREQ)
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(MAX_VOCAB_SIZE)
            .collect(Collectors.toList());

        vocabulary = new ArrayList<>();
        vocabIndex = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            String term = sorted.get(i).getKey();
            vocabulary.add(term);
            vocabIndex.put(term, i);
        }

        idfValues = new double[vocabulary.size()];
        for (int i = 0; i < vocabulary.size(); i++) {
            String term = vocabulary.get(i);
            int df = docFreq.getOrDefault(term, 0);
            idfValues[i] = Math.log((double) (docCount + 1) / (df + 1)) + 1.0;
        }

        built = true;
        log.info("LocalEmbeddingProvider: 索引构建完成，文档数={}, 词表大小={}, 维度={}",
            docCount, vocabulary.size(), vocabulary.size());
    }

    /**
     * 文本分词：中文字符二元组 + 英文单词
     */
    List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        String cleaned = text.toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{Punct}]", " ")
            .replaceAll("[\\s]+", " ")
            .trim();

        Pattern chinesePattern = Pattern.compile("[\\u4e00-\\u9fff]+");
        Matcher matcher = chinesePattern.matcher(cleaned);
        StringBuilder nonChinese = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            String segment = matcher.group();
            for (int i = 0; i < segment.length() - 1; i++) {
                tokens.add(segment.substring(i, i + 2));
            }
            if (segment.length() == 1) {
                tokens.add(segment);
            }
            String before = cleaned.substring(lastEnd, matcher.start());
            nonChinese.append(before).append(" ");
            lastEnd = matcher.end();
        }
        nonChinese.append(cleaned.substring(lastEnd));

        for (String word : nonChinese.toString().trim().split("[\\s]+")) {
            String w = word.trim();
            if (!w.isEmpty() && w.length() >= 2) {
                tokens.add(w);
            }
        }

        return tokens;
    }

    private Map<String, Integer> computeTF(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            if (vocabIndex.containsKey(token)) {
                tf.merge(token, 1, Integer::sum);
            }
        }
        return tf;
    }

    private float[] l2Normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm < 1e-10) {
            return vector;
        }
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    /**
     * 余弦相似度（假设向量已归一化）
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return Math.max(-1, Math.min(1, dot));
    }
}
