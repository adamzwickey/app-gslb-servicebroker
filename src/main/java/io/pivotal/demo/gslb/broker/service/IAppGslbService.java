package io.pivotal.demo.gslb.broker.service;

import io.pivotal.demo.gslb.broker.model.AppGslb;

public interface IAppGslbService {

    AppGslb createGslbServiceInstance(AppGslb lb);
    void deleteGslbServiceInstance(AppGslb lb);
    AppGslb createGslbServiceInstanceBinding(AppGslb lb);
    void deleteGslbServiceInstanceBinding(AppGslb lb);


}
