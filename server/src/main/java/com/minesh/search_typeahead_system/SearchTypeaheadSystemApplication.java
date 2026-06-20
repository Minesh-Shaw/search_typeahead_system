package com.minesh.search_typeahead_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SearchTypeaheadSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchTypeaheadSystemApplication.class, args);
	}

}
