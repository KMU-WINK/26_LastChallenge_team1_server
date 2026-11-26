package com.wink.ichibang.submission.history;

import com.wink.ichibang.auth.CurrentMember;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
public class MySubmissionHistoryController {
    private final MySubmissionHistoryService service;
    private final CurrentMember currentMember;

    @GetMapping("/me")
    public MySubmissionHistoryResponse.Page getMine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return service.getMine(currentMember.requireId(), page, size);
    }
}