package com.wink.ichibang.port.stub;

import com.wink.ichibang.domain.PostType;
import com.wink.ichibang.port.FormPort;
import org.springframework.stereotype.Component;

/**
 * 임시 구현. BE-3이 FormPort를 구현한 클래스를 올리면 이 파일을 삭제한다.
 * (같은 타입의 빈이 두 개가 되면 실행이 실패한다)
 */
@Component
public class StubFormPort implements FormPort {

    @Override
    public int createDefaultItems(Long postId, PostType type, String postTitle) {
        return type == PostType.STUDY ? 3 : 1;
    }

    @Override
    public int countItems(Long postId) {
        return 1; // 게시 테스트가 되도록 항상 항목이 있다고 가정
    }
}
