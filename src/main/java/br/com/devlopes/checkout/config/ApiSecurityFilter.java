package br.com.devlopes.checkout.config;

import br.com.devlopes.checkout.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de segurança HTTP responsável por:
 * <ul>
 *   <li>Headers defensivos (nosniff, DENY, no-referrer, no-store em APIs).</li>
 *   <li>Validação opcional de chave interna ({@code X-Internal-Api-Key}) com comparação em tempo constante.</li>
 *   <li>Limite de tamanho de corpo JSON (anti-DoS).</li>
 *   <li>Rate limiting em memória por IP com limpeza automática de janelas expiradas.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiSecurityFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 5_000;

    private final ObjectMapper objectMapper;
    private final byte[] expectedApiKeyBytes;
    private final boolean apiKeyEnabled;
    private final boolean rateLimitEnabled;
    private final int writeRpm;
    private final int readRpm;
    private final long maxJsonBodyBytes;
    private final boolean trustProxyHeaders;
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public ApiSecurityFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.internal-api-key:}") String internalApiKey,
            @Value("${app.security.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${app.security.rate-limit.write-requests-per-minute:30}") int writeRpm,
            @Value("${app.security.rate-limit.read-requests-per-minute:120}") int readRpm,
            @Value("${app.security.max-json-body-bytes:65536}") long maxJsonBodyBytes,
            @Value("${app.security.trust-proxy-headers:false}") boolean trustProxyHeaders
    ) {
        this.objectMapper = objectMapper;
        String trimmedKey = internalApiKey != null ? internalApiKey.trim() : "";
        this.apiKeyEnabled = !trimmedKey.isEmpty();
        this.expectedApiKeyBytes = trimmedKey.getBytes(StandardCharsets.UTF_8);
        this.rateLimitEnabled = rateLimitEnabled;
        this.writeRpm = Math.max(1, writeRpm);
        this.readRpm = Math.max(1, readRpm);
        this.maxJsonBodyBytes = Math.max(1024L, maxJsonBodyBytes);
        this.trustProxyHeaders = trustProxyHeaders;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        applySecurityHeaders(request, response);

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method) || "/health".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (apiKeyEnabled && isProtectedPath(path)) {
            String provided = request.getHeader(API_KEY_HEADER);
            if (!isValidApiKey(provided)) {
                writeError(
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Não autorizado",
                        "Credencial interna ausente ou inválida.",
                        path
                );
                return;
            }
        }

        if (path.startsWith("/api/") && request.getContentLengthLong() > maxJsonBodyBytes) {
            writeError(
                    response,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Payload muito grande",
                    "O corpo da requisição excede o limite máximo permitido.",
                    path
            );
            return;
        }

        if (rateLimitEnabled && path.startsWith("/api/")) {
            RateCheckResult rateCheck = checkRateLimit(request, method);
            if (!rateCheck.allowed()) {
                response.setHeader("Retry-After", String.valueOf(rateCheck.retryAfterSeconds()));
                writeError(
                        response,
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas requisições",
                        "Limite temporário de requisições atingido. Tente novamente em instantes.",
                        path
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void applySecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
    }

    private boolean isProtectedPath(String path) {
        return path.startsWith("/api/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    private boolean isValidApiKey(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        byte[] providedBytes = provided.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedApiKeyBytes, providedBytes);
    }

    private RateCheckResult checkRateLimit(HttpServletRequest request, String method) {
        long now = System.currentTimeMillis();
        if (buckets.size() >= MAX_TRACKED_KEYS) {
            buckets.entrySet().removeIf(entry -> entry.getValue().resetAt <= now);
        }

        boolean isWrite = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
        String bucketName = isWrite ? "write" : "read";
        int limit = isWrite ? writeRpm : readRpm;

        String clientKey = bucketName + ":" + resolveClientKey(request);
        RateBucket bucket = buckets.compute(clientKey, (k, existing) -> {
            if (existing == null || existing.resetAt <= now) {
                return new RateBucket(now + WINDOW_MS);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (bucket.count.get() > limit) {
            long retryAfter = Math.max(1L, (long) Math.ceil((bucket.resetAt - now) / 1000.0));
            return new RateCheckResult(false, retryAfter);
        }
        return new RateCheckResult(true, 0L);
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first.length() > 64 ? first.substring(0, 64) : first;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String erro,
            String mensagem,
            String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                erro,
                mensagem,
                path,
                List.of()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class RateBucket {
        final AtomicInteger count = new AtomicInteger(1);
        final long resetAt;

        RateBucket(long resetAt) {
            this.resetAt = resetAt;
        }
    }

    private record RateCheckResult(boolean allowed, long retryAfterSeconds) {
    }
}
