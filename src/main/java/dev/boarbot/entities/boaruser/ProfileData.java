package dev.boarbot.entities.boaruser;

import java.sql.Timestamp;

public record ProfileData(
    String lastBoarID,
    long boarBucks,
    long totalBoars,
    int numDailies,
    Timestamp lastDailyTimestamp,
    int uniqueBoars,
    int numSkyblock,
    int streak,
    int blessings,
    int streakBless,
    int uniqueBless,
    int questBless,
    int otherBless
) {
    public ProfileData() {
        this(
            null,
            0,
            0,
            0,
            null,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }
}
