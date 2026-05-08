package com.campusflow.domain.career.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityType {
    CERTIFICATE("자격증"),
    LANGUAGE_TEST("어학시험"),
    INTERNSHIP("인턴십"),
    CONTEST("공모전/대회"),
    VOLUNTEER("봉사활동"),
    STUDY_GROUP("스터디"),
    TRAINING("교육/연수"),
    OTHER("기타");

    private final String label;
}
