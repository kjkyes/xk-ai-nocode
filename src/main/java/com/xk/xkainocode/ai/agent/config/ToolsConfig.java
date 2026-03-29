package com.xk.xkainocode.ai.agent.config;

import com.xk.xkainocode.ai.agent.tool.*;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ToolsConfig {

    List<ToolSpecification> toolSpecs = new ArrayList<>();

    @Value("${search-api.api_key}")
    private String apiKey;

    @Bean
    public List<ToolSpecification> allTools() {
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(FileOperationTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(WebSearchTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(WebScrapTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(TerminalOperationTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(ResourceDownloadTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(PDFGenerationTool.class));
        toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(TerminateTool.class));
        return toolSpecs;
    }

    @Bean
    public WebSearchTool webSearchTool() {
        // 将 apiKey 注入到工具实例中
        return new WebSearchTool(apiKey);
    }
}
