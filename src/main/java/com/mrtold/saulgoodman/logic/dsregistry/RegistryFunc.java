package com.mrtold.saulgoodman.logic.dsregistry;

import com.mrtold.saulgoodman.logic.model.Client;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;

/**
 * @author Mr_Told
 */
@FunctionalInterface
public interface RegistryFunc<T> {

    void process(T t, Map<Integer, Client> clientMap, StringBuilder sb, Guild guild);

}
