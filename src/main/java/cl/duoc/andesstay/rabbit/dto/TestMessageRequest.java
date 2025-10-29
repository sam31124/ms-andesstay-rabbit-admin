package cl.duoc.andesstay.rabbit.dto;

import jakarta.validation.constraints.NotBlank;

public class TestMessageRequest {

    @NotBlank(message = "El exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    private boolean triggerError = false;

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isTriggerError() { return triggerError; }
    public void setTriggerError(boolean triggerError) { this.triggerError = triggerError; }
}