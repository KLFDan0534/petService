package com.pet.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.ai.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文档 MyBatis-Plus Mapper 接口（当前未直接使用，数据存储在 Chroma 向量库）
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
