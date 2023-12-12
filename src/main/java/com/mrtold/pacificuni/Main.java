package com.mrtold.pacificuni;

import com.mrtold.pacificuni.database.DatabaseConnector;
import com.mrtold.pacificuni.discord.CommandAdapter;
import com.mrtold.pacificuni.discord.DiscordUtils;
import com.mrtold.pacificuni.model.Faculty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Mr_Told
 */
public class Main {

    public static final String CMD_ENROLL = "enroll";
    public static final String CMD_GRADUATE = "graduate";
    public static final String CMD_EXPEL = "expel";

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
                .setActivity(Activity.watching("на Sunrise"))
                .build();

        dsUtils = new DiscordUtils(jda)
                .setManagerRoleIds(1182417414135222353L, 1177294186878357575L,
                        1182418198847561809L, 1182357071182692422L)
                .setTeacherRoleIds(1177294186878357575L,
                        1182418198847561809L, 1182357071182692422L)
                .setStudentRoleId(1177290807976394813L)
                .setGraduateRoleId(1182433678484316200L)
                .addDict(
                        "cmd.name.enroll", CMD_ENROLL,
                        "cmd.desc.enroll", "Зачислить студента на курс",
                        "cmd.name.graduate", CMD_GRADUATE,
                        "cmd.desc.graduate", "Выпустить студента (успешное окончание обучения)",
                        "cmd.name.expel", CMD_EXPEL,
                        "cmd.desc.expel", "Отчислить студента (неуспешное окончание обучения)",
                        "cmd.arg.user", "пользователь",
                        "cmd.arg.pass", "паспорт",
                        "cmd.arg.course", "курс",
                        "cmd.arg.expel_reason", "причина",
                        "cmd.desc.user", "Выберите пользователя или укажите его ID",
                        "cmd.desc.pass", "Укажите номер паспорта студента",
                        "cmd.desc.course", "Укажите направление зачисления",
                        "cmd.desc.expel_reason", "Укажите причину отчисления",
                        "str.not_spec", "Не указано",
                        "cmd.err.no_perm", "У вас нет разрешения на использование этой команды.",
                        "cmd.err.no_guild", "Команду можно использовать только на сервере",
                        "embed.author.name", "GTA 5 RP | Pacific University",
                        "embed.author.url", null,
                        "embed.author.icon", "https://i.imgur.com/rbfWXcu.png",
                        "embed.title.url", "https://gta5rp.com/",
                        "embed.footer.icon", "https://i.imgur.com/4cJIDXV.png",
                        "embed.title.enroll", "Зачисление студента",
                        "embed.title.graduate", "Выпуск студента",
                        "embed.title.expel", "Отчисление студента");

        jda.addEventListener(new CommandAdapter(dsUtils, db));

        OptionData userOpt = new OptionData(OptionType.USER, dsUtils.dict("cmd.arg.user"),
                dsUtils.dict("cmd.desc.user"), true);
        OptionData passOpt = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("cmd.desc.pass"), true);
        OptionData reasonOpt = new OptionData(OptionType.STRING, dsUtils.dict("cmd.arg.expel_reason"),
                dsUtils.dict("cmd.desc.expel_reason"), true);
        OptionData passOptNotReq = new OptionData(OptionType.INTEGER, dsUtils.dict("cmd.arg.pass"),
                dsUtils.dict("cmd.desc.pass"), false);

        jda.updateCommands().addCommands(
                generateMemberCommand("cmd.name.enroll", "cmd.desc.enroll",
                        generateMemberSubcommands(userOpt, passOpt)),
                generateMemberCommand("cmd.name.graduate", "cmd.desc.graduate",
                        generateMemberSubcommands(userOpt, passOptNotReq)),
                generateMemberCommand("cmd.name.expel", "cmd.desc.expel",
                        generateMemberSubcommands(userOpt, reasonOpt, passOptNotReq))
        ).queue();

        cli = new CLI(this);
        cli.start();
    }

    private SlashCommandData generateMemberCommand(String dictKeyName, String dictKeyDesc, SubcommandData... subcommands) {
        return Commands.slash(dsUtils.dict(dictKeyName), dsUtils.dict(dictKeyDesc))
                .setGuildOnly(true)
                .addSubcommands(subcommands);
    }

    private SubcommandData[] generateMemberSubcommands(OptionData... options) {
        SubcommandData[] sd = new SubcommandData[Faculty.values().length];
        for (int i = 0; i < Faculty.values().length; i++) {
            sd[i] = generateMemberSubcommand(Faculty.values()[i], options);
        }
        return sd;
    }

    private SubcommandData generateMemberSubcommand(Faculty faculty, OptionData... options) {
        SubcommandData sd =  new SubcommandData(faculty.getAlias(), faculty.getDesc()).addOptions(
                new OptionData(OptionType.STRING,
                        dsUtils.dict("cmd.arg.course"), dsUtils.dict("cmd.desc.course"), true)
                        .addChoices(Arrays.stream(faculty.getCourses())
                                .map(course -> new Command.Choice(course.getName(), course.getId()))
                                .collect(Collectors.toSet()))
        );
        if (options != null && options.length > 0)
            sd.addOptions(options);
        return sd;
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