package dev.boarbot.commands.boardev;

import dev.boarbot.commands.Subcommand;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.BoarUserFactory;
import dev.boarbot.util.data.DataUtil;
import dev.boarbot.util.generators.EmbedImageGenerator;
import dev.boarbot.util.interaction.SpecialReply;
import dev.boarbot.util.logging.ExceptionHandler;
import dev.boarbot.util.logging.Log;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

public class BanSubcommand extends Subcommand {
    public BanSubcommand(SlashCommandInteractionEvent event) {
        super(event);
    }

    @Override
    public void execute() {
        if (!Arrays.asList(CONFIG.getMainConfig().getDevs()).contains(this.user.getId())) {
            try {
                FileUpload fileUpload = new EmbedImageGenerator(STRS.getNoPermission(), COLORS.get("error")).generate()
                    .getFileUpload();
                this.interaction.replyFiles(fileUpload).setEphemeral(true)
                    .queue(null, e -> ExceptionHandler.replyHandle(this.interaction, this, e));
            } catch (IOException exception) {
                SpecialReply.sendErrorMessage(this.interaction, this);
                Log.error(this.user, this.getClass(), "Failed to generate no permission message", exception);
            }

            return;
        }

        User userInput = Objects.requireNonNull(this.event.getOption("user")).getAsUser();
        int duration = this.event.getOption("duration") != null
            ? Objects.requireNonNull(this.event.getOption("duration")).getAsInt()
            : 0;
        String banStr = duration > 0
            ? "Successfully banned %s for %,d hour(s)".formatted(userInput.getName(), duration)
            : "Successfully unbanned %s".formatted(userInput.getName());

        try (Connection connection = DataUtil.getConnection()) {
            BoarUser boarUser = BoarUserFactory.getBoarUser(userInput);
            boarUser.baseQuery().setBanDuration(connection, duration);
            this.interaction.reply(banStr).setEphemeral(true).
                queue(null, e -> ExceptionHandler.replyHandle(this.interaction, this, e));
        } catch (SQLException exception) {
            SpecialReply.sendErrorMessage(this.interaction, this);
            Log.error(this.user, this.getClass(), "Failed to ban user", exception);
        }
    }
}
