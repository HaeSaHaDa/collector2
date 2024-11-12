package solomonm.ugo.collector.dbtoexcel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "exception-sender")
@Configuration
public class ExceptionConfig {

    /**
     * exception-sender:
     *     url: http://192.168.0.111:14766/api/notifications
     *     max-retry-count: 3      # 최대 재시도 횟수
     *     retry-delay-ms: 2000    # 재시도 대기 시간 (밀리초)
     *     connection-timeout-ms: 5000  # 연결 타임아웃 시간 (밀리초)
     *     sender: solomonm3@solomonm.co.kr
     *     recipients: -1002166045687  # TELEGRAM으로 연락 시 Chat ID입력, EMAIL 연락 시 EMAIL 주소 입력, 여러개 입력시 콤마로 구분
     *     delivery-type: TELEGRAM  # TELEGRAM,EMAIL
     */
    private String url;
    private int maxRetryCount;
    private int retryDelayMs;
    private int connectionTimeoutMs;
    private String sender;
    private List<Object> recipients;
    private String deliveryType;
}
