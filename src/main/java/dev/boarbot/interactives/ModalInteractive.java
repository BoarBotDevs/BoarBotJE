package dev.boarbot.interactives;

import dev.boarbot.modals.ModalHandler;
import dev.boarbot.util.time.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.Interaction;

@Setter
@Getter
public abstract class ModalInteractive extends Interactive {
    protected ModalHandler modalHandler = null;

    protected ModalInteractive(Interaction interaction) {
        super(interaction);
    }

    public void attemptExecute(
        GenericComponentInteractionCreateEvent compEvent, ModalInteractionEvent modalEvent, long startTime
    ) {
        if (startTime < this.lastEndTime) {
            return;
        }

        this.curStopTime = TimeUtil.getCurMilli() + this.config.getNumberConfig().getInteractiveIdle();

        if (compEvent != null) {
            this.execute(compEvent);
        } else {
            this.modalExecute(modalEvent);
        }

        this.lastEndTime = TimeUtil.getCurMilli();
    }

    public abstract void modalExecute(ModalInteractionEvent modalEvent);
}