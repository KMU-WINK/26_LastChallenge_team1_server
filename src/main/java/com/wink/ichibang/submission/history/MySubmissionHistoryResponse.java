package com.wink.ichibang.submission.history;

import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;

import java.time.LocalDate;
import java.util.List;

public final class MySubmissionHistoryResponse {
    private MySubmissionHistoryResponse() {}

    public record Item(
            long no,
            long postId,
            PostType type,
            String title,
            String summary,
            PostStatus postStatus,
            Integer dDay,
            LocalDate submittedAt,
            String myStatus
    ) {}

    public record Page(
            List<Item> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}