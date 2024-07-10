package com.mrtold.saulgoodman.discord.event;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

/**
 * @author Mr_Told
 */
public interface ButtonEventConsumer {

    void accept(ButtonInteractionEvent e, String[] args);

}
