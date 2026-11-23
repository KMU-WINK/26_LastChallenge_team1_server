package com.wink.ichibang.post.port.stub;

import com.wink.ichibang.post.port.SubmissionPort;
import org.springframework.stereotype.Component;

/**
 * 임시 구현. BE-2가 SubmissionPort를 구현한 클래스를 올리면 이 파일을 삭제한다.
 */
@Component
public class StubSubmissionPort implements SubmissionPort {

    @Override
    public long countActiveSubmissions(Long postId) {
        return 0;
    }
}
