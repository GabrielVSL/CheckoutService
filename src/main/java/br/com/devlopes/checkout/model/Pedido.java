package br.com.devlopes.checkout.model;

import br.com.devlopes.checkout.config.SessionContext;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidade principal do domínio de Checkout, persistida no banco relacional (H2/PostgreSQL).
 */
@Entity
@Table(
        name = "pedidos",
        indexes = {
                @Index(name = "idx_pedidos_session_criado", columnList = "session_id, criado_em"),
                @Index(name = "idx_pedidos_criado_em", columnList = "criado_em")
        }
)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 120)
    private String clienteNome;

    @Column(nullable = false, length = 160)
    private String clienteEmail;

    @Column(nullable = false, length = 30)
    private String modalidadeFrete;

    @Column(nullable = false, length = 30)
    private String formaPagamento;

    @Column(length = 32)
    private String cupomAplicado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorItens;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorFrete;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDesconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(
            String clienteNome,
            String clienteEmail,
            String modalidadeFrete,
            String formaPagamento,
            String cupomAplicado,
            BigDecimal valorItens,
            BigDecimal valorFrete,
            BigDecimal valorDesconto,
            BigDecimal valorTotal,
            StatusPedido status
    ) {
        this(
                SessionContext.getCurrentSessionId(),
                clienteNome,
                clienteEmail,
                modalidadeFrete,
                formaPagamento,
                cupomAplicado,
                valorItens,
                valorFrete,
                valorDesconto,
                valorTotal,
                status
        );
    }

    public Pedido(
            String sessionId,
            String clienteNome,
            String clienteEmail,
            String modalidadeFrete,
            String formaPagamento,
            String cupomAplicado,
            BigDecimal valorItens,
            BigDecimal valorFrete,
            BigDecimal valorDesconto,
            BigDecimal valorTotal,
            StatusPedido status
    ) {
        this.sessionId = SessionContext.sanitize(sessionId);
        this.clienteNome = clienteNome;
        this.clienteEmail = clienteEmail;
        this.modalidadeFrete = modalidadeFrete;
        this.formaPagamento = formaPagamento;
        this.cupomAplicado = cupomAplicado;
        this.valorItens = valorItens;
        this.valorFrete = valorFrete;
        this.valorDesconto = valorDesconto;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    @PrePersist
    void prePersist() {
        if (this.sessionId == null || this.sessionId.isBlank()) {
            this.sessionId = SessionContext.DEFAULT_SESSION_ID;
        }
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public String getModalidadeFrete() {
        return modalidadeFrete;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public String getCupomAplicado() {
        return cupomAplicado;
    }

    public BigDecimal getValorItens() {
        return valorItens;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
