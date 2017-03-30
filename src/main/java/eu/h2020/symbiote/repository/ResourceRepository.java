package eu.h2020.symbiote.repository;

import eu.h2020.symbiote.core.model.resources.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by mateuszl on 22.09.2016.
 */

@RepositoryRestResource(collectionResourceRel = "resource", path = "resource")
public interface ResourceRepository extends MongoRepository<Resource, String> {
}