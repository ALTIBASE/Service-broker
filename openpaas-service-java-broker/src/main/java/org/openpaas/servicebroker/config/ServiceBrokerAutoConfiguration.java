package org.openpaas.servicebroker.config;

import org.openpaas.servicebroker.model.BrokerApiVersion;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
/**
 * ���� ���Ŀ Auto Configuration �� �����ϴ� Ŭ���� (spring boot autoconfigure ���)
 * 
 * @author ��â��
 * @date 2015.0629
 */
@Configuration
@ComponentScan(basePackages = {"org.openpaas.servicebroker"})
@ConditionalOnWebApplication
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class ServiceBrokerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(BrokerApiVersion.class)
	public BrokerApiVersion brokerApiVersion() {
		//example
		//"2.5"
		//"2.5, 2.6, 2.7"
		return new BrokerApiVersion("2.5, 2.6");
	}
}