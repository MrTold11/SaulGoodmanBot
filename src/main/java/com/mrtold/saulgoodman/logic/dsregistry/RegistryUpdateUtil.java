package com.mrtold.saulgoodman.logic.dsregistry;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Mr_Told
 */
public class RegistryUpdateUtil {

    private static final Strings s = Strings.getInstance();
    private static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy");

    public static void updateClientsRegistry() {
        updateRegistry(s.get("embed.title.registry"), 15132410,
                "Соглашение - Имя, Фамилия - Паспорт - Тег - Канал",
                DatabaseConnector.getInstance().getActiveAgreements(),
                (agreement, clientMap, sb, guild) -> {
                    Client c = clientMap.get(agreement.getClient());
                    if (c == null || c.getPassport() == 0) return;
                    sb
                            .append(agreement.getNumber()).append(" - ")
                            .append(c.getName()).append(" - ")
                            .append(c.getPassport()).append(" - ");

                    registryAppendDs(guild, sb, c);
                    sb.append("\n");
                });
    }

    public static void updateReceiptRegistry() {
        updateRegistry(s.get("embed.title.receipt_registry"), 15132410,
                "Номер - Имя, Фамилия - Паспорт - Тег - Канал - Сумма - Дата",
                DatabaseConnector.getInstance().getActiveReceipts(),
                (receipt, clientMap, sb, guild) -> {
                    Client c = clientMap.get(receipt.getClient());
                    if (c == null || c.getPassport() == 0) return;
                    sb
                            .append(receipt.getId()).append(" - ")
                            .append(c.getName()).append(" - ")
                            .append(c.getPassport()).append(" - ");

                    registryAppendDs(guild, sb, c);
                    sb.append(" - ").append(receipt.getAmount())
                            .append("$ - ").append(df.format(receipt.getIssued()));
                    sb.append("\n");
                });
    }

    private static void registryAppendDs(Guild guild, StringBuilder sb, Client c) {
        try {
            if (c.getDsUserId() == null) throw new RuntimeException();
            Member m = guild.retrieveMemberById(c.getDsUserId()).complete();
            sb.append(m.getAsMention());
        } catch (Exception e) {
            sb.append("NO");
        }
        sb.append(" - ");

        TextChannel tc;
        if (c.getDsUserChannel() != null
                && (tc = guild.getTextChannelById(c.getDsUserChannel())) != null)
            sb.append(tc.getAsMention());
        else sb.append("NO");
    }

    public static <T> void updateRegistry(String title, int color, String tableTitle,
                                           List<T> items, RegistryFunc<T> func) {
        Config config = Config.getInstance();
        Guild guild = DsUtils.getGuild();
        TextChannel registryChannel = guild.getTextChannelById(config.getRegistryChannelId());
        if (registryChannel == null) {
            LoggerFactory.getLogger(RegistryUpdateUtil.class).error(
                    "Could not find channel for clients registry w/ id {}",
                    config.getRegistryChannelId());
            return;
        }

        Message registry = null;
        for (Message m : registryChannel.getHistory().retrievePast(5).complete()) {
            if (m.getAuthor().getIdLong() == guild.getJDA().getSelfUser().getIdLong() &&
                    Objects.equals(m.getEmbeds().get(0).getTitle(), title)) {
                registry = m;
                break;
            }
        }

        if (registry == null) {
            registry = registryChannel.sendMessage(MessageCreateData.fromEmbeds(
                    DsUtils.prepareEmbedBuilder(color, title)
                            .build())).complete();
        }

        StringBuilder sb = new StringBuilder(tableTitle).append("\n");
        Map<Integer, Client> clientMap = new HashMap<>();
        DatabaseConnector.getInstance().getAllClients()
                .forEach(client -> clientMap.put(client.getPassport(), client));

        for (T t : items)
            func.process(t, clientMap, sb, guild);

        registry.editMessage(MessageEditData.fromEmbeds(
                DsUtils.prepareEmbedBuilder(color, title)
                        .setDescription(sb.toString())
                        .build()
        )).queue();
    }

}
