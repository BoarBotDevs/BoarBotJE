package dev.boarbot.entities.boaruser.queries;

import dev.boarbot.api.util.Configured;
import dev.boarbot.bot.config.quests.IndivQuestConfig;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.util.boar.BoarUtil;
import dev.boarbot.util.quests.QuestType;
import dev.boarbot.util.data.QuestDataUtil;
import dev.boarbot.util.quests.QuestInfo;
import dev.boarbot.util.quests.QuestUtil;
import dev.boarbot.util.time.TimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestQueries implements Configured {
    private final BoarUser boarUser;

    private final static Map<Integer, String> numStrs = new HashMap<>();

    static {
        numStrs.put(0, "one");
        numStrs.put(1, "two");
        numStrs.put(2, "three");
        numStrs.put(3, "four");
        numStrs.put(4, "five");
        numStrs.put(5, "six");
        numStrs.put(6, "seven");
    }

    public QuestQueries(BoarUser boarUser) {
        this.boarUser = boarUser;
    }

    public QuestInfo addProgress(
        QuestType quest, List<String> boarIDs, Connection connection
    ) throws SQLException {
        int questIndex = QuestDataUtil.getQuestIndex(quest, connection);

        if (questIndex == -1) {
            return null;
        }

        String requiredRarity = this.getRequiredRarity(quest, questIndex);
        int val = 0;

        for (String boarID : boarIDs) {
            if (BoarUtil.findRarityKey(boarID).equals(requiredRarity)) {
                val++;
            }
        }

        return this.addProgress(quest, questIndex, val, connection);
    }

    public QuestInfo addProgress(QuestType quest, long val, Connection connection) throws SQLException {
        int questIndex = QuestDataUtil.getQuestIndex(quest, connection);

        if (questIndex == -1) {
            return null;
        }

        return this.addProgress(quest, questIndex, val, connection);
    }

    private QuestInfo addProgress(
        QuestType quest, int questIndex, long val, Connection connection
    ) throws SQLException {
        String columnNum = numStrs.get(questIndex);
        List<QuestType> quests = QuestDataUtil.getQuests(connection);
        List<Integer> requiredAmts = new ArrayList<>();

        for (QuestType curQuest : quests) {
            requiredAmts.add(QuestUtil.getRequiredAmt(curQuest, questIndex));
        }

        if (quest.equals(QuestType.POW_FAST) && val < requiredAmts.get(questIndex)) {
            val = 1;
        }

        if (val == 0) {
            return null;
        }

        String query = """
            SELECT
                one_progress,
                two_progress,
                three_progress,
                four_progress,
                five_progress,
                six_progress,
                seven_progress,
                %s_claimed,
                full_claimed,
                auto_claim
            FROM user_quests
            WHERE user_id = ?;
        """.formatted(columnNum);

        boolean shouldClaim = false;
        boolean shouldClaimBonus = true;
        int progress = 0;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, this.boarUser.getUserID());

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    progress = results.getInt("%s_progress".formatted(columnNum));
                    boolean complete = quest.equals(QuestType.POW_FAST)
                        ? val < requiredAmts.get(questIndex)
                        : progress + val >= requiredAmts.get(questIndex);
                    boolean claimed = results.getBoolean("%s_claimed".formatted(columnNum));
                    boolean autoClaim = results.getBoolean("auto_claim");
                    shouldClaim = complete && autoClaim && !claimed;
                    shouldClaimBonus = autoClaim && !results.getBoolean("full_claimed");

                    for (int i=0; i<quests.size() && shouldClaimBonus; i++) {
                        int curProgress = results.getInt("%s_progress".formatted(numStrs.get(i)));

                        if (i == questIndex) {
                            shouldClaimBonus = complete;
                        } else {
                            shouldClaimBonus = quests.get(i)
                               .equals(QuestType.POW_FAST) == (curProgress < requiredAmts.get(i));
                        }
                    }
                }
            }
        }

        String update = """
            UPDATE user_quests
            SET
                %s_progress = ?,
                %s_claimed = (%s_claimed OR ?),
                full_claimed = (full_claimed OR ?),
                fastest_full_millis = LEAST(fastest_full_millis, ?)
            WHERE user_id = ?;
        """.formatted(columnNum, columnNum, columnNum);

        try (PreparedStatement statement = connection.prepareStatement(update)) {
            long adjustedVal = quest.equals(QuestType.POW_FAST)
                ? Math.min(progress, val)
                : progress + val;
            statement.setLong(1, adjustedVal);
            statement.setBoolean(2, shouldClaim);
            statement.setBoolean(3, shouldClaimBonus);
            statement.setLong(4, shouldClaimBonus
                ? TimeUtil.getCurMilli() - TimeUtil.getLastQuestResetMilli()
                : Integer.MAX_VALUE
            );
            statement.setString(5, this.boarUser.getUserID());
            statement.executeUpdate();
        }

        List<IndivQuestConfig> questConfigs = new ArrayList<>();
        IndivQuestConfig questConfig = CONFIG.getQuestConfig().get(quest.toString()).getQuestVals()[questIndex/2];
        String rewardType = questConfig.getRewardType();
        int rewardAmt = questConfig.getRewardAmt();

        if (rewardType.equals("bucks")) {
            QuestInfo bucksQuest = this.addProgress(QuestType.COLLECT_BUCKS, rewardAmt, connection);

            if (bucksQuest != null) {
                questConfigs.add(bucksQuest.quests().getFirst());
                shouldClaimBonus = shouldClaimBonus || bucksQuest.gaveBonus();
            }
        }

        if (shouldClaim) {
            questConfigs.add(questConfig);
            this.giveReward(questConfig, true, connection);
            return new QuestInfo(questConfigs, shouldClaimBonus);
        }

        return null;
    }

    private void giveReward(IndivQuestConfig questConfig, boolean force, Connection connection) throws SQLException {
        String rewardType = questConfig.getRewardType();
        int rewardAmt = questConfig.getRewardAmt();

        if (rewardType.equals("bucks")) {
            this.boarUser.baseQuery().giveBucks(connection, rewardAmt);
        } else {
            this.boarUser.powQuery().addPowerup(connection, rewardType, rewardAmt, force);
        }
    }

    private String getRequiredRarity(QuestType quest, int index) {
        if (quest.equals(QuestType.COLLECT_RARITY) || quest.equals(QuestType.CLONE_RARITY)) {
            return CONFIG.getQuestConfig().get(quest.toString()).getQuestVals()[index/2].getRequirement();
        }

        throw new IllegalArgumentException("Unsupported quest type: " + quest);
    }
}