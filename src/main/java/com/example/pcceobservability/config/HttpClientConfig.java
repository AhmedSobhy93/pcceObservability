package com.example.pcceobservability.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class HttpClientConfig {

    @Bean
    SSLSocketFactory pcceSSLSocketFactory(PcceProperties pcceProperties) throws Exception {
        PcceProperties.Ssl ssl = pcceProperties.getSsl();
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (ssl != null && StringUtils.hasText(ssl.getTrustStorePath())) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = ssl.getTrustStorePassword() == null ? new char[0] : ssl.getTrustStorePassword().toCharArray();
            try (InputStream inputStream = Files.newInputStream(Path.of(ssl.getTrustStorePath()))) {
                trustStore.load(inputStream, password);
            }
            trustManagerFactory.init(trustStore);
        } else {
            trustManagerFactory.init((KeyStore) null);
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    @Bean
    HostnameVerifier pcceHostnameVerifier(PcceProperties pcceProperties) {
        HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        return (hostname, session) -> pcceProperties.getSsl() == null
                || pcceProperties.getSsl().isVerifyHostname()
                ? defaultVerifier.verify(hostname, session)
                : true;
    }
}
