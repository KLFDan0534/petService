package com.pet.module.rag.embedding;

import com.pet.module.rag.entity.DocumentEmbedding;
import com.pet.module.rag.entity.KnowledgeDocument;
import com.pet.module.rag.mapper.DocumentEmbeddingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int CHUNK_SIZE = 200;

    private final DocumentEmbeddingMapper embeddingMapper;

    public EmbeddingService(DocumentEmbeddingMapper embeddingMapper) {
        this.embeddingMapper = embeddingMapper;
    }

    public float[] embed(String text) {
        Map<String, Double> tfidf = computeTfidf(text);
        int dimension = 256;
        float[] vector = new float[dimension];
        for (Map.Entry<String, Double> entry : tfidf.entrySet()) {
            int hash = Math.abs(entry.getKey().hashCode()) % dimension;
            vector[hash] += entry.getValue().floatValue();
        }
        double magnitude = 0;
        for (float v : vector) magnitude += (double) v * v;
        magnitude = Math.sqrt(magnitude);
        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
        return vector;
    }

    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length == 0 || vecB.length == 0 || vecA.length != vecB.length) return 0;
        double dot = 0;
        for (int i = 0; i < vecA.length; i++) {
            dot += (double) vecA[i] * vecB[i];
        }
        return dot;
    }

    @Transactional
    public void indexDocument(KnowledgeDocument doc) {
        try {
            List<String> chunks = chunkText(doc.getContent());
            for (int i = 0; i < chunks.size(); i++) {
                float[] vector = embed(chunks.get(i));
                if (vector.length == 0) continue;
                DocumentEmbedding de = new DocumentEmbedding();
                de.setDocumentId(doc.getId());
                de.setEmbedding(vectorToString(vector));
                de.setDimension(vector.length);
                de.setChunkIndex(i);
                de.setChunkText(chunks.get(i));
                embeddingMapper.insert(de);
            }
            log.info("Indexed document {} ({} chunks)", doc.getId(), chunks.size());
        } catch (Exception e) {
            log.warn("Failed to index document {}: {}", doc.getId(), e.getMessage());
        }
    }

    @Transactional
    public void indexAll(List<KnowledgeDocument> docs) {
        for (KnowledgeDocument doc : docs) {
            indexDocument(doc);
        }
    }

    public List<KnowledgeDocument> search(String query, Collection<KnowledgeDocument> candidateDocs) {
        try {
            float[] queryVec = embed(query);
            if (queryVec.length == 0) return List.of();

            List<DocumentEmbedding> allEmbeddings = embeddingMapper.selectList(null);
            if (allEmbeddings.isEmpty()) return List.of();

            Set<Long> candidateIds = candidateDocs.stream().map(KnowledgeDocument::getId).collect(Collectors.toSet());

            Map<Long, Double> docScores = new HashMap<>();
            Map<Long, Integer> docMatches = new HashMap<>();
            for (DocumentEmbedding de : allEmbeddings) {
                if (!candidateIds.contains(de.getDocumentId())) continue;
                float[] docVec = parseVector(de.getEmbedding());
                double score = cosineSimilarity(queryVec, docVec);
                docScores.merge(de.getDocumentId(), score, Math::max);
                docMatches.merge(de.getDocumentId(), 1, Integer::sum);
            }

            return candidateDocs.stream()
                    .filter(d -> docScores.containsKey(d.getId()))
                    .sorted((a, b) -> {
                        int cmp = Double.compare(
                                docScores.getOrDefault(b.getId(), 0.0),
                                docScores.getOrDefault(a.getId(), 0.0));
                        if (cmp != 0) return cmp;
                        return Integer.compare(
                                docMatches.getOrDefault(b.getId(), 0),
                                docMatches.getOrDefault(a.getId(), 0));
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Embedding search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Double> computeTfidf(String text) {
        String[] terms = text.toLowerCase().split("[\\s\\p{Punct}，。、；：！？\"\"''（）【】《》]+");
        Map<String, Integer> termFreq = new HashMap<>();
        int totalTerms = 0;
        for (String term : terms) {
            if (term.isEmpty() || term.length() < 2) continue;
            termFreq.merge(term, 1, Integer::sum);
            totalTerms++;
        }
        Map<String, Double> tfidf = new HashMap<>();
        double idf = Math.log(1.0 + 1.0 / 1.0);
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            double tf = (double) entry.getValue() / totalTerms;
            tfidf.put(entry.getKey(), tf * idf);
        }
        return tfidf;
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;
        String[] lines = text.split("\n");
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (current.length() + line.length() > CHUNK_SIZE && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private String vectorToString(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.US, "%.6f", vec[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] parseVector(String str) {
        String cleaned = str.replace("[", "").replace("]", "").trim();
        if (cleaned.isEmpty()) return new float[0];
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
