package io.pivotal.demo.gslb.broker.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeRequestInitializer;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.*;
import io.pivotal.demo.gslb.broker.model.AppGslb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GCPGslbService implements InitializingBean, IAppGslbService {

    private static Logger LOG = LoggerFactory.getLogger(GCPGslbService.class);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            ComputeScopes.CLOUD_PLATFORM
    );

    private static DataStoreFactory dataStoreFactory;

    /** Global instance of the HTTP transport. */
    private static HttpTransport httpTransport;

    private static final ComputeRequestInitializer _cri = new ComputeRequestInitializer();

    @Value("classpath:${gcp.auth.json:auth.json}")
    private Resource _jsonFile;

    @Value("${spring.application.name}")
    private String APPLICATION_NAME;

    @Value("${gcp.prefix:app-gslb}")
    private String _prefix;

    @Value("${gcp.project}")
    private String _project;

    @Value("${gcp.zones}")
    private List<String> _zones;

    @Value("${gcp.instanceGroup}")
    private String _instanceGroup;

    private GoogleCredential _cred;
    final private List<Backend> _backends = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("Initializing GCP Service...");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new MemoryDataStoreFactory();

        //Init GCP Creds
        _cred = GoogleCredential.fromStream(_jsonFile.getInputStream())
                .createScoped(SCOPES);
        LOG.info("GoogleCredential initialized");
    }

    @Override
    public AppGslb createGslbServiceInstance(AppGslb lb) {
        try {
            //Create Health Check
            HttpHealthCheck httpHealthCheck = createHealthCheck(lb.getHost(), lb.getHealthCheck(), lb.getDomain());

            //Create Backend
            BackendService backendService = createBackendService(lb.getHost(), httpHealthCheck.getSelfLink());

            //Create Host Mapping
            createUrlMap(lb.getHost(), lb.getDomain(), backendService.getSelfLink());

        } catch(IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("something went wrong");
        }

        return lb;
    }

    @Override
    public void deleteGslbServiceInstance(AppGslb lb) {
        //delete GCP stuff --
        try {
            deleteUrlMap(lb.getHost(), lb.getDomain());
            deleteLbBackend(lb.getHost());
            deleteHealthCheck(lb.getHost());
        } catch(IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("something went wrong");
        }
    }

    @Override
    public AppGslb createGslbServiceInstanceBinding(AppGslb lb) {
        try {
            addBackends(lb.getHost());
        } catch(IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("something went wrong");
        }
        return lb;
    }

    @Override
    public void deleteGslbServiceInstanceBinding(AppGslb lb) {
        try {
            deleteBackends(lb.getHost());
        } catch(IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("something went wrong");
        }
    }

    private Compute getCompute() {
        return new Compute.Builder(
                httpTransport, JSON_FACTORY, null).setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(_cred).build();
    }

    private HttpHealthCheck createHealthCheck(String host, String healthCheckPath, String domain) throws IOException {
        //Check if it already exists
        Compute.HttpHealthChecks.Get get = getCompute().httpHealthChecks().get(_project, _prefix + "-" + host);
        _cri.initializeJsonRequest(get);
        try {
            HttpHealthCheck existingHc = get.execute();
            LOG.info("Health check [" + _prefix + "-" + host + "] exists");
            return existingHc;
        } catch(GoogleJsonResponseException exception) {   } //swallow because this means it exists


        LOG.info("Creating health check...");
        HttpHealthCheck hc = new HttpHealthCheck()
                .setName(_prefix + "-" + host)
                .setDescription("Application [" + host + "] healthcheck for GSLB")
                .setHost(host + "." + domain)
                .setPort(80)
                .setRequestPath(healthCheckPath)
                .setCheckIntervalSec(10)
                .setHealthyThreshold(2)
                .setUnhealthyThreshold(3)
                .setTimeoutSec(5);
        Compute.HttpHealthChecks.Insert insert = getCompute().httpHealthChecks().insert(_project, hc);
        _cri.initializeJsonRequest(insert);
        Operation saved = insert.execute();
        LOG.info("Health check operation created: " + saved);
        waitForOperation(saved);
        LOG.info("Health check created: " + saved);
        return hc.setSelfLink(saved.getTargetLink());
    }

    private void deleteHealthCheck(String host) throws IOException {
        //Verify if there is still backend from another region bound
        if(additionalRegionBound(host)) {
            LOG.info("Backend still available in another region; skipping delete");
            return;
        }

        Compute.HttpHealthChecks.Delete delete = getCompute().httpHealthChecks().delete(_project, _prefix + "-" + host);
        _cri.initializeJsonRequest(delete);
        Operation deleted = delete.execute();
        LOG.info("Health check delete operation created: " + deleted);
        waitForOperation(deleted);
        LOG.info("Health check delete created: " + deleted);
        return;
    }

    private BackendService createBackendService(String host, String hc) throws IOException {
        //Check if it already exists
        Compute.BackendServices.Get get = getCompute().backendServices().get(_project, _prefix + "-" + host);
        _cri.initializeJsonRequest(get);
        try {
            BackendService existingBackend = get.execute();
            LOG.info("Backend [" + _prefix + "-" + host + "] exists");
            return existingBackend;
        } catch(GoogleJsonResponseException exception) {   } //swallow because this means it exists

        LOG.info("Creating Backend Service...");
        BackendService bs = new BackendService()
                .setName(_prefix + "-" + host)
                .setDescription("Application [" + host + "] Backend Service for GSLB")
                .setTimeoutSec(5)
                .setHealthChecks(Arrays.asList(hc));
        Compute.BackendServices.Insert insert = getCompute().backendServices().insert(_project, bs);
        _cri.initializeJsonRequest(insert);
        Operation saved = insert.execute();

        LOG.info("Backend service operation created: " + saved);
        waitForOperation(saved);
        LOG.info("Backend service created: " + saved);
        return bs.setSelfLink(saved.getTargetLink());
    }

    private BackendService addBackends(String host) throws IOException {
        //Retrieving existing....
        BackendService orig = getBackend(host);

        //add Backends
        final List<Backend> backends = new ArrayList(getBackends());
        if(orig.getBackends() != null) backends.addAll(orig.getBackends());

        LOG.info("Adding Backends...");
        Operation saved = saveBackends(backends, host);
        LOG.info("Backends created: " + saved);
        return getBackend(host);
    }

    private BackendService deleteBackends(String host) throws IOException {
        //Retrieving existing....
        BackendService orig = getBackend(host);

        //remove backends for this deployment
        final List<Backend> backends = new ArrayList(orig.getBackends());
        orig.getBackends().forEach(backend -> {
            _zones.forEach(s -> {
                if(backend.getGroup().contains(s)) backends.remove(backend);
            } );
        });

        LOG.info("Saving updated backends...");
        Operation saved = saveBackends(backends, host);
        LOG.info("Backends created: " + saved);
        return getBackend(host);

    }

    private void deleteLbBackend(String host) throws IOException {
        //Verify if there is still backend from another region bound
        if(additionalRegionBound(host)) {
            LOG.info("Backend still available in another region; skipping delete");
            return;
        }

        Compute.BackendServices.Delete delete = getCompute().backendServices().delete(_project, _prefix + "-" + host);
        _cri.initializeJsonRequest(delete);
        Operation deleted = delete.execute();

        LOG.info("Backends delete operation created: " + deleted);
        waitForOperation(deleted);
        LOG.info("Backends delete operation finalized: " + deleted);

    }

    private BackendService getBackend(String host) throws IOException {
        Compute.BackendServices.Get get = getCompute().backendServices().get(_project, _prefix + "-" + host);
        _cri.initializeJsonRequest(get);
        return get.execute();
    }

    private Operation saveBackends(List<Backend> backends, String host) throws IOException {
        BackendService bs = new BackendService()
                .setBackends(backends);
        Compute.BackendServices.Patch patch = getCompute().backendServices().patch(_project, _prefix + "-" + host, bs);
        _cri.initializeJsonRequest(patch);
        Operation saved = patch.execute();

        LOG.info("Backends operation created: " + saved);
        waitForOperation(saved);
        LOG.info("Backends operation finalized: " + saved);
        return saved;
    }

    private UrlMap createUrlMap(String host, String domain, String backend) throws IOException {
        UrlMap origMap = getUrlMap();

        //check if there is a map for this host
        GCPGslbService.ObjectWrapper existing = new ObjectWrapper(new Boolean(false));
        origMap.getHostRules().forEach(hostRule -> {
            if (hostRule.getHosts().contains(host + "." + domain)) {
                LOG.info("URlMap host [" + host + "." + domain + "] exists");
                existing.setObject(new Boolean(true));
            }
        });
        if(((Boolean) existing.getObject()).booleanValue()) return origMap;

        //add new HostRule
        List hr = origMap.getHostRules();
        if(hr == null) hr = new ArrayList();
        hr.add(new HostRule()
                    .setPathMatcher(_prefix + "-" + host)
                    .setHosts(Arrays.asList(host + "." + domain))
        );
        //add new PathMatcher
        List pm = origMap.getPathMatchers();
        if(pm == null) pm = new ArrayList();
        pm.add(new PathMatcher().setName(_prefix + "-" + host).setDefaultService(backend));

        LOG.info("Patching Url Map...");
        UrlMap urlMap = new UrlMap()
                .setHostRules(hr)
                .setPathMatchers(pm);
        return patchUrlMap(urlMap);
    }

    private void deleteUrlMap(String host, String domain) throws IOException {

        //Verify if there is still backend from another region bound
        if(additionalRegionBound(host)) {
            LOG.info("Backend still available in another region; skipping delete");
            return;
        }

        UrlMap origMap = getUrlMap();

        //remove the  HostRule
        List<HostRule> hr = origMap.getHostRules();
        List<HostRule> newHr = new ArrayList<>();
        List<String> matchersToRemove = new ArrayList<>();
        if(hr != null) {
            hr.forEach(o -> {
                //Add Everything except the ones we're removing
                if(!o.getHosts().contains(host +"." + domain)) {
                    newHr.add(o);
                } else {
                    matchersToRemove.add(o.getPathMatcher());
                }
            });
        }
        LOG.info("New HostRules: " + newHr);

        //remove the Path Matcher
        List<PathMatcher> pm = origMap.getPathMatchers();
        List<PathMatcher> newPm = new ArrayList<>();
        if(pm != null) {
            pm.forEach(pathMatcher -> {
                //Add everything except what we're deleting
                if(!matchersToRemove.contains(pathMatcher.getName())) newPm.add(pathMatcher);
            });
        }
        LOG.info("New Path Matchers: " + newPm);

        LOG.info("Patching Url Map...");
        UrlMap urlMap = new UrlMap()
                .setHostRules(newHr)
                .setPathMatchers(newPm);
        patchUrlMap(urlMap);
        return;
    }

    private boolean additionalRegionBound(String host) throws IOException {
        try {
            BackendService backend = getBackend(host);
            LOG.info("Checking if additional regions are bound to this GSLB [" + host + "]");
            LOG.info("Backends List: " + backend.getBackends());
            if(backend.getBackends() == null || backend.getBackends().isEmpty()) {
                LOG.info("Backends List Empty");
                return false;
            } else {
                LOG.info("Backends List still populated");
                return true;
            }
        } catch(GoogleJsonResponseException exception) {   } //swallow because this means it exists
        return false;
    }

    private UrlMap getUrlMap() throws IOException {
        Compute.UrlMaps.Get get = getCompute().urlMaps().get(_project, _prefix);
        _cri.initializeJsonRequest(get);
        return get.execute();
    }

    private UrlMap patchUrlMap(UrlMap urlMap) throws IOException {
        Compute.UrlMaps.Patch patch = getCompute().urlMaps().patch(_project, _prefix, urlMap);
        _cri.initializeJsonRequest(patch);
        Operation saved = patch.execute();

        LOG.info("URL Map operation created: " + saved);
        waitForOperation(saved);
        LOG.info("URL Map created: " + saved);
        return urlMap.setSelfLink(saved.getTargetLink());
    }

    private List<Backend> getBackends() throws IOException {
        synchronized (this) {
            if(_backends.isEmpty()) {
                //get Instance Groups for each zone and create backend
                for(String z : _zones) {
                    Compute.InstanceGroups.List l = getCompute().instanceGroups().list(_project, z);
                    _cri.initializeJsonRequest(l);
                    InstanceGroupList igList = l.execute();
                    for(InstanceGroup ig : igList.getItems()) {
                        if(_instanceGroup.equalsIgnoreCase(ig.getName())) _backends.add(new Backend().setGroup(ig.getSelfLink()));
                    }
                }

            }
        }
        return _backends;
    }

    private void waitForOperation(Operation op) throws IOException {
        LOG.info("Verifying Operation [" + op.getSelfLink() + "] completes");
        boolean ready = false;
        while(!ready) {
            try { Thread.sleep(500); } catch (InterruptedException ex) { ex.printStackTrace();}
            Compute.GlobalOperations.Get get = getCompute().globalOperations().get(_project, op.getName());
            _cri.initializeJsonRequest(get);
            Operation operation = get.execute();
            String status = operation.getStatus();
            LOG.info("Operation status: " + status);
            ready = ("Done".equalsIgnoreCase(status)) ? true : false;
        }
    }

    class ObjectWrapper {
        private Object obj;
        public ObjectWrapper(Object obj) { this.obj = obj; }
        public Object getObject() {return obj;}
        public void setObject(Object obj) { this.obj = obj; }
    }
}
