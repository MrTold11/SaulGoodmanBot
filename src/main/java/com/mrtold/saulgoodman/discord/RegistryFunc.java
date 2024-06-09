package com.mrtold.saulgoodman.discord;

import com.mrtold.saulgoodman.model.Client;

import java.util.Map;

/**
 * @author Mr_Told
 */
@FunctionalInterface
public interface RegistryFunc<T> {

    void process(T t, Map<Integer, Client> clientMap, StringBuilder sb);

}
