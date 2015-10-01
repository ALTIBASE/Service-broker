package org.openpaas.servicebroker.model;

/**
 * ���� �ν��Ͻ��� ���� �� �� �ʿ��� Request ������ ������ �ִ� ������ �� bean Ŭ����. 
 * Json ������̼��� ����ؼ� JSON ���·� ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
public class DeleteServiceInstanceRequest {

	private final String serviceInstanceId;
	private final String serviceId;
	private final String planId;

	public DeleteServiceInstanceRequest(String instanceId, String serviceId,
			String planId) {
		this.serviceInstanceId = instanceId; 
		this.serviceId = serviceId;
		this.planId = planId;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getPlanId() {
		return planId;
	}

}
