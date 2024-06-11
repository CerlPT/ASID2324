package com.consumer.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component

public class ConsumerListener {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ConsumerRepository consumerRepository;

    private final static String QUEUE_NAME = "view-queue";

    @PostConstruct
    public void start() {
        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                ObjectMapper objectMapper = new ObjectMapper();
                Consumer consumer = objectMapper.readValue(message, Consumer.class);
                consumerRepository.save(consumer);
                System.out.println("Message read");
            };

            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @RestController
    @RequestMapping("/population")
    public class ConsumerController {

        @Autowired
        private ConsumerRepository consumerRepository;

        @GetMapping
        public ResponseEntity<List<Consumer>> getAllConsumers() {
            List<Consumer> consumers = consumerRepository.findAll();
            return ResponseEntity.ok(consumers);
        }
    }

}
