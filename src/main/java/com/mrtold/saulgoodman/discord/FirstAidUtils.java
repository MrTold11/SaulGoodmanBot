package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.logic.manager.firstaid.FirstAidManager;
import com.mrtold.saulgoodman.logic.manager.firstaid.FirstAidRequest;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

/**
 * @author Mr_Told
 */
public class FirstAidUtils {

    final static Logger log = LoggerFactory.getLogger(FirstAidUtils.class);

    @Nullable
    public static Long initFirstAidMessages() {
        Strings s = Strings.getInstance();
        DsUtils.publishInitMessage(Config.getInstance().getFirstAidChannelId(),
                channel -> channel.sendMessage(s.get("message.firstaid"))
                        .setActionRow(Button.danger("firstaid_request", s.get("embed.button.firstaid")))
                        .complete().getIdLong());
        return DsUtils.publishInitMessage(Config.getInstance().getShiftChannelId(),
                channel -> channel.sendMessage(MessageCreateData.fromEmbeds(
                        DsUtils.prepareEmbedBuilder(15132410, s.get("embed.title.shift_registry")).build()))
                        .setContent(s.get("message.duty_header"))
                        .setActionRow(
                                Button.success("firstaid_enter", s.get("embed.button.firstaid_enter")),
                                Button.danger("firstaid_exit", s.get("embed.button.firstaid_exit")))
                        .complete().getIdLong(),
                message -> message.editMessageEmbeds(
                        DsUtils.prepareEmbedBuilder(15132410, s.get("embed.title.shift_registry")).build()
                        ).complete()
        );
    }

    public static void updateShiftRegistry(Set<Advocate> onShift, FirstAidManager manager) {
        Message registry = DsUtils.getMessageById(Config.getInstance().getShiftChannelId(),
                manager.getShiftRegistryMessageId());
        if (registry == null) {
            log.warn("Could not find shift registry! Creating a new one...");
            Long mId = initFirstAidMessages();
            manager.setShiftRegistryMessageId(mId);
            registry = DsUtils.getMessageById(Config.getInstance().getShiftChannelId(), mId);
            if (registry == null) {
                log.error("Could not create shift registry!");
                return;
            }
        }

        StringBuilder builder = new StringBuilder("Имя, Фамилия - Тег - Телефон\n");

        for (Advocate advocate : onShift) {
            builder
                    .append(advocate.getName()).append(" - ")
                    .append(DsUtils.getMemberAsMention(advocate.getDsUserId())).append(" - ")
                    .append(DsUtils.getEmbedData(advocate.getPhone())).append("\n");
        }

        registry.editMessageEmbeds(
                DsUtils.prepareEmbedBuilder(15132410, Strings.getS("embed.title.shift_registry"))
                        .setDescription(builder.toString())
                        .build()).queue();
    }

    public static void publishRequest(FirstAidRequest request, Set<Advocate> onShift) {
        Strings s = Strings.getInstance();
        DsUtils.getRequestsChannel()
                .sendMessage(MessageCreateData.fromEmbeds(
                        DsUtils.prepareEmbedBuilder(14357564, s.get("embed.title.firstaid_request"))
                                .setDescription(String.format(Locale.getDefault(),
                                        s.get("embed.body.firstaid_request"),
                                        DsUtils.getMemberAsMention(request.getClient().getDsUserId()),
                                        request.getClient().getName(),
                                        request.getClient().getPassport()
                                )).build()))
                .setContent(activeAdvocatesMention(onShift))
                .setActionRow(
                        Button.success("faReq_" + request.getStart(), s.get("embed.button.request_accept"))
                ).queue();
    }

    private static String activeAdvocatesMention(Set<Advocate> onShift) {
        if (onShift == null || onShift.isEmpty())
            return DsUtils.getRoleAsMention(Config.getInstance().getAdvocateRoleId());

        StringBuilder builder = new StringBuilder();
        for (Advocate advocate : onShift) {
            builder.append(DsUtils.getMemberAsMention(advocate.getDsUserId())).append(" ");
        }
        return builder.toString().trim();
    }

}
