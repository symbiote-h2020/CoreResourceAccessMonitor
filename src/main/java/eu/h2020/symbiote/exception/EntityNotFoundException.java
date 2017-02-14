package eu.h2020.symbiote.exception;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 *
 * @author Vasileios Glykantzis <vasgl@intracom-telecom.com>
 */
public class EntityNotFoundException extends AmqpRejectAndDontRequeueException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
}