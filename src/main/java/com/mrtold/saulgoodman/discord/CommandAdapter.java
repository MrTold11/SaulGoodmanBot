package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.Main;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.logic.endpoint.*;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachName;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachPhone;
import com.mrtold.saulgoodman.logic.endpoint.attach.AttachSignature;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final Strings s = Strings.getInstance();
    final Config config = Config.getInstance();
    final DatabaseConnector db = DatabaseConnector.getInstance();
    final Logger log = LoggerFactory.getLogger(CommandAdapter.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(s.get("cmd.err.no_guild")).setEphemeral(true).queue();
            return;
        }

        if (DiscordUtils.hasNotAdvocatePerms(event.getMember())) {
            event.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        Consumer<String> failureConsumer = s -> event.getHook().sendMessage(s).queue();
        Function<String, Runnable> successFunc = s1 -> () -> event.getHook().sendMessage(s.get(s1)).queue();

        long advocateId = event.getUser().getIdLong();
        Member targetMember = event.getOption(s.get("cmd.arg.user"), OptionMapping::getAsMember);
        Long clientDsId = targetMember == null ? null : targetMember.getIdLong();
        Integer passport = event.getOption(s.get("cmd.arg.pass"), OptionMapping::getAsInt);
        String name = event.getOption(s.get("cmd.arg.name"), OptionMapping::getAsString);
        final Message.Attachment attachment = event.getOption(s.get("cmd.arg.signature"), OptionMapping::getAsAttachment);

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_SIGN:
                checkNotNull(clientDsId, passport, attachment, name);
                int num = Objects.requireNonNull(event.getOption(s.get("cmd.arg.num"), OptionMapping::getAsInt));
                //noinspection DataFlowIssue
                new SignAgreement(advocateId, clientDsId, name, passport, num,
                        DiscordUtils.attachmentSupplier(attachment))
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_TERMINATE:
                String reason = event.getOption(s.get("cmd.arg.term_reason"), s.get("str.not_spec"), OptionMapping::getAsString);

                new TerminateAgreement(advocateId, clientDsId, passport, reason)
                        .exec(successFunc.apply("str.cmd_success"), failureConsumer);
                break;

            case Main.CMD_INVITE:
                checkNotNull(clientDsId, passport, attachment);
                //noinspection DataFlowIssue
                new InviteAdvocate(advocateId, clientDsId, passport, name,
                        DiscordUtils.attachmentSupplier(attachment))
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
                        new AttachSignature(advocateId, DiscordUtils.attachmentSupplier(doc))
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
                event.getHook().sendMessage(s.get("cmd.err.incorrect_cmd")).queue();
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;
        if (event.getComponentId().equals("bill_payed")) {
            event.deferReply(true).queue();
            new CloseBill(event.getUser().getIdLong(), event.getMessage())
                    .exec(
                            () -> event.getHook().sendMessage(s.get("str.receipt_paid")).queue(),
                            s -> event.getHook().sendMessage(s).queue());
        } else if (event.getComponentId().equals("agreement_request")) {
            Client client = db.getClientByDiscord(event.getUser().getIdLong());
            if (client != null) {
                event.reply(s.get("message.already_client")).setEphemeral(true).queue();
                return;
            }

            TextInput name = TextInput.create("agreement_request_name",
                            s.get("embed.modal.name.label"), TextInputStyle.SHORT)
                    .setPlaceholder(s.get("embed.modal.name.desc"))
                    .setMinLength(5)
                    .setMaxLength(50)
                    .build();
            TextInput pass = TextInput.create("agreement_request_pass",
                            s.get("embed.modal.passport.label"), TextInputStyle.SHORT)
                    .setPlaceholder(s.get("embed.modal.passport.desc"))
                    .setMinLength(1)
                    .setMaxLength(8)
                    .build();
            TextInput desc = TextInput.create("agreement_request_desc",
                            s.get("embed.modal.description.label"), TextInputStyle.PARAGRAPH)
                    .setPlaceholder(s.get("embed.modal.description.desc"))
                    .setRequired(false)
                    .setMaxLength(1500)
                    .build();

            Modal modal = Modal.create("agreement_request_form", s.get("embed.modal.request"))
                    .addComponents(ActionRow.of(name), ActionRow.of(pass), ActionRow.of(desc))
                    .build();
            event.replyModal(modal).queue();
        } else if (event.getComponentId().startsWith("aReq_")) {
            if (DiscordUtils.hasNotAdvocatePerms(event.getMember())) {
                event.reply(s.get("cmd.err.no_perm"))
                        .setEphemeral(true).queue();
                return;
            }

            Advocate advocate = advocateSearch(event.getUser(), event);
            if (advocate == null) return;

            event.deferReply(true).queue();
            int pass = Integer.parseInt(event.getComponentId().split("_")[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || client.getDsUserChannel() == null || client.getDsUserId() == null) {
                event.getHook().sendMessage(s.get("cmd.err.client_nf")).queue();
                return;
            }

            TextChannel tc = event.getGuild().getTextChannelById(client.getDsUserChannel());
            if (tc == null) {
                db.deleteClient(client);
                event.getMessage().editMessage(s.get("str.request_cancelled"))
                        .setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_cancelled")).queue();
                return;
            }

            if (event.getComponentId().startsWith("aReq_acc_")) {
                Objects.requireNonNull(tc).getManager()
                        .putMemberPermissionOverride(advocate.getDsUserId(),
                                Permission.getRaw(Permission.VIEW_CHANNEL), 0)
                        .putMemberPermissionOverride(client.getDsUserId(),
                                Permission.getRaw(Permission.VIEW_CHANNEL), 0)
                        .queue();
                event.getMessage().editMessage(s.get("str.request_accepted_by") +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_accepted")).queue();
            } else {
                db.deleteClient(client);
                tc.delete().queue();
                event.getMessage().editMessage(s.get("str.request_declined_by") +
                        Objects.requireNonNull(event.getMember()).getAsMention()).setComponents().queue();
                event.getHook().sendMessage(s.get("str.request_declined")).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != Config.getInstance().getGuildId())
            return;
        if (event.getModalId().equals("agreement_request_form")) {
            event.deferReply(true).queue();
            String name = extractModalValue(event, "agreement_request_name");
            String pass = extractModalValue(event, "agreement_request_pass");
            String desc = extractModalValue(event, "agreement_request_desc");
            int passport;
            long dsId = Objects.requireNonNull(event.getMember()).getIdLong();

            try {
                if (pass == null) throw new RuntimeException();
                passport = Integer.parseInt(pass);
                if (passport < 1) throw new RuntimeException();
            } catch (Exception e) {
                event.getHook().sendMessage(s.get("message.request_wrong_pass"))
                        .queue();
                return;
            }

            Client client = db.getClientByPass(passport);
            if (client != null || name == null) {
                event.getHook().sendMessage(s.get("message.request_failed"))
                        .queue();
                return;
            }

            TextChannel tc = DiscordUtils.createPersonalChannel( null, "❕・" + name, dsId, null);
            client = new Client(passport, dsId, name, tc.getIdLong());

            Objects.requireNonNull(event.getGuild().getTextChannelById(config.getRequestsChannelId()))
                    .sendMessage(MessageCreateData.fromEmbeds(
                            DiscordUtils.prepareEmbedBuilder(15132410, s.get("embed.title.agreement_request"))
                                    .setDescription(String.format(Locale.getDefault(),
                                            s.get("embed.body.agreement_request"),
                                            event.getMember().getAsMention(),
                                            DiscordUtils.getEmbedData(name),
                                            passport,
                                            tc.getAsMention(),
                                            DiscordUtils.getEmbedData(desc))).build()))
                    .setActionRow(
                            Button.success("aReq_acc_" + passport, s.get("embed.button.request_accept")),
                            Button.danger("aReq_dec_" + passport, s.get("embed.button.request_decline")))
                    .queue();

            db.saveClient(client);
            event.getHook().sendMessage(s.get("message.request_accepted")).queue();
            tc.sendMessage(event.getMember().getAsMention() +
                    s.get("message.personal_welcome")).queue();
            tc.sendMessage(String.format(s.get("message.request_desc"), desc)).queue();
        }
    }

    private @Nullable String extractModalValue(@NotNull ModalInteractionEvent event, String id) {
        ModalMapping m = event.getValue(id);
        if (m == null) return null;
        return m.getAsString();
    }

    private Advocate advocateSearch(@NotNull User user, IReplyCallback event) {
        Advocate advocate = db.getAdvocateByDiscord(user.getIdLong());
        if (advocate == null || advocate.getSignature() == null) {
            log.error("Could not find advocate for user {}", user.getName());
            event.reply(s.get("cmd.err.no_perm")).setEphemeral(true).queue();
        }
        return advocate;
    }

    private void checkNotNull(Object... oa) {
        for (Object o : oa)
            if (o == null)
                throw new NullPointerException();
    }

}
