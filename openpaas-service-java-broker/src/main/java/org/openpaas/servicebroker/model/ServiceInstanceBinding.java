package org.openpaas.servicebroker.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * ���� �ν��Ͻ� ���ε�� ������ ������ �ִ� ������ �� bean Ŭ����. 
 * Json ������̼��� ����ؼ� JSON ���·� ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceInstanceBinding {

	private String id;
	private String serviceInstanceId;
	private Map<String,Object> credentials = new HashMap<String,Object>();
	private String syslogDrainUrl;
	private String appGuid;
	
	@JsonIgnore
	private HttpStatus httpStatus = HttpStatus.CREATED;
	
	public ServiceInstanceBinding(String id, 
			String serviceInstanceId, 
			Map<String,Object> credentials,
			String syslogDrainUrl, String appGuid) {
		this.id = id;
		this.serviceInstanceId = serviceInstanceId;
		setCredentials(credentials);
		this.syslogDrainUrl = syslogDrainUrl;
		this.appGuid = appGuid;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public Map<String, Object> getCredentials() {
		return credentials;
	}

	public void setServiceInstanceId(String serviceInstanceId) {
		this.serviceInstanceId = serviceInstanceId;
	}

	private void setCredentials(Map<String, Object> credentials) {
		if (credentials == null) {
			credentials = new HashMap<String,Object>();
		} else {
			this.credentials = credentials;
		}
	}

	public String getSyslogDrainUrl() {
		return syslogDrainUrl;
	}
	
	public String getAppGuid() {
		return appGuid;
	}
	
	public void setHttpStatusOK(){
		this.httpStatus=HttpStatus.OK;
	}
	
	public HttpStatus getHttpStatus(){
		return httpStatus;
	}
	
}
