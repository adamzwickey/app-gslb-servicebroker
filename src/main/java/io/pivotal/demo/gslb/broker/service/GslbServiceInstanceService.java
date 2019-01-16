package io.pivotal.demo.gslb.broker.service;

import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.HttpHealthCheck;
import io.pivotal.demo.gslb.broker.model.AppGslb;
import io.pivotal.demo.gslb.broker.repository.AppGslbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class GslbServiceInstanceService implements ServiceInstanceService {

    private static Logger LOG = LoggerFactory.getLogger(GslbServiceInstanceService.class);

    public static final String PARAM_DOMAIN = "domain";
    public static final String PARAM_HOST = "host";
    public static final String PARAM_HEALTHCHECK = "healthcheck";

    @Autowired
    private IAppGslbService _service;
    @Autowired
    private AppGslbRepository _repo;

    @Value("${gslb.defaultDomain}")
    private String _defaultDomain;

    @Value("${gslb.defaultHealthCheckPath}")
    private String _defaultHealthCheckPath;

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        LOG.info("Creating service instance: " + createServiceInstanceRequest);
        CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

        Map<String, Object> params = createServiceInstanceRequest.getParameters();
        AppGslb lb = new AppGslb();
        lb.setHost((String)params.get(PARAM_HOST));
        lb.setDomain((String) params.getOrDefault(PARAM_DOMAIN, _defaultDomain));
        lb.setHealthCheck((String) params.getOrDefault(PARAM_HEALTHCHECK, _defaultHealthCheckPath));
        lb.setId(createServiceInstanceRequest.getServiceInstanceId());

        _service.createGslbServiceInstance(lb);

        //store in repo
        _repo.save(lb);

        return responseBuilder.build();
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) {
        LOG.info("Deleting service instance: " + deleteServiceInstanceRequest);
        DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();

        Optional<AppGslb> op = _repo.findById(deleteServiceInstanceRequest.getServiceInstanceId());

        if(op.isPresent()) {
            _service.deleteGslbServiceInstance(op.get());
        }

        //delete from repo
        _repo.deleteById(deleteServiceInstanceRequest.getServiceInstanceId());

        return responseBuilder.build();
    }
}
