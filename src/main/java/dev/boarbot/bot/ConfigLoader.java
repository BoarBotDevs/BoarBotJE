package dev.boarbot.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.*;
import dev.boarbot.bot.config.commands.CommandConfig;
import dev.boarbot.bot.config.components.ComponentConfig;
import dev.boarbot.bot.config.items.BadgeItemConfig;
import dev.boarbot.bot.config.items.BaseItemConfig;
import dev.boarbot.bot.config.items.BoarItemConfig;
import dev.boarbot.bot.config.items.PowerupItemConfig;
import dev.boarbot.bot.config.modals.ModalConfig;
import dev.boarbot.bot.config.prompts.PromptConfig;
import dev.boarbot.bot.config.quests.QuestConfig;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;

@Slf4j
class ConfigLoader {
    private static final BotConfig config = BoarBotApp.getBot().getConfig();
    
    public static void loadConfig() {
        try {
            log.debug("Attempting to load config...");

            String basePath = "config/";

            config.setMainConfig(getFromJson(basePath + "config.json", MainConfig.class));

            config.setStringConfig(getFromJson(basePath + "lang/en_us.json", StringConfig.class));

            config.setColorConfig(getFromJson(
                basePath + "util/colors.json", new TypeToken<Map<String, String>>(){}.getType()
            ));
            config.setNumberConfig(getFromJson(basePath + "util/constants.json", NumberConfig.class));
            config.setPathConfig(getFromJson(basePath + "util/paths.json", PathConfig.class));

            config.setCommandConfig(getFromJson(
                basePath + "discord/commands.json", new TypeToken<Map<String, CommandConfig>>(){}.getType()
            ));
            config.setComponentConfig(getFromJson(basePath + "discord/components.json", ComponentConfig.class));
            config.setModalConfig(getFromJson(
                basePath + "discord/modals.json", new TypeToken<Map<String, ModalConfig>>(){}.getType()
            ));

            config.setPromptConfig(getFromJson(
                basePath + "game/pow_prompts.json", new TypeToken<Map<String, PromptConfig>>(){}.getType()
            ));
            config.setQuestConfig(getFromJson(
                basePath + "game/quests.json", new TypeToken<Map<String, QuestConfig>>(){}.getType()
            ));
            config.setRarityConfigs(getFromJson(
                basePath + "game/rarities.json", new TypeToken<Map<String, RarityConfig>>(){}.getType()
            ));

            config.getItemConfig().setBadges(getFromJson(
                basePath + "items/badges.json", new TypeToken<Map<String, BadgeItemConfig>>(){}.getType()
            ));
            config.getItemConfig().setBoars(getFromJson(
                basePath + "items/boars.json", new TypeToken<Map<String, BoarItemConfig>>(){}.getType()
            ));
            config.getItemConfig().setPowerups(getFromJson(
                basePath + "items/powerups.json", new TypeToken<Map<String, PowerupItemConfig>>(){}.getType()
            ));

            for (BoarItemConfig boar : config.getItemConfig().getBoars().values()) {
                setNames(boar);
            }

            for (PowerupItemConfig powerup : config.getItemConfig().getPowerups().values()) {
                setNames(powerup);
            }

            for (RarityConfig rarityConfig : config.getRarityConfigs().values()) {
                if (rarityConfig.getPluralName() == null) {
                    rarityConfig.setPluralName(rarityConfig.getName() + "s");
                }
            }

            String fontPath = config.getPathConfig().getFontAssets() + config.getPathConfig().getMainFont();

            try {
                InputStream is = BoarBotApp.getResourceStream(fontPath);
                BoarBotApp.getBot().setFont(Font.createFont(Font.TRUETYPE_FONT, is));
            } catch (Exception exception) {
                log.error("There was a problem when creating font from font file", exception);
            }

            fixStrings();

            log.debug("Successfully loaded config");
        } catch (IOException exception) {
            log.error("Unable to find one or more config files", exception);
            System.exit(-1);
        }
    }

    private static void setNames(BaseItemConfig item) {
        if (item.getPluralName() == null) {
            item.setPluralName(item.getName() + "s");
        }

        if (item.getShortName() == null) {
            item.setShortName(item.getName());
        }

        if (item.getShortPluralName() == null) {
            item.setShortPluralName(item.getShortName() + "s");
        }
    }

    private static void fixStrings() {
        StringConfig strs = config.getStringConfig();
        Map<String, PowerupItemConfig> pows = config.getItemConfig().getPowerups();
        Map<String, CommandConfig> cmds = config.getCommandConfig();

        strs.setNoSetup(strs.getNoSetup().formatted(
            cmds.get("manage").getName(), cmds.get("manage").getSubcommands().get("setup").getName()
        ));
        strs.setError(strs.getError().formatted(
            cmds.get("main").getName(), cmds.get("main").getSubcommands().get("report").getName()
        ));

        strs.setSetupFinishedAll(strs.getSetupFinishedAll().formatted(
            cmds.get("main").getName(), cmds.get("main").getSubcommands().get("daily").getName()
        ));
        strs.setSetupInfoResponse1(strs.getSetupInfoResponse1().formatted(
            cmds.get("manage").getName(), cmds.get("manage").getSubcommands().get("setup").getName()
        ));
        strs.setSetupInfoResponse2(strs.getSetupInfoResponse2().formatted(
            cmds.get("manage").getName(), cmds.get("manage").getSubcommands().get("setup").getName()
        ));

        strs.setDailyUsed(strs.getDailyUsed().formatted(
            cmds.get("main").getName(), cmds.get("main").getSubcommands().get("daily").getName(), "%s"
        ));
        strs.setDailyFirstTime(strs.getDailyFirstTime().formatted(
            pows.get("miracle").getPluralName(),
            pows.get("gift").getPluralName(),
            cmds.get("main").getName(),
            cmds.get("main").getSubcommands().get("help").getName()
        ));

        strs.setDailyTitle(strs.getDailyTitle().formatted(strs.getMainItemName()));

        strs.setProfileTotalLabel(strs.getProfileTotalLabel().formatted(strs.getMainItemPluralName()));
        strs.setProfileDailiesLabel(strs.getProfileDailiesLabel().formatted(strs.getMainItemPluralName()));
        strs.setProfileUniquesLabel(strs.getProfileUniquesLabel().formatted(strs.getMainItemPluralName()));
        strs.setProfileStreakLabel(strs.getProfileStreakLabel().formatted(strs.getMainItemName()));
        strs.setProfileNextDailyLabel(strs.getProfileNextDailyLabel().formatted(strs.getMainItemName()));

        strs.setCompFavoriteSuccess(
            strs.getCompFavoriteSuccess().formatted("%s", strs.getMainItemName().toLowerCase())
        );
        strs.setCompUnfavoriteSuccess(
            strs.getCompUnfavoriteSuccess().formatted("%s", strs.getMainItemName().toLowerCase())
        );
        strs.setCompCloneTitle(strs.getCompCloneTitle().formatted(strs.getMainItemName()));
        strs.setCompTransmuteConfirm(strs.getCompTransmuteConfirm().formatted("%s", "%s", strs.getMainItemName()));

        strs.setStatsDailiesLabel(strs.getStatsDailiesLabel().formatted(strs.getMainItemPluralName()));
        strs.setStatsDailiesMissedLabel(strs.getStatsDailiesMissedLabel().formatted(strs.getMainItemPluralName()));
        strs.setStatsLastDailyLabel(strs.getStatsLastDailyLabel().formatted(strs.getMainItemName()));
        strs.setStatsLastBoarLabel(strs.getStatsLastBoarLabel().formatted(strs.getMainItemName()));
        strs.setStatsFavBoarLabel(strs.getStatsFavBoarLabel().formatted(strs.getMainItemName()));
        strs.setStatsUniquesLabel(strs.getStatsUniquesLabel().formatted(strs.getMainItemPluralName()));
        strs.setStatsStreakLabel(strs.getStatsStreakLabel().formatted(strs.getMainItemName()));
        strs.setStatsMiraclesActiveLabel(
            strs.getStatsMiraclesActiveLabel().formatted(pows.get("miracle").getShortPluralName())
        );
        strs.setStatsMiracleRollsLabel(
            strs.getStatsMiracleRollsLabel().formatted(pows.get("miracle").getShortName())
        );

        strs.setNotificationSuccess(strs.getNotificationSuccess().formatted(
            cmds.get("main").getName(), cmds.get("main").getSubcommands().get("daily").getName()
        ));
        strs.setNotificationDailyReady(
            strs.getNotificationDailyReady().formatted(strs.getMainItemName().toLowerCase())
        );
    }

    private static <T> T getFromJson(String path, Class<T> clazz) throws IOException {
        try (InputStream stream = BoarBotApp.getResourceStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Resource not found " + path);
            }

            InputStreamReader streamReader = new InputStreamReader(stream);
            return new Gson().fromJson(streamReader, clazz);
        }
    }

    private static <T> T getFromJson(String path, Type type) throws IOException {
        try (InputStream stream = BoarBotApp.getResourceStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Resource not found " + path);
            }

            InputStreamReader streamReader = new InputStreamReader(stream);
            return new Gson().fromJson(streamReader, type);
        }
    }
}
