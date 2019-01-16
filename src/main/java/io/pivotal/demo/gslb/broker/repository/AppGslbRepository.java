package io.pivotal.demo.gslb.broker.repository;

import io.pivotal.demo.gslb.broker.model.AppGslb;
import org.springframework.data.repository.CrudRepository;

public interface AppGslbRepository extends CrudRepository<AppGslb, String> {

}
