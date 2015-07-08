package org.openpaas.servicebroker.apiplatform.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openpaas.servicebroker.apiplatform.common.ApiPlatformUtils;
import org.openpaas.servicebroker.apiplatform.dao.APIServiceInstanceDAO;
import org.openpaas.servicebroker.apiplatform.exception.APIServiceInstanceException;
import org.openpaas.servicebroker.apiplatform.model.APIServiceInstance;
import org.openpaas.servicebroker.apiplatform.model.APIUser;
import org.openpaas.servicebroker.common.HttpClientUtils;
import org.openpaas.servicebroker.common.JsonUtils;
import org.openpaas.servicebroker.exception.ServiceBrokerException;
import org.openpaas.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.openpaas.servicebroker.exception.ServiceInstanceExistsException;
import org.openpaas.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.openpaas.servicebroker.model.CreateServiceInstanceRequest;
import org.openpaas.servicebroker.model.DeleteServiceInstanceRequest;
import org.openpaas.servicebroker.model.ServiceDefinition;
import org.openpaas.servicebroker.model.ServiceInstance;
import org.openpaas.servicebroker.model.UpdateServiceInstanceRequest;
import org.openpaas.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class APIServiceInstanceService implements ServiceInstanceService {

	private static final Logger logger = LoggerFactory.getLogger(APIServiceInstanceService.class);
	
	@Autowired
	private Environment env;
	
	@Autowired
	APIServiceInstanceDAO dao;
	
	@Autowired
	LoginService loginService;
	
	@Autowired
	APICatalogService apiCatalogService;
	
	@Override
	public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request) throws ServiceInstanceExistsException, ServiceBrokerException 
	{
		ServiceInstance instance = new ServiceInstance(request);
		logger.info("create ServiceInstance");
		
		String organizationGuid =instance.getOrganizationGuid();
		String serviceInstanceId = instance.getServiceInstanceId();
		String spaceGuid = instance.getSpaceGuid();
		String serviceId = instance.getServiceDefinitionId();
		String planId = instance.getPlanId();
		String planName = planId.split(" ")[2];
		
		String serviceName = serviceId.split(" ")[0];
		String serviceVersion = serviceId.split(" ")[1];
		String serviceProvider = apiCatalogService.svc.getMetadata().get("providerDisplayName").toString();
		
		HttpEntity<String> httpEntity;
		ResponseEntity<String> responseEntity;
		HttpHeaders headers;
		
		String cookie = "";
		
		
	//요청된 플랜명이 설정된 플랜명과 동일한지 확인
		//TODO
		boolean planAvailable = false;
		String planSetted;
		int i = 1;
		do{
			planSetted = env.getProperty("AvailablePlan"+i);
			i++;
			if(planSetted.equals(planName)){
				planAvailable=true;
				System.out.println(planName);
				break;
			}
		} while((env.getProperty("AvailablePlan"+i)!=null));

		if(!planAvailable){
			throw new ServiceBrokerException("invalid PlanName :["+request.getPlanId().split(" ")[2]+"]");
		}

		
	//해당 인스턴스 아이디로 DB에 저장된 정보가 있는지 확인한다.
		boolean instanceExsistsAtDB = false;
		APIServiceInstance apiServiceInstacne = new APIServiceInstance();
		try {
			apiServiceInstacne=dao.getAPIServiceByInstanceAndOrgID(serviceInstanceId, organizationGuid);
			//이미 삭제된 인스턴스와 동일한 인스턴스 ID 및 org ID로 요청이 들어온 경우
			//TODO 삭제된 인스턴스 처리를 어떻게 할지에 대한 결정에 따라 가변적
			if(apiServiceInstacne.getDelyn().equals("Y")){
				logger.info("Service Instance : ["+serviceInstanceId+"] already removed" );
				throw new ServiceBrokerException("already removed Service Instance : ["+serviceInstanceId+"]");
			}
			instanceExsistsAtDB = true;

		} catch (EmptyResultDataAccessException e) {
			//EmptyResultDataAccessException이 던져지는 경우는 DB에 해당 인스턴스 아이디가 없다는 의미이므로instanceExsistsAtDB 값을 false로 변경한다.
			instanceExsistsAtDB = false;
			logger.debug("not found Service at APIPlatform Database");
		} catch (Exception e){
			//다른 DB에러에 대한 예외를 처리한다.
			logger.error("APIPlatform Database Error - getAPIServiceByInstanceID");
			throw new ServiceBrokerException(e.getMessage());
		}
		
	//해당 인스턴스 아이디로 DB에 저장된 서비스와 요청이들어온 서비스가 다를 경우, 409 예외를 발생시킨다.
		if(instanceExsistsAtDB){
			//인스턴스 아이디에 대한 org 아이디가 DB의 org아이디와 요청된 org아이디가 다른 경우이다.
			if(!apiServiceInstacne.getOrganization_id().equals(organizationGuid)){
				logger.error("incorrect orgID requested. OrgId :["+organizationGuid+"]");
				throw new ServiceInstanceExistsException(instance);			
			}
			//DB에 저장된 서비스명, 플랜명과 일치하면 이미 존재하는 인스턴스이므로 예외없이 통과시킨다.
			if(!apiServiceInstacne.getService_id().equals(serviceId)&&!apiServiceInstacne.getPlan_id().equals(planId)){
				logger.error("invalid ServiceName :["+request.getServiceDefinitionId().split(" ")[0]+"]. valid ServiceName: ["+apiServiceInstacne.getService_id().split(" ")[0]+"], "
						+ "invalid PlanName :["+request.getPlanId().split(" ")[2]+"]. valid PlanName: ["+apiServiceInstacne.getPlan_id().split(" ")[2]+"]");
				throw new ServiceInstanceExistsException(instance);
			}
			else if(!apiServiceInstacne.getService_id().equals(serviceId)){
				logger.error("invalid ServiceName :["+request.getServiceDefinitionId().split(" ")[0]+"]. valid ServiceName: ["+apiServiceInstacne.getService_id().split(" ")[0]+"]");
				throw new ServiceInstanceExistsException(instance);
			}
			else if(!apiServiceInstacne.getPlan_id().equals(planId)){
				logger.error("invalid PlanName :["+request.getPlanId().split(" ")[2]+"]. valid PlanName: ["+apiServiceInstacne.getPlan_id().split(" ")[2]+"]");
				throw new ServiceInstanceExistsException(instance);
			}
			else{
				logger.info("Service Instance already exists. InstanceID :["+serviceInstanceId+"]");
			}
		}

	//서비스 브로커의 DB에서 organizationGuid를 확인하여 API플랫폼 로그인 아이디와 비밀번호를 획득한다.
		
		APIUser userInfo = new APIUser();
		try{
			userInfo = dao.getAPIUserByOrgID(organizationGuid);
		} catch(EmptyResultDataAccessException e){
			//새로운 유저인 경우, API User DB에 유저정보가 없기 때문에 DAO에서 예외를 던진다.
			logger.info("not found User at APIPlatform Database");
		} catch (Exception e){
			//다른 DB에러에 대한 예외를 처리한다.
			logger.error("APIPlatform Database Error - getAPIUserByOrgID");
			throw new ServiceBrokerException(e.getMessage());
		}
		
		String userId = userInfo.getUser_id();
		String userPassword = userInfo.getUser_password();
		logger.info("User Id : "+userId);
		
	//API플랫폼에 등록되지 않은 유저일때, 유저아이디를 생성하여 등록한다.
		boolean userIdDuplication;
		if(userId==null){
			logger.info("not registered API Platform User");
			userPassword=env.getProperty("UserSignupPassword");
			
			//API플랫폼에서 유저아이디의 중복여부를 확인하여, 중복인 경우 유저아이디를 재생성하여 등록한다.
			do{
				userId = makeUserId();
				userIdDuplication = userSignup(userId,userPassword);
				logger.info("API Platform User Duplication Check");
			} while(userIdDuplication);
			logger.info("API Platform User Created userId : "+userId);
			
			//DB에 생성한 유저정보를 저장하고 문제가 있을 경우 예외를 발생시킨다.
			try {
				dao.insertAPIUser(organizationGuid, userId, userPassword);				
			} catch (Exception e) {
				logger.error("Insert API User Error");
				throw new ServiceBrokerException("Database Error - Insert API User");	
			}
			logger.info("APIUser insert finished");
			
			//생성한 user ID로 로그인하고 쿠키값을 변수에 저장한다.
			cookie = loginService.getLogin(userId, userPassword);
			logger.info("API Platform Login - UserID : "+userId);
		}
		//요청받은 organizationGuid와 매칭되는 API플랫폼 유저정보를 DB에서 찾은 경우이다.
		else {
			try {
				cookie = loginService.getLogin(userId, userPassword);
			} catch (Exception e) {
				//DB에는 유저정보가 저장되어 있으나 API플랫폼에서는 유저가 삭제된 경우이다.
				if(e.getMessage().equals("No User")){
					//DB에 저장된 유저정보로 API플랫폼의 유저를 생성하고 로그인하여 쿠키값을 저장한다.
					userSignup(userId,userPassword);
					cookie = loginService.getLogin(userId, userPassword);
					logger.info("API Platform Login - UserID : "+userId);
				} else{
					//로그인시 유저가 없는 경우를 제외한 나머지 예외가 발생시 처리한다.
					throw new ServiceBrokerException(e.getMessage());	
				}
			}		
		}
		
	//Add an Application API를 사용한다. error:false와 status: APPROVED가 리턴되어야 정상
		boolean applicationExsists = false;
		
		logger.info("Add An Application API Start");
		
		headers = new HttpHeaders();
		headers.set("Cookie", cookie);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		//Add an Application API를 보낸다.
		
		String addApplicationUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.AddAnApplication");
		String addApplicationParameters = 
				"action=addApplication&application="+serviceInstanceId+"&tier="+planName+"&description=&callbackUrl=";		
		httpEntity = new HttpEntity<String>(addApplicationParameters, headers);

		responseEntity = HttpClientUtils.send(addApplicationUri, httpEntity, HttpMethod.POST);
		
		JsonNode addApplicationResponseJson = null;

		try {
			addApplicationResponseJson = JsonUtils.convertToJson(responseEntity);
			ApiPlatformUtils.apiPlatformErrorMessageCheck(addApplicationResponseJson);
			applicationExsists = true;
			//어플리케이션이 정상적으로 생성 되었다면 넘어간다.
			logger.info("Application "+serviceInstanceId+" created");		
		} catch (ServiceBrokerException e) {
			if(e.getMessage().equals("Duplication")){
				//어플리케이션이 이미 존재하는 경우이므로 applicatioExsists를 true로 변경한다.
				logger.info("application already exists");
				applicationExsists = true;
			} else{
				//그밖의 예외처리
				logger.error("API Platform response error - Add an Application");
				throw new ServiceBrokerException(e.getMessage());
			}
		}
		
		logger.info("API Platform Application added");
		
	//Generate Key API를 사용하여 API플랫폼 어플리케이션의 키값을 생성한다.
		logger.info("API Platform Generate Key Started. Application : "+serviceInstanceId);
		//두가지 키타입으로 키를 생성해야하기 때문에 두번 실행한다.
		for(i=1;i<3;i++) {	
			logger.debug("API Platform Generate Key Started - KEYTYPE : "+env.getProperty("Keytype"+i));
			headers.set("Cookie", cookie);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			String generateKeyUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.GenerateAnApplicationKey");
			String generateKeyParameters = 
					"action=generateApplicationKey&application="+serviceInstanceId+"&keytype="+env.getProperty("Keytype"+i)+"&callbackUrl=&authorizedDomains=ALL&validityTime=360000";
		
			httpEntity = new HttpEntity<String>(generateKeyParameters, headers);
			responseEntity = HttpClientUtils.send(generateKeyUri, httpEntity, HttpMethod.POST);
	
			JsonNode generateKeyResponseJson = null;
			
			try {
				generateKeyResponseJson = JsonUtils.convertToJson(responseEntity);
				ApiPlatformUtils.apiPlatformErrorMessageCheck(generateKeyResponseJson);
				logger.debug(env.getProperty("Keytype"+i)+" Key generated. Application : "+serviceInstanceId);
			} catch (ServiceBrokerException e) {
				if(e.getMessage().equals("Key already generated"))
				{
					logger.debug(env.getProperty("Keytype"+i)+" Key already generated");
				} else {
					logger.error("API Platform response error - Generate Key KEYTYPE : "+env.getProperty("Keytype"+i));
					throw new ServiceBrokerException(e.getMessage());					
				}
				logger.debug("API Platform Generate Key finished - KEYTYPE : "+env.getProperty("Keytype"+i));
			}
		}
		logger.info("API Platform Generate Key finished. Application : "+serviceInstanceId);

	//Add Subscription API를 사용하여 API플랫폼 어플리케이션에 API를 사용등록한다.

		boolean subscriptionExists=false;
		logger.info("API Platform Add a Subscription started");
		String addSubscriptionUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.AddSubscription");
		String addSubscriptionParameters = 
				"action=addAPISubscription&name="+serviceName+"&version="+serviceVersion+"&provider="+serviceProvider+"&tier="+planName+"&applicationName="+serviceInstanceId;
		
		headers.set("Cookie", cookie);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		httpEntity = new HttpEntity<String>(addSubscriptionParameters, headers);

		ResponseEntity<String> addSubscriptionResponseHttp = HttpClientUtils.send(addSubscriptionUri, httpEntity, HttpMethod.POST);
		
		JsonNode addSubscriptionResponseJson = null;

		try {
			
			addSubscriptionResponseJson = JsonUtils.convertToJson(addSubscriptionResponseHttp);
			ApiPlatformUtils.apiPlatformErrorMessageCheck(addSubscriptionResponseJson);
			//사용등록이 정상적으로 이루어진 경우
			subscriptionExists = true;		
		} catch (ServiceBrokerException e){
			if(e.getMessage().equals("Duplication"))
			{	//이미 사용등록이 되어있는 경우
				subscriptionExists = true;
				logger.info("API already subscribed");
			}
			else{
				logger.error("API Platform response error - Add a Subscription");
				throw new ServiceBrokerException(e.getMessage());
			}
		}
		logger.info("API Platform Subscription finished");
		

		//인스턴스ID가 존재하고 해당 어플리케이션, 서비스가 모두 존재하는 경우
		//TODO 
		if(instanceExsistsAtDB&&applicationExsists&&subscriptionExists){
			logger.info("Service instance already exists");
			instance.setHttpStatusOK();
			return instance;
		}
		else {	
		//API서비스 인스턴스 정보를 API플랫폼 DB에 저장한다.
			try {
				dao.insertAPIServiceInstance(organizationGuid,serviceInstanceId,spaceGuid,serviceId,planId);
			} catch (Exception e) {
				logger.error("API Service Instance insert failed");
				throw new APIServiceInstanceException("Database Error: API ServiceInstance insert");		
			}
			logger.info("API Service Instance insert finished");
		}
		
		logger.info("createServiceInstance() completed");
		return instance;
	}

	
	

	
	@Override
	public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) throws ServiceBrokerException 
	{
		
		String serviceInstanceId = request.getServiceInstanceId();
		String serviceId = request.getServiceId();
		String planId = request.getPlanId();
		
		APIServiceInstance apiServiceInstance;
		try {
			apiServiceInstance = dao.getAPIInfoByInstanceID(serviceInstanceId);	

			logger.debug("get API Information");
		} catch (EmptyResultDataAccessException e) {
			logger.error("not found infomation about instance ID : "+serviceInstanceId);
			return null;
		} catch (Exception e){
			logger.error("Database Error - getServiceInstanceByInstanceID()");
			throw new ServiceBrokerException(e.getMessage());
		}
		//DB의 인스턴스 정보와 CF에서 디프로비전을 요청한 인스턴스 정보가 일치하는지 확인한다.
		if(!apiServiceInstance.getService_id().equals(serviceId)||!apiServiceInstance.getPlan_id().equals(planId)){
			logger.error("Database found Instance - InstanceID:["+serviceInstanceId+"], ServiceID: ["+apiServiceInstance.getService_id()+"] PlanID: ["+apiServiceInstance.getPlan_id()+"]");
			throw new ServiceBrokerException("Invalid information requested");
		}
		
	//로그인한다.
		String userId = apiServiceInstance.getUser_id();
		String userPassword = apiServiceInstance.getUser_password();
		String cookie = "";
		
		try {
			cookie = loginService.getLogin(userId, userPassword);
			logger.info("API Platform login success");
		} catch (ServiceBrokerException e) {
			if(e.getMessage().equals("No User")){
			//유저가 API플랫폼에 등록이 되어있지 않을 경우, 유저등록을 한다. 유저는 반드시 존재해야 하기 때문에 유저를 생성하고 예외를 던진다.
				logger.warn("not found API Platform User");
				userSignup(userId, userPassword);
				throw new ServiceBrokerException("API Platform Error : removed API Platfotm user. User created ["+userId+"]");
			}
			else {
				logger.error("APIPlatform User Signup Error");
				throw new ServiceBrokerException("APIPlatform Error :"+e.getMessage());
			}
		}
		
	//getApplication API를 이용하여 어플리케이션 ID를 가져온다.
		HttpEntity<String> httpEntity;
		ResponseEntity<String> responseEntity;
		HttpHeaders headers = new HttpHeaders();
	
		headers.set("Cookie", cookie);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	
		String getApplicationsUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.GetApplications");
		String getApplicationParameter = "action=getApplications";
		
		httpEntity = new HttpEntity<String>("", headers);
		responseEntity = HttpClientUtils.send(getApplicationsUri+"?"+getApplicationParameter, httpEntity, HttpMethod.GET);
		
		JsonNode getApplicationsResponseJson = null;
		
		try {
			getApplicationsResponseJson =JsonUtils.convertToJson(responseEntity);
			ApiPlatformUtils.apiPlatformErrorMessageCheck(getApplicationsResponseJson);
			logger.debug("Get Applications completed");
		} catch (Exception e) {
			throw new ServiceBrokerException("APIPlatform Error :"+e.getMessage());
		}
		
		JsonNode applications = getApplicationsResponseJson.get("applications");
		
		int applicationId = 0;
		boolean applicationExists= false;
		for(JsonNode application:applications){
			if(application.get("name").asText().equals(serviceInstanceId)){
				applicationId = application.get("id").asInt();
				applicationExists = true;
				break;
			}
		}
		
	//어플리케이션이 존재하는 경우 Remove a Subscription API를 이용해 사용등록을 해제한다.
		if(applicationExists){	
		//removeSubscription API를 사용한다.
			String serviceName = serviceId.split(" ")[0];
			String serviceVersion = serviceId.split(" ")[1];
			ServiceDefinition serviceDefinition = apiCatalogService.getServiceDefinition(serviceId);
			boolean serviceExists = true;
			//서비스가 존재하지 않는 경우에 대한 처리를 한다. API플랫폼에서 서비스의 라이프 사이클을 변경했거나 API를 삭제한 경우이다.
			if(serviceDefinition==null){
				serviceExists =false;
				logger.warn("Service not found at API Platform.  Service : ["+serviceName+"]");
			}
			if(serviceExists){
				String serviceProvider = serviceDefinition.getMetadata().get("providerDisplayName").toString();
				
				String removeSubscriptionUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.RemoveSubscription");
				String removeSubscriptionParameters = "action=removeSubscription&name="+serviceName+"&version="+serviceVersion+"&provider="+serviceProvider+"&applicationId="+applicationId;
				
				httpEntity = new HttpEntity<String>(removeSubscriptionParameters, headers);
				responseEntity = HttpClientUtils.send(removeSubscriptionUri, httpEntity, HttpMethod.POST);
				
				JsonNode removeSubscriptionResponseJson = null;

				try {
					removeSubscriptionResponseJson =JsonUtils.convertToJson(responseEntity);
					ApiPlatformUtils.apiPlatformErrorMessageCheck(removeSubscriptionResponseJson);
					logger.debug("Remove a Subscription completed");
				} catch(ServiceBrokerException e){
					//Remove a Subscription API를 사용할때, 보내는 파라미터가 적절하지 않은 경우이다.
					if(e.getMessage().equals("invalid parameters")){
						logger.error("Invalid parameters Service name : ["+serviceName+"], Version : ["+serviceVersion+"], API Provider : ["+serviceProvider+"], Application ID : ["+applicationId+"]");
						throw new ServiceBrokerException("API Platform Error : Invalid Parameters Service "+serviceName);
					}
				} catch (Exception e) {
					throw new ServiceBrokerException("APIPlatform Error :"+e.getMessage());
				}
			}
				
		//Remove an Application API로 어플리케이션을 삭제한다.
			
			String removeApplicationUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.RemoveApplication");
			String removeApplicationParameters = "action=removeApplication&application="+serviceInstanceId;
			
			httpEntity = new HttpEntity<String>(removeApplicationParameters, headers);
			responseEntity = HttpClientUtils.send(removeApplicationUri, httpEntity, HttpMethod.POST);
			
			JsonNode removeApplicationResponseJson = null;
			
			try {
				removeApplicationResponseJson =JsonUtils.convertToJson(responseEntity);
				ApiPlatformUtils.apiPlatformErrorMessageCheck(removeApplicationResponseJson);
				logger.debug("Remove an Application completed");
			} catch(ServiceBrokerException e){
				//어플리케이션이 없거나 파라미터가 잘못 들어간 경우이다. 코드상 일어날 수 없는 에러
				if(e.getMessage().equals("No Application")){
					logger.debug("not found Application :"+serviceInstanceId);
				}
				
			} catch (Exception e) {
				throw new ServiceBrokerException("APIPlatform Error :"+e.getMessage());
			}
		}
		//API플랫폼에 해당 어플리케이션이 존재하지 않는 경우이다.
		else {
			
			logger.warn("APIPlatform Application not Exists.");
		}
		
		//DB에서 서비스 인스턴스 정보를 삭제상태로 변경한다.
		String oranizationGuid = apiServiceInstance.getOrganization_id();
		try {
			dao.deleteAPIServiceInstance(oranizationGuid, serviceInstanceId, serviceId, planId);			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		
		return new ServiceInstance(request);
	}
	
//서비스 인스턴스 정보를 가져온다.
	@Override
	public ServiceInstance getServiceInstance(String serviceInstanceId) throws ServiceBrokerException
	{
		APIServiceInstance apiServiceInstance = new APIServiceInstance();		
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest();
	
		try {
			apiServiceInstance=dao.getAPIInfoByInstanceID(serviceInstanceId);
			logger.debug("get API Information");
			//이미 delete명령으로 제거한 서비스인스턴스의 인스턴스 아이디로 요청이 들어온 경우
			//TODO 삭제된 인스턴스를 DB에서 어떻게 처리할지에 대해서 결정이 되면 수정
			if(apiServiceInstance.getDelyn().equals("Y")){
				logger.info("Service Instance : ["+serviceInstanceId+"] already removed" );
				throw new ServiceBrokerException("already removed Service Instance : ["+serviceInstanceId+"]");
			}
		} catch (EmptyResultDataAccessException e) {			
			logger.error("not found infomation about instance ID : "+serviceInstanceId);
			return null;
		} catch (Exception e){
			logger.error("Database Error - getServiceInstanceByInstanceID()");
			throw new ServiceBrokerException(e.getMessage());
		}
		
		String organizationGuid = request.getOrganizationGuid();
		String spaceId = apiServiceInstance.getSpace_guid();
		String serviceId = apiServiceInstance.getService_id();
		String planId = apiServiceInstance.getPlan_id();

		request = new CreateServiceInstanceRequest(serviceId,planId,organizationGuid,spaceId);
		
		ServiceInstance instance = new ServiceInstance(request.withServiceInstanceId(serviceInstanceId));
		logger.info("get Service Instance completed InstanceId : ["+serviceInstanceId+"] ServiceId : ["+serviceId+"]");
		
		return instance;
	}

//서비스 인스턴스의 플랜정보를 업데이트 한다.
	@Override
	public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest request) throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException 
	{
		String serviceInstanceId = request.getServiceInstanceId();
		String planId = request.getPlanId();
		String planName = planId.split(" ")[2];
		APIServiceInstance apiServiceInstance = null;
		
	//요청된 서비스 인스턴스에 대해 DB에 저장된 정보를 가져온다.
		try {
			apiServiceInstance =dao.getAPIInfoByInstanceID(serviceInstanceId);			
		} catch (EmptyResultDataAccessException e) {
			logger.error("not found InstanceID : "+serviceInstanceId);
			throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
		} catch (Exception e) {
			logger.error("Database Error - getAPIInfoByInstanceID");
			throw new ServiceBrokerException(e.getMessage());
		}
		
		if(apiServiceInstance.getPlan_id().equals(planId)){
			logger.debug("already setted plan requseted");
			throw new ServiceBrokerException("Plan : ["+planName+"] already setted");
		}
/*		
//	플랜의 업데이트 가능 여부를 확인한다.
		String planUpdatable;
		planUpdatable = env.getProperty("PlanUpdatable");
		
		//서비스 브로커의 플랜 업데이트 설정이 되어있지 않다면, 예외를 발생시킨다. 		
		if (!planUpdatable.equals("yes")){
			logger.info("Service Broker plan update not allowed");
			throw new ServiceInstanceUpdateNotSupportedException("Service Plan not updatable. Service Broker Updatable setting : ["+planUpdatable+"]");	
		}
	
*/	
	//업데이트 요청이 들어온 플랜명이 사용가능한 플랜명인지 확인한다.
		boolean planAvailable = false;
		String nextPlan = null;
		int i =1;
		do{
			if(env.getProperty("AvailablePlan"+i)!=null&&env.getProperty("AvailablePlan"+i).equals(planName)){
				planAvailable =true;
				logger.debug("Plan : ["+planName+"] availbale");
			}
			else {
				i++;
				nextPlan= env.getProperty("APIPlatformTier"+i);
			}
		} while(planAvailable==false&&nextPlan!=null);
		
		if(!planAvailable){
			logger.error("not supported Plan :"+planName);
			throw new ServiceBrokerException("Plan : ["+planName+"] not supported");
		}
		
	//플랜 업데이트를 가능하도록 설정했을때, API플랫폼 어플리케이션의 티어값을 업데이트한다.
		//요청된 인스턴스에 대한 API플랫폼 유저아이디와 패스워드로 API플랫폼에 로그인한다.
		String userId = apiServiceInstance.getUser_id();
		String userPassword = apiServiceInstance.getUser_password();
		String cookie = "";
		cookie = loginService.getLogin(userId, userPassword);	

		//Update an Application API를 사용한다.
		HttpEntity<String> httpEntity;
		ResponseEntity<String> responseEntity;
		HttpHeaders headers = new HttpHeaders();
		
		headers.set("Cookie", cookie);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		String updateApplicationUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.UpdateApplication");
		String updateApplicationParameters = "action=updateApplication&applicationOld="+serviceInstanceId+"&applicationNew="+serviceInstanceId+"&callbackUrlNew=&descriptionNew=&tier="+planName;
		
		httpEntity = new HttpEntity<String>(updateApplicationParameters, headers);
		responseEntity = HttpClientUtils.send(updateApplicationUri, httpEntity, HttpMethod.POST);
		
		JsonNode updateApplicationResponseJson = null;
		
		try {
			updateApplicationResponseJson =JsonUtils.convertToJson(responseEntity);
			ApiPlatformUtils.apiPlatformErrorMessageCheck(updateApplicationResponseJson);
			logger.debug("");
		} catch (Exception e) {
			throw new ServiceBrokerException(e.getMessage());
		}
		
		//Update an Application API는 에러메시지를 보내지 않기 때문에 Get Apllications API를 사용하여 API플랫폼에서 플랜업데이트가 정상적으로 이루어졌는지 확인한다. 
		String getApplicationsUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.GetApplications");
		String getApplicationParameter = "action=getApplications";
		
		httpEntity = new HttpEntity<String>("", headers);
		responseEntity = HttpClientUtils.send(getApplicationsUri+"?"+getApplicationParameter, httpEntity, HttpMethod.GET);
		
		JsonNode getApplicationsResponseJson = null;
		
		try {
			getApplicationsResponseJson =JsonUtils.convertToJson(responseEntity);
			ApiPlatformUtils.apiPlatformErrorMessageCheck(getApplicationsResponseJson);
			logger.debug("Get Applications completed");
		} catch (Exception e) {
			throw new ServiceBrokerException(e.getMessage());
		}
		
		//Json객체를 통해 API플랫폼에 등록된 어플리케이션과 플랜값이 업데이트를 요청한 어플리케이션, 플랜값과 일치하는지 확인한다.
		JsonNode applications =getApplicationsResponseJson.get("applications");
		String applicationName = null;
		String applicationTier=null;
		boolean updateSuccess =false;
		for(JsonNode application: applications){
			applicationName = application.get("name").asText();
			applicationTier = application.get("tier").asText();
			if(applicationName.equals(serviceInstanceId)&&applicationTier.equals(planName)){
				updateSuccess = true;
				break;
			}
		}
	
	//DB의 인스턴스 정보를 수정한다.
		if(updateSuccess){
			try {
				dao.updateAPIServiceInstance(serviceInstanceId, planId);
				logger.debug("updateAPIServiceInstance() completed. InstanceID : "+serviceInstanceId+" Plan : "+planId);
			} catch (Exception e) {
				logger.error("Database Error - updateAPIServiceInstance");
				throw new ServiceBrokerException("Database Error - updateAPIServiceInstance");
			}
			
			String oldPlanName = apiServiceInstance.getPlan_id().split(" ")[2];
			logger.info("Plan changed. InstanceID : ["+serviceInstanceId+"] Plan ["+oldPlanName+"] to ["+planName+"]");
		}
		else {
			//API플랫폼에 해당 어플리케이션이 존재하지 않거나 요청된 플랜명을 API플랫폼의 티어에서 찾을 수 없는 경우이다.
			logger.error("Application does not exists or Invalid Tier. User Id :["+userId+"], Application : ["+serviceInstanceId+"], Tier : ["+planName+"]");
			throw new ServiceBrokerException("Update failed caused by API Platform");
		}


		
		ServiceInstance instance = new ServiceInstance(request);
		logger.info("Update ServiceInstance completed. InstanceID : "+serviceInstanceId+" Plan : "+planId);
		return instance;
	}


//API 플랫폼의 유저생성을 위해 30자 이내의 유니크 유저아이디를 생성 
	private String makeUserId(){
		
		logger.debug("Start makeUserId()");
		
		String userId = UUID.randomUUID().toString().replace("-", "").substring(0, 30); 
		//TODO 30자 내로 유니크 문자를  얻을 수 있는 다른 방법을 찾아본다.
		
		return userId;
	}
	
//API플랫폼의 유저를 생성하고  아이디 중복여부를 판단
	public boolean userSignup(String userId, String userPassword) throws ServiceBrokerException{
		logger.info("API Platform UserSignup started");
		String cookie = "";
		boolean duplication= false;
		
		HttpHeaders headers = new HttpHeaders();	
		headers.set("Cookie", cookie);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		String signupUri = env.getProperty("APIPlatformServer")+":"+env.getProperty("APIPlatformPort")+env.getProperty("URI.UserSignup");
		String signupParameters = "action=addUser&username="+userId+"&password="+userPassword+"&allFieldsValues=";		
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(signupParameters, headers);

		ResponseEntity<String> responseEntity = HttpClientUtils.send(signupUri, httpEntity, HttpMethod.POST);
		
		JsonNode userSignupResponseJson = null;
		
		try {
			userSignupResponseJson = JsonUtils.convertToJson(responseEntity);
			
		//API플랫폼의 유저아이디를 생성하면서, 해당 아이디의 존재여부를 판단한다.
			if (userSignupResponseJson.get("error").asText()!="false") { 
				String apiPlatformMessage = userSignupResponseJson.get("message").asText();
				
				logger.info("API Platform Message : "+apiPlatformMessage);
				
				if(apiPlatformMessage.equals("User name already exists")){
					duplication = true;
					return duplication;
				}
				else{
					logger.error("API Platform response error - User Signup");
					throw new ServiceBrokerException(userSignupResponseJson.get("message").asText());
				}
			}
			
			ApiPlatformUtils.apiPlatformErrorMessageCheck(userSignupResponseJson);
		} catch (ServiceBrokerException e) {			
			throw new APIServiceInstanceException(e.getMessage());
		}
		logger.debug("API Platform User created");
		return duplication;
	}

}
