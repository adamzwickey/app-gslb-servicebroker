# app-gslb-servicebroker
Service Broker that provides app-specific GSLB capabilities for PCF Apps.  When consumed by an application the service broker create-service command prepares and configures an region-specfic app pool with healthcheck.  The bind-service command registers region-specific applications (or the routers serving the application) with the application pool.  By deploying the service broker to two or more PCF foundations, your application traffic will be globally distributed with application-specific health checks to ensure that the application is available in a specific foundation.

 <kbd>!["Generic IaaS Arch"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/refarch.png?raw=true)</kbd> 
 
 ## GCP Architecture and Design
 The implementation of the service broker that integrates with GCP load balancing services is designed as follows:
 
 ** Prerequisite: An existing GCP Load Balancer
  
 * Executing create-service creates an HTTP health check object for the application service, creates an empty backend representing the application and links the health check with the backend, and creates a URL map that will route all traffic to the host + domain of the application to the backend.
 <kbd>!["Generic IaaS Arch"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/create.png?raw=true)</kbd>
 
 * Executing bind-service creates an instancegroup for each zone that has at least one PCF router within the load balancer backend. 
 <kbd>!["Generic IaaS Arch"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/bind.png?raw=true)</kbd>
 
 ### Steps to Deploy
 1. 
 2. 
 3. 