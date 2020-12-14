package com.elmakers.mine.bukkit.world.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.elmakers.mine.bukkit.magic.MagicController;

public class MagicSpawnHandler {
    public static final String BUILTIN_CLASSPATH = "com.elmakers.mine.bukkit.world.spawn.builtin";

    protected final MagicController controller;
    private final Map<EntityType, List<SpawnRule>> entityTypeMap = new HashMap<>();
    private final List<SpawnRule> globalRules = new ArrayList<>();
    protected String worldName;

    public MagicSpawnHandler(MagicController controller) {
        this.controller = controller;
    }

    @Nullable
    public LivingEntity process(Plugin plugin, LivingEntity entity) {
        List<SpawnRule> entityRules = entityTypeMap.get(entity.getType());
         if (entityRules != null) {
            for (SpawnRule rule : entityRules) {
                LivingEntity result = rule.process(plugin, entity);
                if (result != null) {
                    return result;
                }
            }
        }

        for (SpawnRule rule : globalRules) {
            LivingEntity result = rule.process(plugin, entity);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    protected void addRule(SpawnRule rule) {
        EntityType targetType = rule.getTargetType();
        if (targetType != null) {
            List<SpawnRule> entityRules = entityTypeMap.get(rule.getTargetType());
            if (entityRules == null) {
                entityRules = new ArrayList<>();
                entityTypeMap.put(targetType, entityRules);
            }
            entityRules.add(rule);
        } else {
            globalRules.add(rule);
        }
    }

    public void load(String worldName, ConfigurationSection config) {
        this.worldName = worldName;
        for (String key : config.getKeys(false)) {
            ConfigurationSection handlerConfig = config.getConfigurationSection(key);
            if (handlerConfig == null) {
                controller.getLogger().warning("Was expecting a properties section in world entity_spawn config for key '" + worldName + "', but got: " + config.get(key));
                continue;
            }
            if (!handlerConfig.getBoolean("enabled", true)) {
                continue;
            }

            String className = handlerConfig.getString("class");
            SpawnRule handler = createSpawnRule(className);
            if (handler != null) {
                addRule(handler);
            }
        }
    }

    public void finalizeLoad() {
        Collections.sort(globalRules);
        for (SpawnRule rule : globalRules) {
            rule.finalizeLoad(worldName);
        }
        for (List<SpawnRule> rules : entityTypeMap.values()) {
            Collections.sort(rules);
            for (SpawnRule rule : rules) {
                rule.finalizeLoad(worldName);
            }
        }
    }

    @Nullable
    protected SpawnRule createSpawnRule(String className) {
        if (className == null) return null;

        if (className.indexOf('.') <= 0) {
            className = BUILTIN_CLASSPATH + "." + className;
        }

        Class<?> handlerClass = null;
        try {
            handlerClass = Class.forName(className);
        } catch (Throwable ex) {
            controller.getLogger().warning("Error loading handler: " + className + ", " + ex.getMessage());
            return null;
        }

        Object newObject;
        try {
            newObject = handlerClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            controller.getLogger().warning("Error loading handler: " + className);
            ex.printStackTrace();
            return null;
        }

        if (newObject == null || !(newObject instanceof SpawnRule)) {
            controller.getLogger().warning("Error loading handler: " + className + ", does it extend SpawnRule?");
            return null;
        }

        return (SpawnRule)newObject;
    }

}