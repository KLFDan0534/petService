package com.pet.module.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.module.rag.entity.KnowledgeDocument;
import com.pet.module.rag.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final KnowledgeDocumentMapper documentMapper;

    public RagService(KnowledgeDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public List<KnowledgeDocument> listAll() {
        return documentMapper.selectList(null);
    }

    public List<KnowledgeDocument> search(String query, String category) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KnowledgeDocument::getCategory, category);
        }
        List<KnowledgeDocument> allDocs = documentMapper.selectList(wrapper);

        if (query == null || query.isEmpty()) {
            return allDocs;
        }

        Set<String> termSet = new LinkedHashSet<>();
        String[] rawTerms = query.toLowerCase().split("[\\s,，。、]+");
        for (String term : rawTerms) {
            termSet.add(term);
            for (int i = 0; i < term.length(); i++) {
                termSet.add(String.valueOf(term.charAt(i)));
            }
        }
        String[] queryTerms = termSet.toArray(new String[0]);

        return allDocs.stream()
                .map(doc -> {
                    String content = doc.getContent().toLowerCase();
                    String title = doc.getTitle().toLowerCase();
                    int score = 0;
                    for (String term : queryTerms) {
                        if (term.isEmpty()) continue;
                        if (title.contains(term)) score += 10;
                        if (content.contains(term)) {
                            score += countOccurrences(content, term) * 2;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(doc, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public String answer(String question) {
        List<KnowledgeDocument> relevantDocs = search(question, null);
        if (relevantDocs.isEmpty()) {
            return "抱歉，知识库中没有找到相关信息";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(3, relevantDocs.size()); i++) {
            KnowledgeDocument doc = relevantDocs.get(i);
            context.append("[").append(doc.getCategory()).append("] ")
                    .append(doc.getTitle()).append(":\n")
                    .append(doc.getContent()).append("\n\n");
        }

        return "根据知识库，为您找到以下相关信息：\n\n" + context.toString().trim();
    }

    public KnowledgeDocument create(KnowledgeDocument doc) {
        if (doc.getContent() != null) {
            doc.setWordCount(doc.getContent().length());
        }
        documentMapper.insert(doc);
        return doc;
    }

    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }
}
