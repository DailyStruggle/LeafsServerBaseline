package net.leaf.artifacts;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

/**
 * Declarative description of a single attribute bonus an artifact grants while
 * active. Reconciled into a tagged {@link AttributeModifier}.
 *
 * @param attribute the attribute to modify
 * @param amount    the modifier amount
 * @param operation how the amount is applied
 */
public record AttributeSpec(Attribute attribute, double amount, AttributeModifier.Operation operation) {

    public static AttributeSpec add(Attribute attribute, double amount) {
        return new AttributeSpec(attribute, amount, AttributeModifier.Operation.ADD_NUMBER);
    }

    public static AttributeSpec scale(Attribute attribute, double amount) {
        return new AttributeSpec(attribute, amount, AttributeModifier.Operation.ADD_SCALAR);
    }
}
