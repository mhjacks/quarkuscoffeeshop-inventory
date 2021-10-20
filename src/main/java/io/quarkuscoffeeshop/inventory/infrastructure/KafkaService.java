package io.quarkuscoffeeshop.inventory.infrastructure;

import io.quarkuscoffeeshop.inventory.domain.RestockItemCommand;
import io.quarkuscoffeeshop.inventory.domain.StockRoom;
import io.smallrye.common.annotation.Blocking;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.transaction.Transactional;

@ApplicationScoped
public class KafkaService {

    Logger logger = LoggerFactory.getLogger(KafkaService.class);

    private Jsonb jsonb = JsonbBuilder.create();

    @Inject
    StockRoom stockRoom;

    @Inject
    @Channel("inventory-out")
    Emitter<RestockItemCommand> inventoryEmitter;

    @Incoming("inventory-in")
    @Blocking
    @Transactional
    public void processRestockCommand(final RestockItemCommand restockItemCommand) {
        logger.debug("\nRestockItemCommand Received: {}", restockItemCommand);
            stockRoom.handleRestockItemCommand(restockItemCommand.getItem())
                    .thenApply(c -> {
                        return inventoryEmitter.send(c);
                    });
    }
}
