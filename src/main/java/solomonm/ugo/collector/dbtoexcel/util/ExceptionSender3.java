package solomonm.ugo.collector.dbtoexcel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@PropertySource(value = "classpath:application.yml", encoding = "UTF-8")
class ExceptionSender3 {
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
    private String[] recipients;
    private String title = "123";
    @Value("${exception-sender.delivery-type}")
    private String deliveryType;

    /**
     * 예외 알림 메시지 JSON 생성
     */
    private String createJsonMessage(String content) {
        String recipientsJson = String.format("[\"%s\"]", String.join("\", \"", recipients));
        return String.format(
                "{" +
                        "\"sender\": \"%s\", " +
                        "\"recipients\": %s, " +
                        "\"title\": \"%s\", " +
                        "\"content\": \"%s\", " +
                        "\"deliveryType\": \"%s\" " +
                        "}",
                sender, recipientsJson, title, content, deliveryType
        );
    }

    /**
     * 서버와의 연결 시도 및 응답 코드 확인
     */
    private HttpURLConnection attemptConnection() {
        for (int attempt = 1; attempt <= maxRetryCount; attempt++) {
            try {
                HttpURLConnection connection = openConnection();
                if (connection != null && connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                    log.info("Connection success with response code: {}", connection.getResponseCode());
                    return connection;
                }
            } catch (IOException e) {
                log.warn("Attempt {} failed: {}", attempt, e.getMessage());
                delayRetry();
            }
        }
        log.error("All connection attempts failed after {} retries.", maxRetryCount);
        return null;
    }

    /**
     * HttpURLConnection 객체 생성 및 설정
     */
    private HttpURLConnection openConnection() throws IOException {
        System.out.println(" 커넥션 start");
        URL url = new URL(exceptionUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(connectionTimeoutMs);

        System.out.println(connection);
        return connection;
    }

    /**
     * 재시도 대기
     */
    private void delayRetry() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry interrupted: {}", e.getMessage());
        }
    }

    /**
     * JSON 메시지 전송
     */
    private void sendJsonMessage(HttpURLConnection connection, String jsonMessage) {
        if (connection == null) {
            log.warn("Connection is null. Cannot send JSON message.");
            return;
        }

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to send JSON message: {}", e.getMessage());
        }
    }

    /**
     * 서버 응답 수신
     */
    private String getResponse(HttpURLConnection connection) {
        if (connection == null) return "";

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (IOException e) {
            log.error("Failed to receive response: {}", e.getMessage());
        }
        return response.toString();
    }

    /**
     * 예외 발생 시 알림 전송
     */
    public void sendExceptionAlert(String content) {
        log.info("알림 전송을 시작합니다.");
        HttpURLConnection connection = attemptConnection();
        String jsonMessage = createJsonMessage(content.substring(0, Math.min(100, content.length())));

        sendJsonMessage(connection, jsonMessage);
        String response = getResponse(connection);
        log.info("Server response: {}", response.isEmpty() ? "없음" : response);
    }
}
