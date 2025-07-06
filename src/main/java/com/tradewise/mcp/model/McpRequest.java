package com.tradewise.mcp.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonRpc = "2.0";
    
    private String id;
    private String method;
    private Object params;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallParams {
        private String name;
        private Map<String, Object> arguments;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeParams {
        private String clientInfo;
        private String protocolVersion;
        private Map<String, Object> capabilities;
    }
}
