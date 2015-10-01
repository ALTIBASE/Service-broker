package org.openpaas.servicebroker.exception;

/**
 * ���� �ν��Ͻ� Plan ���� ����� �������� ���� ���� ExceptionŬ���� . Exception Ŭ������ �����.
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceInstanceUpdateNotSupportedException extends Exception {

	private static final long serialVersionUID = 4719676639792071582L;

	public ServiceInstanceUpdateNotSupportedException(String message) {
		super(message);
	}

	public ServiceInstanceUpdateNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceInstanceUpdateNotSupportedException(Throwable cause) {
		super(cause);
	}

}
