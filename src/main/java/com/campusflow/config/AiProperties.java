package com.campusflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private Claude claude = new Claude();
    private Openai openai = new Openai();

    @Getter
    @Setter
    public static class Claude {
        private String apiKey;
        private String model = "claude-opus-4-7";
    }

    @Getter
    @Setter
    public static class Openai {
        private String apiKey;
        private String model = "gpt-4o";
    }
}
