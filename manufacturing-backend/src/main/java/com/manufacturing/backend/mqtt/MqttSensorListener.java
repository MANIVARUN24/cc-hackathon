package com.manufacturing.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manufacturing.backend.dto.SensorReadingDTO;
import com.manufacturing.backend.service.SensorService;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT message listener that receives sensor data from AWS IoT Core.
 *
 * This bean:
 * 1. Implements MqttCallback (messageArrived, connectionLost, deliveryComplete)
 * 2. Receives JSON messages from topic: sdk/test/python
 * 3. Deserializes to SensorReadingDTO (matching Python gateway JSON format)
 * 4. Passes to SensorService for validation and storage
 *
 * Reconnection is handled by Eclipse Paho's automaticReconnect option.
 * This listener logs connect/disconnect events for monitoring.
 */
@Component
public class MqttSensorListener implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttSensorListener.class);

    private final SensorService sensorService;
    private final ObjectMapper objectMapper;

    @Value("${aws.iot.reconnect.delay-seconds:5}")
    private int reconnectDelaySecs;

    /** Paho client reference — set by MqttConfig after connection */
    private MqttClient client;
    private String topic;

    /** Connection status for health endpoint */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** Running message counter */
    private final AtomicInteger messagesReceived = new AtomicInteger(0);

    public MqttSensorListener(SensorService sensorService, ObjectMapper objectMapper) {
        this.sensorService = sensorService;
        this.objectMapper = objectMapper;
    }

    // ========================================================
    // MqttCallback implementations
    // ========================================================

    /**
     * Called when an MQTT message arrives on the subscribed topic.
     * Parses JSON and hands off to SensorService.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        int count = messagesReceived.incrementAndGet();

        log.info("MQTT message #{} received on topic '{}': {}", count, topic, payload);

        try {
            SensorReadingDTO dto = objectMapper.readValue(payload, SensorReadingDTO.class);
            boolean saved = sensorService.saveReading(dto);
            if (saved) {
                log.debug("Message #{} stored in database", count);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Malformed JSON — log and discard, do NOT crash the application
            log.error("Malformed JSON received on topic '{}': {} | Payload: {}", topic, e.getMessage(), payload);
        } catch (Exception e) {
            log.error("Unexpected error processing MQTT message: {}", e.getMessage(), e);
        }
    }

    /**
     * Called when connection to AWS IoT Core is lost.
     * Eclipse Paho's automaticReconnect will attempt reconnection automatically.
     */
    @Override
    public void connectionLost(Throwable cause) {
        connected.set(false);
        log.warn("MQTT connection interrupted: {}", cause.getMessage());
        log.info("MQTT automatic reconnection is enabled. Attempting to reconnect to AWS IoT Core...");
    }

    /**
     * Called when a QoS 1/2 message delivery is confirmed.
     * Not used for subscriptions.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No-op for subscriber-only client
    }

    // ========================================================
    // Public state accessors (used by HealthController)
    // ========================================================

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public int getMessagesReceived() {
        return messagesReceived.get();
    }

    // ========================================================
    // Configuration setters (called by MqttConfig)
    // ========================================================

    public void setClient(MqttClient client) {
        this.client = client;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
