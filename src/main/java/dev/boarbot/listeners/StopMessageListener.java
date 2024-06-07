package dev.boarbot.listeners;

import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.BotConfig;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.BoarUserFactory;
import dev.boarbot.util.data.DataUtil;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
public class StopMessageListener extends ListenerAdapter implements Runnable {
    private final BotConfig config = BoarBotApp.getBot().getConfig();

    private MessageReceivedEvent event = null;

    public StopMessageListener() {
        super();
    }

    public StopMessageListener(MessageReceivedEvent event) {
        this.event = event;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        new Thread(new StopMessageListener(event)).start();
    }

    @Override
    public void run() {
        boolean fromDM = this.event.getMessage().isFromType(ChannelType.PRIVATE);
        boolean fromAuthor = this.event.getMessage().getAuthor().isBot();
        boolean ignoreMsg = !fromDM || fromAuthor;

        if (ignoreMsg) {
            return;
        }

        BoarUser boarUser = BoarUserFactory.getBoarUser(this.event.getAuthor());
        boolean notificationsOn = false;

        try (Connection connection = DataUtil.getConnection()) {
            notificationsOn = boarUser.getNotificationStatus(connection);
        } catch (SQLException exception) {
            log.error("An error occurred while getting notification status.", exception);
        }

        if (!notificationsOn) {
            return;
        }

        String[] words = this.event.getMessage().getContentDisplay().split(" ");

        for (String word : words) {
            if (word.trim().equalsIgnoreCase("stop")) {
                try (Connection connection = DataUtil.getConnection()) {
                    boarUser.disableNotifications(connection);
                    this.event.getMessage().reply(this.config.getStringConfig().getNotificationDisabledStr()).queue();
                } catch (SQLException exception) {
                    log.error("An error occurred while disabling notifications.", exception);
                }

                break;
            }
        }
    }
}
