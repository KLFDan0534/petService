package com.pet.ai.service;

import com.pet.ai.dto.RagDocumentCreateRequestDTO;
import com.pet.ai.entity.KnowledgeDocument;

import java.util.List;

public interface RagService {

    /**
     * 获取所有知识文档列表
     * @return 知识文档列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<KnowledgeDocument> listAll();

    /**
     * 根据查询关键词和分类搜索知识文档
     * @param query 查询关键词
     * @param category 文档分类
     * @return 知识文档列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<KnowledgeDocument> search(String query, String category);

    /**
     * 根据问题获取AI回答
     * @param question 用户问题
     * @return AI回答内容
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    String answer(String question);

    /**
     * 根据问题和宠物档案获取AI回答
     * @param question 用户问题
     * @param petProfile 宠物档案信息
     * @return AI回答内容
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    String answer(String question, String petProfile);

    /**
     * 创建知识文档
     * @param request 创建请求DTO
     * @return 创建后的知识文档
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    KnowledgeDocument create(RagDocumentCreateRequestDTO request);

    /**
     * 从文件创建知识文档
     * @param fileName 文件名
     * @param fileBytes 文件字节数组
     * @param title 文档标题
     * @param category 文档分类
     * @return 创建后的知识文档
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    KnowledgeDocument createFromFile(String fileName, byte[] fileBytes, String title, String category);

    /**
     * 根据ID删除知识文档
     * @param id 文档ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void delete(Long id);
}

