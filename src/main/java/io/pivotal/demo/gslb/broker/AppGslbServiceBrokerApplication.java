package io.pivotal.demo.gslb.broker;

import io.pivotal.demo.gslb.broker.service.GCPGslbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppGslbServiceBrokerApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(AppGslbServiceBrokerApplication.class, args);
	}

	@Autowired
	GCPGslbService service;

	@Override
	public void run(String... args) throws Exception {
//		try {
//			service.createHealthCheck("adam", "/testing", "apps.public.cloud.zwickey.net");
//		} catch (Exception ex) {ex.printStackTrace();}
//		try {
//			service.createBackendService("adam",
//					"https://www.googleapis.com/compute/v1/projects/fe-azwickey/global/httpHealthChecks/app-gslb-adam");
//		} catch (Exception ex) {ex.printStackTrace();}
//		try {
//			service.createUrlMap("adam", "apps.public.cloud.zwickey.net",
//					"https://www.googleapis.com/compute/v1/projects/fe-azwickey/global/backendServices/app-gslb-adam");
//		} catch (Exception ex) {ex.printStackTrace();}
//		try {
//			service.addBackends("adam");
//		} catch (Exception ex) {ex.printStackTrace();}
	}
}
