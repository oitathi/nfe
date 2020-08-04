package com.b2wdigital.fazemu.config;

import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.b2wdigital.fazemu.domain.KafkaConfigurationVO;

@Configuration
public class KafkaConfig {

	@Autowired
	private	Environment environment;

	/**
	 * Retorna o objeto contendo as propriedades para os consumers e producers kafka
	 * 
	 * @return
	 */
	@Bean
	public KafkaConfigurationVO loadConfigurations() {

		//propriedades do producer
		Properties producerProperties = new Properties();
		producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("bootstrap.servers"));
		producerProperties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, environment.getProperty("max.request.size"));
		producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, environment.getProperty("enable.idempotence"));
		producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, environment.getProperty("max.in.flight.requests.per.connection"));
		producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
		producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

		KafkaConfigurationVO configurationVO = new KafkaConfigurationVO();
		configurationVO.setProducerProperties(producerProperties);
		return configurationVO;
	}
}
