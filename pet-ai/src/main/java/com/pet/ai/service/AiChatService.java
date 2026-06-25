package com.pet.ai.service;

import java.util.List;
import java.util.Map;

public interface AiChatService {
    /**
     * 与AI进行对话（带系统提示词）
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return AI回复内容
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    String chat(String systemPrompt, String userMessage);

    /**
     * 与AI进行多轮对话
     * @param messages 消息列表（包含角色和内容）
     * @return AI回复内容
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    String chat(List<Map<String, String>> messages);
}
