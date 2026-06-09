package net.leaf.skills;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Selection + buff authority for the advancement-driven skill tree.
 *
 * <p>The leaf-skilltree datapack owns the visible tree and writes the chosen
 * node id into the {@code skill_pick} trigger objective. This controller reads
 * that value, enforces the classless prerequisites/exclusions/hard-gates and
 * the rising XP-level cost, awards the matching advancement, and keeps each
 * owned node's buff reconciled with the player.</p>
 */
public final class SkillController {

    private static final String NODE_CRITERION = "chosen";
    private static final String PICK_OBJECTIVE = "skill_pick";
    private static final String POINTS_OBJECTIVE = "skill_points";
    private static final String NODE_NAMESPACE = "leafskills";

    private final Plugin plugin;
    private final SkillConfig config;
    private final Logger logger;

    public SkillController(Plugin plugin, SkillConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    public Plugin plugin() {
        return plugin;
    }

    /** Runs once per poll on the player's own region/entity thread. */
    public void pollAndReconcile(Player player) {
        processSelection(player);
        reconcile(player);
    }

    private void processSelection(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective pick = board.getObjective(PICK_OBJECTIVE);
        if (pick == null) {
            return; // datapack not loaded yet
        }
        Score score = pick.getScore(player.getName());
        if (!score.isScoreSet() || score.getScore() <= 0) {
            return;
        }
        int requestedId = score.getScore();
        score.setScore(0); // consume the request immediately

        SkillNode node = config.byId(requestedId);
        if (node == null) {
            player.sendMessage("[Skills] No skill has id " + requestedId + ".");
            return;
        }
        attemptSelect(player, node);
    }

    private void attemptSelect(Player player, SkillNode node) {
        Advancement adv = nodeAdvancement(node.key());
        if (adv == null) {
            logger.warning("Advancement leafskills:node/" + node.key()
                    + " not found; is the leaf-skilltree datapack installed?");
            player.sendMessage("[Skills] That skill is not available (datapack missing).");
            return;
        }
        if (player.getAdvancementProgress(adv).isDone()) {
            player.sendMessage("[Skills] You already have " + node.key() + ".");
            return;
        }
        for (String req : node.requires()) {
            if (!owns(player, req)) {
                player.sendMessage("[Skills] " + node.key() + " requires " + req + " first.");
                return;
            }
        }
        for (String exc : node.excludes()) {
            if (owns(player, exc)) {
                player.sendMessage("[Skills] " + node.key() + " cannot be taken alongside " + exc + ".");
                return;
            }
        }
        if (node.hasGate() && !gateComplete(player, node.gateKey())) {
            player.sendMessage("[Skills] " + node.key() + " is locked until you complete " + node.gateKey() + ".");
            return;
        }

        int pointsSpent = pointsSpent(player);
        int cost = config.costFor(pointsSpent);
        if (player.getLevel() < cost) {
            player.sendMessage("[Skills] " + node.key() + " costs " + cost
                    + " levels; you have " + player.getLevel() + ".");
            return;
        }

        player.giveExpLevels(-cost);
        setPoints(player, pointsSpent + 1);
        player.getAdvancementProgress(adv).awardCriteria(NODE_CRITERION);
        applyNode(player, node);
        player.sendMessage("[Skills] Allocated " + node.key() + " for " + cost + " levels.");
    }

    /** Re-applies attribute modifiers for owned nodes and strips them for unowned ones. */
    public void reconcile(Player player) {
        for (SkillNode node : config.nodes().values()) {
            if (owns(player, node.key())) {
                applyNode(player, node);
            } else {
                clearAttribute(player, node);
            }
        }
    }

    /** Removes every managed advancement, buff, and resets the point counter. No XP refund. */
    public void reset(Player player) {
        for (SkillNode node : config.nodes().values()) {
            Advancement adv = nodeAdvancement(node.key());
            if (adv != null) {
                AdvancementProgress progress = player.getAdvancementProgress(adv);
                if (progress.getAwardedCriteria().contains(NODE_CRITERION)) {
                    progress.revokeCriteria(NODE_CRITERION);
                }
            }
            clearAttribute(player, node);
            clearEffect(player, node);
        }
        setPoints(player, 0);
        player.sendMessage("[Skills] Your skill selections were reset (experience is not refunded).");
    }

    // --- helpers ---------------------------------------------------------

    private boolean owns(Player player, String nodeKey) {
        Advancement adv = nodeAdvancement(nodeKey);
        return adv != null && player.getAdvancementProgress(adv).isDone();
    }

    private boolean gateComplete(Player player, String gateKey) {
        NamespacedKey key = NamespacedKey.fromString(gateKey);
        if (key == null) {
            return false;
        }
        Advancement adv = Bukkit.getAdvancement(key);
        return adv != null && player.getAdvancementProgress(adv).isDone();
    }

    private Advancement nodeAdvancement(String nodeKey) {
        NamespacedKey key = NamespacedKey.fromString(NODE_NAMESPACE + ":node/" + nodeKey);
        return key == null ? null : Bukkit.getAdvancement(key);
    }

    private int pointsSpent(Player player) {
        Objective points = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(POINTS_OBJECTIVE);
        if (points == null) {
            return 0;
        }
        Score score = points.getScore(player.getName());
        return score.isScoreSet() ? score.getScore() : 0;
    }

    private void setPoints(Player player, int value) {
        Objective points = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(POINTS_OBJECTIVE);
        if (points != null) {
            points.getScore(player.getName()).setScore(value);
        }
    }

    private NamespacedKey modifierKey(SkillNode node) {
        return new NamespacedKey(plugin, "node_" + node.key());
    }

    private void applyNode(Player player, SkillNode node) {
        if (node.hasAttribute()) {
            Attribute attribute = resolveAttribute(node.attributeKey());
            AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
            if (instance != null && !hasModifier(instance, modifierKey(node))) {
                instance.addModifier(new AttributeModifier(
                        modifierKey(node), node.attributeAmount(), AttributeModifier.Operation.ADD_NUMBER));
            }
        }
        if (node.hasEffect()) {
            PotionEffectType type = resolveEffect(node.effectKey());
            if (type != null) {
                PotionEffect current = player.getPotionEffect(type);
                if (current == null || current.getAmplifier() < node.effectAmplifier()) {
                    player.addPotionEffect(new PotionEffect(
                            type, PotionEffect.INFINITE_DURATION, node.effectAmplifier(), true, false, false));
                }
            }
        }
    }

    private void clearAttribute(Player player, SkillNode node) {
        if (!node.hasAttribute()) {
            return;
        }
        Attribute attribute = resolveAttribute(node.attributeKey());
        AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        NamespacedKey key = modifierKey(node);
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
            if (key.equals(mod.getKey())) {
                toRemove.add(mod);
            }
        }
        for (AttributeModifier mod : toRemove) {
            instance.removeModifier(mod);
        }
    }

    private void clearEffect(Player player, SkillNode node) {
        if (!node.hasEffect()) {
            return;
        }
        PotionEffectType type = resolveEffect(node.effectKey());
        if (type != null) {
            player.removePotionEffect(type);
        }
    }

    private static boolean hasModifier(AttributeInstance instance, NamespacedKey key) {
        for (AttributeModifier mod : instance.getModifiers()) {
            if (key.equals(mod.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static Attribute resolveAttribute(String key) {
        NamespacedKey nk = NamespacedKey.minecraft(key);
        return Registry.ATTRIBUTE.get(nk);
    }

    private static PotionEffectType resolveEffect(String key) {
        NamespacedKey nk = NamespacedKey.minecraft(key);
        return Registry.EFFECT.get(nk);
    }
}
