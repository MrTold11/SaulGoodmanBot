package com.mrtold.pacificuni.discord;

import com.mrtold.pacificuni.Main;
import com.mrtold.pacificuni.database.DatabaseConnector;
import com.mrtold.pacificuni.model.Course;
import com.mrtold.pacificuni.model.Faculty;
import com.mrtold.pacificuni.model.Student;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Mr_Told
 */
public class CommandAdapter extends ListenerAdapter {

    final DiscordUtils dsUtils;
    final DatabaseConnector db;
    final Logger log;
    final ZoneId timezone = ZoneId.of("Europe/Moscow");
    final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");

    public CommandAdapter(DiscordUtils dsUtils, DatabaseConnector db) {
        this.dsUtils = dsUtils;
        this.db = db;
        this.log = LoggerFactory.getLogger(CommandAdapter.class);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(dsUtils.dict("cmd.err.no_guild")).setEphemeral(true).queue();
            return;
        }

        if (!dsUtils.hasManagerPermission(event.getMember())) {
            event.reply(dsUtils.dict("cmd.err.no_perm")).setEphemeral(true).queue();
            return;
        }

        Faculty targetFaculty;
        Member targetMember;
        Course course;
        Integer passport;
        String studentName;
        boolean expel = false;

        switch (event.getName().toLowerCase(Locale.ROOT)) {
            case Main.CMD_ENROLL:
                event.deferReply().queue();
                targetFaculty = Faculty.byAlias(Objects.requireNonNull(event.getSubcommandName()));
                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                course = Course.getById(Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.course"), OptionMapping::getAsInt)));
                passport = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt));
                studentName = dsUtils.getMemberNick(targetMember);

                db.enrollStudent(passport, targetMember.getIdLong(), studentName, course.getId());

                event.getHook().sendMessage(MessageCreateData.fromEmbeds(
                        prepareEmbedBuilder(8453888, dsUtils.dict("embed.title.enroll"))
                                .setDescription(String.format(Locale.getDefault(),
                                        """
                                                Тег студента: %s
                                                Имя студента: %s
                                                Номер паспорта: %d
                                                Факультет: **%s**
                                                Направление: %s
                                                Автор: %s
                                                """,
                                        targetMember.getAsMention(),
                                        dsUtils.getEmbedData(studentName),
                                        passport,
                                        targetFaculty.getName(),
                                        course.getName(),
                                        event.getUser().getAsMention())
                                )
                                .build()

                )).queue();

                guild.addRoleToMember(targetMember, guild.getRoleById(targetFaculty.getMemberRoleId())).queue();
                guild.addRoleToMember(targetMember, guild.getRoleById(course.getRoleId())).queue();
                guild.addRoleToMember(targetMember, dsUtils.getStudentRole()).queue();
                break;
            case Main.CMD_EXPEL:
                expel = true;
            case Main.CMD_GRADUATE:
                if (!dsUtils.hasTeacherPermission(event.getMember())) {
                    event.reply(dsUtils.dict("cmd.err.no_perm"))
                            .setEphemeral(true).queue();
                    return;
                }

                event.deferReply().queue();
                targetFaculty = Faculty.byAlias(Objects.requireNonNull(event.getSubcommandName()));
                targetMember = Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.user"), OptionMapping::getAsMember));
                course = Course.getById(Objects.requireNonNull(event.getOption(dsUtils.dict("cmd.arg.course"), OptionMapping::getAsInt)));
                passport = event.getOption(dsUtils.dict("cmd.arg.pass"), OptionMapping::getAsInt);
                studentName = dsUtils.getMemberNick(targetMember);

                Student student = db.expelStudent(targetMember.getIdLong(), course.getId());

                if (passport == null && student != null)
                    passport = student.getPassport();

                int color = expel ? 14357564 : 15132410;
                String title = dsUtils.dict(expel ? "embed.title.expel" : "embed.title.graduate");
                String reason = expel ?
                        String.format(Locale.getDefault(), "\nПричина: **%s**",
                                event.getOption(
                                        dsUtils.dict("cmd.arg.expel_reason"),
                                        dsUtils.dict("str.not_spec"),
                                        OptionMapping::getAsString)
                        ) : "";

                event.getHook().sendMessage(MessageCreateData.fromEmbeds(
                        prepareEmbedBuilder(color, title)
                                .setDescription(String.format(Locale.getDefault(),
                                        """
                                                Тег студента: %s
                                                Имя студента: %s
                                                Номер паспорта: %s
                                                Факультет: **%s**
                                                Направление: %s
                                                Автор: %s%s
                                                """,
                                        targetMember.getAsMention(),
                                        dsUtils.getEmbedData(studentName),
                                        dsUtils.getEmbedData(passport),
                                        targetFaculty.getName(),
                                        course.getName(),
                                        event.getUser().getAsMention(),
                                        reason)
                                )
                                .build()

                )).queue();

                boolean removeFacultyRole = !dsUtils.hasTeacherPermission(targetMember);
                if (student == null || student.getCourses().isEmpty()) {
                    guild.removeRoleFromMember(targetMember, dsUtils.getStudentRole()).queue();
                } else if (removeFacultyRole) {
                    for (int cid : student.getCourses()) {
                        if (Course.getById(cid).getFaculty() == targetFaculty)
                            removeFacultyRole = false;
                    }
                }

                if (removeFacultyRole)
                    guild.removeRoleFromMember(targetMember, guild.getRoleById(targetFaculty.getMemberRoleId())).queue();
                guild.removeRoleFromMember(targetMember, guild.getRoleById(course.getRoleId())).queue();

                if (!expel)
                    guild.addRoleToMember(targetMember, dsUtils.getGraduateRole()).queue();
                break;
            default:
                log.warn("Unknown command {} used by {}", event.getName(), event.getUser().getName());
                break;
        }
    }

    private EmbedBuilder prepareEmbedBuilder(int color, String title) {
        return new EmbedBuilder()
                .setAuthor(dsUtils.dict("embed.author.name"),
                        dsUtils.dict("embed.author.url"),
                        dsUtils.dict("embed.author.icon"))
                .setTitle(title, dsUtils.dict("embed.title.url"))
                .setColor(color)
                .setFooter(timestampFormat.format(LocalDateTime.now(timezone)),
                        dsUtils.dict("embed.footer.icon"));
    }

}
