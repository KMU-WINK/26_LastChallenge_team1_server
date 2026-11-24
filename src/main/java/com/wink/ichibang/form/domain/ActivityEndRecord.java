package com.wink.ichibang.form.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_activity_end")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityEndRecord {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    public ActivityEndRecord(Long postId, LocalDateTime endedAt) {
        this.postId = postId;
        this.endedAt = endedAt;
    }
}