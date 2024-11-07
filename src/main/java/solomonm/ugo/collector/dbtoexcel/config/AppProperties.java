package solomonm.ugo.collector.dbtoexcel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "")
public class AppProperties {

    private FileGen filegen;
    private ExceptionSender exceptionSender;



    @Data
    public static class FileGen {
        private String filepath;
        private String filename;
        private String fileExtension;
        private String fileheader;
    }

    @Data
    public static class ExceptionSender {
        private Connector connector;
        private Message message;

        @Data
        public static class Connector {
            private String url;
            private int maxRetryCount;
            private int retryDelayMs;
            private int connectionTimeoutMs;
        }

        @Data
        public static class Message {
            private String sender;
            private Object recipients;
            private String title;
            private String deliveryType;
        }
    }
}
