package br.com.devlopes.checkout.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "Monitoramento", description = "Endpoint leve para health check e keep-alive")
public class HealthController {

    @GetMapping("/health")
    @Operation(
            summary = "Verificar status da aplicação",
            description = "Retorna 200 OK com status UP sem consultar o banco de dados."
    )
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "checkout-api", LocalDateTime.now()));
    }

    public record HealthResponse(String status, String service, LocalDateTime timestamp) {
    }
}
