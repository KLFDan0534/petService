package com.pet.module.agent.controller;

import com.pet.common.Result;
import com.pet.module.agent.AgentService;
import com.pet.security.JwtAuthenticationToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "AI智能体", description = "AI智能体交互，通过自然语言执行业务操作")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/execute")
    @Operation(summary = "执行智能体指令", description = "通过自然语言输入让AI智能体执行业务操作")
    public Result<Map<String, Object>> execute(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestBody Map<String, Object> body) {
        String userInput = (String) body.get("input");
        Double latitude = body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : null;
        Double longitude = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null;
        return Result.success(agentService.execute(token.getUserId(), userInput, latitude, longitude));
    }
}
