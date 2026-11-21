package br.com.devlopes.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO imutável (Java 21 Record) que representa um item enviado na requisição de checkout.
 */
public record ItemCheckoutRequest(

        @Schema(description = "Identificador único do produto", example = "PROD-001")
        @NotBlank(message = "O ID do produto é obrigatório")
        @Size(max = 64, message = "O ID do produto deve ter no máximo 64 caracteres")
        String produtoId,

        @Schema(description = "Nome do produto", example = "Teclado Mecânico Keychron K2")
        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 160, message = "O nome do produto deve ter no máximo 160 caracteres")
        String nomeProduto,

        @Schema(description = "Quantidade comprada", example = "2")
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade mínima por item é 1")
        @Max(value = 99, message = "A quantidade máxima por item é 99")
        Integer quantidade,

        @Schema(description = "Preço unitário do produto em reais", example = "349.90")
        @NotNull(message = "O preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço unitário deve ser maior que zero")
        @DecimalMax(value = "1000000.00", message = "O preço unitário máximo permitido é R$ 1.000.000,00")
        BigDecimal precoUnitario
) {
    /**
     * Calcula o subtotal deste item normalizando a escala do preço unitário antes da multiplicação,
     * garantindo consistência exata com a entidade persistida {@code ItemPedido}.
     */
    public BigDecimal calcularSubtotal() {
        return precoUnitario
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
