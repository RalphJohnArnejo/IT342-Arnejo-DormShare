package edu.cit.arnejo.dormshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DormshareApplication {

	public static void main(String[] args) {
		SpringApplication.run(DormshareApplication.class, args);
	}

}
