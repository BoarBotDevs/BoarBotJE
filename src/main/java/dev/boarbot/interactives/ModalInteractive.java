package dev.boarbot.interactives;

import dev.boarbot.modals.ModalHandler;
import dev.boarbot.util.logging.Log;
import dev.boarbot.util.time.TimeUtil;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.Interaction;

@Setter
@Getter
public abstract class ModalInteractive extends UserInteractive {
    protected ModalHandler modalHandler = null;

    protected ModalInteractive(Interaction interaction) {
        super(interaction);
    }

    protected ModalInteractive(Interaction interaction, boolean isMsg, long waitTime, long hardTime) {
        super(interaction, isMsg, waitTime, hardTime);
    }

    public synchronized void attemptExecute(
        GenericComponentInteractionCreateEvent compEvent, ModalInteractionEvent modalEvent, long startTime
    ) {
        if (startTime < this.lastEndTime) {
            Log.debug(
                compEvent != null ? compEvent.getUser() : modalEvent.getUser(), this.getClass(), "Interacted too fast!"
            );
            return;
        }

        this.curStopTime = TimeUtil.getCurMilli() + NUMS.getInteractiveIdle();

        if (compEvent != null) {
            this.execute(compEvent);
        } else if (modalEvent != null) {
            this.modalExecute(modalEvent);
        }

        this.lastEndTime = TimeUtil.getCurMilli() + NUMS.getInteractiveIdle();
    }

    public abstract void modalExecute(ModalInteractionEvent modalEvent);
}
