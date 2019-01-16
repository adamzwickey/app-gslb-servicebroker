package io.pivotal.demo.gslb.broker.service;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.CatalogService;
import org.springframework.stereotype.Service;

@Service
public class GslbCatalogService implements CatalogService {

    @Override
    public Catalog getCatalog() {
        return Catalog.builder()
                .serviceDefinitions(getServiceDefinition("123-abc"))
                .build();
    }

    @Override
    public ServiceDefinition getServiceDefinition(String serviceId) {
        return ServiceDefinition.builder()
                .id(serviceId)
                .name("app-gslb")
                .description("Application Binding into a GSLB w/ application-specific health checks")
                .bindable(true)
                .tags("pivotal", "gslb")
                .plans(getPlan())
                .build();
    }

    private Plan getPlan() {
        return Plan.builder()
                .id("xyz-789")
                .name("standard")
                .description("Application Binding into a GSLB w/ application-specific health checks")
                .free(true)
                .build();
    }
}
