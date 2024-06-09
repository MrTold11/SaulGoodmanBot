package com.mrtold.saulgoodman;

import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.discord.CommandAdapter;
import com.mrtold.saulgoodman.discord.DiscordUtils;
import com.mrtold.saulgoodman.image.DocUtils;
import com.mrtold.saulgoodman.imgur.ImgurUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Mr_Told
 */
public class Main {

    public static final String CMD_SIGN = "sign";
    public static final String CMD_TERMINATE = "terminate";
    public static final String CMD_INVITE = "invite";
    public static final String CMD_NAME = "name";
    public static final String CMD_CLAIM = "claim";
    public static final String CMD_RECEIPT = "bill";
    public static final String CMD_ATTACH = "attach";
    public static final String CMD_ATTACH_PASS = "паспорт";
    public static final String CMD_ATTACH_LICENSE = "лицензия";
    public static final String CMD_ATTACH_SIGNATURE = "подпись";
    public static final String CMD_ATTACH_PHONE = "телефон";
    public static final String CMD_ATTACH_NAME = "имя";
    public static int DEFAULT_AGREEMENT_BILL_AMOUNT = 10000;

    final CLI cli;
    final ImgurUtils imgurUtils;
    final DiscordUtils dsUtils;
    final DatabaseConnector db;
    final JDA jda;

    public Main(Logger log) throws IOException, InterruptedException {
        DocUtils.init();

        Properties secrets = new Properties();
        secrets.load(new FileInputStream("scrt.properties"));
        String token = secrets.getProperty("ds_token");
        String dbPwd = secrets.getProperty("db_pass");
        String imgurClientId = secrets.getProperty("imgur_client_id");
        String dbHost = secrets.getProperty("db_host", "localhost");
        int dbPort = Integer.parseInt(secrets.getProperty("db_port", "5432"));
        String dbName = secrets.getProperty("db_name", "mba_lg");
        String dbUser = secrets.getProperty("db_user", "saulgoodman");

        if (token == null || token.isBlank()) {
            log.error("No token (ds_token) provided in scrt.properties! Exiting.");
            System.exit(1);
        }
        if (dbPwd == null || dbPwd.isBlank()) {
            log.error("No database password (db_pass) provided in scrt.properties! Exiting.");
            System.exit(1);
        }
        if (imgurClientId == null || imgurClientId.isBlank()) {
            log.error("No imgur client ID (imgur_client_id) provided in scrt.properties! Exiting.");
            System.exit(1);
        }

        imgurUtils = new ImgurUtils(imgurClientId);

        db = new DatabaseConnector(dbHost, dbPort, dbName, dbUser, dbPwd);

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
                .setRequestChannelId(1238835145159606303L)
                .setRequestsChannelId(1238834941039480852L)
                .setGuildId(1177287408467845132L)
                .addDict(
                        "cmd.name.sign", CMD_SIGN,
                        "cmd.desc.sign", "Подписать соглашение с клиентом",
                        "cmd.name.terminate", CMD_TERMINATE,
                        "cmd.desc.terminate", "Расторгнуть соглашение с клиентом",
                        "cmd.name.invite", CMD_INVITE,
                        "cmd.desc.invite", "Принять нового адвоката",
                        "cmd.name.name", CMD_NAME,
                        "cmd.desc.name", "Изменить имя, фамилию клиента",
                        "cmd.name.claim", CMD_CLAIM,
                        "cmd.desc.claim", "Сформировать шаблон иска",
                        "cmd.name.receipt", CMD_RECEIPT,
                        "cmd.desc.receipt", "Выставить чек на оплату",
                        "cmd.name.attach", CMD_ATTACH,
                        "cmd.desc.attach", "Прикрепить документ адвоката",
                        "cmd.arg.num", "номер",
                        "cmd.arg.user", "пользователь",
                        "cmd.arg.name", "имя-фамилия",
                        "cmd.arg.pass", "паспорт",
                        "cmd.arg.signature", "подпись",
                        "cmd.arg.term_reason", "причина",
                        "cmd.arg.doc_img", "документ",
                        "cmd.arg.phone", "телефон",
                        "cmd.arg.amount", "сумма",
                        "arg.desc.num", "Укажите порядковый номер соглашения",
                        "arg.desc.user", "Выберите пользователя или укажите его ID",
                        "arg.desc.name", "Укажите имя фамилию",
                        "arg.desc.pass", "Укажите номер паспорта",
                        "arg.desc.signature", "Картинка подписи",
                        "arg.desc.term_reason", "Укажите причину расторжения",
                        "arg.desc.pass_img", "Картинка паспорта",
                        "arg.desc.doc_img", "Картинка документа",
                        "arg.desc.phone", "Номер телефона",
                        "arg.desc.amount", "Сумма к оплате",
                        "sub.desc.pass", "Приложить картинку паспорта",
                        "sub.desc.license", "Приложить картинку лицензии адвоката",
                        "str.not_spec", "Не указано",
                        "str.data_upd_ok", "Данные успешно обновлены!",
                        "str.receipt_paid", "Счет #%d помечен как оплаченный!",
                        "cmd.err.no_perm", "У вас нет разрешения на использование этой команды.",
                        "cmd.err.no_guild", "Команду можно использовать только на сервере.",
                        "cmd.err.client_nf", "Клиент не найден.",
                        "cmd.err.client_name_ok", "Имя клиента уже соответствует заданному.",
                        "cmd.err.already_has_agreement", "С клиентом уже заключено соглашение.",
                        "cmd.err.amount_low", "Счет не может быть меньше 1$.",
                        "cmd.err.receipt_nf", "Счет не найден.",
                        "embed.author.name", "GTA 5 RP | Секретарь Джейсона Мориса",
                        "embed.author.url", null,
                        "embed.author.icon", "https://i.imgur.com/IKvjkjp.png",
                        "embed.title.url", "https://gta5rp.com/",
                        "embed.footer.icon", "https://i.imgur.com/4cJIDXV.png",
                        "embed.title.sign", "Подписание договора",
                        "embed.title.invite", "Добавление адвоката",
                        "embed.title.terminate", "Расторжение договора",
                        "embed.title.registry", "Реестр клиентов",
                        "embed.title.receipt_registry", "Реестр выставленных счетов",
                        "embed.title.receipt", "Выставлен счет",
                        "embed.title.agreement_request", "Заявка нового клиента",
                        "embed.button.payed", "Оплачено");

        jda.addEventListener(new CommandAdapter(imgurUtils, dsUtils, db));

        OptionData userOpt = new OptionData(OptionType.USER, dsUtils.dict("cmd.arg.user"),
                dsUtils.dict("arg.desc.user"), true);
        OptionData userOptNotReq = new OptionData(OptionType.USER, dsUtils.dict("cmd.arg.user"),
                dsUtils.dict("arg.desc.user"), false);
        OptionData nameOpt = new OptionData(OptionType.STRING, dsUtils.dict("cmd.arg.name"),
                dsUtils.dict("arg.desc.name"), true);
        OptionData passOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("arg.desc.pass"), true);
        OptionData phoneOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.phone"),
                dsUtils.dict("arg.desc.phone"), true);
        OptionData numOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.num"),
                dsUtils.dict("arg.desc.num"), true);
        OptionData signImgOpt = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.signature"),
                dsUtils.dict("arg.desc.signature"), true);
        OptionData docImgOpt = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.doc_img"),
                dsUtils.dict("arg.desc.doc_img"), true);
        OptionData passImgOpt = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("arg.desc.pass_img"), true);
        OptionData reasonOpt = new OptionData(OptionType.STRING, dsUtils.dict("cmd.arg.term_reason"),
                dsUtils.dict("arg.desc.term_reason"), true);
        OptionData passOptNotReq = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("arg.desc.pass"), false);
        OptionData signOptNotReq = new OptionData(OptionType.ATTACHMENT, dsUtils.dict("cmd.arg.signature"),
                dsUtils.dict("arg.desc.signature"), false);
        OptionData amountOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.amount"),
                dsUtils.dict("arg.desc.amount"), true);

        jda.updateCommands().addCommands(
                generateMemberCommand("cmd.name.sign", "cmd.desc.sign",
                        numOpt, userOpt, nameOpt, passOpt, signImgOpt), //todo signature not required
                generateMemberCommand("cmd.name.invite", "cmd.desc.invite",
                        userOpt, nameOpt, passOpt, signImgOpt),
                generateMemberCommand("cmd.name.terminate", "cmd.desc.terminate",
                        reasonOpt, userOptNotReq, passOptNotReq),
                generateMemberCommand("cmd.name.name", "cmd.desc.name",
                        passOpt, nameOpt),
                generateMemberCommand("cmd.name.receipt", "cmd.desc.receipt",
                        userOpt, amountOpt, passOptNotReq),
                //generateMemberCommand("cmd.name.claim","cmd.desc.claim",
                //        passImgOpt, phoneOpt),
                Commands.slash(dsUtils.dict("cmd.name.attach"), dsUtils.dict("cmd.desc.attach"))
                        .setGuildOnly(true)
                        .addSubcommands(
                                //new SubcommandData(CMD_ATTACH_PASS,
                                //        dsUtils.dict("arg.desc.pass_img")).addOptions(docImgOpt),
                                //new SubcommandData(CMD_ATTACH_LICENSE,
                                //        dsUtils.dict("sub.desc.license")).addOptions(docImgOpt),
                                new SubcommandData(CMD_ATTACH_SIGNATURE,
                                        dsUtils.dict("arg.desc.signature")).addOptions(docImgOpt),
                                new SubcommandData(CMD_ATTACH_PHONE,
                                        dsUtils.dict("arg.desc.phone")).addOptions(phoneOpt),
                                new SubcommandData(CMD_ATTACH_NAME,
                                        dsUtils.dict("arg.desc.name")).addOptions(nameOpt))
        ).complete();

        cli = new CLI(this);
        cli.start();

        initRequestMessage();
    }

    private void initRequestMessage() throws InterruptedException {
        jda.awaitReady();
        TextChannel requestChannel = Objects.requireNonNull(jda.getGuildById(dsUtils.getGuildId()))
                .getTextChannelById(dsUtils.getRequestChannelId());
        if (requestChannel != null) {
            for (Message m : requestChannel.getHistory().retrievePast(5).complete()) {
                if (m.getAuthor().getIdLong() == jda.getSelfUser().getIdLong()) {
                    return;
                }
            }

            requestChannel.sendMessage(" Если вам необходима юридическая помощь, " +
                            "или вы хотите заключить соглашение с Адвокатским бюро **MBA Legal Group**, " +
                            "просто нажмите на кнопку ниже 👇 ")
                    .setActionRow(Button.primary("agreement_request", "Подать заявку"))
                    .complete();

        }
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