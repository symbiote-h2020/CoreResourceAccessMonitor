package eu.h2020.symbiote.cram.repository;

import eu.h2020.symbiote.cram.model.CramPersistentVariables;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by lebro_000 on 6/30/2017.
 */
@RepositoryRestResource(collectionResourceRel = "cramPersistentVariables", path = "cramPersistentVariables")
public interface CramPersistentVariablesRepository extends MongoRepository<CramPersistentVariables, String> {
}