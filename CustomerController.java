package com.infosys.infytel.customer.controller;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import com.infosys.infytel.customer.dto.CustomerDTO;
import com.infosys.infytel.customer.dto.LoginDTO;
import com.infosys.infytel.customer.dto.PlanDTO;
import com.infosys.infytel.customer.service.CustomerService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@RestController
@CrossOrigin
public class CustomerController {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	CustomerService custService;
	@Autowired
	 DiscoveryClient client;

	/*
	@Value("${friend.uri}")
	//String friendUri;

	@Value("${plan.uri}")
	//String planUri;

	*/
	// Create a new customer
	@PostMapping(value = "/customers",  consumes = MediaType.APPLICATION_JSON_VALUE)
	public void createCustomer(@RequestBody CustomerDTO custDTO) {
		logger.info("Creation request for customer {}", custDTO);
		custService.createCustomer(custDTO);
	}

	// Login
	@PostMapping(value = "/login",consumes = MediaType.APPLICATION_JSON_VALUE)
	public boolean login(@RequestBody LoginDTO loginDTO) {
		logger.info("Login request for customer {} with password {}", loginDTO.getPhoneNo(),loginDTO.getPassword());
		return custService.login(loginDTO);
	}

	// Fetches full profile of a specific customer
	@HystrixCommand(fallbackMethod="getCustomerProfileFallback")
	@GetMapping(value = "/customers/{phoneNo}",  produces = MediaType.APPLICATION_JSON_VALUE)
	public CustomerDTO getCustomerProfile(@PathVariable Long phoneNo) {
		System.out.println("===In Profile===="+phoneNo);
		logger.info("Profile request for customer {}", phoneNo);
		
		CustomerDTO custDTO=custService.getCustomerProfile(phoneNo);
		PlanDTO planDTO=new RestTemplate().getForObject("http://PlanMS/"+"/plans/"+custDTO.getCurrentPlan().getPlanId(), PlanDTO.class);
		custDTO.setCurrentPlan(planDTO);
		
		@SuppressWarnings("unchecked")
		List<Long> friends=new RestTemplate().getForObject("http://FriendFamilyMS/"+"/customers/"+phoneNo+"/friends", List.class);
		custDTO.setFriendAndFamily(friends);
		
		
           List<ServiceInstance> instances=client.getInstances("FriendFamilyMS");
           ServiceInstance instance=instances.get(0);
            URI friendUri = instance.getUri();
            
            List<ServiceInstance> instances1=client.getInstances("PlanMS");
            ServiceInstance instance1=instances1.get(0);
             URI palnUri = instance1.getUri(); 
		          return custDTO;
	}
	public CustomerDTO getCustomerProfileFallback(Long phoneNo) {
		System.out.println("===In FallBack===="+phoneNo);
		return new CustomerDTO();

}	}
