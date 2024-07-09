package com.mrtold.saulgoodman.logic.firstaid;

import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.discord.FirstAidUtils;
import com.mrtold.saulgoodman.logic.model.Advocate;
import com.mrtold.saulgoodman.logic.model.Client;
import com.mrtold.saulgoodman.utils.Strings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Mr_Told
 */
public class FirstAidManager {

    static FirstAidManager instance;

    public static FirstAidManager getInstance() {
        if (instance == null)
            throw new NullPointerException("FirstAidManager has not been initialized yet.");
        return instance;
    }

    public static void init() {
        instance = new FirstAidManager(FirstAidUtils.initFirstAidMessages());
    }

    Long shiftRegistryMessageId;
    final Set<Advocate> onShift = new HashSet<>();
    final Map<Long, FirstAidRequest> requests = new HashMap<>(); // request id - request
    final Map<Integer, Long> lastRequestTime = new HashMap<>();     // client id - request time

    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public FirstAidManager(Long shiftRegistryMessageId) {
        this.shiftRegistryMessageId = shiftRegistryMessageId;
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
