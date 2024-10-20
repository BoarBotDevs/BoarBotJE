package dev.boarbot.jobs;

import dev.boarbot.BoarBotApp;
import dev.boarbot.api.util.Configured;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.BoarUserFactory;
import dev.boarbot.util.boar.BoarUtil;
import dev.boarbot.util.data.BoarDataUtil;
import dev.boarbot.util.data.DataUtil;
import dev.boarbot.util.data.GuildDataUtil;
import dev.boarbot.util.data.UserDataUtil;
import dev.boarbot.util.logging.ExceptionHandler;
import dev.boarbot.util.logging.Log;
import dev.boarbot.util.time.TimeUtil;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.quartz.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class NotificationJob implements Job, Configured {
    @Getter private final static JobDetail job = JobBuilder.newJob(NotificationJob.class).build();
    @Getter private final static Trigger trigger = TriggerBuilder.newTrigger()
        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 ? * *").inTimeZone(TimeZone.getTimeZone("UTC")))
        .build();
    private final static JDA jda = BoarBotApp.getBot().getJDA();
    private final static Map<String, User> notifUsers = new ConcurrentHashMap<>();
    private final static Map<Integer, Integer> dynamicValues = new HashMap<>();
    private final static int streakIndex = 10;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<String> notifUserIDs;

        TextChannel pingChannel = jda.getTextChannelById(CONFIG.getMainConfig().getPingChannel());

        if (pingChannel != null) {
            pingChannel.sendMessage(STRS.getNotificationPingChannel())
                .queue(null, e -> Log.warn(this.getClass(), "Failed to sent legacy notification"));
        }

        try (Connection connection = DataUtil.getConnection()) {
            updateDynamicValues(connection);
            notifUserIDs = UserDataUtil.getNotifUserIDs(connection);
        } catch (SQLException exception) {
            Log.error(NotificationJob.class, "Failed to get relevant notification data", exception);
            return;
        }

        Semaphore semaphore = new Semaphore(2);

        for (String notifUserID : notifUserIDs) {
            try (Connection connection = DataUtil.getConnection()) {
                if (notifUsers.containsKey(notifUserID)) {
                    BoarUser boarUser = BoarUserFactory.getBoarUser(notifUsers.get(notifUserID));
                    String notificationStr = getNotificationStr(
                        connection, boarUser.baseQuery().getNotificationChannel(connection), boarUser
                    );

                    sendNotification(notifUsers.get(boarUser.getUserID()), notificationStr);
                    continue;
                }

                semaphore.acquireUninterruptibly();
                jda.retrieveUserById(notifUserID).queue(
                    user -> {
                        semaphore.release();

                        if (user.getMutualGuilds().isEmpty()) {
                            return;
                        }

                        notifUsers.put(notifUserID, user);

                        try {
                            BoarUser boarUser = BoarUserFactory.getBoarUser(notifUsers.get(notifUserID));
                            String notificationStr = getNotificationStr(
                                connection, boarUser.baseQuery().getNotificationChannel(connection), boarUser
                            );

                            sendNotification(user, notificationStr);
                        } catch (SQLException exception) {
                            Log.error(NotificationJob.class, "Failed to get notification channel", exception);
                        }
                    },
                    e -> {
                        semaphore.release();
                        ExceptionHandler.handle(NotificationJob.class, e);
                    }
                );
            } catch (SQLException exception) {
                Log.error(NotificationJob.class, "Failed to get notification channel", exception);
            } catch (RuntimeException exception) {
                Log.error(NotificationJob.class, "A problem occurred while sending notifications", exception);
            }
        }
    }

    private static void sendNotification(User user, String str) {
        user.openPrivateChannel().queue(
            ch -> ch.sendMessage(str).setSuppressEmbeds(true)
                .queue(null, e -> ExceptionHandler.handle(user, NotificationJob.class, e)),
            e -> ExceptionHandler.handle(user, NotificationJob.class, e)
        );
    }

    private static void updateDynamicValues(Connection connection) throws SQLException {
        dynamicValues.put(3, BoarUtil.getTotalUniques());
        dynamicValues.put(5, UserDataUtil.getTotalUsers(connection));
        dynamicValues.put(9, GuildDataUtil.getTotalGuilds(connection));
        dynamicValues.put(15, BoarDataUtil.getTotalBoars(connection));
    }

    private static String getNotificationStr(
        Connection connection, String channelID, BoarUser boarUser
    ) throws SQLException {
        String notificationEnding = STRS.getNotificationEnding().formatted(channelID);

        if (TimeUtil.isHalloween()) {
            return "## " + STRS.getNotificationHalloween() + notificationEnding;
        }

        if (TimeUtil.isChristmas()) {
            return "## " + STRS.getNotificationChristmas() + notificationEnding;
        }

        int randIndex = (int) (Math.random() * STRS.getNotificationExtras().length);

        for (int key : dynamicValues.keySet()) {
            if (key == randIndex) {
                return "## " + STRS.getNotificationExtras()[randIndex]
                    .formatted(dynamicValues.get(key)) + notificationEnding;
            }
        }

        if (randIndex == streakIndex) {
            int userStreak = boarUser.baseQuery().getStreak(connection);
            return "## " + STRS.getNotificationExtras()[randIndex].formatted(userStreak) + notificationEnding;
        }

        return "## " + STRS.getNotificationExtras()[randIndex] + notificationEnding;
    }

    public static void cacheNotifUsers() {
        try (Connection connection = DataUtil.getConnection()) {
            JDA jda = BoarBotApp.getBot().getJDA();
            List<String> notifUserIDs = UserDataUtil.getNotifUserIDs(connection);

            Log.debug(
                NotificationJob.class,
                "Attempting to cache %,d user(s) for notifications".formatted(notifUserIDs.size())
            );

            for (String notifUserID : notifUserIDs) {
                jda.retrieveUserById(notifUserID).queue(
                    user -> {
                        if (user.getMutualGuilds().isEmpty()) {
                            return;
                        }

                        notifUsers.put(notifUserID, user);
                    },
                    e -> ExceptionHandler.handle(NotificationJob.class, e)
                );
            }
        } catch (SQLException exception) {
            Log.error(NotificationJob.class, "Failed to get relevant notification data", exception);
            System.exit(-1);
        } catch (RuntimeException exception) {
            Log.error(NotificationJob.class, "A problem occurred when caching notification users", exception);
            System.exit(-1);
        }
    }
}
