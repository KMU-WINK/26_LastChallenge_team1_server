package com.wink.ichibang.form.repository;

import com.wink.ichibang.form.domain.ActivityEndRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEndRepository
        extends JpaRepository<ActivityEndRecord, Long> {
}