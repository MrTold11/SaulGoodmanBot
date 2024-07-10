package com.mrtold.saulgoodman.discord.event;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Mr_Told
 */
public class ModalEventListener {

    final Function<String, Boolean> condition;
    final Consumer<ModalInteractionEvent> eventConsumer;

    public ModalEventListener(Function<String, Boolean> condition, Consumer<ModalInteractionEvent> eventConsumer) {
        this.condition = condition;
        this.eventConsumer = eventConsumer;
    }

    public boolean apply(ModalInteractionEvent event) {
        if (condition.apply(event.getModalId())) {
            eventConsumer.accept(event);
            return true;
        }
        return false;
    }

}
