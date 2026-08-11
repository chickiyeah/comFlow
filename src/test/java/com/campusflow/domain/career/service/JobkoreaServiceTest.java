package com.campusflow.domain.career.service;

import com.campusflow.domain.career.dto.JobSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobkoreaServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void mapsApplicationPeriodEndToDeadlineInsteadOfCreatedAt() throws Exception {
        String response = """
                {
                  "content": [{
                    "id": "49623716",
                    "title": "IT인프라 및 F&B시스템 운영 담당자",
                    "companyName": "풀무원푸드앤컬처",
                    "createdAt": "2026-07-22T00:03:06.257+09:00",
                    "applicationPeriod": {
                      "start": "2026-07-22T00:00:00+09:00",
                      "end": "2026-07-27T23:00:00+09:00"
                    }
                  }]
                }
                """;
        JobkoreaService service = new JobkoreaService(new ObjectMapper());
        Method parseResponse = JobkoreaService.class.getDeclaredMethod("parseResponse", String.class);
        parseResponse.setAccessible(true);

        List<JobSearchResult> results =
                (List<JobSearchResult>) parseResponse.invoke(service, response);

        assertThat(results).singleElement()
                .extracting(JobSearchResult::deadline)
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }
}
