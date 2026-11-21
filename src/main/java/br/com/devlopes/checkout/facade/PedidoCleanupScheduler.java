package br.com.devlopes.checkout.facade;

import br.com.devlopes.checkout.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Rotina agendada para limpeza periódica de pedidos antigos no banco H2 em memória,
 * prevenindo crescimento ilimitado de RAM em ambientes de produção/demonstração.
 */
@Component
public class PedidoCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PedidoCleanupScheduler.class);

    private final PedidoRepository pedidoRepository;
    private final long retentionHours;

    public PedidoCleanupScheduler(
            PedidoRepository pedidoRepository,
            @Value("${app.orders.retention-hours:24}") long retentionHours
    ) {
        this.pedidoRepository = pedidoRepository;
        this.retentionHours = Math.max(1L, retentionHours);
    }

    @Scheduled(fixedRateString = "${app.orders.cleanup-interval-ms:3600000}")
    @Transactional
    public void limparPedidosExpirados() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        long removidos = pedidoRepository.deleteByCriadoEmBefore(cutoff);
        if (removidos > 0) {
            log.info("Limpeza automática H2: {} pedido(s) com mais de {}h removido(s).", removidos, retentionHours);
        }
    }
}
