package com.eotcicon.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MediaConfig {

	@Bean
	public Function<String, String> mediaUrlResolver() {
		return key -> key == null ? null : "/media/" + key;
	}
}
