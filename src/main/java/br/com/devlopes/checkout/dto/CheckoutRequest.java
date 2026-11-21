package br.com.devlopes.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO imutável (Java 21 Record) para solicitação de fechamento de pedido (checkout).
 */
public record CheckoutRequest(

        @Schema(description = "Nome completo do cliente", example = "Fred Lopes")
        @NotBlank(message = "O nome do cliente é obrigatório")
        @Size(min = 2, max = 120, message = "O nome do cliente deve ter entre 2 e 120 caracteres")
        String clienteNome,

        @Schema(description = "E-mail do cliente", example = "fred@devlopes.com.br")
        @NotBlank(message = "O e-mail do cliente é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 160, message = "O e-mail do cliente deve ter no máximo 160 caracteres")
        String clienteEmail,

        @Schema(description = "Lista de itens do pedido")
        @NotEmpty(message = "O pedido deve conter ao menos um item")
        @Size(max = 30, message = "O pedido pode conter no máximo 30 itens")
        @Valid
        List<ItemCheckoutRequest> itens,

        @Schema(description = "Modalidade de frete desejada (SEDEX ou PAC)", example = "SEDEX")
        @NotBlank(message = "A modalidade de frete é obrigatória")
        @Size(max = 30, message = "A modalidade de frete deve ter no máximo 30 caracteres")
        String modalidadeFrete,

        @Schema(description = "Forma de pagamento (CARTAO_CREDITO, PIX ou BOLETO)", example = "PIX")
        @NotBlank(message = "A forma de pagamento é obrigatória")
        @Size(max = 30, message = "A forma de pagamento deve ter no máximo 30 caracteres")
        String formaPagamento,

        @Schema(description = "Código promocional opcional (ex: DEV10, BEMVINDO20)", example = "DEV10")
        @Size(max = 32, message = "O cupom de desconto deve ter no máximo 32 caracteres")
        String cupomDesconto,

        @Schema(description = "Peso total da remessa em quilogramas", example = "2.5")
        @NotNull(message = "O peso total é obrigatório")
        @DecimalMin(value = "0.1", message = "O peso total deve ser de pelo menos 0.1 kg")
        @DecimalMax(value = "500.00", message = "O peso total máximo permitido é 500 kg")
        BigDecimal pesoTotalKg
) {
    /**
     * Calcula a soma dos subtotais de todos os itens do carrinho.
     */
    public BigDecimal calcularValorTotalItens() {
        return itens.stream()
                .map(ItemCheckoutRequest::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
