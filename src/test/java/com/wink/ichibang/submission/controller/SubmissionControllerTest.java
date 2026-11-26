package com.wink.ichibang.submission.controller;

import com.wink.ichibang.auth.CurrentMember;
import com.wink.ichibang.global.exception.GlobalExceptionHandler;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.response.SubmissionCreatedResponse;
import com.wink.ichibang.submission.dto.response.SubmissionUpdatedResponse;
import com.wink.ichibang.submission.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그인 부원 확인(X-Member-Id)과 오류 응답 모양. DB 없이 컨트롤러만 확인한다 */
@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    private static final String URL = "/api/posts/100/submissions";

    @Mock
    SubmissionService submissionService;
    @Mock
    HttpServletRequest servletRequest;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // CurrentMember는 요청의 헤더를 읽는다. 테스트에서는 한 요청만 보내므로 mock 요청의 헤더를 직접 정한다
        SubmissionController controller = new SubmissionController(submissionService, new CurrentMember(servletRequest));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void loggedInAs(String memberId) {
        when(servletRequest.getHeader("X-Member-Id")).thenReturn(memberId);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder) {
        return builder.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @Test
    void 헤더가_없으면_제출은_401() throws Exception {
        mockMvc.perform(json(post(URL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        verifyNoInteractions(submissionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", " ", "1.5"})
    void 회원_ID가_1_이상의_숫자가_아니면_제출_수정_취소_모두_401(String memberId) throws Exception {
        loggedInAs(memberId);

        mockMvc.perform(json(post(URL))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        mockMvc.perform(json(put(URL + "/me"))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        mockMvc.perform(delete(URL + "/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(submissionService, never()).submit(anyLong(), anyLong(), any());
        verify(submissionService, never()).update(anyLong(), anyLong(), any());
        verify(submissionService, never()).cancel(anyLong(), anyLong());
    }

    @Test
    void 올바른_회원이면_그_ID로_서비스를_호출한다() throws Exception {
        loggedInAs(" 7 ");
        when(submissionService.submit(anyLong(), anyLong(), any())).thenReturn(
                new SubmissionCreatedResponse(1L, SubmissionStatus.SUBMITTED, LocalDateTime.of(2026, 11, 25, 14, 0)));
        when(submissionService.update(anyLong(), anyLong(), any())).thenReturn(
                new SubmissionUpdatedResponse(1L, SubmissionStatus.SUBMITTED, LocalDateTime.of(2026, 11, 25, 14, 5)));

        mockMvc.perform(json(post(URL))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.submissionId").value(1));
        mockMvc.perform(json(put(URL + "/me"))).andExpect(status().isOk());
        mockMvc.perform(delete(URL + "/me")).andExpect(status().isNoContent());

        verify(submissionService).submit(eq(100L), eq(7L), any());
        verify(submissionService).update(eq(100L), eq(7L), any());
        verify(submissionService).cancel(100L, 7L);
    }
}
