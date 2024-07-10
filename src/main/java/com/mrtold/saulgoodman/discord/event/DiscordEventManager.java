package com.mrtold.saulgoodman.discord.event;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Mr_Told
 */
public class DiscordEventManager {

    final static Set<ButtonEventListener> buttonEventListeners = ConcurrentHashMap.newKeySet();
    final static Set<ModalEventListener> modalEventListeners = ConcurrentHashMap.newKeySet();
    final static Map<String, Consumer<SlashCommandInteractionEvent>> commandEventListeners = new ConcurrentHashMap<>();

    public static ButtonEventListener addButtonListenerExact(String name, ButtonEventConsumer eventConsumer) {
        return addButtonListener(name::equals, eventConsumer);
    }

    public static ButtonEventListener addButtonListenerStartsWith(String prefix, ButtonEventConsumer eventConsumer) {
        return addButtonListener(s -> s.startsWith(prefix), eventConsumer);
    }

    public static ButtonEventListener addButtonListener(Function<String, Boolean> condition,
                                                        ButtonEventConsumer eventConsumer) {
        return addButtonListener(new ButtonEventListener(condition, eventConsumer));
    }

    public static ButtonEventListener addButtonListener(ButtonEventListener buttonEventListener) {
        buttonEventListeners.add(buttonEventListener);
        return buttonEventListener;
    }

    public static void removeButtonListener(ButtonEventListener buttonEventListener) {
        buttonEventListeners.remove(buttonEventListener);
    }

    public static void addSlashCommandListener(String cmd, Consumer<SlashCommandInteractionEvent> consumer) {
        commandEventListeners.put(cmd, consumer);
    }

    public static void removeSlashCommandListener(String cmd) {
        commandEventListeners.remove(cmd);
    }

    public static ModalEventListener addModalListenerExact(String name,
                                                           Consumer<ModalInteractionEvent> eventConsumer) {
        return addModalListener(name::equals, eventConsumer);
    }

    public static ModalEventListener addModalListenerStartsWith(String prefix,
                                                                Consumer<ModalInteractionEvent> eventConsumer) {
        return addModalListener(s -> s.startsWith(prefix), eventConsumer);
    }

    public static ModalEventListener addModalListener(Function<String, Boolean> condition,
                                                      Consumer<ModalInteractionEvent> eventConsumer) {
        return addModalListener(new ModalEventListener(condition, eventConsumer));
    }

    public static ModalEventListener addModalListener(ModalEventListener modalEventListener) {
        modalEventListeners.add(modalEventListener);
        return modalEventListener;
    }

    public static void removeModalListener(ModalEventListener modalEventListener) {
        modalEventListeners.remove(modalEventListener);
    }

    public static boolean onButtonInteractionEvent(ButtonInteractionEvent e) {
        for (ButtonEventListener listener : buttonEventListeners) {
            if (listener.apply(e)) return true;
        }
        return false;
    }

    public static boolean onSlashCommandInteractionEvent(@NotNull SlashCommandInteractionEvent e) {
        Consumer<SlashCommandInteractionEvent> consumer = commandEventListeners.get(
                e.getName().toLowerCase(Locale.ROOT)
        );
        if (consumer == null) return false;
        consumer.accept(e);
        return true;
    }

    public static boolean onModalInteractionEvent(@NotNull ModalInteractionEvent e) {
        for (ModalEventListener listener : modalEventListeners) {
            if (listener.apply(e)) return true;
        }
        return false;
    }

}
