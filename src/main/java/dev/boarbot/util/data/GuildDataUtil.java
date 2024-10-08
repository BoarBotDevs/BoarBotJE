package dev.boarbot.util.data;

import dev.boarbot.BoarBotApp;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GuildDataUtil {
    public static List<String> getValidChannelIDs(Connection connection, String guildID) throws SQLException {
        ArrayList<String> channelIDs = new ArrayList<>();

        String query = "SELECT channel_one, channel_two, channel_three FROM guilds WHERE guild_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, guildID);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    if (results.getString("channel_one") != null) {
                        channelIDs.add(results.getString("channel_one"));
                    }

                    if (results.getString("channel_two") != null) {
                        channelIDs.add(results.getString("channel_two"));
                    }

                    if (results.getString("channel_three") != null) {
                        channelIDs.add(results.getString("channel_three"));
                    }
                }
            }
        }

        return channelIDs;
    }

    public static boolean isSkyblockGuild(Connection connection, String guildID) throws SQLException {
        boolean isSkyblock = false;
        String query = """
            SELECT is_skyblock_community
            FROM guilds
            WHERE guild_id = ?
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, guildID);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    isSkyblock = results.getBoolean("is_skyblock_community");
                }
            }
        }

        return isSkyblock;
    }

    public static Map<String, List<TextChannel>> getAllChannels(Connection connection) throws SQLException {
        Map<String, List<TextChannel>> channels = new HashMap<>();

        String query = """
            SELECT guild_id, channel_one, channel_two, channel_three
            FROM guilds;
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String guildID = results.getString("guild_id");
                    channels.put(guildID, getGuildChannels(guildID, results));
                }
            }
        }

        return channels;
    }

    public static Set<Message> getPowerupMessages(Connection connection) throws SQLException {
        Set<Message> messages = new HashSet<>();

        String query = """
            SELECT
                guild_id,
                channel_one,
                channel_two,
                channel_three,
                powerup_message_one,
                powerup_message_two,
                powerup_message_three
            FROM guilds;
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String guildID = results.getString("guild_id");
                    List<TextChannel> guildChannels = getGuildChannels(guildID, results);

                    tryAddPowerupMessage(results.getString("powerup_message_one"), guildChannels, 0, messages);
                    tryAddPowerupMessage(results.getString("powerup_message_two"), guildChannels, 1, messages);
                    tryAddPowerupMessage(results.getString("powerup_message_three"), guildChannels, 2, messages);
                }
            }
        }

        return messages;
    }

    public static void updatePowerupMessages(Connection connection, List<Message> messages) throws SQLException {
        Map<String, String[]> guildMessages = new HashMap<>();
        Map<String, String[]> guildChannels = new HashMap<>();

        String query = """
            SELECT guild_id, channel_one, channel_two, channel_three
            FROM guilds;
        """;

        String update = """
            UPDATE guilds
            SET powerup_message_one = ?, powerup_message_two = ?, powerup_message_three = ?
            WHERE guild_id = ?;
        """;

        try (
            PreparedStatement statement1 = connection.prepareStatement(query);
            PreparedStatement statement2 = connection.prepareStatement(update)
        ) {
            try (ResultSet results = statement1.executeQuery()) {
                while (results.next()) {
                    String guildID = results.getString("guild_id");

                    guildChannels.put(guildID, new String[3]);

                    guildChannels.get(guildID)[0] = results.getString("channel_one");
                    guildChannels.get(guildID)[1] = results.getString("channel_two");
                    guildChannels.get(guildID)[2] = results.getString("channel_three");
                }
            }

            for (Message message : messages) {
                String guildID = message.getGuildId();
                guildMessages.putIfAbsent(guildID, new String[3]);

                if (message.getChannelId().equals(guildChannels.get(guildID)[0])) {
                    guildMessages.get(guildID)[0] = message.getId();
                } else if (message.getChannelId().equals(guildChannels.get(guildID)[1])) {
                    guildMessages.get(guildID)[1] = message.getId();
                } else if (message.getChannelId().equals(guildChannels.get(guildID)[2])) {
                    guildMessages.get(guildID)[2] = message.getId();
                }
            }

            for (String guildID : guildChannels.keySet()) {
                int numMsgs = guildMessages.containsKey(guildID)
                    ? guildMessages.get(guildID).length
                    : 0;

                statement2.setString(1, numMsgs > 0 ? guildMessages.get(guildID)[0] : null);
                statement2.setString(2, numMsgs > 1 ? guildMessages.get(guildID)[1] : null);
                statement2.setString(3, numMsgs > 2 ? guildMessages.get(guildID)[2] : null);
                statement2.setString(4, guildID);
                statement2.addBatch();
            }

            statement2.executeBatch();
        }
    }

    private static List<TextChannel> getGuildChannels(String guildID, ResultSet results) throws SQLException {
        JDA jda = BoarBotApp.getBot().getJDA();
        List<TextChannel> guildChannels = new ArrayList<>();
        Guild guild = jda.getGuildById(guildID);

        tryAddGuildChannel(results.getString("channel_one"), guildChannels, guild);
        tryAddGuildChannel(results.getString("channel_two"), guildChannels, guild);
        tryAddGuildChannel(results.getString("channel_three"), guildChannels, guild);

        return guildChannels;
    }

    private static void tryAddGuildChannel(String channelID, List<TextChannel> guildChannels, Guild guild) {
        TextChannel channel = channelID != null && guild != null ? guild.getTextChannelById(channelID) : null;
        if (channel != null) {
            guildChannels.add(channel);
        }
    }

    private static void tryAddPowerupMessage(
        String messageID, List<TextChannel> guildChannels, int index, Set<Message> messages
    ) {
        try {
            Message message = messageID != null && guildChannels.size() > index
                ? guildChannels.get(index).retrieveMessageById(messageID).complete()
                : null;
            if (message != null) {
                messages.add(message);
            }
        } catch (ErrorResponseException | InsufficientPermissionException ignored) {}
    }

    public static int getTotalGuilds(Connection connection) throws SQLException {
        String query = """
            SELECT COUNT(*)
            FROM guilds;
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getInt(1);
                }
            }
        }

        return 0;
    }

    public static boolean getEventNotify(String guildID, Connection connection) throws SQLException {
        String query = """
            SELECT event_notify_flag
            FROM guilds
            WHERE guild_id = ?;
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, guildID);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getBoolean(1);
                }
            }
        }

        return false;
    }

    public static void setEventNotify(Set<String> guildIDs, Connection connection) throws SQLException {
        String resetQuery = """
            UPDATE guilds
            SET event_notify_flag = false;
        """;

        String updateQuery = """
            UPDATE guilds
            SET event_notify_flag = true
            WHERE guild_id = ?;
        """;

        try (
            PreparedStatement statement1 = connection.prepareStatement(resetQuery);
            PreparedStatement statement2 = connection.prepareStatement(updateQuery)
        ) {
            statement1.executeUpdate();

            for (String guildID : guildIDs) {
                statement2.setString(1, guildID);
                statement2.addBatch();
            }

            statement2.executeBatch();
        }
    }
}
