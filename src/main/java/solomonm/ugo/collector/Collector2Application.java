package solomonm.ugo.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class Collector2Application {

	public static void main(String[] args) {
		SpringApplication.run(Collector2Application.class, args);
	}

}
