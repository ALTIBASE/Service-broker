package org.openpaas.servicebroker.model;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * ���� �ν��Ͻ� Plan ������ ������Ʈ �� �� �ʿ��� Request ������ ������ �ִ� ������ �� bean Ŭ����. 
 * Json ������̼��� ����ؼ� JSON ���·� ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateServiceInstanceRequest {

	@NotEmpty
	@JsonSerialize
	@JsonProperty("plan_id")
	private String planId;
	
	@JsonIgnore
	private String serviceInstanceId;

	public UpdateServiceInstanceRequest() {} 
	
	public UpdateServiceInstanceRequest(String planId) {
		this.planId = planId;
	}

	public String getPlanId() {
		return planId;
	}
	
	public String getServiceInstanceId() { 
		return serviceInstanceId;
	}

	public UpdateServiceInstanceRequest withInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId; 
		return this;
	}
}
