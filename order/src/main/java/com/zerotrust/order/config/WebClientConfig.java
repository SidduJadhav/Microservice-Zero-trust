package com.zerotrust.order.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${server.ssl.key-store:/certs/keystore.p12}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password:changeit}")
    private String keyStorePassword;

    @Value("${server.ssl.trust-store:/certs/truststore.p12}")
    private String trustStorePath;

    @Value("${server.ssl.trust-store-password:changeit}")
    private String trustStorePassword;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    // ── Connection pool shared by both SSL and plain HTTP ──────────────────
    // maxIdleTime    : evict a connection if it has been idle for > 20s.
    //                  Keep this BELOW your downstream server's own idle/keep-alive
    //                  timeout so Netty never hands out a half-closed socket.
    // maxLifeTime    : hard cap — recycle any connection older than 60s regardless
    //                  of activity, preventing TLS session-reuse edge cases.
    // evictInBackground : a background thread sweeps the pool every 30s and
    //                     removes connections that have exceeded the above limits.
    //                     Without this, eviction only happens on acquire/release,
    //                     which is too late when traffic is low.
    private ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("order-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public WebClient webClient() {

        if (!sslEnabled) {
            log.info("[WebClient] SSL disabled — plain HTTP client with connection pool");

            HttpClient plainClient = HttpClient.create(connectionProvider())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(25, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(25, TimeUnit.SECONDS))
                    );

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(plainClient))
                    .filter(logRequest())
                    .filter(logResponse())
                    .build();
        }

        log.info("[WebClient] SSL enabled — building mTLS WebClient");
        try {
            // Load keystore — our client certificate (proves who we are)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream kis = new FileInputStream(keyStorePath)) {
                keyStore.load(kis, keyStorePassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePassword.toCharArray());

            // Load truststore — CA cert (who we trust)
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream tis = new FileInputStream(trustStorePath)) {
                trustStore.load(tis, trustStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Build Netty SSL context with keystore + truststore
            io.netty.handler.ssl.SslContext sslContext =
                    io.netty.handler.ssl.SslContextBuilder.forClient()
                            .keyManager(kmf)
                            .trustManager(tmf)
                            .build();

            HttpClient httpClient = HttpClient.create(connectionProvider())  // ← pool
                    .secure(spec -> spec
                            .sslContext(sslContext)
                            .handshakeTimeout(Duration.ofSeconds(10)))        // ← explicit
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(25, TimeUnit.SECONDS))
                            .addHandlerLast(new WriteTimeoutHandler(25, TimeUnit.SECONDS))
                    );

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .filter(logRequest())
                    .filter(logResponse())
                    .build();

        } catch (Exception e) {
            log.error("[WebClient] Failed to build mTLS WebClient: {}", e.getMessage());
            throw new RuntimeException("mTLS WebClient initialization failed", e);
        }
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.info("→ {} {}", req.method(), req.url());
            return Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            log.info("← HTTP {}", res.statusCode());
            return Mono.just(res);
        });
    }
}