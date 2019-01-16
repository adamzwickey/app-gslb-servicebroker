package io.pivotal.demo.gslb.broker.service;

import io.pivotal.demo.gslb.broker.model.AppGslb;
import io.pivotal.demo.gslb.broker.repository.AppGslbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class GslbServiceInstanceBindingService implements ServiceInstanceBindingService {

    private static Logger LOG = LoggerFactory.getLogger(GslbServiceInstanceBindingService.class);

    @Autowired
    private IAppGslbService _service;
    @Autowired
    private AppGslbRepository _repo;

    @Override
    public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest) {
        LOG.info("Creating service instance binding: " + createServiceInstanceBindingRequest);
        CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder();

        Optional<AppGslb> lb = _repo.findById(createServiceInstanceBindingRequest.getServiceInstanceId());
        _service.createGslbServiceInstanceBinding(lb.get());

        return responseBuilder.build();
    }

    @Override
    public DeleteServiceInstanceBindingResponse deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) {
        LOG.info("Deleting service instance binding: " + deleteServiceInstanceBindingRequest);
        DeleteServiceInstanceBindingResponseBuilder responseBuilder = DeleteServiceInstanceBindingResponse.builder();

        Optional<AppGslb> lb = _repo.findById(deleteServiceInstanceBindingRequest.getServiceInstanceId());
        _service.deleteGslbServiceInstanceBinding(lb.get());

        return responseBuilder.build();
    }
}
