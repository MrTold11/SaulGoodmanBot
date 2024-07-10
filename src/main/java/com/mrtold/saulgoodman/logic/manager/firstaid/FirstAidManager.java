package com.mrtold.saulgoodman.logic.manager.firstaid;

import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.discord.FirstAidUtils;
import com.mrtold.saulgoodman.discord.event.DiscordEventManager;
import com.mrtold.saulgoodman.logic.manager.AbstractLogicManager;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_10;
import static com.mrtold.saulgoodman.discord.DsUtils.MSG_DELETE_60;

/**
 * @author Mr_Told
 */
public class FirstAidManager extends AbstractLogicManager {

    public static void init() {
        new FirstAidManager(FirstAidUtils.initFirstAidMessages());
    }

    Long shiftRegistryMessageId;
    final Set<Advocate> onShift = new HashSet<>();
    final Map<Long, FirstAidRequest> requests = new HashMap<>(); // request id - request
    final Map<Integer, Long> lastRequestTime = new HashMap<>();  // client id - request time

    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public FirstAidManager(Long shiftRegistryMessageId) {
        this.shiftRegistryMessageId = shiftRegistryMessageId;
    }

    @Override
    protected void initEventListeners() {
        DatabaseConnector db = DatabaseConnector.getInstance();
        Strings s = Strings.getInstance();

        DiscordEventManager.addButtonListenerExact("firstaid_request", (e, args) -> {
            e.deferReply(true).queue();
            Client client = db.getClientByDiscord(e.getUser().getIdLong());
            if (client == null) {
                List<Client> twinks = db.getClientTwinks(e.getUser().getIdLong());
                twinks.removeIf(c -> !db.clientHasActiveAgreement(c.getPassport()));
                if (twinks.isEmpty()) {
                    e.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                    return;
                }

                if (twinks.size() == 1)
                    client = twinks.get(0);
                else {
                    e.getHook().sendMessage(s.get("message.select_twink"))
                            .setActionRow(twinks.stream().map(
                                    c -> Button.primary("twink_faReq_%d".formatted(c.getPassport()), c.getName())
                            ).toList()).queue(MSG_DELETE_60);
                    return;
                }
            } else if (!db.clientHasActiveAgreement(client.getPassport())) {
                e.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                return;
            }

            if (registerRequest(client))
                e.getHook().sendMessage(s.get("message.firstaid_accepted")).queue(MSG_DELETE_10);
            else
                e.getHook().sendMessage(s.get("message.firstaid_timeout")).queue(MSG_DELETE_10);
        });

        DiscordEventManager.addButtonListenerStartsWith("twink_faReq_", (e, args) -> {
            e.deferReply(true).queue();
            int pass = Integer.parseInt(args[2]);
            Client client = db.getClientByPass(pass);
            if (client == null || !Objects.equals(client.getDsUserId(), e.getUser().getIdLong())) {
                e.getHook().sendMessage(s.get("cmd.err.no_perm")).queue(MSG_DELETE_10);
                return;
            }

            if (registerRequest(client))
                e.getHook().sendMessage(s.get("message.firstaid_accepted")).queue(MSG_DELETE_10);
            else
                e.getHook().sendMessage(s.get("message.firstaid_timeout")).queue(MSG_DELETE_10);
        });

        DiscordEventManager.addButtonListenerStartsWith("faReq_", (e, args) -> {
            e.deferReply(true).queue();
            long id = Long.parseLong(args[1]);
            Advocate advocate = advocateSearch(e);
            if (advocate == null) return;

            e.getMessage().editMessage(s.get("str.request_accepted_by") +
                    DsUtils.getMemberAsMention(e.getUser().getIdLong())).setComponents().queue();
            acceptRequest(id, advocate, m -> e.getHook().sendMessage(m).queue(MSG_DELETE_10));
        });

        DiscordEventManager.addButtonListenerExact("firstaid_enter", (e, args) -> {
            e.deferReply(true).queue();
            Advocate advocate = advocateSearch(e);
            if (advocate == null) return;
            startShift(advocate);
            e.getHook().sendMessage(s.get("message.duty_enter")).queue(MSG_DELETE_10);
        });

        DiscordEventManager.addButtonListenerExact("firstaid_exit", (e, args) -> {
            e.deferReply(true).queue();
            Advocate advocate = advocateSearch(e);
            if (advocate == null) return;
            endShift(advocate);
            e.getHook().sendMessage(s.get("message.duty_exit")).queue(MSG_DELETE_10);
        });
    }

    public void startShift(Advocate advocate) {
        onShift.add(advocate);
        FirstAidUtils.updateShiftRegistry(onShift, this);
    }

    public void endShift(Advocate advocate) {
        onShift.remove(advocate);
        FirstAidUtils.updateShiftRegistry(onShift, this);
    }

    public boolean registerRequest(@NotNull Client client) {
        FirstAidRequest request;

        synchronized (requests) {
            Long previousRequest = lastRequestTime.get(client.getPassport());
            if (previousRequest != null && System.currentTimeMillis() - previousRequest < 1000 * 60 * 3)
                return false;

            request = new FirstAidRequest(client);
            //dumb way but okay
            while (requests.containsKey(request.getStart())) {
                request = new FirstAidRequest(request.getStart() + 1, client);
            }
            requests.put(request.getStart(), request);
            lastRequestTime.put(client.getPassport(), request.getStart());
        }

        FirstAidUtils.publishRequest(request, onShift);
        return true;
    }

    public void acceptRequest(long id, @NotNull Advocate advocate, @NotNull Consumer<String> onResult) {
        Strings s = Strings.getInstance();
        FirstAidRequest request;

        synchronized (requests) {
            request = requests.remove(id);
        }

        if (request == null || request.isExpired()) {
            onResult.accept(s.get("str.request_nf"));
            return;
        }

        TextChannel channel = DsUtils.getChannelById(request.getClient().getDsUserChannel());
        long advocateUserId = advocate.getDsUserId();
        if (channel != null) {
            if (DsUtils.hasNotViewPermission(advocateUserId, channel.getIdLong())) {
                long channelId = channel.getIdLong();
                channel.upsertPermissionOverride(DsUtils.getGuildMember(advocateUserId))
                        .grant(Permission.VIEW_CHANNEL).queue();
                scheduledExecutor.schedule(() -> {
                    TextChannel ch = DsUtils.getChannelById(channelId);
                    if (ch != null)
                        ch.upsertPermissionOverride(DsUtils.getGuildMember(advocateUserId))
                                .clear(Permission.VIEW_CHANNEL).queue();
                }, 60, TimeUnit.MINUTES);
            }

            channel.sendMessage(s.get("message.firstaid_personal_format").formatted(
                    DsUtils.getMemberAsMention(request.getClient().getDsUserId()),
                    DsUtils.getMemberAsMention(advocateUserId),
                    DsUtils.getEmbedData(advocate.getPhone()))
            ).queue();
        }

        onResult.accept(s.get("str.request_accepted").formatted(DsUtils.getChannelAsMention(channel)));
    }

    public Long getShiftRegistryMessageId() {
        return shiftRegistryMessageId;
    }

    public void setShiftRegistryMessageId(Long shiftRegistryMessageId) {
        this.shiftRegistryMessageId = shiftRegistryMessageId;
    }
}
