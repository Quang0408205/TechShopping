package com.example.Tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TechApplication {

	/** Windows reports the legacy "Asia/Saigon" zone id, which PostgreSQL rejects on connect. */
	private static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
		SpringApplication.run(TechApplication.class, args);
	}

}
