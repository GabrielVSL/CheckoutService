package br.com.devlopes.checkout.repository;

import br.com.devlopes.checkout.model.Pedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para persistência e consulta de {@link Pedido}.
 *
 * <p><b>Singleton (Spring IoC):</b> O proxy gerado em tempo de execução pelo Spring Data
 * para esta interface é registrado no container como Singleton.</p>
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("SELECT p.id FROM Pedido p WHERE p.sessionId = :sessionId ORDER BY p.criadoEm DESC, p.id DESC")
    List<Long> findRecentIdsBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByIdInOrderByCriadoEmDescIdDesc(Collection<Long> ids);

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findWithItensByIdAndSessionId(Long id, String sessionId);

    long countBySessionId(String sessionId);

    long deleteByCriadoEmBefore(LocalDateTime cutoff);
}
