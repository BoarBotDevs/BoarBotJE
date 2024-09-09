package dev.boarbot.util.generators.megamenu;

import dev.boarbot.bot.config.quests.IndivQuestConfig;
import dev.boarbot.bot.config.quests.QuestConfig;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.QuestData;
import dev.boarbot.util.graphics.Align;
import dev.boarbot.util.graphics.GraphicsUtil;
import dev.boarbot.util.graphics.TextDrawer;
import dev.boarbot.util.graphics.TextUtil;
import dev.boarbot.util.time.TimeUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class QuestsImageGenerator extends MegaMenuGenerator {
    private static final int[] ORIGIN = {0, 0};
    private static final int QUEST_WIDTH = 815;
    private static final int QUEST_X = 57;
    private static final int QUEST_START_Y = 305;
    private static final int QUEST_VALUE_Y_OFFSET = 78;
    private static final int QUEST_Y_SPACING = 162;
    private static final int BUCKS_X = 1042;
    private static final int BUCKS_START_Y = 356;
    private static final int POW_TEXT_X = 931;
    private static final int POW_TEXT_START_Y = 352;
    private static final int POW_X = 927;
    private static final int POW_START_Y = 266;
    private static final int[] POW_SIZE = {142, 142};
    private static final int RIGHT_X = 1502;
    private static final int RIGHT_START_Y = 505;
    private static final int RIGHT_VALUE_Y_OFFSET = 78;
    private static final int RIGHT_Y_SPACING = 198;

    private final QuestData questData;
    private final List<String> questIDs;

    private boolean allQuestsDone = true;

    public QuestsImageGenerator(
        int page,
        BoarUser boarUser,
        List<String> badgeIDs,
        String firstJoinedDate,
        QuestData questData,
        List<String> questIDs
    ) {
        super(page, boarUser, badgeIDs, firstJoinedDate);
        this.questData = questData;
        this.questIDs = questIDs;
    }

    @Override
    public MegaMenuGenerator generate() throws IOException, URISyntaxException {
        String underlayPath = this.pathConfig.getMegaMenuAssets() + this.pathConfig.getQuestUnderlay();

        this.generatedImage = new BufferedImage(IMAGE_SIZE[0], IMAGE_SIZE[1], BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = generatedImage.createGraphics();

        GraphicsUtil.drawImage(g2d, underlayPath, ORIGIN, IMAGE_SIZE);

        this.textDrawer = new TextDrawer(
            g2d, "", ORIGIN, Align.LEFT, this.colorConfig.get("font"), this.nums.getFontMedium(), QUEST_WIDTH
        );

        for (int i=0; i<this.questIDs.size(); i++) {
            this.drawQuest(g2d, i);
        }

        int[] resetLabelPos = {RIGHT_X, RIGHT_START_Y};
        String resetStr = TimeUtil.getTimeDistance(TimeUtil.getQuestResetMilli(), false);
        resetStr = Character.toUpperCase(resetStr.charAt(0)) + resetStr.substring(1);
        int[] resetPos = {RIGHT_X, resetLabelPos[1] + RIGHT_VALUE_Y_OFFSET};

        int[] completeLabelPos = {RIGHT_X, resetLabelPos[1] + RIGHT_Y_SPACING};
        String completeStr = "%,d".formatted(this.questData.questsCompleted());
        int[] completePos = {RIGHT_X, completeLabelPos[1] + RIGHT_VALUE_Y_OFFSET};

        int[] fullCompleteLabelPos = {RIGHT_X, completeLabelPos[1] + RIGHT_Y_SPACING};
        String fullCompleteStr = "%,d".formatted(this.questData.perfectWeeks());
        int[] fullCompletePos = {RIGHT_X, fullCompleteLabelPos[1] + RIGHT_VALUE_Y_OFFSET};

        int[] bonusLabelPos = {RIGHT_X, fullCompleteLabelPos[1] + RIGHT_Y_SPACING};

        String bonusStr = this.strConfig.getQuestNoBonusLabel();
        if (this.allQuestsDone && this.questData.fullClaimed()) {
            bonusStr = this.strConfig.getQuestClaimedBonusLabel();
        } else if (this.allQuestsDone) {
            bonusStr = this.strConfig.getQuestUnclaimedBonusLabel();
        }

        int[] bonusPos = {RIGHT_X, bonusLabelPos[1] + RIGHT_VALUE_Y_OFFSET};

        this.textDrawer.setAlign(Align.CENTER);

        TextUtil.drawLabel(this.textDrawer, this.strConfig.getQuestResetLabel(), resetLabelPos);
        TextUtil.drawValue(this.textDrawer, resetStr, resetPos);

        TextUtil.drawLabel(this.textDrawer, this.strConfig.getStatsQuestsCompletedLabel(), completeLabelPos);
        TextUtil.drawValue(this.textDrawer, completeStr, completePos);

        TextUtil.drawLabel(this.textDrawer, this.strConfig.getStatsFullQuestsCompletedLabel(), fullCompleteLabelPos);
        TextUtil.drawValue(this.textDrawer, fullCompleteStr, fullCompletePos);

        TextUtil.drawLabel(this.textDrawer, this.strConfig.getQuestBonusLabel(), bonusLabelPos);
        TextUtil.drawValue(this.textDrawer, bonusStr, bonusPos);

        this.drawTopInfo();
        return this;
    }

    private void drawQuest(Graphics2D g2d, int index) throws IOException, URISyntaxException {
        this.drawQuestStr(index);
        this.drawQuestValue(index);
        this.drawReward(g2d, index);
    }

    private void drawQuestStr(int index) {
        String questStr = this.getQuestStr(this.questIDs.get(index), index);

        this.textDrawer.setText(questStr);
        this.textDrawer.setFontSize(this.nums.getFontSmallMedium());
        this.textDrawer.setPos(new int[] {QUEST_X, QUEST_START_Y + index * QUEST_Y_SPACING});
        this.textDrawer.setAlign(Align.LEFT);
        this.textDrawer.setWidth(QUEST_WIDTH);

        this.textDrawer.drawText();
    }

    private String getQuestStr(String questID, int index) {
        QuestConfig questConfig = this.config.getQuestConfig().get(questID);
        String requirement = questConfig.getQuestVals()[index/2].getRequirement();

        return switch (questID) {
            case "daily", "cloneBoars", "powWin" -> {
                boolean isMultiple = Integer.parseInt(requirement) > 1;

                yield isMultiple
                    ? questConfig.getDescriptionAlt().formatted(requirement)
                    : questConfig.getDescription().formatted(requirement);
            }

            case "collectRarity", "cloneRarity" -> {
                char firstChar = requirement.charAt(0);
                boolean isVowel = firstChar == 'a' || firstChar == 'e' || firstChar == 'i' || firstChar == 'o' ||
                    firstChar == 'u';
                String rarityName = this.config.getRarityConfigs().get(requirement).getName();

                yield isVowel
                    ? questConfig.getDescriptionAlt().formatted(requirement, rarityName)
                    : questConfig.getDescription().formatted(requirement, rarityName);
            }

            case "spendBucks", "collectBucks", "powFast" -> questConfig.getDescription().formatted(requirement);

            case "sendGifts", "openGifts" -> {
                boolean isMultiple = Integer.parseInt(requirement) > 1;
                String giftStr = isMultiple
                    ? this.itemConfig.getPowerups().get("gift").getPluralName()
                    : this.itemConfig.getPowerups().get("gift").getName();

                yield questConfig.getDescription().formatted(requirement, giftStr);
            }

            default -> this.strConfig.getUnavailable();
        };
    }

    private void drawQuestValue(int index) {
        String questValue = this.getQuestValue(
            this.questIDs.get(index), this.questData.questProgress().get(index), index
        );

        this.textDrawer.setText(questValue);
        this.textDrawer.setFontSize(this.nums.getFontMedium());
        this.textDrawer.setPos(new int[] {QUEST_X, QUEST_START_Y + index * QUEST_Y_SPACING + QUEST_VALUE_Y_OFFSET});
        this.textDrawer.setWidth(-1);

        this.textDrawer.drawText();
    }

    private String getQuestValue(String questID, int progress, int index) {
        QuestConfig questConfig = this.config.getQuestConfig().get(questID);
        String requirement = questConfig.getQuestVals()[index/2].getRequirement();
        String questValue = "<>%s<>%d/%d";

        return switch (questID) {
            case "daily", "spendBucks", "collectBucks", "cloneBoars", "sendGifts", "openGifts", "powWin" -> {
                int requirementAmt = Integer.parseInt(requirement);
                boolean requirementMet = progress >= requirementAmt;
                String colorKey = requirementMet ? "green" : "silver";

                if (!requirementMet) {
                    this.allQuestsDone = false;
                }

                yield questValue.formatted(colorKey, progress, requirementAmt);
            }

            case "collectRarity", "cloneRarity", "powFast" -> {
                boolean requirementMet = progress >= 1;
                String colorKey = requirementMet ? "green" : "silver";

                if (!requirementMet) {
                    this.allQuestsDone = false;
                }

                yield questValue.formatted(colorKey, progress, 1);
            }
            default -> questValue.formatted("silver", 0, 0);
        };
    }

    private void drawReward(Graphics2D g2d, int index) throws IOException, URISyntaxException {
        IndivQuestConfig indivQuestConfig = this.config.getQuestConfig()
            .get(this.questIDs.get(index)).getQuestVals()[index/2];
        String rewardType = indivQuestConfig.getRewardType();
        int rewardAmt = indivQuestConfig.getRewardAmt();

        if (rewardType.equals("bucks")) {
            this.textDrawer.setText("+<>bucks<>$%d".formatted(rewardAmt));
            this.textDrawer.setFontSize(this.nums.getFontMedium());
            this.textDrawer.setPos(new int[] {BUCKS_X, BUCKS_START_Y + index * QUEST_Y_SPACING});
            this.textDrawer.setAlign(Align.RIGHT);
            this.textDrawer.setWidth(-1);

            this.textDrawer.drawText();
            return;
        }

        String filePath = this.pathConfig.getPowerups() + this.itemConfig.getPowerups().get(rewardType).getFile();

        this.textDrawer.setText("+<>powerup<>%d".formatted(rewardAmt));
        this.textDrawer.setFontSize(this.nums.getFontSmallest());
        this.textDrawer.setPos(new int[] {POW_TEXT_X, POW_TEXT_START_Y + index * QUEST_Y_SPACING});
        this.textDrawer.setAlign(Align.RIGHT);
        this.textDrawer.setWidth(-1);

        this.textDrawer.drawText();

        GraphicsUtil.drawImage(g2d, filePath, new int[] {POW_X, POW_START_Y + index * QUEST_Y_SPACING}, POW_SIZE);
    }
}
