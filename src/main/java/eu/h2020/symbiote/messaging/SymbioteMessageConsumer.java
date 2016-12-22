package eu.h2020.symbiote.messaging;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by Mael on 07/09/2016.
 */
public abstract class SymbioteMessageConsumer<T> extends DefaultConsumer {

    private static Log log = LogFactory.getLog(SymbioteMessageConsumer.class);

    private final TypeToken<T> typeToken = new TypeToken<T>(getClass()) { };
    private final Type type = typeToken.getType();

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public SymbioteMessageConsumer(Channel channel) {
        super(channel);
    }


    public Type getType() {
        return type;
    }

    /**
     * Method extracts Object from received JSON message and pass it to handleEventObject method, that lets use that object
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param body
     * @throws IOException
     */
    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        String message = new String(body, "UTF-8");
        log.info(" [x] Received '" + message + "'");

        Gson gson = new Gson();
        T deliveredObject = gson.fromJson(message, getType() );
        handleEventObject( deliveredObject );
    }

    protected abstract void handleEventObject(T deliveredObject);
}