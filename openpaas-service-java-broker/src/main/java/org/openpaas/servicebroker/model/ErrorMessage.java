package org.openpaas.servicebroker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error  �޼����� ������ �ִ� ������ �� bean Ŭ����
 * Json ������̼��� ����ؼ� JSON ���·� ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ErrorMessage {

	@JsonProperty("description")
	private String message;

	public ErrorMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
