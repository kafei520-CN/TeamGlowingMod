package cn.kafei.TeamGlowing.fabric;

import java.lang.reflect.Field;
import net.minecraft.world.InteractionResult;

final class FabricInteractionResultCompat {
    private static final InteractionResult PASS = readResult("PASS", "field_5811");

    private FabricInteractionResultCompat() {
    }

    static InteractionResult pass() {
        return PASS;
    }

    private static InteractionResult readResult(String namedField, String intermediaryField) {
        InteractionResult result = tryReadResult(namedField);
        if (result != null) {
            return result;
        }
        result = tryReadResult(intermediaryField);
        if (result != null) {
            return result;
        }
        throw new IllegalStateException("Missing Minecraft interaction result field: " + namedField);
    }

    private static InteractionResult tryReadResult(String fieldName) {
        try {
            Field field = InteractionResult.class.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof InteractionResult result) {
                return result;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
