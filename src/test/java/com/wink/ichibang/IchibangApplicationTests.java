package com.wink.ichibang;

import com.wink.ichibang.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;

/** 앱이 뜨는지 확인한다. 마이그레이션 적용과 엔티티·스키마 검증(validate)도 여기서 걸린다 */
@MySqlIntegrationTest
class IchibangApplicationTests {

    @Test
    void contextLoads() {
    }

}
