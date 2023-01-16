package com.udframework.database.spring;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class SpringDbObjectApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SpringDbObjectApplication.class, args);
		System.out.println(new Gender().fetchAll().run());
	}

	@Bean(name = "dataSource" )
	public DataSource getDataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/dating");
		dataSource.setUsername("dating");
		dataSource.setPassword("dating");
		return dataSource;
	}

}
