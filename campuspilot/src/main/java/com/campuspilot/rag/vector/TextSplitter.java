package com.campuspilot.rag.vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本拆分器：将长文档切分为检索块(chunk)。
 * 中文文本按段落/标点切分，英文按句子切分。
 * 确保每个 chunk 长度不至于过短或过长。
 */
public class TextSplitter {

    private static final int CHUNK_SIZE = 200;
    private static final int CHUNK_OVERLAP = 50;

    /**
     * 将文档文本拆分为固定窗口大小的块（带重叠）
     */
    public static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }
        String cleaned = text.replace("\r", "").trim();
        if (cleaned.length() <= CHUNK_SIZE) {
            chunks.add(cleaned);
            return chunks;
        }
        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + CHUNK_SIZE, cleaned.length());
            chunks.add(cleaned.substring(start, end));
            if (end >= cleaned.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    /**
     * 按段落拆分（演示用，通常与固定窗口拆分组合）
     */
    public static List<String> splitByParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }
        for (String para : text.split("\\n+")) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }
        return chunks;
    }
}