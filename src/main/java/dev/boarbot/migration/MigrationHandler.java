package dev.boarbot.migration;

import dev.boarbot.api.util.Configured;
import dev.boarbot.bot.ConfigUpdater;
import dev.boarbot.migration.globaldata.UserMarketData;
import dev.boarbot.migration.userdata.OldUserData;
import dev.boarbot.util.logging.Log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MigrationHandler implements Configured {
    public static void doMigration() {
        try {
            ConfigUpdater.setMaintenance(true);
        } catch (IOException exception) {
            Log.error(MigrationHandler.class, "Failed to enable maintenance mode", exception);
            System.exit(-1);
        }

        Map<String, UserMarketData> userMarketData = MigrationReader.getUserMarketData();
        List<OldUserData> oldUsers = MigrationReader.getOldUsers(userMarketData);

        MigrationWriter.writeGuilds(MigrationReader.getOldGuilds());
        MigrationWriter.writeUsers(oldUsers);
        MigrationWriter.writeBoars(MigrationReader.getBoars(oldUsers, userMarketData));
    }
}
