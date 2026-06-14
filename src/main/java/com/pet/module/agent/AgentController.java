package com.pet.module.agent;

import com.pet.common.Result;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(
            @AuthenticationPrincipal JwtAuthenticationToken token,
            @RequestBody Map<String, Object> body) {
        String userInput = (String) body.get("input");
        Double latitude = body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : null;
        Double longitude = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null;
        return Result.success(agentService.execute(token.getUserId(), userInput, latitude, longitude));
    }
}
