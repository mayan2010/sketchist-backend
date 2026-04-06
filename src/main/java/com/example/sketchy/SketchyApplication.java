package com.example.sketchy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://localhost:5173")
@SpringBootApplication
public class SketchyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SketchyApplication.class, args);
	}

}
