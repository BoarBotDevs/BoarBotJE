package dev.boarbot.api.bot;

import dev.boarbot.bot.config.BotConfig;
import dev.boarbot.interactives.Interactive;
import dev.boarbot.commands.Subcommand;
import dev.boarbot.modals.ModalHandler;
import net.dv8tion.jda.api.JDA;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public interface Bot {
    void create();
    JDA getJDA();
    BotConfig getConfig();
    void setFont(Font font);
    Font getFont();
    void deployCommands();
    Map<String, byte[]> getByteCacheMap();
    Map<String, BufferedImage> getImageCacheMap();
    Map<String, Constructor<? extends Subcommand>> getSubcommands();
    ConcurrentMap<String, Interactive> getInteractives();
    Map<String, ModalHandler> getModalHandlers();
}