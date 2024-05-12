package com.mrtold.saulgoodman;

import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.CommandAdapter;
import com.mrtold.saulgoodman.discord.DiscordUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Mr_Told
 */
public class Main {

    public static final String CMD_SIGN = "sign";
    public static final String CMD_TERMINATE = "terminate";
    public static final String CMD_INVITE = "invite";

    final CLI cli;
    final DiscordUtils dsUtils;
    final DatabaseConnector db;
    final JDA jda;

    public Main(Logger log) throws IOException {
        Properties secrets = new Properties();
        secrets.load(new FileInputStream("scrt.properties"));
        String token = secrets.getProperty("ds_token");
        String dbPwd = secrets.getProperty("db_pass");

        if (token == null || token.isBlank()) {
            log.error("No token (ds_token) provided in scrt.properties! Exiting.");
            System.exit(1);
        }
        if (dbPwd == null || dbPwd.isBlank()) {
            log.error("No database password (db_pass) provided in scrt.properties! Exiting.");
            System.exit(1);
        }

        db = new DatabaseConnector("localhost", 5432, "pacific_uni", "pacific", dbPwd);

        jda = JDABuilder.createLight(token, Collections.emptyList())
                .setActivity(Activity.watching("за правосудием на Sunrise"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        dsUtils = new DiscordUtils(jda)
                .setClientRoleId(1177290807976394813L)
                .setAdvocateRoleId(1177294186878357575L)
                .setHeadsRoleId(1182418198847561809L)
                .setClientRegistryChannelId(1238839369511604345L)
                .setAuditChannelId(1182670819709685791L)
                .addDict(
                        "cmd.name.sign", CMD_SIGN,
                        "cmd.desc.sign", "Подписать соглашение с клиентом",
                        "cmd.name.terminate", CMD_TERMINATE,
                        "cmd.desc.terminate", "Расторгнуть соглашение с клиентом",
                        "cmd.name.invite", CMD_INVITE,
                        "cmd.desc.invite", "Принять нового адвоката",
                        "cmd.arg.num", "номер",
                        "cmd.arg.user", "пользователь",
                        "cmd.arg.name", "имя-фамилия",
                        "cmd.arg.pass", "паспорт",
                        "cmd.arg.signature", "подпись",
                        "cmd.arg.term_reason", "причина",
                        "cmd.desc.num", "Укажите порядковый номер соглашения",
                        "cmd.desc.user", "Выберите пользователя или укажите его ID",
                        "cmd.desc.name", "Укажите имя фамилию клиента",
                        "cmd.desc.pass", "Укажите номер паспорта клиента",
                        "cmd.desc.signature", "Картинка подписи в виде приложения",
                        "cmd.desc.term_reason", "Укажите причину расторжения",
                        "str.not_spec", "Не указано",
                        "cmd.err.no_perm", "У вас нет разрешения на использование этой команды.",
                        "cmd.err.no_guild", "Команду можно использовать только на сервере",
                        "embed.author.name", "GTA 5 RP | Секретарь Джейсона Мориса",
                        "embed.author.url", null,
                        "embed.author.icon", "https://i.imgur.com/IKvjkjp.png",
                        "embed.title.url", "https://gta5rp.com/",
                        "embed.footer.icon", "https://i.imgur.com/4cJIDXV.png",
                        "embed.title.sign", "Подписание договора",
                        "embed.title.invite", "Добавление адвоката",
                        "embed.title.terminate", "Расторжение договора",
                        "embed.title.registry", "Реестр клиентов");

        jda.addEventListener(new CommandAdapter(dsUtils, db));

        OptionData userOpt = new OptionData(OptionType.USER, dsUtils.dict("cmd.arg.user"),
                dsUtils.dict("cmd.desc.user"), true);
        OptionData nameOpt = new OptionData(OptionType.STRING, dsUtils.dict("cmd.arg.name"),
                dsUtils.dict("cmd.desc.name"), true);
        OptionData passOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("cmd.desc.pass"), true);
        OptionData numOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.num"),
                dsUtils.dict("cmd.desc.num"), true);
        OptionData signOpt = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.signature"),
                dsUtils.dict("cmd.desc.signature"), true);
        OptionData reasonOpt = new OptionData(OptionType.STRING, dsUtils.dict("cmd.arg.term_reason"),
                dsUtils.dict("cmd.desc.term_reason"), true);
        OptionData passOptNotReq = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("cmd.desc.pass"), false);
        OptionData signOptNotReq = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.signature"),
                dsUtils.dict("cmd.desc.signature"), false);

        jda.updateCommands().addCommands(
                generateMemberCommand("cmd.name.sign", "cmd.desc.sign",
                        numOpt, userOpt, nameOpt, passOpt, signOpt), //todo signature not required
                generateMemberCommand("cmd.name.invite", "cmd.desc.invite",
                        userOpt, nameOpt, passOpt, signOpt),
                generateMemberCommand("cmd.name.terminate", "cmd.desc.terminate",
                        userOpt, reasonOpt, passOptNotReq)
        ).queue();

        cli = new CLI(this);
        cli.start();
    }

    private SlashCommandData generateMemberCommand(String dictKeyName, String dictKeyDesc, OptionData... options) {
        SlashCommandData cmd = Commands.slash(dsUtils.dict(dictKeyName), dsUtils.dict(dictKeyDesc))
                .setGuildOnly(true);
        if (options != null && options.length != 0)
            cmd.addOptions(options);
        return cmd;
    }

    public void stop() {
        jda.shutdown();
        cli.close();
        db.close();
    }

    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(Main.class);
        try {
            new Main(log);
        } catch (Exception e) {
            log.error("Exception during initialization", e);
        }
    }

}