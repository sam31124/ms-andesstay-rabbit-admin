package cl.duoc.andesstay.rabbit.dto;

import jakarta.validation.constraints.NotBlank;

public class BindingRequest {

    @NotBlank(message = "La cola de destino es obligatoria")
    private String destinationQueue;

    @NotBlank(message = "El exchange de origen es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    public String getDestinationQueue() { return destinationQueue; }
    public void setDestinationQueue(String destinationQueue) { this.destinationQueue = destinationQueue; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}