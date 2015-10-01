package org.openpaas.servicebroker.exception;

import org.openpaas.servicebroker.model.ServiceInstance;


/**
 * id �� �ش��ϴ� ���� �ν��Ͻ���  �����ϴ� ���ExceptionŬ���� . Exception Ŭ������ �����.
 * 
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceInstanceExistsException extends Exception {

	private static final long serialVersionUID = -914571358227517785L;
	
	public ServiceInstanceExistsException(ServiceInstance instance) {
		super("ServiceInstance with the given ID already exists: " +
				"ServiceInstance.id = " + instance.getServiceInstanceId() +
				", Service.id = " + instance.getServiceDefinitionId());
	}

}
