package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
* <h1>Subscribe to message queues./h1>
* The MessagingSubscription class declares the platform and resource exchanges
* and binds event-specific queues (CREATED, DELETED, UPDATED) 
* to the respective exchange.
*
* @author  Vasilis Glykantzis, Tilemachos Pechlivanoglou
* @version 1.0
* @since   2016-12-22 
*/

public class MessagingSubscriptions {

        private enum Exchange {
        PLATFORM,
        RESOURCE;  

        private String toLowerCase() {
            return name().toLowerCase();
        }      
    }

    private enum Event {
        CREATED,
        DELETED,
        UPDATED;  

        private String toLowerCase() {
            return name().toLowerCase();
        }      
    }


    //Value("${spring.application.name}")
    //private String component; // Consider a workaround to make it static. Spring does not support @Value to static fields
    private static String COMPONENT = "CoreResourceAccessMonitor"; // FIX ME: Take the value from the bootstrap proporties

    private static Log log = LogFactory.getLog(MessagingSubscriptions.class);

    private static boolean AUTO_ACK = false; // Enable Message acknowledgments

    private static boolean DURABLE = true; // Make the message durable

    private static boolean EXCLUSIVE = false; // Make the message durable

    private static boolean AUTO_DELETE = false; // Make the message durable

    /**
     * Use that method if you want to subscribe to receive messages.
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public static void subscribeForCRAM() throws IOException, TimeoutException {
        subscribeToEvent(Exchange.PLATFORM, Event.CREATED);
        subscribeToEvent(Exchange.PLATFORM, Event.UPDATED);
        subscribeToEvent(Exchange.PLATFORM, Event.DELETED);

        subscribeToEvent(Exchange.RESOURCE, Event.CREATED);
        subscribeToEvent(Exchange.RESOURCE, Event.UPDATED);
        subscribeToEvent(Exchange.RESOURCE, Event.DELETED);

    }


    /**
     * Creates a queue for a platform/resource event and binds it to an exchange.
     *
     * @param exchange The name of the exchange (i.e symbIoTe.platform or symbIoTe.resource)
     * @param event The name of the event (i.e. CREATED, DELETED, UPDATED)
     * @throws IOException
     * @throws TimeoutException
     */
    public static void subscribeToEvent( Exchange exchange, Event event ) throws IOException, TimeoutException {
        Channel channel = getChannel();
        String exchangeName = "symbIoTe." + exchange.toLowerCase();
        String queueName = "symbIoTe-" + COMPONENT + "-" + exchange.toLowerCase() + "-" + event.toLowerCase();
        String bindingKey = exchangeName + "." + event.toLowerCase();
        channel.exchangeDeclare(exchangeName, "topic");
        channel.queueDeclare(queueName, DURABLE, EXCLUSIVE, AUTO_DELETE, null);

        channel.queueBind(queueName, exchangeName, bindingKey);

        log.info("Receiver waiting for messages in queue " + queueName + " which is binded " + 
                 "in exchange " + exchangeName + " with a binding key " + bindingKey + " ....");

        switch (exchange) {
            case PLATFORM:
                platformEventConsumer (channel, event, queueName);
                break;
            case RESOURCE:
                resourceEventConsumer (channel, event, queueName);
                break;
        }

    }
    

    /**
     * Declares a consumer for platform events.
     *
     * @param Channel The name of the channel
     * @param event The name of the event (i.e. CREATED, DELETED, UPDATED)
     * @param queueName The name of the queue
     * @throws IOException
     */
    public static void platformEventConsumer (Channel channel, Event event, String queueName) throws IOException {


        switch (event) {
            case CREATED:  
                PlatformCreatedConsumer platformCreatedConsumer = new PlatformCreatedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, platformCreatedConsumer);
                break;
            case DELETED:  
                PlatformDeletedConsumer platformDeletedConsumer = new PlatformDeletedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, platformDeletedConsumer);
                break;
            case UPDATED:  
                PlatformUpdatedConsumer platformUpdatedConsumer = new PlatformUpdatedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, platformUpdatedConsumer);
                break;
        }
    }


    /**
     * Declares a consumer for resource events.
     *
     * @param Channel The name of the channel
     * @param event The name of the event (i.e. CREATED, DELETED, UPDATED)
     * @param queueName The name of the queue
     * @throws IOException
     */
    public static void resourceEventConsumer (Channel channel, Event event, String queueName) throws IOException {

        switch (event) {
            case CREATED:  
                ResourceCreatedConsumer resourceCreatedConsumer = new ResourceCreatedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, resourceCreatedConsumer);
                break;
            case DELETED:  
                ResourceDeletedConsumer resourceDeletedConsumer = new ResourceDeletedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, resourceDeletedConsumer);
                break;
            case UPDATED:  
                ResourceUpdatedConsumer resourceUpdatedConsumer = new ResourceUpdatedConsumer(channel);
                channel.basicConsume(queueName, AUTO_ACK, resourceUpdatedConsumer);
                break;
        }
    }


    /**
     * Returns channel for rabbit messaging.
     *
     * @return Channel
     * @throws IOException
     * @throws TimeoutException
     */
    public static Channel getChannel() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1"); // FIX ME: Take the value from the bootstrap proporties
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        return channel;
    }
}
