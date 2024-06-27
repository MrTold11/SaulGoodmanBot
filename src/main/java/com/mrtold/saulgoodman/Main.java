package com.mrtold.saulgoodman;

import com.mrtold.saulgoodman.api.WebApi;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.CommandAdapter;
import com.mrtold.saulgoodman.discord.DsUtils;
import com.mrtold.saulgoodman.logic.firstaid.FirstAidManager;
import com.mrtold.saulgoodman.utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Mr_Told
 */
public class Main {

    public static final String
            CMD_SIGN = "sign",
            CMD_TERMINATE = "terminate",
            CMD_INVITE = "invite",
            CMD_UNINVITE = "uninvite",
            CMD_REQUEST = "request",
            CMD_NAME = "name",
            CMD_CLAIM = "claim",
            CMD_RECEIPT = "bill",
            CMD_REMOVE = "remove",
            CMD_ATTACH = "attach",
            //CMD_ATTACH_PASS = "паспорт",
            //CMD_ATTACH_LICENSE = "лицензия",
            CMD_ATTACH_SIGNATURE = "подпись",
            CMD_ATTACH_PHONE = "телефон",
            CMD_ATTACH_NAME = "имя";

    static JDA jda = null;

    public static JDA getJDA() {
        if (jda == null)
            throw new IllegalStateException("JDA not initialized");
        return jda;
    }

    final CLI cli;
    final Strings s;
    final Config config;
    final DatabaseConnector db;
    final WebApi api;

    public Main() throws IOException {
        DocUtils.init();

        config = Config.getInstance().load(new File("config.json"));
        s = Strings.getInstance().load(new File("strings.json"),
                (s) -> s.override("cmd.name.sign", CMD_SIGN)
                        .override("cmd.name.invite", CMD_INVITE)
                        .override("cmd.name.uninvite", CMD_UNINVITE)
                        .override("cmd.name.request", CMD_REQUEST)
                        .override("cmd.name.terminate", CMD_TERMINATE)
                        .override("cmd.name.name", CMD_NAME)
                        .override("cmd.name.receipt", CMD_RECEIPT)
                        .override("cmd.name.remove", CMD_REMOVE)
                        .override("cmd.name.attach", CMD_ATTACH)
                        .override("cmd.name.claim", CMD_CLAIM));

        db = DatabaseConnector.init(config.getDbHost(), config.getDbPort(), config.getDbName(),
                config.getDbUser(), config.getDbPass());

        api = WebApi.init(config.getDiscordClientId(), config.getDiscordClientSecret(),
                config.getOAuth2Redirect(), config.getApiPort());

        jda = JDABuilder.createLight(config.getDiscordToken(), Collections.emptyList())
                .setActivity(Activity.watching("за правосудием на Sunrise"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        jda.addEventListener(new CommandAdapter());

        OptionData userOpt = new OptionData(OptionType.USER, s.get("cmd.arg.user"),
                s.get("cmd.arg.desc.user"), true);
        OptionData userOptNotReq = new OptionData(OptionType.USER, s.get("cmd.arg.user"),
                s.get("cmd.arg.desc.user"), false);
        OptionData nameOpt = new OptionData(OptionType.STRING, s.get("cmd.arg.name"),
                s.get("cmd.arg.desc.name"), true);
        OptionData passOpt = new OptionData(OptionType.INTEGER, s.get("cmd.arg.pass"),
                s.get("cmd.arg.desc.pass"), true);
        OptionData phoneOpt = new OptionData(OptionType.INTEGER, s.get("cmd.arg.phone"),
                s.get("cmd.arg.desc.phone"), true);
        OptionData numOpt = new OptionData(OptionType.INTEGER, s.get("cmd.arg.num"),
                s.get("cmd.arg.desc.num"), true);
        OptionData signImgOpt = new OptionData(OptionType.ATTACHMENT, s.get("cmd.arg.signature"),
                s.get("cmd.arg.desc.signature"), true);
        OptionData docImgOpt = new OptionData(OptionType.ATTACHMENT, s.get("cmd.arg.doc_img"),
                s.get("cmd.arg.desc.doc_img"), true);
        OptionData reasonOpt = new OptionData(OptionType.STRING, s.get("cmd.arg.term_reason"),
                s.get("cmd.arg.desc.term_reason"), true);
        OptionData passOptNotReq = new OptionData(OptionType.INTEGER, s.get("cmd.arg.pass"),
                s.get("cmd.arg.desc.pass"), false);
        OptionData amountOpt = new OptionData(OptionType.INTEGER, s.get("cmd.arg.amount"),
                s.get("cmd.arg.desc.amount"), true);

        jda.updateCommands().complete();
        DsUtils.getGuild().updateCommands().complete();
        DsUtils.getGuild().updateCommands().addCommands(
                generateMemberCommand("sign", numOpt, userOpt, nameOpt, passOpt, signImgOpt),
                generateMemberCommand("invite", userOpt, nameOpt, passOpt, signImgOpt),
                generateMemberCommand("uninvite", reasonOpt, userOptNotReq, passOptNotReq),
                generateMemberCommand("request"),
                generateMemberCommand("terminate", reasonOpt, userOptNotReq, passOptNotReq),
                generateMemberCommand("name", passOpt, nameOpt),
                generateMemberCommand("receipt", userOpt, amountOpt, passOptNotReq),
                generateMemberCommand("remove", passOpt),
                //generateMemberCommand("claim", passImgOpt, phoneOpt),
                Commands.slash(s.get("cmd.name.attach"), s.get("cmd.desc.attach"))
                        .setGuildOnly(true)
                        .addSubcommands(
                                //new SubcommandData(CMD_ATTACH_PASS,
                                //        s.get("cmd.arg.desc.pass_img")).addOptions(docImgOpt),
                                //new SubcommandData(CMD_ATTACH_LICENSE,
                                //        s.get("sub.desc.license")).addOptions(docImgOpt),
                                new SubcommandData(CMD_ATTACH_SIGNATURE,
                                        s.get("cmd.arg.desc.signature")).addOptions(docImgOpt),
                                new SubcommandData(CMD_ATTACH_PHONE,
                                        s.get("cmd.arg.desc.phone")).addOptions(phoneOpt),
                                new SubcommandData(CMD_ATTACH_NAME,
                                        s.get("cmd.arg.desc.name")).addOptions(nameOpt))
        ).complete();

        cli = new CLI(this);
        cli.start();

        initRequestMessage();
        FirstAidManager.init();
    }

    private void initRequestMessage() {
        DsUtils.publishInitMessage(config.getRequestChannelId(),
                channel -> channel.sendMessage(s.get("message.request"))
                        .setActionRow(Button.primary("agreement_request", s.get("embed.button.request_make")))
                        .complete().getIdLong());
    }

    private SlashCommandData generateMemberCommand(String dictKey, OptionData... options) {
        SlashCommandData cmd = Commands.slash(s.get("cmd.name." + dictKey), s.get("cmd.desc." + dictKey))
                .setGuildOnly(true);
        if (options != null && options.length != 0)
            cmd.addOptions(options);
        return cmd;
    }

    public void stop() {
        api.close();
        jda.shutdown();
        cli.close();
        db.close();
    }

    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(Main.class);
        try {
            new Main();
        } catch (Exception e) {
            log.error("Exception during initialization", e);
        }
    }

}