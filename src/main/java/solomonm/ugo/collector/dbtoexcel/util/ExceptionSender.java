package solomonm.ugo.collector.dbtoexcel.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.dto.MailDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@PropertySource(value = "classpath:application.yml", encoding = "UTF-8")
public class ExceptionSender {
    private final ObjectMapper objectMapper;

    @Value("${exception-sender.connector.url}")
    private String exceptionUrl;
    @Value("${exception-sender.connector.max-retry-count}")
    private int maxRetryCount;
    @Value("${exception-sender.connector.retry-delay-ms}")
    private int retryDelayMs;
    @Value("${exception-sender.connector.connection-timeout-ms}")
    private int connectionTimeoutMs;
    @Value("${exception-sender.message.sender}")
    private String sender;
    @Value("${exception-sender.message.recipients}")
    private List<Object> recipients;
    @Value("${exception-sender.message.delivery-type}")
    private String deliveryType;

    ExceptionSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 예외 메시지를 JSON 형식으로 변환하고 지정된 URL로 전송합니다.
     * 실패 시 지정된 횟수만큼 재시도합니다.
     *
     * @param content 전송할 예외 메시지 내용
     */
    public void exceptionSender(String title, String content) {
        try {
            // MailDTO 생성 및 JSON 직렬화
            String jsonInputString = objectMapper.writeValueAsString(
                    MailDTO.builder()
                            .sender(sender)
                            .recipients(recipients)
                            .title(title)
                            .content(content)
                            .deliveryType(deliveryType)
                            .build()
            );

            log.info("JSON Payload: \n{}", jsonInputString);

            // 최대 재시도 횟수만큼 전송 시도
            for (int attempt = 1; attempt <= maxRetryCount; attempt++) {
                if (sendMessage(jsonInputString)) {
                    log.info("Message sent successfully on attempt {}", attempt);
                    return;
                }
                log.warn("Attempt {} failed. Retrying in {} ms...", attempt, retryDelayMs);
                Thread.sleep(retryDelayMs);  // 재시도 전 대기
            }

            log.error("All attempts to send message failed after {} retries.", maxRetryCount);
        } catch (Exception e) {
            log.error("Failed to create JSON message or send request: {}", e.getMessage());
        }
    }

    /**
     * JSON 메시지를 지정된 URL로 전송하는 메서드입니다.
     *
     * @param jsonInputString 전송할 JSON 형식 메시지
     * @return 성공 여부
     */
    private boolean sendMessage(String jsonInputString) {
        try {
            URL url = new URL(exceptionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(connectionTimeoutMs);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 300) {
                log.info("Response Code: {}", responseCode);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    log.info("Response: {}", response.toString());
                }
                return true;  // 성공적으로 전송됨
            }
        } catch (Exception e) {
            log.warn("Send attempt failed: {}", e.getMessage());
        }
        return false;
    }
}
