package com.wink.ichibang.post.port.stub;

import com.wink.ichibang.post.port.SubmissionPort;
import org.springframework.stereotype.Component;

/**
 * 임시 구현. 제출 담당이 SubmissionPort를 구현한 클래스를 올리면 이 파일을 삭제한다.
 * (같은 타입의 빈이 두 개가 되면 실행이 실패한다)
 */
@Component
public class StubSubmissionPort implements SubmissionPort {

    @Override
    public long countSubmissions(Long postId) {
        return 0;
    }
}
