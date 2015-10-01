package org.openpaas.servicebroker.exception;

import org.openpaas.servicebroker.model.ServiceInstanceBinding;



/**
 * Bindig id �� �ش��ϴ� ���� �ν��Ͻ��� �����ϴ� ���ExceptionŬ���� . 
 * Exception Ŭ������ �����.
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class ServiceInstanceBindingExistsException extends Exception {

	private static final long serialVersionUID = -914571358227517785L;
	
	public ServiceInstanceBindingExistsException(ServiceInstanceBinding binding) {
		super("ServiceInstanceBinding already exists: serviceInstanceBinding.id = "
				+ binding.getId()
				+ ", serviceInstance.id = " + binding.getServiceInstanceId());
	}

}
