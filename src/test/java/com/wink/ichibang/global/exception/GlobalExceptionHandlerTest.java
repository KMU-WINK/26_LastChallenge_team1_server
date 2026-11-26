package com.wink.ichibang.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 도메인과 상관없이 같은 잘못된 요청에는 같은 응답이 나가는지 확인한다.
 * (예전에는 post·form·submission 핸들러가 따로 있어 405·JSON 오류 등의 응답이 서로 달랐다)
 */
class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 도메인_예외는_코드의_상태와_문구로_내려간다() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POST_CLOSED"))
                .andExpect(jsonPath("$.message").value("이미 마감된 신청글이에요"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 칸_하나가_잘못된_예외는_fieldErrors에_담긴다() throws Exception {
        mockMvc.perform(get("/probe/field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ANSWER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("answers[1].optionIds"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("하나만 선택할 수 있어요"));
    }

    @Test
    void 인증_오류도_같은_모양이다() throws Exception {
        mockMvc.perform(get("/probe/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void Body_검증_실패는_칸별_사유를_준다() throws Exception {
        mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void 파라미터_검증_실패는_칸별_사유를_준다() throws Exception {
        mockMvc.perform(get("/probe/param").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
    }

    @Test
    void 숫자가_아닌_파라미터는_400() throws Exception {
        mockMvc.perform(get("/probe/param").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
    }

    @Test
    void 필수_파라미터가_없으면_400() throws Exception {
        mockMvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
    }

    @Test
    void JSON_문법_오류는_body_칸으로_400() throws Exception {
        mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"));
    }

    @Test
    void 잘못된_enum_값은_틀린_칸_이름을_알려준다() throws Exception {
        mockMvc.perform(post("/probe/enum").contentType(MediaType.APPLICATION_JSON).content("{\"kind\":\"ABC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("kind"));
    }

    @Test
    void 지원하지_않는_메서드는_405와_같은_본문() throws Exception {
        mockMvc.perform(delete("/probe/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 지원하지_않는_Content_Type은_415와_같은_본문() throws Exception {
        mockMvc.perform(post("/probe/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void 예상하지_못한_오류는_내부_정보_없이_500() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("잠시 후 다시 시도해주세요"));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(ErrorCode.POST_CLOSED);
        }

        @GetMapping("/field")
        void field() {
            throw BusinessException.field(ErrorCode.INVALID_ANSWER, "answers[1].optionIds", "하나만 선택할 수 있어요");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void body(@Valid @RequestBody NameRequest request) {
        }

        @PostMapping(value = "/enum", consumes = MediaType.APPLICATION_JSON_VALUE)
        void enumBody(@RequestBody KindRequest request) {
        }

        @GetMapping("/param")
        void param(@RequestParam @Min(0) int page) {
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("db password=secret");
        }
    }

    record NameRequest(@NotBlank String name) {
    }

    enum Kind {A, B}

    record KindRequest(Kind kind) {
    }
}
