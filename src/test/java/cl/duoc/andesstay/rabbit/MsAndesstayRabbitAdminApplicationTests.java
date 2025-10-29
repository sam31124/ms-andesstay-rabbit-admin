package cl.duoc.andesstay.rabbit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MsAndesstayRabbitAdminApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmqpAdmin amqpAdmin;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void testInitTopology() throws Exception {
        mockMvc.perform(post("/api/rabbit/topology/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.status").value("INITIALIZED"));

        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).declareExchange(any(Exchange.class));
        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).declareQueue(any(Queue.class));
        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).declareBinding(any(Binding.class));
    }

    @Test
    void testGetTopologyStatus() throws Exception {
        mockMvc.perform(get("/api/rabbit/topology/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queues").isArray())
                .andExpect(jsonPath("$.exchanges").isArray());
    }

    @Test
    void testCreateQueue() throws Exception {
        Mockito.when(amqpAdmin.declareQueue(any(Queue.class))).thenReturn("q.test.custom");

        String json = "{\"name\":\"q.test.custom\",\"durable\":true}";
        mockMvc.perform(post("/api/rabbit/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueName").value("q.test.custom"))
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    void testDeleteQueue() throws Exception {
        Mockito.when(amqpAdmin.deleteQueue("q.test.custom")).thenReturn(true);

        mockMvc.perform(delete("/api/rabbit/queues/q.test.custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void testCreateExchange() throws Exception {
        String json = "{\"name\":\"ex.test.custom\",\"type\":\"direct\",\"durable\":true}";
        mockMvc.perform(post("/api/rabbit/exchanges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exchangeName").value("ex.test.custom"))
                .andExpect(jsonPath("$.type").value("direct"));

        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).declareExchange(any(Exchange.class));
    }

    @Test
    void testDeleteExchange() throws Exception {
        Mockito.when(amqpAdmin.deleteExchange("ex.test.custom")).thenReturn(true);

        mockMvc.perform(delete("/api/rabbit/exchanges/ex.test.custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void testCreateBinding() throws Exception {
        String json = "{\"destinationQueue\":\"q.test\",\"exchange\":\"ex.test\",\"routingKey\":\"test.key\"}";
        mockMvc.perform(post("/api/rabbit/bindings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queue").value("q.test"))
                .andExpect(jsonPath("$.exchange").value("ex.test"));

        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).declareBinding(any(Binding.class));
    }

    @Test
    void testDeleteBinding() throws Exception {
        String json = "{\"destinationQueue\":\"q.test\",\"exchange\":\"ex.test\",\"routingKey\":\"test.key\"}";
        mockMvc.perform(delete("/api/rabbit/bindings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queue").value("q.test"));

        Mockito.verify(amqpAdmin, Mockito.atLeastOnce()).removeBinding(any(Binding.class));
    }

    @Test
    void testSendTestMessage() throws Exception {
        String json = "{\"exchange\":\"cmd.direct\",\"routingKey\":\"email.send\",\"message\":\"Prueba mensaje\",\"triggerError\":false}";
        mockMvc.perform(post("/api/rabbit/messages/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SENT"));
    }
}
