package com.pet.module.rag.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("document_embedding")
public class DocumentEmbedding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String embedding;
    private Integer dimension;
    private Integer chunkIndex;
    private String chunkText;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Integer getDimension() { return dimension; }
    public void setDimension(Integer dimension) { this.dimension = dimension; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
