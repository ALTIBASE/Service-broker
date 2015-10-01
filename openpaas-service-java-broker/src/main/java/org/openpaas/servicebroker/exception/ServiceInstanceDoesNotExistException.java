package org.openpaas.servicebroker.exception;

/**
 * id �� �ش��ϴ� ���� �ν��Ͻ���  �����ϴ� �ʴ� ���ExceptionŬ���� . 
 * Exception Ŭ������ �����.
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceInstanceDoesNotExistException extends Exception {
	
	private static final long serialVersionUID = -1879753092397657116L;
	
	public ServiceInstanceDoesNotExistException(String serviceInstanceId) {
		super("ServiceInstance does not exist: id = " + serviceInstanceId);
	}

}
