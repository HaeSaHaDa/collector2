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
class ExceptionSender {
    @Value("${exception_sender.connector.url}")
    private String exceptionUrl;
    @Value("${exception_sender.connector.max_retry_count}")
    private int max_retry_count;
    @Value("${exception_sender.connector.retry_delay_ms}")
    private int retry_delay_ms;
    @Value("${exception_sender.connector.connection_timeout_ms}")
    private int connection_timeout_ms;
    @Value("${exception_sender.message.sender}")
    private String sender;
    @Value("${exception_sender.message.recipients}")
    private String[] recipients;
    @Value("${exception_sender.message.title}")
    private String title;
    @Value("${exception_sender.message.deliveryType}")
    private String deliveryType;

    /**
     * JSON 형식의 메시지 문자열 생성
     */
    private String createJsonMessage(String sender, String[] recipients, String title, String content, String deliveryType) {
        String recipientsJsonArray = String.format("[\"%s\"]", String.join("\", \"", recipients));
        return String.format(
                "{" +
                        "\"sender\": \"%s\", " +
                        "\"recipients\": %s, " +
                        "\"title\": \"%s\", " +
                        "\"content\": \"%s\", " +
                        "\"deliveryType\": \"%s\" " +
                        "}",
                sender, recipientsJsonArray, title, content, deliveryType
        );
    }

    /**
     * HttpURLConnection 객체를 재시도하며 생성하는 메서드
     */
    private HttpURLConnection retryConnection() {
        int attempt = 0;

        while (attempt < max_retry_count) {
            try {
                attempt++;
                HttpURLConnection connection = createConnection();
                if (connection != null) {
                    // 서버 응답 코드 확인
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        log.info("Connection success with response code: " + responseCode);
                        return connection; // 성공적으로 연결을 생성한 경우 반환
                    } else {
                        log.warn("Connection failed with response code: " + responseCode);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection attempt " + attempt + " failed: " + e.getMessage());
            }

            // 재시도 전에 일정 시간 대기
            try {
                Thread.sleep(retry_delay_ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Retry interrupted: " + e.getMessage());
                break; // 인터럽트 발생 시 재시도 중지
            }
        }

        System.err.println("All connection attempts failed after " + max_retry_count + " retries.");
        return null; // 모든 재시도가 실패한 경우 null 반환
    }


    /**
     * HttpURLConnection 객체 생성 및 설정
     */
    private HttpURLConnection createConnection() throws IOException {
        try {
            URL url = new URL(exceptionUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(connection_timeout_ms); // 연결 타임아웃 설정
            return connection;
        } catch (IOException e) {
            System.err.println("Failed to create HTTP connection: " + e.getMessage());
            return null; // 예외 발생 시 null 반환 또는 별도 처리 로직
        }
    }

    /**
     * JSON 메시지를 전송하는 메서드
     */
    private void sendJsonMessage(HttpURLConnection connection, String jsonMessage){
        if (connection == null) {
            System.err.println("Connection is null. Cannot send JSON message.");
            return;
        }

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonMessage.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            System.err.println("Failed to send JSON message: " + e.getMessage());
        }
    }

    /**
     * 서버로부터 응답을 받아 반환하는 메서드
     */
    private String receiveResponse(HttpURLConnection connection) {
        if (connection == null) {
            System.err.println("Connection is null. Cannot receive response.");
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Failed to receive response: " + e.getMessage());
        }

        return response.toString();
    }

    /**
     * 예외 발생 시 메세지 서버로 알림 전송
     */
    public void sendExceptionAlert(String content) {
        System.out.println("this is sparta");
        HttpURLConnection connection = retryConnection();
        String jsonMessage = createJsonMessage(sender, recipients, title, content, deliveryType);

        sendJsonMessage(connection, jsonMessage);
        String response = receiveResponse(connection);

        System.out.println("Response from server: " + response);

    }

}
