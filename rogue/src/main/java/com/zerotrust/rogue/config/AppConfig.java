package com.zerotrust.rogue.config;

import org.springframework.context.annotation.Configuration;

/**
 * No SSL config here — intentionally.
 * In Scenario 3, absence of a valid mTLS cert
 * causes SSLHandshakeException when Order Service
 * tries to connect → attack is blocked at TLS layer.
 */
@Configuration
public class AppConfig {
    // intentionally empty
}