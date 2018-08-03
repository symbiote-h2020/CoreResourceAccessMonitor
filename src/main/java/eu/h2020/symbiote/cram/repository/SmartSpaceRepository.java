package eu.h2020.symbiote.cram.repository;


import eu.h2020.symbiote.model.mim.SmartSpace;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mateuszl on 22.09.2016.
 */
@Repository
public interface SmartSpaceRepository extends MongoRepository<SmartSpace, String> {
}