package dev.boarbot.util.modal;

import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.components.IndivComponentConfig;
import dev.boarbot.bot.config.modals.ModalConfig;
import dev.boarbot.util.interactive.InteractiveUtil;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.internal.interactions.modal.ModalImpl;

import java.util.ArrayList;
import java.util.List;

public final class ModalUtil {
    public static Modal getModal(ModalConfig modalConfig, Interaction compEvent) {
        return new ModalImpl(
            ModalUtil.makeModalID(modalConfig.getId(), compEvent),
            modalConfig.getTitle(),
            ModalUtil.makeModalComponents(modalConfig.getComponents())
        );
    }

    public static List<LayoutComponent> makeModalComponents(List<IndivComponentConfig> components) {
        List<ItemComponent> componentsMade = InteractiveUtil.makeComponents("", components);

        List<LayoutComponent> layoutComponents = new ArrayList<>();
        for (ItemComponent component : componentsMade) {
            layoutComponents.add(ActionRow.of(component));
        }

        return layoutComponents;
    }

    public static String findDuplicateModalHandler(String userID) {
        for (String key : BoarBotApp.getBot().getModalHandlers().keySet()) {
            boolean isUserModalHandler = key.endsWith(userID);

            if (isUserModalHandler) {
                return key;
            }
        }

        return null;
    }

    public static String makeModalID(String modalID, Interaction interaction) {
        return interaction.getId() + "," + interaction.getUser().getId() + "," + modalID;
    }
}
