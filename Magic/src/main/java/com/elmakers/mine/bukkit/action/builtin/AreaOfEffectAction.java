package com.elmakers.mine.bukkit.action.builtin;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.elmakers.mine.bukkit.utility.random.RandomUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import com.elmakers.mine.bukkit.action.CompoundEntityAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import com.elmakers.mine.bukkit.utility.Target;

public class AreaOfEffectAction extends CompoundEntityAction
{
    protected double radius;
    protected double yRadius;
    protected double minRadius;
    protected int targetCount;
    protected boolean targetSource;
    protected boolean ignoreModified;
    protected boolean randomChoose;

    @Override
    public void reset(CastContext context) {
        super.reset(context);
        createActionContext(context, context.getTargetEntity(), context.getTargetLocation());
    }

    @Override
    public void prepare(CastContext context, ConfigurationSection parameters)
    {
        radius = parameters.getDouble("radius", 8);
        minRadius = parameters.getDouble("min_radius", 0);
        yRadius = parameters.getDouble("y_radius", radius);
        targetCount = parameters.getInt("target_count", -1);
        targetSource = parameters.getBoolean("target_source", true);
        ignoreModified = parameters.getBoolean("ignore_modified", false);
        randomChoose = parameters.getBoolean("random_choose", false);

        Mage mage = context.getMage();
        radius = (int)(mage.getRadiusMultiplier() * radius);

        super.prepare(context, parameters);
    }

    @Override
    public void addEntities(CastContext context, List<WeakReference<Entity>> entities)
    {
        Set<UUID> ignore = null;
        UndoList undoList = context.getUndoList();
        if (ignoreModified && undoList != null) {
            ignore = new HashSet<>();
            for (Entity entity : undoList.getAllEntities()) {
                ignore.add(entity.getUniqueId());
            }
        }
        context.addWork((int)Math.ceil(radius) + 10);
        Mage mage = context.getMage();
        Location sourceLocation = context.getTargetLocation();
        if (mage.getDebugLevel() > 8)
        {
            mage.sendDebugMessage(ChatColor.GREEN + "AOE Targeting from " + ChatColor.GRAY + sourceLocation.getBlockX()
                    + ChatColor.DARK_GRAY + ","  + ChatColor.GRAY + sourceLocation.getBlockY()
                    + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + sourceLocation.getBlockZ()
                    + ChatColor.DARK_GREEN + " with radius of " + ChatColor.GREEN + radius
                    + ChatColor.GRAY + " self? " + ChatColor.DARK_GRAY + context.getTargetsCaster(), 14
            );
        }
        Collection<Entity> candidates = CompatibilityLib.getCompatibilityUtils().getNearbyEntities(sourceLocation, radius, yRadius, radius);
        if (minRadius > 0) {
            double minRadiusSquared = minRadius * minRadius;
            Collection<Entity> filtered = new ArrayList<>();
            for (Entity entity : candidates) {
                if (entity.getLocation().distanceSquared(sourceLocation) >= minRadiusSquared) {
                    filtered.add(entity);
                }
            }
            candidates = filtered;
        }
        Entity targetEntity = context.getTargetEntity();
        if (targetCount > 0)
        {
            if (randomChoose) {
                List<Entity> candidatesList = new ArrayList<>();
                for (Entity entity : candidates) {
                    boolean canTarget = entity != targetEntity || targetSource;
                    if (ignore != null && ignore.contains(entity.getUniqueId())) {
                        mage.sendDebugMessage(ChatColor.DARK_RED + "Ignoring Modified Target " + ChatColor.GREEN + entity.getType(), 16);
                        continue;
                    }
                    if (canTarget && context.canTarget(entity)) {
                        candidatesList.add(entity);
                        mage.sendDebugMessage(ChatColor.DARK_GREEN + "Target " + ChatColor.GREEN + entity.getType(), 12);
                    } else if (mage.getDebugLevel() > 7) {
                        mage.sendDebugMessage(ChatColor.DARK_RED + "Skipped Target " + ChatColor.GREEN + entity.getType(), 16);
                    }
                }
                Collections.shuffle(candidatesList);
                for (int i = 0; i < targetCount && i < candidatesList.size(); i++) {
                    entities.add(new WeakReference<>(candidatesList.get(i)));
                }
                return;
            }
            List<Target> targets = new ArrayList<>();
            for (Entity entity : candidates)
            {
                boolean canTarget = true;
                if (entity == targetEntity && !targetSource) canTarget = false;
                if (ignore != null && ignore.contains(entity.getUniqueId())) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Ignoring Modified Target " + ChatColor.GREEN + entity.getType(), 16);
                    continue;
                }
                if (canTarget && context.canTarget(entity))
                {
                    Target target = new Target(sourceLocation, entity, (int)radius, 0);
                    targets.add(target);
                    mage.sendDebugMessage(ChatColor.DARK_GREEN + "Target " + ChatColor.GREEN + entity.getType() + ChatColor.DARK_GREEN + ": " + ChatColor.YELLOW + target.getScore(), 12);
                }
                else if (mage.getDebugLevel() > 7)
                {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Skipped Target " + ChatColor.GREEN + entity.getType(), 16);
                }
            }
            Collections.sort(targets);
            for (int i = 0; i < targetCount && i < targets.size(); i++)
            {
                Target target = targets.get(i);
                entities.add(new WeakReference<>(target.getEntity()));
            }
        }
        else
        {
            for (Entity entity : candidates)
            {
                boolean canTarget = true;
                if (entity == targetEntity && !targetSource) canTarget = false;
                if (ignore != null && ignore.contains(entity.getUniqueId())) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Ignoring Modified Target " + ChatColor.GREEN + entity.getType(), 16);
                    continue;
                }
                if (canTarget && context.canTarget(entity))
                {
                    entities.add(new WeakReference<>(entity));
                    mage.sendDebugMessage(ChatColor.DARK_GREEN + "Target " + ChatColor.GREEN + entity.getType(), 12);
                }
                else if (mage.getDebugLevel() > 7)
                {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Skipped Target " + ChatColor.GREEN + entity.getType(), 16);
                }
            }
        }
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters) {
        super.getParameterNames(spell, parameters);
        parameters.add("radius");
        parameters.add("target_count");
        parameters.add("target_source");
        parameters.add("random_choose");
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples) {
        if (parameterKey.equals("target_count") || parameterKey.equals("radius")) {
            examples.addAll(Arrays.asList(BaseSpell.EXAMPLE_SIZES));
        } else {
            super.getParameterOptions(spell, parameterKey, examples);
        }
    }

    @Override
    public boolean requiresTarget() {
        return true;
    }
}
