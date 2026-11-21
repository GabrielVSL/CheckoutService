package br.com.devlopes.checkout.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.internal-api-key=super-secret-checkout-key",
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.read-requests-per-minute=2"
})
class ApiSecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve permitir /health sem chave, exigir X-Internal-Api-Key em /api/** e aplicar rate limit")
    void deveExigirChaveInternaEAplicarRateLimit() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(get("/api/v1/checkout/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/checkout/pedidos")
                        .header(ApiSecurityFilter.API_KEY_HEADER, "errada"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/checkout/pedidos")
                        .header(ApiSecurityFilter.API_KEY_HEADER, "super-secret-checkout-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(get("/api/v1/checkout/pedidos")
                        .header(ApiSecurityFilter.API_KEY_HEADER, "super-secret-checkout-key"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/checkout/pedidos")
                        .header(ApiSecurityFilter.API_KEY_HEADER, "super-secret-checkout-key"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }
}
