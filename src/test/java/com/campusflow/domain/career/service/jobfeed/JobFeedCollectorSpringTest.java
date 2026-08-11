package com.campusflow.domain.career.service.jobfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class JobFeedCollectorSpringTest {

    @Test
    void registersAllFourCollectorsAsSpringBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            context.register(
                    InThisWorkJobFeedCollector.class,
                    WantedJobFeedCollector.class,
                    ZighangJobFeedCollector.class,
                    RememberJobFeedCollector.class
            );
            context.refresh();

            assertThat(context.getBeansOfType(JobFeedCollector.class))
                    .containsKeys(
                            "inThisWorkJobFeedCollector",
                            "wantedJobFeedCollector",
                            "zighangJobFeedCollector",
                            "rememberJobFeedCollector"
                    );
        }
    }
}
