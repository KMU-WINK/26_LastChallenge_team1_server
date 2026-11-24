package com.wink.ichibang.form.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.form.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wink.ichibang.form.dto.FormResponses;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/form")
public class MemberFormController {
    private final FormService formService;
    private final CurrentMember currentMember;

    @GetMapping
    public FormResponses.MyForm get(@PathVariable Long postId) {
        return formService.myForm(postId, currentMember.requireId());
    }
}