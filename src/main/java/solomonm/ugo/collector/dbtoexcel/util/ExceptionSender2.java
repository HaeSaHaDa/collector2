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
class ExceptionSender2 {
    private final ObjectMapper objectMapper;

    @Value("${exception-sender.url}")
    private String exceptionUrl;
    @Value("${exception-sender.max-retry-count}")
    private int maxRetryCount;
    @Value("${exception-sender.retry-delay-ms}")
    private int retryDelayMs;
    @Value("${exception-sender.connection-timeout-ms}")
    private int connectionTimeoutMs;
    @Value("${exception-sender.sender}")
    private String sender;
    @Value("${exception-sender.recipients}")
    private List<Object> recipients;
    @Value("${exception-sender.delivery-type}")
    private String deliveryType;

    ExceptionSender2(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 예외 메시지를 JSON 형식으로 변환하는 메서드입니다.
     * MailDTO 객체를 생성하고, 이를 JSON 문자열로 직렬화하여 반환합니다.
     * 직렬화 과정에서 예외가 발생하면 오류 로그를 남기고 null을 반환합니다.
     *
     * @param content 예외 메시지 내용
     * @return JSON 형식의 메시지 문자열, 변환 실패 시 null 반환
     */
    private String createJsonMessage(String title, String content) {
        String jsonInputString = null;
        try {
            // MailDTO 객체를 생성하고 예외 메시지 내용을 설정
            MailDTO mailDTO = MailDTO.builder()
                    .sender(sender)
                    .recipients(recipients)
                    .title(title)
                    .content(content)
                    .deliveryType(deliveryType)
                    .build();

            // MailDTO 객체를 JSON 형식의 문자열로 직렬화
            jsonInputString = objectMapper.writeValueAsString(mailDTO);

        } catch (Exception e) {
            log.error("Json Try Fail: {}", e.getMessage());
        }
        return jsonInputString;
    }


    /**
     * 지정된 URL로 예외 메시지를 JSON 형식으로 전송하는 메서드입니다.
     * 실패 시 재시도하며, 최대 재시도 횟수만큼 시도합니다.
     * 메시지가 성공적으로 전송되면(HTTP 200 응답 코드) 서버 응답을 로그에 기록합니다.
     * 모든 재시도가 실패할 경우 오류 로그를 남깁니다.
     *
     * @param content 전송할 예외 메시지 내용
     */
    public void exceptionSender(String title, String content) {
        String jsonInputString = createJsonMessage(title, content);  // Convert the message to JSON format
        log.info("JSON Payload: \n{}", jsonInputString);

        int responseCode = 0;
        String responseString = null;

        // 최대 재시도 횟수만큼 시도
        for (int attempt = 1; attempt <= maxRetryCount; attempt++) {
            try {
                // URL 및 HTTP 연결 설정
                URL url = new URL(exceptionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);  // Enable output to send data in request body
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(connectionTimeoutMs);
                log.info("연결 시도: {}", exceptionUrl);
                // JSON 데이터를 요청 본문에 작성
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                // 응답 코드 확인
                responseCode = connection.getResponseCode();

                // 응답이 성공적이면 로그를 기록하고 메서드를 종료
                if (responseCode < 300) {
                    log.info("Message sent successfully on attempt {}", attempt);

                    // 응답 본문을 읽고 로그에 출력
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        responseString = response.toString();
                    }
                    log.info("Response: {}", responseString);
                    return;  // 성공적인 전송 후 메서드를 종료
                } else {
                    log.warn("Attempt {} failed with response code: {}. Retrying...", attempt, responseCode);
                    Thread.sleep(retryDelayMs);  // 재시도 전에 대기
                }

            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                // 재시도 실패 시 대기 후 재시도
            }
        }
        // 모든 재시도가 실패한 경우 로그에 오류 기록
        log.error("All attempts to send message failed after {} retries.", maxRetryCount);
    }
}
