package org.openpaas.servicebroker.exception;

/**
 * ���� ���Ŀ GeneralExceptionŬ���� . Exception Ŭ������ �����.
 * 
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceBrokerException extends Exception {

	private static final long serialVersionUID = -5544859893499349135L;

	public ServiceBrokerException(String message) {
		super(message);
	}

	public ServiceBrokerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceBrokerException(Throwable cause) {
		super(cause);
	}

}