package com.mrtold.saulgoodman;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Collections;
import java.util.Objects;

class Scratch {
    public static void main(String[] args) throws InterruptedException {
        String token = "MTE4MjY0NzM0MDM3NzU5MTgwOA.GLHksb.R7viQUsePGSmmepZNhagvUuKgvnfD9Ln6tir9c";
        JDA jda = JDABuilder.createLight(token, Collections.emptyList())
                .setActivity(Activity.watching("за правосудием на Sunrise"))
                .build();

        Thread.sleep(2000);
        Objects.requireNonNull(Objects.requireNonNull(jda.getGuildById(1177287408467845132L))
                        .getTextChannelById(1239032836334424147L))
                .getPermissionContainer().getManager().putMemberPermissionOverride(673856157055713300L,
                Permission.getRaw(Permission.VIEW_CHANNEL), 0).queue();

        jda.shutdown();
    }
}