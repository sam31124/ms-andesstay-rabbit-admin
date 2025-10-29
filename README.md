# Microservicio Administrador de RabbitMQ - AndesStay

**Asignatura:** DSY1107 - Desarrollo Cloud Native I  
**Evaluación:** Evaluación Parcial N°3 y N°4  
**Equipo:** Amaro Ulloa, Lucas Salazar, Samuel Urzúa  
**Puerto:** `8084`  

---

## 1. Descripción
Microservicio desacoplado construido en **Java 21** y **Spring Boot 3.2.3** que permite la administración dinámica de la topología de mensajería (colas, exchanges y bindings) en tiempo de ejecución utilizando `AmqpAdmin`.

---

## 2. Puesta en Marcha

### A. Levantar el Clúster RabbitMQ de 2 Nodos:
```bash
docker compose up -d
```
* **Consola Nodo 1:** `http://localhost:15672` (Credenciales: `guest` / `guest`)
* **Consola Nodo 2:** `http://localhost:15673` (Credenciales: `guest` / `guest`)

### B. Ejecutar el Microservicio:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

---

## 3. Endpoints REST de Administración

| Método | Endpoint | Descripción | Body (JSON) |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/rabbit/topology/init` | Inicializa la topología oficial de AndesStay (3 exchanges, 6 colas, bindings). | N/A |
| **GET** | `/api/rabbit/topology/status` | Retorna el estado en vivo de colas y exchanges. | N/A |
| **POST** | `/api/rabbit/queues` | Crea una nueva cola con validación DTO. | `{"name":"q.custom", "durable":true}` |
| **GET** | `/api/rabbit/queues/{name}` | Consulta propiedades y métricas de una cola. | N/A |
| **DELETE** | `/api/rabbit/queues/{name}` | Elimina una cola en tiempo de ejecución. | N/A |
| **POST** | `/api/rabbit/exchanges` | Crea un exchange (`direct`, `topic`, `fanout`). | `{"name":"ex.custom", "type":"direct"}` |
| **DELETE** | `/api/rabbit/exchanges/{name}` | Elimina un exchange. | N/A |
| **POST** | `/api/rabbit/bindings` | Asocia un exchange a una cola con una routing key. | `{"exchange":"ex.custom", "destinationQueue":"q.custom", "routingKey":"custom.key"}` |
| **DELETE** | `/api/rabbit/bindings` | Desvincula una asociación. | `{"exchange":"ex.custom", "destinationQueue":"q.custom", "routingKey":"custom.key"}` |

---

## 4. Tests Automatizados
El proyecto cuenta con suites de pruebas unitarias e integración que validan el 100% de los endpoints y servicios:
```bash
.\mvnw.cmd test
```
* **Resultado:** `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).
