package dev.boarbot.util.generators;

import com.google.gson.Gson;
import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.*;
import dev.boarbot.bot.config.items.IndivItemConfig;
import dev.boarbot.util.boar.BoarUtil;
import dev.boarbot.util.graphics.Align;
import dev.boarbot.util.graphics.GraphicsUtil;
import dev.boarbot.util.graphics.TextDrawer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class ItemImageGenerator extends ImageGenerator {
    public static final int[] IMAGE_SIZE = {930, 1080};

    private static final int[] ORIGIN = {0, 0};
    private static final int[] ITEM_POS = {33, 174};
    private static final int[] TITLE_POS = {473, 77};
    private static final int[] NAME_POS = {473, 115};
    private static final int USER_TAG_X = 110;
    private static final int USER_AVATAR_X = 43;
    private static final int USER_AVATAR_Y_OFFSET = 9;
    private static final int USER_AVATAR_WIDTH = 52;
    private static final int USER_BOX_EXTRA = 95;
    private static final int BOX_TEXT_X = 48;
    private static final int BOX_TEXT_Y_OFFSET = 52;
    private static final int BOX_TEXT_EXTRA = 33;
    private static final int BOX_X = 33;
    private static final int BOX_ONE_Y = 195;
    private static final int BOX_TWO_Y = 266;
    private static final int BOX_THREE_Y = 358;
    private static final int BOX_FOUR_Y = 429;
    private static final int BOX_HEIGHT = 71;

    private final User user;
    private final String title;

    private String itemID;
    @Getter @Setter private String itemName;
    @Getter @Setter private String filePath;
    @Getter @Setter private String staticFilePath;
    @Getter @Setter private String colorKey;
    @Getter @Setter private User giftingUser;
    @Getter @Setter private long bucks;

    public ItemImageGenerator(User user, String title, String itemID) {
        this(user, title, itemID, false);
    }

    public ItemImageGenerator(User user, String title, String itemID, boolean isBadge) {
        this(user, title, itemID, isBadge, null);
    }

    public ItemImageGenerator(User user, String title, String itemID, boolean isBadge, User giftingUser) {
        this(user, title, itemID, isBadge, giftingUser, 0);
    }

    public ItemImageGenerator(User user, String title, String itemID, boolean isBadge, User giftingUser, long bucks) {
        this.user = user;
        this.title = title;
        this.itemID = itemID;

        if (isBadge) {
            IndivItemConfig badgeInfo = this.config.getItemConfig().getBadges().get(itemID);
            this.itemName = badgeInfo.name;
            this.filePath = this.config.getPathConfig().getBadges() + badgeInfo.file;
            this.staticFilePath = null;
            this.colorKey = "badge";
        } else {
            IndivItemConfig boarInfo = this.config.getItemConfig().getBoars().get(itemID);
            this.itemName = boarInfo.name;
            this.filePath = this.config.getPathConfig().getBoars() + boarInfo.getFile();
            this.staticFilePath = boarInfo.getStaticFile() != null
                ? this.config.getPathConfig().getBoars() + boarInfo.getStaticFile()
                : null;
            this.colorKey = BoarUtil.findRarityKey(itemID);
        }

        this.giftingUser = giftingUser;
        this.bucks = bucks;
    }

    public ItemImageGenerator(User user, String title, String itemName, String filePath, String colorKey) {
        this(user, title, itemName, filePath, colorKey, null);
    }

    public ItemImageGenerator(
        User user, String title, String itemName, String filePath, String colorKey, User giftingUser
    ) {
        this(user, title, itemName, filePath, colorKey, giftingUser, 0);
    }

    public ItemImageGenerator(
        User user, String title, String itemName, String filePath, String colorKey, User giftingUser, long bucks
    ) {
        this.user = user;
        this.title = title;

        this.itemName = itemName;
        this.filePath = filePath;
        this.staticFilePath = null;
        this.colorKey = colorKey;

        this.giftingUser = giftingUser;
        this.bucks = bucks;
    }

    public ItemImageGenerator generate() throws IOException, URISyntaxException {
        return this.generate(false);
    }

    public ItemImageGenerator generate(boolean forceStatic) throws IOException, URISyntaxException {
        String extension = this.filePath.split("[.]")[1];

        if (extension.equals("gif") && !forceStatic) {
            this.generatedImageBytes = BoarBotApp.getBot().getByteCacheMap().get(
                "animitem" + this.title.toLowerCase().replaceAll("[^a-z]+", "") + this.itemName + this.colorKey
            );

            if (this.generatedImageBytes == null) {
                this.generateStatic(false);
                this.generateAnimated();

                BoarBotApp.getBot().getByteCacheMap().put(
                    "animitem" + this.title.toLowerCase().replaceAll("[^a-z]+", "") + this.itemName + this.colorKey,
                    this.generatedImageBytes
                );
            }

            if (this.user != null) {
                this.addAnimatedUser();
            }
        } else {
            this.generatedImageBytes = BoarBotApp.getBot().getByteCacheMap().get(
                "item" + this.title.toLowerCase().replaceAll("[^a-z]+", "") + this.itemName + this.colorKey
            );

            if (this.generatedImageBytes == null) {
                this.generateStatic(true);

                BoarBotApp.getBot().getByteCacheMap().put(
                    "item" + this.title.toLowerCase().replaceAll("[^a-z]+", "") + this.itemName + this.colorKey,
                    this.generatedImageBytes
                );
            }

            if (this.user != null) {
                this.addStaticUser();
            }
        }

        return this;
    }

    private void generateAnimated() throws IOException {
        Gson g = new Gson();

        Process pythonProcess = new ProcessBuilder(
            "python",
            this.config.getPathConfig().getMakeImageScript(),
            g.toJson(this.config.getPathConfig()),
            g.toJson(this.config.getNumberConfig()),
            this.filePath
        ).start();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));
        OutputStream stdOut = pythonProcess.getOutputStream();

        stdOut.write(this.generatedImageBytes);
        stdOut.close();

        String result = stdIn.readLine();

        if (result == null) {
            String tempErrMessage;
            String errMessage = "";

            while ((tempErrMessage = stdErr.readLine()) != null) {
                errMessage = errMessage.concat(tempErrMessage + "\n");
            }

            log.error(errMessage);
        }

        this.generatedImageBytes = Base64.getDecoder().decode(result);
    }

    private void addAnimatedUser() throws IOException {
        Gson g = new Gson();

        Process pythonProcess = new ProcessBuilder(
            "python",
            this.config.getPathConfig().getOverlayScript(),
            g.toJson(this.config.getPathConfig()),
            g.toJson(this.config.getColorConfig()),
            g.toJson(this.config.getNumberConfig()),
            this.user.getEffectiveAvatarUrl(),
            this.user.getName(),
            "%,d".formatted(this.bucks),
            this.giftingUser == null ? "" : this.giftingUser.getEffectiveAvatarUrl(),
            this.giftingUser == null ? "" : this.giftingUser.getName()
        ).start();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));
        OutputStream stdOut = pythonProcess.getOutputStream();

        stdOut.write(this.generatedImageBytes);
        stdOut.close();

        String result = stdIn.readLine();

        if (result == null) {
            String tempErrMessage;
            String errMessage = "";

            while ((tempErrMessage = stdErr.readLine()) != null) {
                errMessage = errMessage.concat(tempErrMessage + "\n");
            }

            log.error(errMessage);
        }

        this.generatedImageBytes = Base64.getDecoder().decode(result);
    }

    private void generateStatic(boolean makeWithItem) throws IOException, URISyntaxException {
        NumberConfig nums = this.config.getNumberConfig();
        PathConfig pathConfig = this.config.getPathConfig();
        Map<String, String> colorConfig = this.config.getColorConfig();

        String itemAssetsFolder = pathConfig.getItemAssets();
        String underlayPath = itemAssetsFolder + pathConfig.getItemUnderlay();
        String backplatePath = itemAssetsFolder + pathConfig.getItemBackplate();

        int[] itemSize = nums.getBigBoarSize();

        BufferedImage generatedImage = new BufferedImage(IMAGE_SIZE[0], IMAGE_SIZE[1], BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = generatedImage.createGraphics();

        GraphicsUtil.drawRect(g2d, ORIGIN, IMAGE_SIZE, colorConfig.get(colorKey));
        g2d.setComposite(AlphaComposite.DstIn);
        GraphicsUtil.drawImage(g2d, underlayPath, ORIGIN, IMAGE_SIZE);
        g2d.setComposite(AlphaComposite.SrcOver);

        GraphicsUtil.drawImage(g2d, backplatePath, ORIGIN, IMAGE_SIZE);
        if (makeWithItem && this.itemID != null) {
            BufferedImage itemImage = BoarBotApp.getBot().getImageCacheMap().get("big" + this.itemID);
            g2d.drawImage(itemImage, ITEM_POS[0], ITEM_POS[1], null);
        } else if (makeWithItem) {
            GraphicsUtil.drawImage(g2d, this.filePath, ITEM_POS, itemSize);
        }

        TextDrawer textDrawer = new TextDrawer(
            g2d,
            this.title,
            TITLE_POS,
            Align.CENTER,
            colorConfig.get("font"),
            nums.getFontMedium()
        );

        textDrawer.drawText();

        textDrawer.setText(this.itemName);
        textDrawer.setPos(NAME_POS);
        textDrawer.setColorVal(colorConfig.get(this.colorKey));
        textDrawer.setWidth(itemSize[0]);

        textDrawer.drawText();

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ImageIO.write(generatedImage, "png", byteArrayOS);
        this.generatedImageBytes = byteArrayOS.toByteArray();
    }

    private void addStaticUser() throws IOException, URISyntaxException {
        ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(this.generatedImageBytes);
        BufferedImage generatedImage = ImageIO.read(byteArrayIS);
        Graphics2D g2d = generatedImage.createGraphics();

        g2d.drawImage(this.generateUserImageData(), BOX_X, BOX_ONE_Y, null);

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ImageIO.write(generatedImage, "png", byteArrayOS);
        this.generatedImageBytes = byteArrayOS.toByteArray();
    }

    private BufferedImage generateUserImageData() throws IOException, URISyntaxException {
        NumberConfig nums = this.config.getNumberConfig();
        Map<String, String> colorConfig = this.config.getColorConfig();

        int userBoxY = BOX_ONE_Y;

        String userAvatar = this.user.getEffectiveAvatarUrl();
        String username = this.user.getName().substring(
            0, Math.min(this.user.getName().length(), this.config.getNumberConfig().getMaxUsernameLength())
        );

        BufferedImage userDataImage = new BufferedImage(IMAGE_SIZE[0], IMAGE_SIZE[1], BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = userDataImage.createGraphics();

        TextDrawer textDrawer = new TextDrawer(
            g2d, "", ORIGIN, Align.LEFT, colorConfig.get("font"), nums.getFontSmallMedium()
        );
        FontMetrics fm = g2d.getFontMetrics();

        if (this.giftingUser != null) {
            userBoxY = BOX_TWO_Y;

            String giftingUserAvatar = this.giftingUser.getEffectiveAvatarUrl();
            String giftingUsername = this.giftingUser.getName().substring(
                0, Math.min(this.giftingUser.getName().length(), this.config.getNumberConfig().getMaxUsernameLength())
            );

            g2d.setPaint(Color.decode(colorConfig.get("dark")));
            g2d.fillRect(BOX_X, BOX_ONE_Y, nums.getBorder(), BOX_HEIGHT);
            g2d.fill(new RoundRectangle2D.Double(
                BOX_X,
                BOX_ONE_Y,
                fm.stringWidth("To") + BOX_TEXT_EXTRA,
                BOX_HEIGHT,
                nums.getBorder() * 2,
                nums.getBorder() * 2
            ));

            int[] toPos = {BOX_TEXT_X, BOX_ONE_Y + BOX_TEXT_Y_OFFSET};

            textDrawer.setText("To");
            textDrawer.setPos(toPos);
            textDrawer.drawText();

            g2d.setPaint(Color.decode(colorConfig.get("dark")));
            g2d.fillRect(BOX_X, BOX_THREE_Y, nums.getBorder(), BOX_HEIGHT);
            g2d.fill(new RoundRectangle2D.Double(
                BOX_X,
                BOX_THREE_Y,
                fm.stringWidth("From") + BOX_TEXT_EXTRA,
                BOX_HEIGHT,
                nums.getBorder() * 2,
                nums.getBorder() * 2
            ));

            int[] fromPos = {BOX_TEXT_X, BOX_THREE_Y + BOX_TEXT_Y_OFFSET};

            textDrawer.setText("From");
            textDrawer.setPos(fromPos);
            textDrawer.drawText();

            g2d.setPaint(Color.decode(colorConfig.get("dark")));
            g2d.fillRect(BOX_X, BOX_FOUR_Y, nums.getBorder(), BOX_HEIGHT);
            g2d.fill(new RoundRectangle2D.Double(
                BOX_X,
                BOX_FOUR_Y,
                fm.stringWidth(giftingUsername) + USER_BOX_EXTRA,
                BOX_HEIGHT,
                nums.getBorder() * 2,
                nums.getBorder() * 2
            ));

            int[] giftingPos = {BOX_TEXT_X, BOX_FOUR_Y + BOX_TEXT_Y_OFFSET};

            textDrawer.setText(giftingUsername);
            textDrawer.setPos(giftingPos);
            textDrawer.drawText();

            int[] giftingAvatarPos = {USER_AVATAR_X, BOX_FOUR_Y + USER_AVATAR_Y_OFFSET};

            GraphicsUtil.drawCircleImage(g2d, giftingUserAvatar, giftingAvatarPos, USER_AVATAR_WIDTH);
        }

        g2d.setPaint(Color.decode(colorConfig.get("dark")));
        g2d.fillRect(BOX_X, userBoxY, nums.getBorder(), BOX_HEIGHT);
        g2d.fill(new RoundRectangle2D.Double(
            BOX_X,
            userBoxY,
            fm.stringWidth(username) + USER_BOX_EXTRA,
            BOX_HEIGHT,
            nums.getBorder() * 2,
            nums.getBorder() * 2
        ));

        int[] userPos = {USER_TAG_X, userBoxY + BOX_TEXT_Y_OFFSET};

        textDrawer.setText(username);
        textDrawer.setPos(userPos);
        textDrawer.drawText();

        int[] userAvatarPos = {USER_AVATAR_X, userBoxY + USER_AVATAR_Y_OFFSET};

        GraphicsUtil.drawCircleImage(g2d, userAvatar, userAvatarPos, USER_AVATAR_WIDTH);

        if (this.bucks > 0 && this.giftingUser == null) {
            String formattedBucks = "%,d".formatted(this.bucks);

            g2d.setPaint(Color.decode(colorConfig.get("dark")));
            g2d.fillRect(BOX_X, BOX_TWO_Y, nums.getBorder(), BOX_HEIGHT);
            g2d.fill(new RoundRectangle2D.Double(
                BOX_X,
                BOX_TWO_Y,
                fm.stringWidth("+$" + formattedBucks) + BOX_TEXT_EXTRA,
                BOX_HEIGHT,
                nums.getBorder() * 2,
                nums.getBorder() * 2
            ));

            int[] bucksPos = {BOX_TEXT_X, BOX_TWO_Y + BOX_TEXT_Y_OFFSET};

            textDrawer.setText("+<>bucks<>$" + formattedBucks);
            textDrawer.setPos(bucksPos);
            textDrawer.drawText();
        }

        return userDataImage.getSubimage(
            BOX_X, BOX_ONE_Y, nums.getBigBoarSize()[0], BOX_FOUR_Y + BOX_HEIGHT - BOX_ONE_Y
        );
    }
}
