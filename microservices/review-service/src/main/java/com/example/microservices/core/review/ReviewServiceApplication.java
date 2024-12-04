package com.example.microservices.core.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("com.example")
@Slf4j
public class ReviewServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);
		String postgresDbUrl = ctx.getEnvironment().getProperty("spring.datasource.url");
		log.info("Connected to PostgresDb: {}", postgresDbUrl);
	}

	@Bean
	public Scheduler jdbcScheduler(@Value("${app.threadPoolSize:10}")Integer threadPoolSize,
								   @Value("${app.taskQueueSize:100}")Integer taskQueueSize) {
		return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
	}

}
