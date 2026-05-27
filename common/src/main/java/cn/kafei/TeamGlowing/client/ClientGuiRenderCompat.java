package cn.kafei.TeamGlowing.client;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ClientGuiRenderCompat {
    private static final Class<?> RENDER_PIPELINE_CLASS = findClass("com.mojang.blaze3d.pipeline.RenderPipeline");
    private static final Object GUI_TEXTURED_PIPELINE = findGuiTexturedPipeline();
    private static final Method MODERN_BLIT_TEXTURE = findMethod(
        GuiGraphics.class,
        "blit",
        RENDER_PIPELINE_CLASS,
        ResourceLocation.class,
        int.class,
        int.class,
        float.class,
        float.class,
        int.class,
        int.class,
        int.class,
        int.class,
        int.class
    );
    private static final Method MODERN_BLIT_SPRITE = findMethod(
        GuiGraphics.class,
        "blitSprite",
        RENDER_PIPELINE_CLASS,
        TextureAtlasSprite.class,
        int.class,
        int.class,
        int.class,
        int.class,
        int.class
    );
    private static final Method LEGACY_SET_COLOR = findMethod(
        GuiGraphics.class,
        "setColor",
        float.class,
        float.class,
        float.class,
        float.class
    );
    private static final Method LEGACY_BLIT_TEXTURE = findMethod(
        GuiGraphics.class,
        "blit",
        ResourceLocation.class,
        int.class,
        int.class,
        float.class,
        float.class,
        int.class,
        int.class,
        int.class,
        int.class
    );
    private static final Method LEGACY_BLIT_SPRITE = findMethod(
        GuiGraphics.class,
        "blit",
        int.class,
        int.class,
        int.class,
        int.class,
        int.class,
        TextureAtlasSprite.class,
        float.class,
        float.class,
        float.class,
        float.class
    );
    private static final Method DRAW_COMPONENT = findMethodByNames(
        GuiGraphics.class,
        new Class<?>[] {Font.class, Component.class, int.class, int.class, int.class},
        "drawString",
        "method_27535",
        "method_27534"
    );
    private static final Method DRAW_COMPONENT_SHADOW = findMethodByNames(
        GuiGraphics.class,
        new Class<?>[] {Font.class, Component.class, int.class, int.class, int.class, boolean.class},
        "drawString",
        "method_51439"
    );
    private static final Method DRAW_STRING = findMethodByNames(
        GuiGraphics.class,
        new Class<?>[] {Font.class, String.class, int.class, int.class, int.class},
        "drawString",
        "method_25303",
        "method_25300"
    );
    private static final Method DRAW_STRING_SHADOW = findMethodByNames(
        GuiGraphics.class,
        new Class<?>[] {Font.class, String.class, int.class, int.class, int.class, boolean.class},
        "drawString",
        "method_51433"
    );
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final AtomicBoolean TEXT_WARNED = new AtomicBoolean();

    private ClientGuiRenderCompat() {
    }

    public static void blitTexture(
        GuiGraphics context,
        ResourceLocation texture,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        int argb
    ) {
        try {
            if (GUI_TEXTURED_PIPELINE != null && MODERN_BLIT_TEXTURE != null) {
                MODERN_BLIT_TEXTURE.invoke(context, GUI_TEXTURED_PIPELINE, texture, x, y, u, v, width, height, textureWidth, textureHeight, argb);
                return;
            }
            if (LEGACY_SET_COLOR != null && LEGACY_BLIT_TEXTURE != null) {
                Color color = Color.fromArgb(argb);
                LEGACY_SET_COLOR.invoke(context, color.red(), color.green(), color.blue(), color.alpha());
                try {
                    LEGACY_BLIT_TEXTURE.invoke(context, texture, x, y, u, v, width, height, textureWidth, textureHeight);
                } finally {
                    LEGACY_SET_COLOR.invoke(context, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                return;
            }
        } catch (ReflectiveOperationException exception) {
            warnOnce("HUD 纹理绘制兼容调用失败", exception);
            return;
        }
        warnOnce("当前 Minecraft 版本缺少可用的 HUD 纹理绘制方法", null);
    }

    public static void blitSprite(GuiGraphics context, TextureAtlasSprite sprite, int x, int y, int width, int height, int argb) {
        try {
            if (GUI_TEXTURED_PIPELINE != null && MODERN_BLIT_SPRITE != null) {
                MODERN_BLIT_SPRITE.invoke(context, GUI_TEXTURED_PIPELINE, sprite, x, y, width, height, argb);
                return;
            }
            if (LEGACY_BLIT_SPRITE != null) {
                Color color = Color.fromArgb(argb);
                LEGACY_BLIT_SPRITE.invoke(context, x, y, 0, width, height, sprite, color.red(), color.green(), color.blue(), color.alpha());
                return;
            }
        } catch (ReflectiveOperationException exception) {
            warnOnce("HUD 图集精灵绘制兼容调用失败", exception);
            return;
        }
        warnOnce("当前 Minecraft 版本缺少可用的 HUD 图集精灵绘制方法", null);
    }

    public static void drawString(GuiGraphics context, Font font, Component text, int x, int y, int color) {
        drawString(context, font, text, x, y, color, false);
    }

    public static void drawString(GuiGraphics context, Font font, Component text, int x, int y, int color, boolean shadow) {
        try {
            Method method = shadow
                ? (DRAW_COMPONENT_SHADOW != null ? DRAW_COMPONENT_SHADOW : DRAW_COMPONENT)
                : (DRAW_COMPONENT != null ? DRAW_COMPONENT : DRAW_COMPONENT_SHADOW);
            if (method != null) {
                if (method.getParameterCount() == 6) {
                    method.invoke(context, font, text, x, y, color, shadow);
                } else {
                    method.invoke(context, font, text, x, y, color);
                }
                return;
            }
        } catch (ReflectiveOperationException exception) {
            warnTextOnce("GUI 文字绘制兼容调用失败", exception);
            return;
        }
        warnTextOnce("当前 Minecraft 版本缺少可用的 GUI 文字绘制方法", null);
    }

    public static void drawString(GuiGraphics context, Font font, String text, int x, int y, int color) {
        drawString(context, font, text, x, y, color, false);
    }

    public static void drawString(GuiGraphics context, Font font, String text, int x, int y, int color, boolean shadow) {
        try {
            Method method = shadow
                ? (DRAW_STRING_SHADOW != null ? DRAW_STRING_SHADOW : DRAW_STRING)
                : (DRAW_STRING != null ? DRAW_STRING : DRAW_STRING_SHADOW);
            if (method != null) {
                if (method.getParameterCount() == 6) {
                    method.invoke(context, font, text, x, y, color, shadow);
                } else {
                    method.invoke(context, font, text, x, y, color);
                }
                return;
            }
        } catch (ReflectiveOperationException exception) {
            warnTextOnce("GUI 文字绘制兼容调用失败", exception);
            return;
        }
        warnTextOnce("当前 Minecraft 版本缺少可用的 GUI 文字绘制方法", null);
    }

    private static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static Object findGuiTexturedPipeline() {
        try {
            Class<?> pipelines = Class.forName("net.minecraft.client.renderer.RenderPipelines");
            Field field = pipelines.getField("GUI_TEXTURED");
            return field.get(null);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Method findMethodByNames(Class<?> owner, Class<?>[] parameterTypes, String... names) {
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType == null) {
                return null;
            }
        }
        for (String name : names) {
            try {
                return owner.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (WARNED.compareAndSet(false, true)) {
            if (throwable == null) {
                TeamGlowingConstants.LOGGER.warn(message);
            } else {
                TeamGlowingConstants.LOGGER.warn(message, throwable);
            }
        }
    }

    private static void warnTextOnce(String message, Throwable throwable) {
        if (TEXT_WARNED.compareAndSet(false, true)) {
            if (throwable == null) {
                TeamGlowingConstants.LOGGER.warn(message);
            } else {
                TeamGlowingConstants.LOGGER.warn(message, throwable);
            }
        }
    }

    private record Color(float red, float green, float blue, float alpha) {
        private static Color fromArgb(int argb) {
            return new Color(
                ((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F
            );
        }
    }
}
