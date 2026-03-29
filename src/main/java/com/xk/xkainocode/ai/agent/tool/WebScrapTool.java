package com.xk.xkainocode.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * 网页抓取工具类（抓取指定网页的内容）
 */
public class WebScrapTool {
    @Tool("Scarp web page content")
    public String doScrap(@P("The required url") String url) {
        try {
            Document result = Jsoup.connect(url).get();
            return result.html();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
