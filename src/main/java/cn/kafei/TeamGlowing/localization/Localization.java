package cn.kafei.TeamGlowing.localization;

import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class Localization
{
    private final Map<String, Properties> bundles = new HashMap<>();

    public Localization()
    {
        this.load("zh_cn");
        this.load("en_us");
    }

    private void load(String locale)
    {
        String path = "assets/" + TeamGlowingConstants.MODID + "/lang/" + locale + ".lang";
        Properties properties = new Properties();
        try (java.io.InputStream inputStream = Localization.class.getClassLoader().getResourceAsStream(path))
        {
            if (inputStream == null)
            {
                this.bundles.put(locale, properties);
                return;
            }

            try (java.io.Reader reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))
            {
                properties.load(reader);
                this.bundles.put(locale, properties);
            }
        }
        catch (Exception exception)
        {
            this.bundles.put(locale, properties);
        }
    }

    public String translate(ICommandSender sender, String key, Object... args)
    {
        String locale = this.resolveLocale(sender);
        String pattern = this.lookup(locale, key);
        return MessageFormat.format(pattern, args);
    }

    private String lookup(String locale, String key)
    {
        Properties localized = this.bundles.get(locale);
        if (localized != null && localized.containsKey(key))
        {
            return localized.getProperty(key);
        }

        Properties fallback = this.bundles.get("en_us");
        if (fallback != null && fallback.containsKey(key))
        {
            return fallback.getProperty(key);
        }

        return key;
    }

    private String resolveLocale(ICommandSender sender)
    {
        return sender instanceof EntityPlayerMP ? "zh_cn" : "en_us";
    }
}
