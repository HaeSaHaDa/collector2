package solomonm.ugo.collector.dbtoexcel.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExceptionSender {
    public void mailSender() throws IOException {
        URL url = new URL("https://www.example.com"); // 요청을 보낼 URL을 정의
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(); // URL에 대한 연결을 염

        connection.setRequestMethod("POST"); // HTTP 요청 방식을 POST로 설정
        connection.setDoOutput(true); // 요청 본문에 데이터를 전송할 수 있도록 함
        connection.setRequestProperty("Content-Type", "application/json"); // 요청의 컨텐츠 타입을 JSON으로 설정

        String jsonInputString = "{\"MSG_HEADER\":{},\"MSG_BODY\":{\"USER_ID\":\"EXAMPLE\",\"USER_SECRET\":\"SECRET_EXAMPLE\"}}";
        // 서버로 전송할 JSON 형식의 문자열을 정의

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8"); // JSON 문자열을 바이트 배열로 변환
            os.write(input, 0, input.length); // 변환된 바이트 배열을 출력 스트림을 통해 전송
        }

        int responseCode = connection.getResponseCode(); // 서버로부터 받은 HTTP 응답 코드를 가져옴
        System.out.println("Response Code : " + responseCode); // 응답 코드를 콘솔에 출력

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())); // 서버로부터의 응답을 읽기 위한 BufferedReader를 생성
        String inputLine;
        StringBuffer response = new StringBuffer(); // 서버 응답을 저장할 StringBuffer 객체를 생성

        while ((inputLine = in.readLine()) != null) { // 서버 응답의 끝까지 한 줄씩 읽어 들임
            response.append(inputLine); // 읽은 데이터를 StringBuffer 객체에 추가
        }
        in.close(); // BufferedReader를 닫아 리소스를 해제

        System.out.println(response.toString());  // 받은 JSON 응답을 콘솔에 출력
    }
}
