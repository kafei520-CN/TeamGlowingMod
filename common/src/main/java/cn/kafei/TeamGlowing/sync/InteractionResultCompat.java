package cn.kafei.TeamGlowing.sync;

import java.lang.reflect.Field;
import net.minecraft.world.InteractionResult;

public final class InteractionResultCompat {
    private static final InteractionResult PASS = readResult("PASS", "field_5811");
    private static final InteractionResult SUCCESS = readResult("SUCCESS", "field_5812");
    private static final InteractionResult CONSUME = readOptionalResult("CONSUME", "field_21466", SUCCESS);
    private static final InteractionResult FAIL = readResult("FAIL", "field_5814");

    private InteractionResultCompat() {
    }

    public static InteractionResult pass() {
        return PASS;
    }

    public static InteractionResult success() {
        return SUCCESS;
    }

    public static InteractionResult consume() {
        return CONSUME;
    }

    public static InteractionResult fail() {
        return FAIL;
    }

    public static boolean isPass(InteractionResult result) {
        return result == null || result == PASS;
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

    private static InteractionResult readOptionalResult(String namedField, String intermediaryField, InteractionResult fallback) {
        InteractionResult result = tryReadResult(namedField);
        if (result != null) {
            return result;
        }
        result = tryReadResult(intermediaryField);
        return result == null ? fallback : result;
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
