package com.wink.ichibang.form.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.form.dto.FormResponses.MyForm;
import com.wink.ichibang.form.service.FormService;
import com.wink.ichibang.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 부원용 신청서 조회 (COM-01: 로그인 필요) */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/form")
public class MemberFormController {

    private final FormService formService;
    private final CurrentMember currentMember;

    /** FORM-02 신청서 조회. 이미 신청했으면 mySubmission에 내 답변이 들어 있다 */
    @GetMapping
    public ApiResponse<MyForm> get(@PathVariable Long postId) {
        return ApiResponse.of(formService.myForm(postId, currentMember.requireId()));
    }
}
