package cl.duoc.andesstay.rabbit.controller;

import cl.duoc.andesstay.rabbit.dto.BindingRequest;
import cl.duoc.andesstay.rabbit.dto.ExchangeRequest;
import cl.duoc.andesstay.rabbit.dto.QueueRequest;
import cl.duoc.andesstay.rabbit.dto.TestMessageRequest;
import cl.duoc.andesstay.rabbit.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/rabbit")
@CrossOrigin(origins = "*")
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    // 1. Topología
    @PostMapping("/topology/init")
    public ResponseEntity<?> initTopology() {
        return ResponseEntity.ok(Map.of(
                "message", "Topología oficial de AndesStay inicializada con éxito",
                "details", adminService.initTopology()
        ));
    }

    @GetMapping("/topology/status")
    public ResponseEntity<?> getTopologyStatus() {
        return ResponseEntity.ok(adminService.getTopologyStatus());
    }

    // 2. Colas (Creación y Eliminación)
    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(@Valid @RequestBody QueueRequest request) {
        boolean created = adminService.createQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Cola creada con éxito",
                "queueName", request.getName(),
                "created", created
        ));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<?> deleteQueue(@PathVariable String name) {
        boolean deleted = adminService.deleteQueue(name);
        return ResponseEntity.ok(Map.of(
                "message", "Solicitud de eliminación de cola ejecutada",
                "queueName", name,
                "deleted", deleted
        ));
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<?> getQueue(@PathVariable String name) {
        Properties props = adminService.getQueueProperties(name);
        if (props == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "La cola no existe"));
        }
        return ResponseEntity.ok(Map.of(
                "queueName", name,
                "properties", props
        ));
    }

    // 3. Exchanges (Creación y Eliminación)
    @PostMapping("/exchanges")
    public ResponseEntity<?> createExchange(@Valid @RequestBody ExchangeRequest request) {
        adminService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Exchange creado con éxito",
                "exchangeName", request.getName(),
                "type", request.getType()
        ));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<?> deleteExchange(@PathVariable String name) {
        boolean deleted = adminService.deleteExchange(name);
        return ResponseEntity.ok(Map.of(
                "message", "Solicitud de eliminación de exchange ejecutada",
                "exchangeName", name,
                "deleted", deleted
        ));
    }

    // 4. Bindings (Creación y Eliminación)
    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(@Valid @RequestBody BindingRequest request) {
        adminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Binding creado con éxito",
                "queue", request.getDestinationQueue(),
                "exchange", request.getExchange(),
                "routingKey", request.getRoutingKey()
        ));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> deleteBinding(@Valid @RequestBody BindingRequest request) {
        adminService.deleteBinding(request);
        return ResponseEntity.ok(Map.of(
                "message", "Binding eliminado con éxito",
                "queue", request.getDestinationQueue(),
                "exchange", request.getExchange(),
                "routingKey", request.getRoutingKey()
        ));
    }

    // 5. Envío de Mensaje de Prueba
    @PostMapping("/messages/test")
    public ResponseEntity<?> sendTestMessage(@Valid @RequestBody TestMessageRequest request) {
        Map<String, Object> result = adminService.sendTestMessage(request);
        return ResponseEntity.ok(Map.of(
                "message", "Mensaje de prueba despachado",
                "result", result
        ));
    }
}
