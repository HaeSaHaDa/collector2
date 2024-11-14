package solomonm.ugo.collector.dbtoexcel.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import solomonm.ugo.collector.dbtoexcel.config.ExceptionConfig;
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
public class ExceptionSender {
    private final ObjectMapper objectMapper;

    private final String exceptionUrl;
    private final int maxRetryCount;
    private final int retryDelayMs;
    private final int connectionTimeoutMs;
    private final String sender;
    private final List<Object> recipients;
    private final String deliveryType;

    // ObjectMapper를 생성자로 주입받아 JSON 변환에 사용
    ExceptionSender(ObjectMapper objectMapper, ExceptionConfig exceptionConfig) {
        this.objectMapper = objectMapper;

        exceptionUrl = exceptionConfig.getUrl();
        maxRetryCount = exceptionConfig.getMaxRetryCount();
        retryDelayMs = exceptionConfig.getRetryDelayMs();
        connectionTimeoutMs = exceptionConfig.getConnectionTimeoutMs();
        sender = exceptionConfig.getSender();
        recipients = exceptionConfig.getRecipients();
        deliveryType = exceptionConfig.getDeliveryType();
    }

    /**
     * 예외 메시지를 JSON 형식으로 변환하고 지정된 URL로 전송합니다.
     * 실패 시 지정된 횟수만큼 재시도합니다.
     *
     * @param fileName   exception메일  title 작성에 사용.
     * @param errorContents  exception메일  content 작성에 사용.
     */
    public void exceptionSender(String fileName, String errorContents) {
        try {

            String title = fileName + " 파일 생성 실패";
            String content = String.format("%s%s%s \n -> %s",
                    "다음과 같은 이유로 [ ",
                    fileName,
                    " ] 파일 생성에 실패했습니다.",
                    errorContents
            );

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
                // 메시지 전송이 성공하면 메서드를 종료
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
     * @return 전송 공 여부
     */
    private boolean sendMessage(String jsonInputString) {
        try {
            // 지정된 URL로 HTTP 연결을 설정
            URL url = new URL(exceptionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");                               // POST 방식으로 요청
            connection.setDoOutput(true);                                      // 요청 본문에 데이터를 담기 위해 true 설정
            connection.setRequestProperty("Content-Type", "application/json"); // Content-Type 설정
            connection.setConnectTimeout(connectionTimeoutMs);                 // 연결 타임아웃 설정

            // 요청 본문에 JSON 메시지 작성
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            if (responseCode < 300) {
                log.info("Response Code: {}", responseCode);

                // 응답 본문 읽기
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    log.info("Response: {}", response);
                }
                return true;  // 성공적으로 전송됨
            }
        } catch (Exception e) {
            log.warn("Send attempt failed: {}", e.getMessage());
        }
        return false;
    }


}
