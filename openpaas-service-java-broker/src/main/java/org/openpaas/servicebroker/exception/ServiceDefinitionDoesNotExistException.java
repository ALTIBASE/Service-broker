package org.openpaas.servicebroker.exception;

/**
 * ServiceDefinition ������ �������� ���� ���ExceptionŬ���� . 
 * Exception Ŭ������ �����.
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceDefinitionDoesNotExistException extends Exception {
	
	private static final long serialVersionUID = -62090827040416788L;

	public ServiceDefinitionDoesNotExistException(String serviceDefinitionId) {
		super("ServiceDefinition does not exist: id = " + serviceDefinitionId);
	}
	
}