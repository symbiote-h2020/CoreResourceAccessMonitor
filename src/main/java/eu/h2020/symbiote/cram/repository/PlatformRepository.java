package eu.h2020.symbiote.cram.repository;


import eu.h2020.symbiote.model.mim.Platform;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by mateuszl on 22.09.2016.
 */
@RepositoryRestResource(collectionResourceRel = "platform", path = "platform")
public interface PlatformRepository extends MongoRepository<Platform, String> {
}