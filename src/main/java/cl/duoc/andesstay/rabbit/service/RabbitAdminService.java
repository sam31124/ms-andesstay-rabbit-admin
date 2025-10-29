package cl.duoc.andesstay.rabbit.service;

import cl.duoc.andesstay.rabbit.dto.BindingRequest;
import cl.duoc.andesstay.rabbit.dto.ExchangeRequest;
import cl.duoc.andesstay.rabbit.dto.QueueRequest;
import cl.duoc.andesstay.rabbit.dto.TestMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE_DIRECT = "cmd.direct";
    public static final String EXCHANGE_TOPIC = "cmd.topic";
    public static final String EXCHANGE_DLX = "cmd.dead.dlx";

    public static final String QUEUE_EMAIL = "q.cmd.email";
    public static final String QUEUE_HOUSEKEEPING = "q.cmd.housekeeping";
    public static final String QUEUE_VOUCHER = "q.cmd.voucher";

    public static final String QUEUE_EMAIL_DLQ = "q.cmd.email.dlq";
    public static final String QUEUE_HOUSEKEEPING_DLQ = "q.cmd.housekeeping.dlq";
    public static final String QUEUE_VOUCHER_DLQ = "q.cmd.voucher.dlq";

    public static final String ROUTING_KEY_EMAIL_SEND = "email.send";
    public static final String ROUTING_KEY_HOUSEKEEPING_TICKET = "housekeeping.ticket";
    public static final String ROUTING_KEY_VOUCHER_GEN = "voucher.gen";

    public static final String ROUTING_KEY_EMAIL_DLQ = "email.dlq";
    public static final String ROUTING_KEY_HOUSEKEEPING_DLQ = "housekeeping.dlq";
    public static final String ROUTING_KEY_VOUCHER_DLQ = "voucher.dlq";

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean createQueue(QueueRequest request) {
        Queue queue = new Queue(
                request.getName().trim(),
                request.isDurable(),
                request.isExclusive(),
                request.isAutoDelete(),
                request.getArguments()
        );
        String result = amqpAdmin.declareQueue(queue);
        log.info("[RabbitAdmin] Cola creada: {}", request.getName());
        return result != null;
    }

    public boolean deleteQueue(String queueName) {
        boolean deleted = amqpAdmin.deleteQueue(queueName);
        log.info("[RabbitAdmin] Cola eliminada: {} (Resultado: {})", queueName, deleted);
        return deleted;
    }

    public Properties getQueueProperties(String queueName) {
        return amqpAdmin.getQueueProperties(queueName);
    }

    public void createExchange(ExchangeRequest request) {
        Exchange exchange = switch (request.getType().toLowerCase()) {
            case "topic" -> new TopicExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            case "fanout" -> new FanoutExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            case "headers" -> new HeadersExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            default -> new DirectExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
        };
        amqpAdmin.declareExchange(exchange);
        log.info("[RabbitAdmin] Exchange creado: {} (Tipo: {})", request.getName(), request.getType());
    }

    public boolean deleteExchange(String exchangeName) {
        boolean deleted = amqpAdmin.deleteExchange(exchangeName);
        log.info("[RabbitAdmin] Exchange eliminado: {} (Resultado: {})", exchangeName, deleted);
        return deleted;
    }

    public void createBinding(BindingRequest request) {
        Binding binding = new Binding(
                request.getDestinationQueue().trim(),
                Binding.DestinationType.QUEUE,
                request.getExchange().trim(),
                request.getRoutingKey().trim(),
                null
        );
        amqpAdmin.declareBinding(binding);
        log.info("[RabbitAdmin] Binding creado: {} -> {} con RK {}",
                request.getDestinationQueue(), request.getExchange(), request.getRoutingKey());
    }

    public void deleteBinding(BindingRequest request) {
        Binding binding = new Binding(
                request.getDestinationQueue().trim(),
                Binding.DestinationType.QUEUE,
                request.getExchange().trim(),
                request.getRoutingKey().trim(),
                null
        );
        amqpAdmin.removeBinding(binding);
        log.info("[RabbitAdmin] Binding eliminado: {} -X- {} con RK {}",
                request.getDestinationQueue(), request.getExchange(), request.getRoutingKey());
    }

    public Map<String, Object> sendTestMessage(TestMessageRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("message", request.getMessage());
        payload.put("simulateError", request.isTriggerError());
        payload.put("timestamp", new Date().toString());

        rabbitTemplate.convertAndSend(request.getExchange(), request.getRoutingKey(), payload);
        log.info("[RabbitAdmin] Mensaje de prueba despachado a '{}' con RK '{}' (Error inducido: {})",
                request.getExchange(), request.getRoutingKey(), request.isTriggerError());

        return Map.of(
                "status", "SENT",
                "exchange", request.getExchange(),
                "routingKey", request.getRoutingKey(),
                "payload", payload
        );
    }

    public Map<String, Object> initTopology() {
        log.info("[RabbitAdmin] Inicializando topología oficial AndesStay...");

        // 1. Exchanges
        DirectExchange directEx = new DirectExchange(EXCHANGE_DIRECT, true, false);
        TopicExchange topicEx = new TopicExchange(EXCHANGE_TOPIC, true, false);
        DirectExchange dlxEx = new DirectExchange(EXCHANGE_DLX, true, false);

        amqpAdmin.declareExchange(directEx);
        amqpAdmin.declareExchange(topicEx);
        amqpAdmin.declareExchange(dlxEx);

        // 2. DLQs
        Queue emailDlq = QueueBuilder.durable(QUEUE_EMAIL_DLQ).build();
        Queue hkDlq = QueueBuilder.durable(QUEUE_HOUSEKEEPING_DLQ).build();
        Queue voucherDlq = QueueBuilder.durable(QUEUE_VOUCHER_DLQ).build();

        amqpAdmin.declareQueue(emailDlq);
        amqpAdmin.declareQueue(hkDlq);
        amqpAdmin.declareQueue(voucherDlq);

        amqpAdmin.declareBinding(BindingBuilder.bind(emailDlq).to(dlxEx).with(ROUTING_KEY_EMAIL_DLQ));
        amqpAdmin.declareBinding(BindingBuilder.bind(hkDlq).to(dlxEx).with(ROUTING_KEY_HOUSEKEEPING_DLQ));
        amqpAdmin.declareBinding(BindingBuilder.bind(voucherDlq).to(dlxEx).with(ROUTING_KEY_VOUCHER_DLQ));

        // 3. Colas principales con DLX
        Map<String, Object> emailArgs = Map.of(
                "x-dead-letter-exchange", EXCHANGE_DLX,
                "x-dead-letter-routing-key", ROUTING_KEY_EMAIL_DLQ
        );
        Queue emailQ = QueueBuilder.durable(QUEUE_EMAIL).withArguments(emailArgs).build();

        Map<String, Object> hkArgs = Map.of(
                "x-dead-letter-exchange", EXCHANGE_DLX,
                "x-dead-letter-routing-key", ROUTING_KEY_HOUSEKEEPING_DLQ
        );
        Queue hkQ = QueueBuilder.durable(QUEUE_HOUSEKEEPING).withArguments(hkArgs).build();

        Map<String, Object> voucherArgs = Map.of(
                "x-dead-letter-exchange", EXCHANGE_DLX,
                "x-dead-letter-routing-key", ROUTING_KEY_VOUCHER_DLQ
        );
        Queue voucherQ = QueueBuilder.durable(QUEUE_VOUCHER).withArguments(voucherArgs).build();

        amqpAdmin.declareQueue(emailQ);
        amqpAdmin.declareQueue(hkQ);
        amqpAdmin.declareQueue(voucherQ);

        // 4. Bindings
        amqpAdmin.declareBinding(BindingBuilder.bind(emailQ).to(directEx).with(ROUTING_KEY_EMAIL_SEND));
        amqpAdmin.declareBinding(BindingBuilder.bind(hkQ).to(directEx).with(ROUTING_KEY_HOUSEKEEPING_TICKET));
        amqpAdmin.declareBinding(BindingBuilder.bind(voucherQ).to(directEx).with(ROUTING_KEY_VOUCHER_GEN));

        amqpAdmin.declareBinding(BindingBuilder.bind(emailQ).to(topicEx).with("email.*"));
        amqpAdmin.declareBinding(BindingBuilder.bind(hkQ).to(topicEx).with("housekeeping.#"));
        amqpAdmin.declareBinding(BindingBuilder.bind(voucherQ).to(topicEx).with("voucher.*"));

        return Map.of(
                "status", "INITIALIZED",
                "exchanges", List.of(EXCHANGE_DIRECT, EXCHANGE_TOPIC, EXCHANGE_DLX),
                "queues", List.of(QUEUE_EMAIL, QUEUE_HOUSEKEEPING, QUEUE_VOUCHER),
                "dlqs", List.of(QUEUE_EMAIL_DLQ, QUEUE_HOUSEKEEPING_DLQ, QUEUE_VOUCHER_DLQ)
        );
    }

    public Map<String, Object> getTopologyStatus() {
        List<String> allQueues = List.of(
                QUEUE_EMAIL, QUEUE_HOUSEKEEPING, QUEUE_VOUCHER,
                QUEUE_EMAIL_DLQ, QUEUE_HOUSEKEEPING_DLQ, QUEUE_VOUCHER_DLQ
        );

        List<Map<String, Object>> queueStatusList = new ArrayList<>();
        for (String qName : allQueues) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", qName);
            info.put("isDlq", qName.endsWith(".dlq"));

            try {
                Properties props = amqpAdmin.getQueueProperties(qName);
                if (props != null) {
                    info.put("exists", true);
                    info.put("messageCount", props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT));
                    info.put("consumerCount", props.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
                } else {
                    info.put("exists", false);
                    info.put("messageCount", 0);
                    info.put("consumerCount", 0);
                }
            } catch (Exception e) {
                info.put("exists", false);
                info.put("error", e.getMessage());
            }

            queueStatusList.add(info);
        }

        return Map.of(
                "queues", queueStatusList,
                "exchanges", List.of(EXCHANGE_DIRECT, EXCHANGE_TOPIC, EXCHANGE_DLX)
        );
    }
}
