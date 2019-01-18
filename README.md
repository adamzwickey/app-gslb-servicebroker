# app-gslb-servicebroker
Service Broker that provides app-specific GSLB capabilities for PCF Apps.  When consumed by an application the service broker create-service command prepares and configures a region-specfic app pool with a healthcheck for the app.  The bind-service command registers region-specific applications (or the routers serving the application) with the application pool.  By deploying the service broker to two or more PCF foundations, your application traffic will be globally distributed with application-specific health checks to ensure that the application is available in a specific foundation.

 <kbd>!["Generic IaaS Arch"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/refarch.png?raw=true)</kbd> 
 
 ## GCP Architecture and Design
 The implementation of the service broker that integrates with GCP load balancing services is designed as follows:
 
 ** Prerequisite: An existing GCP Load Balancer **
  
 * Executing create-service creates an HTTP health check object for the application service, creates an empty backend representing the application and links the health check with the backend, and creates a URL map that will route all traffic to the host + domain of the application to the backend.
 ```bash
 cf create-service app-gslb standard fortune-gslb -c '{"host":"fortune"}'
 ```
 <kbd>!["CF Create Service"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/create.png?raw=true)</kbd>
 
 * Executing bind-service creates an instancegroup for each zone that has at least one PCF router within the load balancer backend.
 ```bash
  cf bind-service fortune-app fortune-gslb
  ``` 
 <kbd>!["CF Bind Service"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/bind.png?raw=true)</kbd>
 
 
 Executing this against a service broker deployed to two different PCF foundations deployed to two different GCP regions results in a global load balancing backend that will distribute traffic to healthy backends in both foundations.
 
 <kbd>!["Global App LB"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/global.png?raw=true)</kbd>
 
 <kbd>!["Global App LB Traffic"](https://github.com/azwickey-pivotal/app-gslb-servicebroker/blob/master/imgs/global-traffic.png?raw=true)</kbd>
 
 
 ### Steps to Deploy
  ** Prerequisites **
  * Select a new global wildcard domain, e.g. "*.apps.global.cloud.pcf.com", and generate a wildcard SSL certificate if using TLS.
  * Prepare a new GCP Load Balancer with a frontend bound to a static IP address.  Create a frontend for port 80 and 443 (if using TLS; terminate with the cert you just created)
  * Create a DNS entry that maps the static IP of the Load Balancer to the wildcard domain (e.g. "*.apps.global.cloud.pcf.com")
  * Add the domain created above as an additional shared domain to your PCF foundations
  * Deploy and app to the foundations and add an additional route using a hostname + the new shared domain (e.g. myapp.global.cloud.pcf.com)
  * The running Service Broker application requires a PCF Redis service instance.
  
  __Deployment__
  
 1. Place your service account json ojbect that has access to your GCP project in *src/resources/*
 2. Edit the file *src/resources/application.yml* to reflect your GCP project and name of your json account creds:
 ```yml
 gcp:
  project: fe-azwickey
  auth:
    json: 'secrets/FE-azwickey-44b8446078ff.json'
``` 
 3. Edit the file *src/resources/application.yml* to reflect your global app DNS setup in the pre-reqs section.  This entry should not contain the wildcard asterics:
 ```yml
gslb:
  defaultDomain: apps.global.cloud.pcf.com
``` 
 4. Build the application using maven:
 ```bash
 mvn clean package
``` 
 5. Build the application using maven:
 ```bash
 mvn clean package
``` 
6. Provision a Redis Service Instance to be consumed by the service broker application
 ```bash
cf create-service p-redis shared-vm gslb-database
``` 
7. Push the service broker application to PCF, binding in the redis service instance, and setting environment variables representing the GCP zones that contain PCF routers and the name of the http instance group for PCF:
 ```bash
cf push gslb -b java_buildpack_offline -p target/app-gslb-servicebroker-0.0.1-SNAPSHOT.jar --no-start
cf bind-service gslb gslb-database
cf set-env gslb GCP_ZONES us-east1-b,us-east1-c,us-east1-d
cf set-env gslb GCP_INSTANCEGROUP pcf-http-lb-east
cf start gslb
``` 
8. Repeat step 7 in any additional PCF foundations within different GCP regions.
9. Create and bind the gslb into an application.  The params available to be passed into the create-service command are as follows:

Param | Required | Description
--- | --- | ---
host | true | Set the hostname of the application route.  This will be prefixed to the domain
domain | false | Override the default hostname for application routing
healthcheck | false | Override the default healthcheck path ("/health")

 ```bash
cf create-service app-gslb standard my-app-gslb -c '{"host":"myapp"}'
cf bind-service my-app my-app-gslb
``` 
10. Repeat step 9 in any additional PCF foundations within different GCP regions.
11. After a few minutes (GCP takes about 4 minutes to put a new LB backend in service you app will be available) at myapp.apps.global.cloud.pcf.com
