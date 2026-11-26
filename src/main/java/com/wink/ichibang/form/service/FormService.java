package com.wink.ichibang.form.service;

import com.wink.ichibang.form.dto.FormResponses.*;
import com.wink.ichibang.form.repository.MemberLookup;
import com.wink.ichibang.form.repository.MemberLookup.MemberName;
import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.time.TimeZones;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.domain.QuestionOption;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.domain.SubmissionStatus;
import com.wink.ichibang.submission.dto.SubmissionView;
import com.wink.ichibang.submission.dto.SubmissionView.AnswerView;
import com.wink.ichibang.submission.service.SubmissionQueryService;
import com.wink.ichibang.submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 신청서 조회(FORM-02)와 제출자 관리(FORM-03~06).
 * 제출·답변은 제출 도메인(submission) 것이라 테이블을 직접 읽거나 고치지 않는다.
 * 읽기는 {@link SubmissionQueryService}, 상태 변경은 {@link SubmissionService}를 거치고,
 * 여기서는 신청글(질문)·회원 이름과 합쳐서 응답을 만든다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormService {

    private final PostRepository posts;
    private final MemberLookup members;
    private final SubmissionQueryService submissionQuery;
    private final SubmissionService submissionService;

    // ===== FORM-02 신청서 조회 (부원) =====

    /** 시작 전·삭제 글은 404, 모집 중이 아니면 400 APPLY_POST_NOT_OPEN (SUB-02 기준) */
    public MyForm myForm(Long postId, long memberId) {
        LocalDate today = today();
        Post post = findPost(postId);
        if (!post.isStartedOn(today) || post.statusOn(today) != PostStatus.OPEN) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (post.statusOn(today) != PostStatus.OPEN) {
            throw new BusinessException(ErrorCode.APPLY_POST_NOT_OPEN);
        }

        MySubmission mySubmission = submissionQuery.findMine(postId, memberId)
                .map(submission -> {
                    Map<Long, AnswerView> answers = byQuestion(submission);
                    List<MyAnswer> myAnswers = post.getQuestions().stream()
                            .filter(q -> answers.containsKey(q.getId()))
                            .map(q -> toMyAnswer(q, answers.get(q.getId())))
                            .toList();
                    return new MySubmission(submission.id(), submission.status(), myAnswers);
                })
                .orElse(null);

        return new MyForm(post.getId(), post.getTitle(), post.getType(),
                post.getStartDate(), post.getEndDate(), questionViews(post), mySubmission);
    }

    // ===== FORM-03 제출자 목록 (운영진) =====

    /** @param status null이면 전체. counts는 status와 상관없이 글 전체 기준 */
    public Applicants applicants(Long postId, SubmissionStatus status, int page, int size) {
        Post post = findPost(postId);
        SubmissionQueryService.Counts submissionCounts = submissionQuery.counts(postId);
        Counts counts = new Counts(submissionCounts.total(), submissionCounts.submitted(), submissionCounts.completed());
        long totalElements = status == null ? counts.total()
                : status == SubmissionStatus.SUBMITTED ? counts.submitted() : counts.completed();

        List<SubmissionView> rows = submissionQuery.findPage(postId, status, page, size);

        Map<Long, MemberName> names = members.find(rows.stream().map(SubmissionView::memberId).distinct().toList());
        List<ApplicantRow> content = rows.stream()
                .map(r -> {
                    MemberName m = names.get(r.memberId());
                    return new ApplicantRow(r.id(), m == null ? null : m.name(), m == null ? null : m.studentNo(),
                            r.submittedAt().toLocalDate(), r.status());
                })
                .toList();

        int totalPages = (int) ((totalElements + size - 1) / size);
        return new Applicants(postSummary(post), counts, content, page, size, totalElements, totalPages);
    }

    // ===== FORM-06 제출 상세 (운영진) =====

    public SubmissionDetail detail(Long postId, Long submissionId) {
        Post post = findPost(postId);
        SubmissionView submission = submissionQuery.findDetail(postId, submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBMISSION_NOT_FOUND));
        MemberName m = members.find(List.of(submission.memberId())).get(submission.memberId());
        Map<Long, AnswerView> answers = byQuestion(submission);

        List<AnswerDetail> details = post.getQuestions().stream()
                .map(q -> toAnswerDetail(q, answers.get(q.getId())))
                .toList();
        return new SubmissionDetail(submission.id(), m == null ? null : m.name(), m == null ? null : m.studentNo(),
                submission.status(), submission.submittedAt().truncatedTo(ChronoUnit.SECONDS), details);
    }

    // ===== FORM-04 참여 완료 처리·되돌리기 (운영진) =====

    /** SUBMITTED ↔ COMPLETED. 이미 같은 상태면 409 INVALID_STATUS_TRANSITION. 시점 제한 없음 */
    @Transactional
    public StatusChangeResponse changeStatus(Long postId, Long submissionId, SubmissionStatus target) {
        findPost(postId);
        SubmissionView changed = submissionService.changeStatus(postId, submissionId, target);
        return new StatusChangeResponse(changed.id(), changed.status(), changed.completedAt());
    }

    // ===== FORM-05 CSV 내보내기 (운영진) =====

    public CsvFile exportCsv(Long postId) {
        Post post = findPost(postId);
        List<SubmissionView> rows = submissionQuery.findAll(postId);
        Map<Long, MemberName> names = members.find(rows.stream().map(SubmissionView::memberId).distinct().toList());

        StringBuilder csv = new StringBuilder("\uFEFF"); // BOM: 엑셀 한글 깨짐 방지
        List<String> header = new ArrayList<>(List.of("이름", "학번", "제출일", "상태"));
        post.getQuestions().forEach(q -> header.add(q.getContent()));
        appendLine(csv, header);

        for (SubmissionView r : rows) {
            MemberName m = names.get(r.memberId());
            Map<Long, AnswerView> mine = byQuestion(r);
            List<String> line = new ArrayList<>(List.of(
                    m == null ? "" : nullToEmpty(m.name()),
                    m == null ? "" : nullToEmpty(m.studentNo()),
                    r.submittedAt().toLocalDate().toString(),
                    r.status() == SubmissionStatus.COMPLETED ? "참여 완료" : "제출됨"));
            for (Question q : post.getQuestions()) {
                line.add(csvAnswer(q, mine.get(q.getId())));
            }
            appendLine(csv, line);
        }

        String fileName = "신청_" + safeFileName(post.getTitle()) + "_"
                + today().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return new CsvFile(fileName, csv.toString());
    }

    public record CsvFile(String fileName, String content) {
    }

    // ===== 내부 =====

    /** 질문 ID → 그 질문에 한 답 */
    private static Map<Long, AnswerView> byQuestion(SubmissionView submission) {
        return submission.answers().stream()
                .collect(Collectors.toMap(AnswerView::questionId, Function.identity(), (a, b) -> a, HashMap::new));
    }

    /** 고른 선택지를 질문의 표시 순서대로 */
    private static List<OptionView> selectedOptions(Question q, AnswerView answer) {
        if (answer == null || answer.optionIds().isEmpty()) {
            return List.of();
        }
        return q.getOptions().stream()
                .filter(o -> answer.optionIds().contains(o.getId()))
                .map(o -> new OptionView(o.getId(), o.getContent()))
                .toList();
    }

    private static MyAnswer toMyAnswer(Question q, AnswerView answer) {
        if (q.getType().isChoice()) {
            return new MyAnswer(q.getId(), selectedOptions(q, answer).stream().map(OptionView::id).toList(), null);
        }
        return new MyAnswer(q.getId(), null, answer.text());
    }

    private static AnswerDetail toAnswerDetail(Question q, AnswerView answer) {
        String text = q.getType().isChoice() || answer == null ? null : answer.text();
        return new AnswerDetail(q.getId(), q.getContent(), q.getType(), selectedOptions(q, answer), text);
    }

    /** 복수 선택은 ";"로 이어 붙인다. 답하지 않은 질문은 빈칸 */
    private static String csvAnswer(Question q, AnswerView answer) {
        if (answer == null) {
            return "";
        }
        if (q.getType().isChoice()) {
            return selectedOptions(q, answer).stream().map(OptionView::content).collect(Collectors.joining(";"));
        }
        return nullToEmpty(answer.text());
    }

    private Post findPost(Long postId) {
        return posts.findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private PostSummary postSummary(Post post) {
        return new PostSummary(post.getId(), post.getTitle(), post.getType(), post.statusOn(today()),
                post.getStartDate(), post.getEndDate());
    }

    private static List<QuestionView> questionViews(Post post) {
        return post.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getContent(), q.getDescription(), q.getType(),
                        q.getOptions().stream().map(FormService::optionView).toList()))
                .toList();
    }

    private static OptionView optionView(QuestionOption o) {
        return new OptionView(o.getId(), o.getContent());
    }

    private static void appendLine(StringBuilder csv, List<String> cells) {
        csv.append(cells.stream().map(FormService::cell).collect(Collectors.joining(","))).append("\r\n");
    }

    /** 모든 칸을 따옴표로 감싼다 (줄바꿈·쉼표 보존). =, +, -, @로 시작하면 엑셀 수식 실행을 막는다 */
    private static String cell(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String safeFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static LocalDate today() {
        return LocalDate.now(TimeZones.KST);
    }
}
