package com.pet.ai.service;

import java.util.Map;

public interface AgentService {

    /**
     * 执行AI代理操作
     * @param userId 用户ID
     * @param userInput 用户输入内容
     * @param latitude 纬度坐标
     * @param longitude 经度坐标
     * @return 执行结果Map
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Map<String, Object> execute(Long userId, String userInput, Double latitude, Double longitude);
}

