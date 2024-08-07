package dev.boarbot.util.generators;

import dev.boarbot.BoarBotApp;
import dev.boarbot.bot.config.NumberConfig;
import dev.boarbot.util.graphics.Align;
import dev.boarbot.util.graphics.TextDrawer;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class EmbedImageGenerator extends ImageGenerator {
    private final static int[] ORIGIN = {0, 0};
    private final static int MAX_WIDTH = 1500;

    private String str;
    private String pureStr;
    @Setter private String color;

    private final Font font = BoarBotApp.getBot().getFont()
        .deriveFont((float) this.config.getNumberConfig().getFontBig());

    public EmbedImageGenerator(String str) {
        this(str, null);
    }

    public EmbedImageGenerator(String str, String color) {
        this.str = str;
        this.pureStr = str.replaceAll("<>(.*?)<>", "");
        this.color = color == null ? this.config.getColorConfig().get("font") : color;
    }

    public void setStr(String str) {
        this.str = str;
        this.pureStr = str.replaceAll("<>(.*?)<>", "");
    }

    public EmbedImageGenerator generate() throws IOException {
        NumberConfig nums = this.config.getNumberConfig();

        this.generatedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = this.generatedImage.createGraphics();

        g2d.setFont(this.font);
        FontMetrics fm = g2d.getFontMetrics();

        int width = Math.min(fm.stringWidth(this.pureStr) + nums.getBorder() * 6, MAX_WIDTH);

        TextDrawer textDrawer = new TextDrawer(
            g2d,
            this.str,
            ORIGIN,
            Align.CENTER,
            this.color,
            nums.getFontBig(),
            width - nums.getBorder() * 6,
            true
        );

        int height = (int) textDrawer.drawText() + nums.getBorder() * 8;

        this.generatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = this.generatedImage.createGraphics();

        g2d.setFont(this.font);
        g2d.setColor(Color.decode(this.config.getColorConfig().get("dark")));

        g2d.fillRoundRect(0, 0, width, height, nums.getBorder(), nums.getBorder());

        int[] pos = new int[] {
            width / 2,
            height / 2 + (int) ((fm.getAscent()) * 0.5)
        };

        textDrawer.setG2d(g2d);
        textDrawer.setPos(pos);
        textDrawer.drawText();

        return this;
    }
}
