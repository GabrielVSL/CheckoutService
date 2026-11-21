package br.com.devlopes.checkout.chain;

import br.com.devlopes.checkout.dto.CheckoutRequest;
import br.com.devlopes.checkout.dto.ItemCheckoutRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <h2>Elo 1 da Chain of Responsibility: Validação de Estoque</h2>
 * Verifica se todos os itens solicitados no checkout possuem saldo disponível em estoque,
 * agregando quantidades quando o mesmo produto aparece em múltiplas linhas do carrinho.
 *
 * <p><b>Padrão Singleton (Spring IoC):</b> Bean stateless instanciado uma única vez pelo
 * container Spring ({@code @Component}) e posicionado como primeiro elo via {@code @Order(1)}.</p>
 */
@Component
@Order(1)
public class ValidacaoEstoqueHandler implements ValidacaoPedidoHandler {

    private static final int LIMITE_ESTOQUE_PADRAO = 50;

    /**
     * Simulação de saldos específicos de estoque por produto para demonstração da API.
     * Produtos não listados assumem o estoque padrão de {@value #LIMITE_ESTOQUE_PADRAO} unidades.
     */
    private static final Map<String, Integer> ESTOQUE_SIMULADO = Map.of(
            "PROD-SEM-ESTOQUE", 0,
            "PROD-LIMITADO", 3
    );

    @Override
    public void validar(CheckoutRequest request) {
        Map<String, Integer> quantidadesPorProduto = new LinkedHashMap<>();
        Map<String, ItemCheckoutRequest> referenciaPorProduto = new LinkedHashMap<>();

        for (ItemCheckoutRequest item : request.itens()) {
            String produtoId = item.produtoId().trim().toUpperCase(Locale.ROOT);
            quantidadesPorProduto.merge(produtoId, item.quantidade(), Math::addExact);
            referenciaPorProduto.putIfAbsent(produtoId, item);
        }

        for (Map.Entry<String, Integer> entry : quantidadesPorProduto.entrySet()) {
            String produtoId = entry.getKey();
            int quantidadeTotalSolicitada = entry.getValue();
            int disponivel = ESTOQUE_SIMULADO.getOrDefault(produtoId, LIMITE_ESTOQUE_PADRAO);

            if (quantidadeTotalSolicitada > disponivel) {
                ItemCheckoutRequest ref = referenciaPorProduto.get(produtoId);
                throw new ValidacaoCheckoutException(
                        String.format(
                                "Estoque insuficiente para o produto '%s' (%s). Solicitado: %d, disponível: %d.",
                                sanitizar(ref.nomeProduto(), 80),
                                sanitizar(ref.produtoId(), 40),
                                quantidadeTotalSolicitada,
                                disponivel
                        )
                );
            }
        }
    }

    private static String sanitizar(String valor, int maxLength) {
        String limpo = valor.trim().replaceAll("[\\r\\n\\t]", " ");
        return limpo.length() > maxLength ? limpo.substring(0, maxLength) + "..." : limpo;
    }
}
