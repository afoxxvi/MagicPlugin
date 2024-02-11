package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.CompoundEntityAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.spell.BaseSpell;
import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import com.elmakers.mine.bukkit.utility.Target;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.*;

public class LineOfEffectAction extends CompoundEntityAction {
    protected double radius;
    protected double rangeFront;
    protected double rangeBack;
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
    public void prepare(CastContext context, ConfigurationSection parameters) {
        radius = parameters.getDouble("radius", 8);
        rangeFront = parameters.getDouble("range", 0);
        rangeFront = parameters.getDouble("range_front", rangeFront);
        rangeBack = parameters.getDouble("range_back", 0);
        targetCount = parameters.getInt("target_count", -1);
        targetSource = parameters.getBoolean("target_source", true);
        ignoreModified = parameters.getBoolean("ignore_modified", false);
        randomChoose = parameters.getBoolean("random_choose", false);

        Mage mage = context.getMage();
        radius = (int) (mage.getRadiusMultiplier() * radius);

        super.prepare(context, parameters);
    }

    @Override
    public void addEntities(CastContext context, List<WeakReference<Entity>> entities) {
        Set<UUID> ignore = null;
        UndoList undoList = context.getUndoList();
        if (ignoreModified && undoList != null) {
            ignore = new HashSet<>();
            for (Entity entity : undoList.getAllEntities()) {
                ignore.add(entity.getUniqueId());
            }
        }
        context.addWork((int) Math.ceil(radius) + 10);
        Mage mage = context.getMage();
        Location sourceLocation = context.getTargetLocation();
        if (sourceLocation == null) {
            return;
        }
        if (mage.getDebugLevel() > 8) {
            mage.sendDebugMessage(ChatColor.GREEN + "LineOfEffect Targeting from " + ChatColor.GRAY + sourceLocation.getBlockX()
                    + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + sourceLocation.getBlockY()
                    + ChatColor.DARK_GRAY + "," + ChatColor.GRAY + sourceLocation.getBlockZ()
                    + ChatColor.DARK_GREEN + " with radius of " + ChatColor.GREEN + radius + ","
                    + ChatColor.DARK_GREEN + " rangeFront of " + ChatColor.GREEN + rangeFront + ","
                    + ChatColor.DARK_GREEN + " rangeBack of " + ChatColor.GREEN + rangeBack + ","
                    + ChatColor.GRAY + " self? " + ChatColor.DARK_GRAY + context.getTargetsCaster(), 14
            );
        }
        double queryRange = Math.sqrt(radius * radius + Math.max(rangeFront, rangeBack) * Math.max(rangeFront, rangeBack));
        Collection<Entity> candidates = CompatibilityLib.getCompatibilityUtils().getNearbyEntities(sourceLocation, queryRange, queryRange, queryRange);
        Collection<Entity> filtered = new ArrayList<>();
        for (Entity entity : candidates) {
            Vector targetOffset = entity.getLocation().toVector().subtract(sourceLocation.toVector());
            Vector sourceDirection = sourceLocation.getDirection();
            double cosine = targetOffset.clone().normalize().dot(sourceDirection);
            double sine = Math.sqrt(1 - cosine * cosine);
            double disTangential = targetOffset.length() * sine;
            double disRadial = targetOffset.length() * cosine;
            if (disTangential <= radius && disRadial <= rangeFront && disRadial >= -rangeBack) {
                filtered.add(entity);
            }
        }
        candidates = filtered;
        Entity targetEntity = context.getTargetEntity();
        if (targetCount > 0) {
            if (randomChoose) {
                List<Entity> candidatesList = new ArrayList<>(candidates);
                Collections.shuffle(candidatesList);
                for (int i = 0; i < targetCount && i < candidatesList.size(); i++) {
                    entities.add(new WeakReference<>(candidatesList.get(i)));
                }
                return;
            }
            List<Target> targets = new ArrayList<>();
            for (Entity entity : candidates) {
                boolean canTarget = entity != targetEntity || targetSource;
                if (ignore != null && ignore.contains(entity.getUniqueId())) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Ignoring Modified Target " + ChatColor.GREEN + entity.getType(), 16);
                    continue;
                }
                if (canTarget && context.canTarget(entity)) {
                    Target target = new Target(sourceLocation, entity, (int) radius, 0);
                    targets.add(target);
                    mage.sendDebugMessage(ChatColor.DARK_GREEN + "Target " + ChatColor.GREEN + entity.getType() + ChatColor.DARK_GREEN + ": " + ChatColor.YELLOW + target.getScore(), 12);
                } else if (mage.getDebugLevel() > 7) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Skipped Target " + ChatColor.GREEN + entity.getType(), 16);
                }
            }
            Collections.sort(targets);
            for (int i = 0; i < targetCount && i < targets.size(); i++) {
                Target target = targets.get(i);
                entities.add(new WeakReference<>(target.getEntity()));
            }
        } else {
            for (Entity entity : candidates) {
                boolean canTarget = entity != targetEntity || targetSource;
                if (ignore != null && ignore.contains(entity.getUniqueId())) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Ignoring Modified Target " + ChatColor.GREEN + entity.getType(), 16);
                    continue;
                }
                if (canTarget && context.canTarget(entity)) {
                    entities.add(new WeakReference<>(entity));
                    mage.sendDebugMessage(ChatColor.DARK_GREEN + "Target " + ChatColor.GREEN + entity.getType(), 12);
                } else if (mage.getDebugLevel() > 7) {
                    mage.sendDebugMessage(ChatColor.DARK_RED + "Skipped Target " + ChatColor.GREEN + entity.getType(), 16);
                }
            }
        }
    }

    @Override
    public void getParameterNames(Spell spell, Collection<String> parameters) {
        super.getParameterNames(spell, parameters);
        parameters.add("radius");
        parameters.add("range");
        parameters.add("range_front");
        parameters.add("range_back");
        parameters.add("target_count");
        parameters.add("target_source");
        parameters.add("random_choose");
    }

    @Override
    public void getParameterOptions(Spell spell, String parameterKey, Collection<String> examples) {
        if (parameterKey.equals("target_count") || parameterKey.equals("radius") || parameterKey.equals("range") || parameterKey.equals("range_front") || parameterKey.equals("range_back")) {
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

