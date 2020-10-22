package io.quarkuscoffeeshop.inventory.infrastructure;

import io.quarkuscoffeeshop.inventory.domain.CommandType;
import io.quarkuscoffeeshop.inventory.domain.RestockInventoryCommand;
import io.quarkuscoffeeshop.inventory.domain.RestockItemCommand;
import io.quarkuscoffeeshop.domain.Item;
import io.quarkuscoffeeshop.infrastructure.KafkaIT;
import io.quarkuscoffeeshop.infrastructure.KafkaTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.util.ArrayList;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
public class KafkaServiceTest extends KafkaIT {

    static final Logger logger = LoggerFactory.getLogger(KafkaServiceTest.class);

    Jsonb jsonb = JsonbBuilder.create();

    String KAKFA_TOPIC = "inventory";

    @Test
    public void testRestockRequest() {

        RestockInventoryCommand restockInventoryCommand = new RestockInventoryCommand(Item.COFFEE_BLACK);
        producerMap.get(KAKFA_TOPIC).send(new ProducerRecord<>(KAKFA_TOPIC, jsonb.toJson(restockInventoryCommand)));

        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
            assertNull(e);
        }

        // Get the appropriate consumer, point to the first message, and pull all messages
        final KafkaConsumer inventoryConsumer = consumerMap.get(KAKFA_TOPIC);
        inventoryConsumer.seekToBeginning(new ArrayList<TopicPartition>());
        final ConsumerRecords<String, String> inventoryRecords = inventoryConsumer.poll(Duration.ofMillis(1000));

        assertEquals(2,inventoryRecords.count());

        int numberOfRestockInventoryCommands = 0;
        int numberOfRestockBaristaCommands = 0;

        for (ConsumerRecord<String, String> record : inventoryRecords) {
            logger.info(record.value());
            RestockItemCommand restockItemCommand = jsonb.fromJson(record.value(), RestockItemCommand.class);
            if (CommandType.RESTOCK_BARISTA_COMMAND.equals(restockItemCommand.commandType)) {
                numberOfRestockBaristaCommands++;
            }
            if (CommandType.RESTOCK_INVENTORY_COMMAND.equals(restockItemCommand.commandType)) {
                numberOfRestockInventoryCommands++;
            }
        }
        assertEquals(1, numberOfRestockBaristaCommands);
        assertEquals(1, numberOfRestockInventoryCommands);
    }
}
