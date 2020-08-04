package com.b2wdigital.fazemu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@EnableScheduling
@SpringBootApplication
public class FazemuApplication implements CommandLineRunner {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(FazemuApplication.class);

    public static void main(String[] args) throws Exception {
        LOGGER.debug("main");
        
        SpringApplication app = new SpringApplication(FazemuApplication.class);
        app.run(args);
    }
	
    @Override
    public void run(String... args) throws Exception {
    	LOGGER.debug("run");
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("utf-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

}
