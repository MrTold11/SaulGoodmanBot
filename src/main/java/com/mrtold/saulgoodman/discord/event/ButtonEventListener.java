package com.mrtold.saulgoodman.discord.event;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.function.Function;

/**
 * @author Mr_Told
 */
public class ButtonEventListener {

    final Function<String, Boolean> condition;
    final ButtonEventConsumer eventConsumer;

    public ButtonEventListener(Function<String, Boolean> condition, ButtonEventConsumer eventConsumer) {
        this.condition = condition;
        this.eventConsumer = eventConsumer;
    }

    public boolean apply(ButtonInteractionEvent event) {
        if (condition.apply(event.getComponentId())) {
            eventConsumer.accept(event, event.getComponentId().split("_"));
            return true;
        }
        return false;
    }

}
