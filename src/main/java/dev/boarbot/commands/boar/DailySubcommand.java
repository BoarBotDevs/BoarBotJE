package dev.boarbot.commands.boar;

import dev.boarbot.bot.config.StringConfig;
import dev.boarbot.commands.Subcommand;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.BoarUserFactory;
import dev.boarbot.entities.boaruser.Synchronizable;
import dev.boarbot.interactives.Interactive;
import dev.boarbot.interactives.InteractiveFactory;
import dev.boarbot.interactives.boar.daily.DailyNotifyInteractive;
import dev.boarbot.util.boar.BoarObtainType;
import dev.boarbot.util.boar.BoarUtil;
import dev.boarbot.util.data.DataUtil;
import dev.boarbot.util.generators.EmbedImageGenerator;
import dev.boarbot.util.generators.ItemImageGenerator;
import dev.boarbot.util.generators.ItemImageGrouper;
import dev.boarbot.util.time.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DailySubcommand extends Subcommand implements Synchronizable {
    private List<String> boarIDs = new ArrayList<>();
    private final List<Integer> bucksGotten = new ArrayList<>();
    private final List<Integer> boarEditions = new ArrayList<>();

    private boolean canDaily = true;
    private boolean notificationsOn = false;
    private boolean isFirstDaily = false;
    private boolean hasDonePowerup = false;

    public DailySubcommand(SlashCommandInteractionEvent event) {
        super(event);
    }

    @Override
    public void execute() throws InterruptedException {
        if (!this.canInteract()) {
            return;
        }

        try {
            BoarUser boarUser = BoarUserFactory.getBoarUser(this.user);
            boarUser.passSynchronizedAction(this);
            boarUser.decRefs();
        } catch (SQLException exception) {
            log.error("Failed to get boar user", exception);
            return;
        }

        if (!this.canDaily) {
            this.interaction.deferReply().setEphemeral(true).queue();

            String dailyResetDistance = TimeUtil.getTimeDistance(TimeUtil.getNextDailyResetMilli(), false);
            dailyResetDistance = dailyResetDistance.substring(dailyResetDistance.indexOf(' ')+1);

            String replyStr = this.config.getStringConfig().getDailyUsed().formatted(dailyResetDistance);

            MessageEditBuilder editedMsg = new MessageEditBuilder();

            if (!this.notificationsOn) {
                Interactive interactive = InteractiveFactory.constructInteractive(
                    this.event, DailyNotifyInteractive.class
                );
                editedMsg.setComponents(interactive.getCurComponents());

                replyStr += " " + this.config.getStringConfig().getDailyUsedNotify();
            }

            try {
                EmbedImageGenerator embedGen = new EmbedImageGenerator(replyStr);
                editedMsg.setFiles(embedGen.generate().getFileUpload());

                this.interaction.getHook().editOriginal(editedMsg.build()).queue();
            } catch (IOException exception) {
                log.error("Failed to generate next daily reset image.", exception);
            }

            return;
        }

        if (!this.boarIDs.isEmpty()) {
            this.sendResponse();
        }
    }

    @Override
    public void doSynchronizedAction(BoarUser boarUser) {
        try (Connection connection = DataUtil.getConnection()) {
            if (!boarUser.canUseDaily(connection) && !this.config.getMainConfig().isUnlimitedBoars()) {
                this.canDaily = false;
                this.notificationsOn = boarUser.getNotificationStatus(connection);
                return;
            }

            if (!this.hasDonePowerup && this.event.getOption("powerup") != null) {
                this.hasDonePowerup = true;
                this.sendPowResponse();
                return;
            }

            if (!this.hasDonePowerup) {
                this.interaction.deferReply().complete();
            }

            this.isFirstDaily = boarUser.isFirstDaily();

            long blessings = boarUser.getBlessings(connection);
            this.boarIDs = BoarUtil.getRandBoarIDs(blessings, this.interaction.getGuild().getId(), connection);

            boarUser.addBoars(this.boarIDs, connection, BoarObtainType.DAILY, this.bucksGotten, this.boarEditions);
            boarUser.useActiveMiracles(connection);
        } catch (SQLException exception) {
            log.error("Failed to add boar to database for user (%s)!".formatted(this.user.getName()), exception);
        }
    }

    private void sendResponse() {
        List<ItemImageGenerator> itemGens = getItemImageGenerators();

        if (itemGens.size() > 1) {
            Interactive interactive = InteractiveFactory.constructDailyInteractive(
                this.event, itemGens, this.boarIDs, this.boarEditions
            );
            interactive.execute(null);
        } else {
            try (FileUpload imageToSend = ItemImageGrouper.groupItems(itemGens, 0)) {
                MessageEditBuilder editedMsg = new MessageEditBuilder()
                    .setFiles(imageToSend)
                    .setComponents();

                this.interaction.getHook().editOriginal(editedMsg.build()).complete();
            } catch (Exception exception) {
                log.error("Failed to send daily boar response!", exception);
            }
        }

        if (this.isFirstDaily) {
            try {
                EmbedImageGenerator embedGen = new EmbedImageGenerator(this.config.getStringConfig().getDailyFirstTime());
                this.interaction.getHook().sendFiles(embedGen.generate().getFileUpload()).setEphemeral(true).complete();
            } catch (IOException exception) {
                log.error("Failed to generate first daily reward image.", exception);
            }
        }
    }

    private List<ItemImageGenerator> getItemImageGenerators() {
        StringConfig strConfig = this.config.getStringConfig();

        List<ItemImageGenerator> itemGens = new ArrayList<>();

        for (int i=0; i<this.boarIDs.size(); i++) {
            String title = strConfig.getDailyTitle();

            if (this.boarIDs.get(i).equals(strConfig.getFirstBoarID())) {
                title = strConfig.getFirstTitle();
            }

            ItemImageGenerator boarItemGen = new ItemImageGenerator(
                this.event.getUser(), title, this.boarIDs.get(i), false, null, this.bucksGotten.get(i)
            );

            itemGens.add(boarItemGen);
        }
        return itemGens;
    }

    private void sendPowResponse() {
        this.interaction.deferReply().complete();

        try {
            EmbedImageGenerator embedGen = new EmbedImageGenerator(this.config.getStringConfig().getDailyPow());

            Interactive interactive = InteractiveFactory.constructDailyPowerupInteractive(this.event, this);

            MessageEditBuilder editedMsg = new MessageEditBuilder()
                .setFiles(embedGen.generate().getFileUpload())
                .setComponents(interactive.getCurComponents());

            if (interactive.isStopped()) {
                return;
            }

            this.interaction.getHook().editOriginal(editedMsg.build()).complete();
        } catch (IOException exception) {
            log.error("Failed to generate powerup use image.", exception);
        }
    }
}
