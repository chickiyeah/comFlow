package com.campusflow.domain.facilities.service;

import com.campusflow.domain.facilities.entity.FacilityStat;
import com.campusflow.domain.facilities.repository.FacilityStatRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityStatRepository repo;

    // 기본값 시드 (테이블이 비어있을 때만)
    @PostConstruct
    @Transactional
    public void seed() {
        if (repo.count() > 0) return;
        List<FacilityStat> defaults = List.of(
            new FacilityStat("library_available", "도서관 이용 가능 좌석", "45",  "석",  "auto_stories"),
            new FacilityStat("library_total",     "도서관 전체 좌석",      "120", "석",  "auto_stories"),
            new FacilityStat("library_1f",        "1층 열람실",            "12",  "석",  "table_bar"),
            new FacilityStat("library_2f",        "2층 스터디룸",          "8",   "석",  "groups"),
            new FacilityStat("library_3f",        "3층 그룹실",            "15",  "석",  "meeting_room"),
            new FacilityStat("library_digital",   "디지털 열람실",         "10",  "석",  "computer"),
            new FacilityStat("studyroom_available","스터디룸 예약 가능",    "12",  "개",  "meeting_room"),
            new FacilityStat("locker_arrived",    "택배 보관함 도착",       "3",   "개",  "package_2"),
            new FacilityStat("parking_available", "주차장 잔여",           "42",  "대",  "local_parking"),
            new FacilityStat("dorm_building",     "기숙사 건물",           "IT관","",    "apartment"),
            new FacilityStat("dorm_notice",       "기숙사 공지",           "정상 운영 중","", "campaign")
        );
        repo.saveAll(defaults);
    }

    public List<FacilityStat> getAll() {
        return repo.findAll();
    }

    public Map<String, FacilityStat> getAsMap() {
        Map<String, FacilityStat> map = new java.util.LinkedHashMap<>();
        repo.findAll().forEach(s -> map.put(s.getStatKey(), s));
        return map;
    }

    @Transactional
    public FacilityStat update(String key, String value) {
        FacilityStat s = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("unknown key: " + key));
        s.update(value);
        return s;
    }
}
