package net.leaf.skills;

import java.util.List;

/**
 * Immutable definition of a single passive skill node, mirrored from the
 * {@code leafskills:node/<key>} advancement in the leaf-skilltree datapack.
 *
 * <p>Validation is classless: archetypes emerge from {@link #requires} chains
 * and {@link #excludes} forks rather than any declared class.</p>
 *
 * @param key             datapack node key (advancement is {@code leafskills:node/<key>})
 * @param id              integer the player writes via {@code /trigger skill_pick set <id>}
 * @param requires        node keys that must ALL already be owned
 * @param excludes        node keys that must NONE be owned
 * @param gateKey         optional advancement key that must be completed first (hard gate), or null
 * @param attributeKey    optional vanilla attribute id (e.g. {@code max_health}), or null
 * @param attributeAmount flat ADD_NUMBER amount for the attribute modifier
 * @param effectKey       optional potion effect id (e.g. {@code resistance}) applied while owned, or null
 * @param effectAmplifier amplifier for the infinite potion effect
 */
public record SkillNode(
        String key,
        int id,
        List<String> requires,
        List<String> excludes,
        String gateKey,
        String attributeKey,
        double attributeAmount,
        String effectKey,
        int effectAmplifier) {

    public boolean hasAttribute() {
        return attributeKey != null && !attributeKey.isBlank();
    }

    public boolean hasEffect() {
        return effectKey != null && !effectKey.isBlank();
    }

    public boolean hasGate() {
        return gateKey != null && !gateKey.isBlank();
    }
}
