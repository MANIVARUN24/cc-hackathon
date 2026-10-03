package com.manufacturing.backend.config;

import com.manufacturing.backend.mqtt.MqttSensorListener;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Configures the Eclipse Paho MQTT client to connect to AWS IoT Core
 * using mutual TLS (mTLS) authentication.
 *
 * Uses Bouncy Castle to load PEM private keys in any format:
 *   - EC SEC1   (BEGIN EC PRIVATE KEY)       ← most common from AWS IoT
 *   - RSA PKCS1 (BEGIN RSA PRIVATE KEY)
 *   - PKCS8     (BEGIN PRIVATE KEY)
 *
 * NEVER hardcode private keys or certificates in source code.
 * All certificate paths are read from application.properties or env vars.
 */
@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    static {
        // Register Bouncy Castle as a JCE security provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Value("${aws.iot.endpoint}")
    private String endpoint;

    @Value("${aws.iot.client-id}")
    private String clientId;

    @Value("${aws.iot.topic}")
    private String topic;

    @Value("${aws.iot.port}")
    private int port;

    @Value("${aws.iot.certificate}")
    private String certificatePath;

    @Value("${aws.iot.private-key}")
    private String privateKeyPath;

    @Value("${aws.iot.root-ca}")
    private String rootCaPath;

    @Bean
    public MqttClient mqttClient(MqttSensorListener listener) {
        String brokerUrl = "ssl://" + endpoint + ":" + port;
        log.info("Connecting to AWS IoT Core: {}", brokerUrl);
        log.info("MQTT Client ID: {}", clientId);
        log.info("Subscribing to topic: {}", topic);

        MqttClient client;
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException("Failed to create MQTT client instance", e);
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        try {
            SSLContext sslContext = buildSslContext(certificatePath, privateKeyPath, rootCaPath);
            options.setSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            log.error("Failed to build SSL context from certificates: {}", e.getMessage());
            log.error("Certificate path : {}", certificatePath);
            log.error("Private key path  : {}", privateKeyPath);
            log.error("Root CA path      : {}", rootCaPath);
            throw new RuntimeException("AWS IoT Core SSL configuration failed", e);
        }

        listener.setClient(client);
        listener.setTopic(topic);
        // Set callback before connecting so reconnect events are captured too
        client.setCallback(listener);

        try {
            client.connect(options);
            log.info("Successfully connected to AWS IoT Core as clientId='{}'", clientId);
            client.subscribe(topic, 1);
            log.info("Subscribed to MQTT topic: {}", topic);
        } catch (MqttException e) {
            // Non-fatal: log diagnostics and let Paho's automatic reconnect handle it.
            log.error("Failed to connect to AWS IoT Core: {} (reason code: {})",
                    e.getMessage(), e.getReasonCode());
            log.error(">>> LIKELY CAUSE: AWS IoT policy does not allow clientId='{}' to connect.", clientId);
            log.error(">>> FIX A: In application.properties set aws.iot.client-id=basicPubSub");
            log.error(">>> FIX B: In AWS Console add iot:Connect for client/{} in the Thing policy", clientId);
            log.warn("Backend started WITHOUT live MQTT. Paho will auto-retry. Fix above and restart.");
        }

        return client;
    }

    /**
     * Builds an SSLContext from PEM certificate files for AWS IoT Core mTLS.
     */
    private SSLContext buildSslContext(String certPemPath, String privateKeyPath, String rootCaPath)
            throws Exception {

        log.debug("Loading certificate : {}", certPemPath);
        log.debug("Loading private key  : {}", privateKeyPath);
        log.debug("Loading root CA      : {}", rootCaPath);

        // 1. Load Root CA into TrustStore
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate rootCaCert;
        try (InputStream rootCaStream = new FileInputStream(rootCaPath)) {
            rootCaCert = (X509Certificate) cf.generateCertificate(rootCaStream);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("root-ca", rootCaCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Load client certificate(s)
        Collection<? extends Certificate> clientCerts;
        try (InputStream certStream = new FileInputStream(certPemPath)) {
            clientCerts = cf.generateCertificates(certStream);
        }

        // 3. Load private key using Bouncy Castle (handles EC SEC1, RSA PKCS1, PKCS8)
        PrivateKey privateKey = loadPrivateKeyWithBouncyCastle(privateKeyPath);

        // 4. Build KeyStore with client cert + private key
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        Certificate[] certChain = clientCerts.toArray(new Certificate[0]);
        keyStore.setKeyEntry("client-cert", privateKey, "".toCharArray(), certChain);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // 5. Build SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        log.info("SSL context built successfully using Bouncy Castle");
        return sslContext;
    }

    /**
     * Loads a private key from a PEM file using Bouncy Castle.
     *
     * Handles all formats that AWS IoT Core can generate:
     *   - EC SEC1  format: -----BEGIN EC PRIVATE KEY-----
     *   - RSA PKCS1 format: -----BEGIN RSA PRIVATE KEY-----
     *   - PKCS8 format:    -----BEGIN PRIVATE KEY-----
     *
     * @param path absolute path to the private key PEM file
     */
    private PrivateKey loadPrivateKeyWithBouncyCastle(String path) throws Exception {
        try (FileReader fileReader = new FileReader(path);
             PEMParser pemParser = new PEMParser(fileReader)) {

            Object pemObject = pemParser.readObject();

            if (pemObject == null) {
                throw new RuntimeException(
                        "PEM file is empty or unreadable: " + path);
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (pemObject instanceof PEMKeyPair) {
                // EC SEC1 (BEGIN EC PRIVATE KEY) or RSA PKCS1 (BEGIN RSA PRIVATE KEY)
                log.debug("Key format: PEMKeyPair (EC SEC1 or RSA PKCS1)");
                return converter.getKeyPair((PEMKeyPair) pemObject).getPrivate();

            } else if (pemObject instanceof PrivateKeyInfo) {
                // PKCS8 (BEGIN PRIVATE KEY)
                log.debug("Key format: PrivateKeyInfo (PKCS8)");
                return converter.getPrivateKey((PrivateKeyInfo) pemObject);

            } else {
                throw new RuntimeException(
                        "Unrecognised PEM object type: " + pemObject.getClass().getName() +
                        " in file: " + path);
            }
        }
    }
}
