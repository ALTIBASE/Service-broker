package org.openpaas.servicebroker.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * ���� �ν��Ͻ��� ���ε带 ������ �� Response  ������ ������ �ִ� ������ �� bean Ŭ����.
 * Json ������̼��� ����ؼ� JSON ���·� ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceInstanceBindingResponse {

	ServiceInstanceBinding binding;
	
	public ServiceInstanceBindingResponse() {}
	
	public ServiceInstanceBindingResponse(ServiceInstanceBinding binding) {
		this.binding = binding;
	}

	@NotEmpty
	@JsonSerialize
	@JsonProperty("credentials")
	public Map<String, Object> getCredentials() {
		return binding.getCredentials();
	}

	@JsonSerialize
	@JsonProperty("syslog_drain_url")
	public String getSyslogDrainUrl() {
		return binding.getSyslogDrainUrl();
	}
	
}
