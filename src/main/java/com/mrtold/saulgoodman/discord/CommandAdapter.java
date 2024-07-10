package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.discord.event.DiscordEventManager;
import com.mrtold.saulgoodman.logic.endpoint.*;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachName;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachPhone;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachSignature;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final Strings s = Strings.getInstance();
    final static Logger log = LoggerFactory.getLogger(CommandAdapter.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        boolean isRequest = event.getName().equalsIgnoreCase(Main.CMD_REQUEST);
        if (!isRequest)
            event.deferReply(true).queue();

        Guild guild = event.getGuild();
        if (guild == null) {
            if (isRequest) event.deferReply(true).queue();
            event.getHook().sendMessage(s.get("cmd.err.no_guild")).queue(MSG_DELETE_10);
            return;
        }

        if (DsUtils.hasNotAdvocatePerms(event.getMember())) {
            if (isRequest) event.deferReply(true).queue();
            event.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
            return;
        }

        if (DiscordEventManager.onSlashCommandInteractionEvent(event)) return;

        Consumer<String> failureConsumer = s ->
                event.getHook().sendMessage(s).queue(MSG_DELETE_10);
        Function<String, Runnable> successFunc = s1 -> () ->
                event.getHook().sendMessage(s.get(s1)).queue(MSG_DELETE_10);

        long advocateId = event.getUser().getIdLong();
        Member targetMember = event.getOption(s.get("cmd.arg.user"), OptionMapping::getAsMember);
        Long clientDsId = targetMember == null ? null : targetMember.getIdLong();
        Integer passport = event.getOption(s.get("cmd.arg.pass"), OptionMapping::getAsInt);
        String name = event.getOption(s.get("cmd.arg.name"), OptionMapping::getAsString);
        String reason;
        final Message.Attachment attachment = event.getOption(s.get("cmd.arg.signature"), OptionMapping::getAsAttachment);

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                checkNotNull(clientDsId, passport, attachment, name);
                int num = Objects.requireNonNull(event.getOption(s.get("cmd.arg.num"), OptionMapping::getAsInt));
                //noinspection DataFlowIssue
                new SignAgreement(advocateId, clientDsId, name, passport, num,
                        DsUtils.attachmentSupplier(attachment))
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_TERMINATE:
                reason = event.getOption(s.get("cmd.arg.term_reason"), s.get("str.not_spec"), OptionMapping::getAsString);

                new TerminateAgreement(advocateId, clientDsId, passport, reason)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_INVITE:
                checkNotNull(clientDsId, passport, attachment);
                //noinspection DataFlowIssue
                new InviteAdvocate(advocateId, clientDsId, passport, name,
                        DsUtils.attachmentSupplier(attachment))
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_UNINVITE:
                reason = event.getOption(s.get("cmd.arg.term_reason"), s.get("str.not_spec"), OptionMapping::getAsString);
                new UninviteAdvocate(advocateId, clientDsId, passport, reason)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_NAME:
                checkNotNull(passport, name);
                //noinspection DataFlowIssue
                new RenameClient(advocateId, passport, name)
                        .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                break;

            case Main.CMD_ATTACH:
                String subCmd = Objects.requireNonNull(event.getSubcommandName());

                if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_PHONE)) {
                    int phone = Objects.requireNonNull(event.getOption(s.get("cmd.arg.phone"), OptionMapping::getAsInt));
                    new AttachPhone(advocateId, phone)
                            .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                } else if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_NAME)) {
                    checkNotNull(name);
                    new AttachName(advocateId, name)
                            .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                } else {
                    Message.Attachment doc = Objects.requireNonNull(event.getOption(s.get("cmd.arg.doc_img"), OptionMapping::getAsAttachment));
                    if (subCmd.equalsIgnoreCase(Main.CMD_ATTACH_SIGNATURE)) {
                        new AttachSignature(advocateId, DsUtils.attachmentSupplier(doc))
                                .exec(successFunc.apply("str.data_upd_ok"), failureConsumer);
                    }
                    //todo
                }
                break;

            case Main.CMD_RECEIPT:
                checkNotNull(clientDsId);
                int amount = Objects.requireNonNull(event.getOption(s.get("cmd.arg.amount"), OptionMapping::getAsInt));

                new OpenBill(clientDsId, advocateId, passport, amount)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_REMOVE:
                checkNotNull(passport);
                //noinspection DataFlowIssue
                new RemoveClient(advocateId, passport)
                        .exec(successFunc.apply("str.client_deleted"), failureConsumer);
                break;

            default:
                event.getHook().sendMessage(s.get("cmd.err.incorrect_cmd")).queue(MSG_DELETE_10);
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;

        if (!DiscordEventManager.onButtonInteractionEvent(event))
            log.warn("Got unknown button interaction event, id: {}", event.getComponentId());
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;

        if (!DiscordEventManager.onModalInteractionEvent(event))
            log.warn("Got unknown modal interaction event, id: {}", event.getModalId());
    }

    private void checkNotNull(Object... oa) {
        for (Object o : oa)
            if (o == null)
                throw new NullPointerException();
    }

}
