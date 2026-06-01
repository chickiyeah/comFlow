package com.campusflow.domain.facilities.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "facility_stats")
@Getter
@NoArgsConstructor
public class FacilityStat {

    @Id
    @Column(length = 50)
    private String statKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(length = 20)
    private String unit;

    @Column(length = 50)
    private String icon;

    public FacilityStat(String statKey, String label, String value, String unit, String icon) {
        this.statKey = statKey;
        this.label   = label;
        this.value   = value;
        this.unit    = unit;
        this.icon    = icon;
    }

    public void update(String value) {
        this.value = value;
    }
}
