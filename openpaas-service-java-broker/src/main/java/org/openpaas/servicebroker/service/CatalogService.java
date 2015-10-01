package org.openpaas.servicebroker.service;

import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.model.Catalog;
import org.openpaas.servicebroker.model.ServiceDefinition;


/**
 * Catalog ���񽺰� �����ؾ��ϴ� �޼ҵ带 ������ �������̽� Ŭ����
 * 
 * @author ��â��
 * @date 2015.0629
 */
public interface CatalogService {

	/**
	 * @return The catalog of services provided by this broker.
	 */
	Catalog getCatalog() throws ServiceBrokerException;

	/**
	 * @param serviceId  The id of the service in the catalog
	 * @return The service definition or null if it doesn't exist
	 */
	ServiceDefinition getServiceDefinition(String serviceId) throws ServiceBrokerException;
	
}
