package com.campusflow.domain.career.service.jobfeed;

import com.campusflow.domain.career.dto.JobSearchResult;

import java.util.List;

/** A bounded, unauthenticated feed of recent public job postings. */
public interface JobFeedCollector {

    String source();

    List<JobSearchResult> collectLatest();
}
