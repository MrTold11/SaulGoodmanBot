package com.mrtold.saulgoodman.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Mr_Told
 */
public class DocUtils {

    static final File agreement = new File("docs", "agreement.jpg");
    static final Font tnrPlain = new Font("Times New Roman", Font.PLAIN, 24);
    static final Font tnrItalic = new Font("Times New Roman", Font.ITALIC, 24);
    static final Font tnrTitle = new Font("Times New Roman", Font.BOLD, 28);

    public static void init() {
        if (!agreement.exists()) {
            throw new RuntimeException("Agreement file not found");
        }
    }

    public static byte[] generateAgreement(String aName, int aPass, byte[] aSign,
                                           String cName, int cPass, byte[] cSign, int num) {
        try {
            String l11 = String.format("Частного адвоката %s", aName);
            String l12 = String.format("c номером паспорта %d", aPass);

            String l21 = String.format("и граждан(ином/кой) %s", cName);
            String l22 = String.format("c номером паспорта %d", cPass);

            final ZoneId timezone = ZoneId.of("Europe/Moscow");
            final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String date = timestampFormat.format(LocalDateTime.now(timezone));
            BufferedImage image = ImageIO.read(agreement);
            BufferedImage aSignI = ImageIO.read(new ByteArrayInputStream(aSign));
            BufferedImage cSignI = ImageIO.read(new ByteArrayInputStream(cSign));

            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setFont(tnrPlain);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.setColor(Color.BLACK);
            int p1 = 525;
            g2d.drawString(l11, 319 - fm.stringWidth(l11) / 2, fm.getAscent() + p1);
            g2d.drawString(l12, 319 - fm.stringWidth(l12) / 2, fm.getAscent() + p1 + 28);
            g2d.drawString(l21, 319 - fm.stringWidth(l21) / 2, fm.getAscent() + p1 + 107);
            g2d.drawString(date, 555 - fm.stringWidth(date) / 2, fm.getAscent() + 1970);

            g2d.drawString(l22, 319 - fm.stringWidth(l22) / 2, fm.getAscent() + p1 + 107 + 28);

            g2d.setFont(tnrItalic);
            fm = g2d.getFontMetrics();
            g2d.drawString(aName, 235 - fm.stringWidth(aName) / 2, fm.getAscent() + 2000);
            g2d.drawString(cName, 870 - fm.stringWidth(cName) / 2, fm.getAscent() + 2000);

            g2d.setFont(tnrTitle);
            fm = g2d.getFontMetrics();
            String nums = String.valueOf(num);
            g2d.drawString(nums, 308, fm.getAscent() + 185);

            drawSignature(g2d, aSignI, 233, 1959, 350, 80);
            drawSignature(g2d, cSignI, 862, 1959, 350, 80);
            g2d.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);

            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void drawSignature(Graphics2D g2d, BufferedImage sign,
                                      int xCenter, int yCenter, int wMax, int hMax) {
        BufferedImage image = trimAlpha(toBufferedImage(makeWhiteTransparent(sign)));
        double scale = Math.min(
                (double) wMax / image.getWidth(),
                (double) hMax / image.getHeight());

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.scale(scale, scale);

        g2d.drawImage(image, new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR),
                (int) (xCenter - image.getWidth()  * scale / 2),
                (int) (yCenter - image.getHeight() * scale / 2));
    }

    private static BufferedImage trimAlpha(BufferedImage img) {
        int width = img.getWidth(), height = img.getHeight();
        int x0, x1, y0, y1;
        int j, i;

        leftLoop:
        for (i = 0; i < width; i++) {
            for (j = 0; j < height; j++) {
                if (new Color(img.getRGB(i, j), true).getAlpha() != 0) {
                    break leftLoop;
                }
            }
        }
        x0 = i;
        rightLoop:
        for (i = width - 1; i >= 0; i--) {
            for (j = 0; j < height; j++) {
                if (new Color(img.getRGB(i, j), true).getAlpha() != 0) {
                    break rightLoop;
                }
            }
        }
        x1 = i + 1;

        leftLoop:
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                if (new Color(img.getRGB(j, i), true).getAlpha() != 0) {
                    break leftLoop;
                }
            }
        }
        y0 = i;
        rightLoop:
        for (i = height - 1; i >= 0; i--) {
            for (j = 0; j < width; j++) {
                if (new Color(img.getRGB(j, i), true).getAlpha() != 0) {
                    break rightLoop;
                }
            }
        }
        y1 = i + 1;

        return img.getSubimage(x0, y0, x1 - x0, y1 - y0);
    }

    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage)
            return (BufferedImage) img;

        BufferedImage bimage = new BufferedImage(img.getWidth(null),
                img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    private static final ImageFilter filter = new RGBImageFilter() {
        public final int markerRGB = Color.WHITE.getRGB() | 0xFF000000;

        public int filterRGB(int x, int y, int rgb) {
            if ((rgb | 0xFF000000) == markerRGB)
                return 0x00FFFFFF & rgb;
            else
                return rgb;
        }
    };

    private static Image makeWhiteTransparent(Image im) {
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

}
