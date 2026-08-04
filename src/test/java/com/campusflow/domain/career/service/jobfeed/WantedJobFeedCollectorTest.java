package com.campusflow.domain.career.service.jobfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WantedJobFeedCollectorTest {

    @Test
    void mapsPublicListAndRejectsHiddenOrInactiveJobs() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WantedJobFeedCollector collector = new WantedJobFeedCollector(builder.build(), new ObjectMapper(), 30);

        server.expect(requestTo(WantedJobFeedCollector.LIST_URL + "30&offset=0"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"id":376425,"status":"active","hidden":false,
                           "position":"AI 네이티브 개발자","due_time":"2027-08-31T14:59:59.000Z",
                           "annual_from":3,"annual_to":5,
                           "company":{"name":"메이저맵"},
                           "address":{"full_location":"서울 중구 청계천로 40"}},
                          {"id":2,"status":"closed","hidden":false,"position":"마감 공고","company":{"name":"A"}},
                          {"id":3,"status":"active","hidden":true,"position":"숨김 공고","company":{"name":"B"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var jobs = collector.collectLatest();

        assertThat(jobs).hasSize(1);
        var job = jobs.get(0);
        assertThat(job.id()).isEqualTo("376425");
        assertThat(job.title()).isEqualTo("AI 네이티브 개발자");
        assertThat(job.company()).isEqualTo("메이저맵");
        assertThat(job.location()).isEqualTo("서울 중구 청계천로 40");
        assertThat(job.deadline()).isEqualTo(LocalDate.of(2027, 8, 31));
        assertThat(job.jobType()).isEqualTo("경력 3~5년");
        assertThat(job.url()).isEqualTo("https://www.wanted.co.kr/wd/376425");
        assertThat(job.salary()).isNull();
        assertThat(job.source()).isEqualTo("원티드");
        server.verify();
    }

    @Test
    void zeroExperienceIsMappedToEntryLevel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WantedJobFeedCollector collector = new WantedJobFeedCollector(builder.build(), new ObjectMapper(), 1);
        server.expect(requestTo(WantedJobFeedCollector.LIST_URL + "1&offset=0"))
                .andRespond(withSuccess("""
                        {"data":[{"id":1,"status":"active","hidden":false,"position":"주니어 개발자",
                         "annual_from":0,"annual_to":0,"company":{"name":"스타트업"},"address":{"location":"서울"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(collector.collectLatest()).singleElement()
                .extracting(job -> job.jobType()).isEqualTo("신입");
    }
}
