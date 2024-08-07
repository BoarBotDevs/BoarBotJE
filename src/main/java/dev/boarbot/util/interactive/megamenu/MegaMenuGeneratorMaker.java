package dev.boarbot.util.interactive.megamenu;

import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.BotConfig;
import dev.boarbot.bot.config.RarityConfig;
import dev.boarbot.entities.boaruser.BoarInfo;
import dev.boarbot.entities.boaruser.BoarUser;
import dev.boarbot.entities.boaruser.BoarUserFactory;
import dev.boarbot.interactives.boar.megamenu.MegaMenuInteractive;
import dev.boarbot.interactives.boar.megamenu.MegaMenuView;
import dev.boarbot.util.data.DataUtil;
import dev.boarbot.util.generators.megamenu.CollectionImageGenerator;
import dev.boarbot.util.generators.megamenu.CompendiumImageGenerator;
import dev.boarbot.util.generators.megamenu.MegaMenuGenerator;
import dev.boarbot.util.generators.megamenu.ProfileImageGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class MegaMenuGeneratorMaker {
    private final BotConfig config = BoarBotApp.getBot().getConfig();

    private final MegaMenuInteractive interactive;

    public MegaMenuGeneratorMaker(MegaMenuInteractive interactive) {
        this.interactive = interactive;
    }

    public MegaMenuGenerator make() throws SQLException {
        return switch (this.interactive.getCurView()) {
            case MegaMenuView.PROFILE -> this.makeProfileGen();
            case MegaMenuView.COLLECTION -> this.makeCollectionGen();
            case MegaMenuView.COMPENDIUM -> this.makeCompendiumGen();
            case MegaMenuView.STATS -> this.makeCollectionGen();
            case MegaMenuView.POWERUPS -> this.makeCollectionGen();
            case MegaMenuView.QUESTS -> this.makeCollectionGen();
            case MegaMenuView.BADGES -> this.makeCollectionGen();
        };
    }

    public MegaMenuGenerator makeProfileGen() throws SQLException {
        MegaMenuView view = MegaMenuView.PROFILE;

        boolean notUpdated = this.interactive.getViewsToUpdateData().get(view) == null ||
            !this.interactive.getViewsToUpdateData().get(view);

        if (notUpdated) {
            try (Connection connection = DataUtil.getConnection()) {
                this.interactive.setProfileData(this.interactive.getBoarUser().getProfileData(connection));
                this.interactive.getViewsToUpdateData().put(view, true);
            }
        }

        this.interactive.setMaxPage(0);
        if (this.interactive.getPage() > this.interactive.getMaxPage()) {
            this.interactive.setPrevPage(this.interactive.getPage());
            this.interactive.setPage(this.interactive.getMaxPage());
        }

        return new ProfileImageGenerator(
            this.interactive.getPage(),
            this.interactive.getBoarUser(),
            this.interactive.getBadgeIDs(),
            this.interactive.getFirstJoinedDate(),
            this.interactive.getFavoriteID(),
            this.interactive.isSkyblockGuild(),
            this.interactive.getProfileData()
        );
    }

    private MegaMenuGenerator makeCollectionGen() throws SQLException {
        MegaMenuView view = MegaMenuView.COLLECTION;

        this.updateCompendiumCollection(view);
        this.refreshFilterSort();

        this.interactive.setMaxPage(Math.max((this.interactive.getFilteredBoars().size()-1) / 15, 0));
        if (this.interactive.getPage() > this.interactive.getMaxPage()) {
            this.interactive.setPrevPage(this.interactive.getPage());
            this.interactive.setPage(this.interactive.getMaxPage());
        }

        return new CollectionImageGenerator(
            this.interactive.getPage(),
            this.interactive.getBoarUser(),
            this.interactive.getBadgeIDs(),
            this.interactive.getFirstJoinedDate(),
            this.interactive.getFilteredBoars()
        );
    }

    private MegaMenuGenerator makeCompendiumGen() throws SQLException {
        MegaMenuView view = MegaMenuView.COMPENDIUM;

        this.updateCompendiumCollection(view);
        this.refreshFilterSort();

        this.interactive.setMaxPage(this.interactive.getFilteredBoars().size()-1);
        if (this.interactive.getPage() > this.interactive.getMaxPage()) {
            this.interactive.setPrevPage(this.interactive.getPage());
            this.interactive.setPage(this.interactive.getMaxPage());
        }

        Iterator<Map.Entry<String, BoarInfo>> iterator = this.interactive.getFilteredBoars().entrySet().iterator();
        for (int i=0; i<this.interactive.getPage(); i++) {
            iterator.next();
        }

        this.interactive.setCurBoarEntry(iterator.next());

        return new CompendiumImageGenerator(
            this.interactive.getPage(),
            this.interactive.getBoarUser(),
            this.interactive.getBadgeIDs(),
            this.interactive.getFirstJoinedDate(),
            this.interactive.getFavoriteID() != null &&
                this.interactive.getFavoriteID().equals(this.interactive.getCurBoarEntry().getKey()),
            this.interactive.getCurBoarEntry()
        );
    }

    private void updateCompendiumCollection(MegaMenuView view) throws SQLException {
        boolean notUpdated = this.interactive.getViewsToUpdateData().get(view) == null ||
            !this.interactive.getViewsToUpdateData().get(view);

        if (notUpdated) {
            try (Connection connection = DataUtil.getConnection()) {
                this.interactive.setOwnedBoars(this.interactive.getBoarUser().getOwnedBoarInfo(connection));

                BoarUser interBoarUser = BoarUserFactory.getBoarUser(this.interactive.getInitEvent().getUser());
                this.interactive.setFilterBits(interBoarUser.getFilterBits(connection));
                this.interactive.setSortVal(interBoarUser.getSortVal(connection));
                interBoarUser.decRefs();

                this.interactive.getViewsToUpdateData().put(view, true);
                this.interactive.getViewsToUpdateData().put(MegaMenuView.COLLECTION, true);
            }
        }
    }

    private void refreshFilterSort() {
        this.interactive.setFilteredBoars(new LinkedHashMap<>());
        Map<String, RarityConfig> rarities = this.config.getRarityConfigs();
        int[] rarityBitShift = new int[] {1 + rarities.size()};

        List<String> newKeySet = new ArrayList<>(rarities.keySet());
        Collections.reverse(newKeySet);

        for (String rarityKey : newKeySet) {
            this.applyFilter(rarities.get(rarityKey), rarityKey, rarityBitShift);
        }

        LinkedHashMap<String, BoarInfo> sortedBoars = new LinkedHashMap<>();

        switch (this.interactive.getSortVal()) {
            case RARITY_A -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case AMOUNT_D -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.amountComparator().reversed()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case AMOUNT_A -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.amountComparator()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case RECENT_D -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.recentComparator().reversed()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case RECENT_A -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.recentComparator()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case NEWEST_D -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.newestComparator().reversed()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case NEWEST_A -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(BoarInfo.newestComparator()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case ALPHA_D -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }

            case ALPHA_A -> {
                this.interactive.getFilteredBoars().entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
                    .forEachOrdered(entry -> sortedBoars.put(entry.getKey(), entry.getValue()));
            }
        }

        if (!sortedBoars.isEmpty()) {
            this.interactive.setFilteredBoars(sortedBoars);
        }
    }

    private void applyFilter(RarityConfig rarity, String rarityKey, int[] rarityBitShift) {
        BoarInfo emptyBoarInfo = new BoarInfo(0, rarityKey, -1, -1);

        boolean ownedFilter = this.interactive.getFilterBits() % 2 == 1;
        boolean duplicateFilter = (this.interactive.getFilterBits() >> 1) % 2 == 1;
        boolean raritySelected = this.interactive.getFilterBits() > 3;

        boolean notRarityFilter = (this.interactive.getFilterBits() >> rarityBitShift[0]) % 2 == 0;
        rarityBitShift[0]--;
        if (raritySelected && notRarityFilter) {
            return;
        }

        for (String boarID : rarity.getBoars()) {
            // Owned filter
            if (ownedFilter && !this.interactive.getOwnedBoars().containsKey(boarID)) {
                continue;
            }

            // Duplicate filter
            boolean hasDuplicate = this.interactive.getOwnedBoars().containsKey(boarID) &&
                this.interactive.getOwnedBoars().get(boarID).amount() > 1;
            if (duplicateFilter && !hasDuplicate) {
                continue;
            }

            // No filter
            if (rarity.isHidden() && !this.interactive.getOwnedBoars().containsKey(boarID)) {
                continue;
            }

            this.interactive.getFilteredBoars().put(
                boarID, this.interactive.getOwnedBoars().getOrDefault(boarID, emptyBoarInfo)
            );
        }
    }
}
