package com.mrtold.saulgoodman.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mr_Told
 */
public class DocUtils {

    static final ZoneId timezone = ZoneId.of("Europe/Moscow");
    static final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    static final Font tnrPlain = new Font("Times New Roman", Font.PLAIN, 24);
    static final Font tnrItalic = new Font("Times New Roman", Font.ITALIC, 24);
    static final Font tnrTitle = new Font("Times New Roman", Font.BOLD, 28);

    static final File agreement = new File("docs", "agreement.jpg");
    static final File request = new File("docs", "request.jpg");

    public static void init() {
        if (!agreement.exists()) {
            throw new RuntimeException("Agreement file not found");
        }
        if (!request.exists()) {
            throw new RuntimeException("Request file not found");
        }
    }

    public static byte[] generateAgreement(String aName, int aPass, byte[] aSign,
                                           String cName, int cPass, byte[] cSign, int num) {
        try {
            Strings s = Strings.getInstance();
            String l11 = String.format(s.get("doc.agreement.advocate_format"), aName);
            String l12 = String.format(s.get("doc.agreement.with_pass_format"), aPass);

            String l21 = String.format(s.get("doc.agreement.client_format"), cName);
            String l22 = String.format(s.get("doc.agreement.with_pass_format"), cPass);

            String date = timestampFormat.format(LocalDateTime.now(timezone));
            BufferedImage image = ImageIO.read(agreement);
            BufferedImage aSignI = ImageIO.read(new ByteArrayInputStream(aSign));
            BufferedImage cSignI = ImageIO.read(new ByteArrayInputStream(cSign));

            Graphics2D g2d = image.createGraphics();
            initG2DHints(g2d);
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

    public static byte[] generateRequest(String aName, String discord, Integer phone, byte[] aSign,
                                         String deadline, String target, String input,
                                         int agreement, int num) {
        try {
            Strings s = Strings.getInstance();
            List<String> bodyPartsIn = input.lines()
                    .filter(l -> !l.isBlank())
                    .map(l -> {
                        StringBuilder sb = new StringBuilder(l);
                        char last = l.charAt(l.length() - 1);
                        if (last == '.' || last == ';' || last == ',')
                            sb.setCharAt(sb.length() - 1, ';');
                        else
                            sb.append(';');
                        return sb.toString();
                    }).toList();

            List<String> bodyList = new ArrayList<>(bodyPartsIn.size() + 3);
            bodyList.addAll(bodyPartsIn);

            String dateNum = s.get("doc.request.date_num_format").formatted(timestampFormat.format(LocalDateTime.now(timezone)), num);
            String numReq = s.get("doc.request.agreement_num_format").formatted(agreement);
            String contactPhone = phone == null ? "" : s.get("doc.request.contact_phone_format").formatted(phone);

            if (target != null)
                bodyList.add(s.get("doc.request.request_to_format").formatted(target));

            if (deadline != null)
                bodyList.add(s.get("doc.request.provide_until_format").formatted(deadline));

            bodyList.add(s.get("doc.request.provide_to_format")
                    .formatted(aName, discord, contactPhone));

            BufferedImage image = ImageIO.read(request);
            BufferedImage aSignI = ImageIO.read(new ByteArrayInputStream(aSign));

            Graphics2D g2d = image.createGraphics();

            initG2DHints(g2d);
            g2d.setFont(tnrPlain);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.setColor(Color.BLACK);

            g2d.drawString(dateNum, 275 - fm.stringWidth(dateNum) / 2, fm.getAscent() + 315);
            g2d.drawString(numReq, 731, 642);

            int n = 1;
            int offset = 673;
            for (String part : bodyList) {
                offset = drawListElement(g2d, fm, n, part, offset);
                n++;
            }

            g2d.setFont(tnrItalic);
            fm = g2d.getFontMetrics();
            g2d.drawString(aName, 600 - fm.stringWidth(aName) / 2, fm.getAscent() + 1510);

            drawSignature(g2d, aSignI, 600, 1475, 300, 80);

            g2d.dispose();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", os);
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int drawListElement(Graphics2D g2d, FontMetrics fm, int num, String content, int offset) {
        java.util.List<String> lines = fullJustify(Arrays
                .stream(content.split(" "))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new), 93);
        String numS = num < 10 ? "%d.  ".formatted(num) : "%d. ".formatted(num);
        int yD = fm.getAscent() + 11;
        g2d.drawString(numS, 129,  fm.getAscent() + offset);
        for (String line : lines) {
            g2d.drawString(line, 165, fm.getAscent() + offset);
            offset += yD;
        }
        return offset + fm.getAscent();
    }

    private static java.util.List<String> fullJustify(String[] words, int maxWidth) {
        int n = words.length;
        List<String> justifiedText = new ArrayList<>();
        int currLineIndex = 0;
        int nextLineIndex = getNextLineIndex(currLineIndex, maxWidth, words);
        while (currLineIndex < n) {
            StringBuilder line = new StringBuilder();
            for (int i = currLineIndex; i < nextLineIndex; i++) {
                line.append(words[i]).append(" ");
            }
            currLineIndex = nextLineIndex;
            nextLineIndex = getNextLineIndex(currLineIndex, maxWidth, words);
            justifiedText.add(line.toString());
        }
        for (int i = 0; i < justifiedText.size() - 1; i++) {
            String fullJustifiedLine = getFullJustifiedString(justifiedText.get(i).trim(), maxWidth);
            justifiedText.set(i, fullJustifiedLine);
        }
        String leftJustifiedLine = getLeftJustifiedLine(justifiedText.get(justifiedText.size() - 1).trim(), maxWidth);
        justifiedText.remove(justifiedText.size() - 1);
        justifiedText.add(leftJustifiedLine);
        return justifiedText;
    }

    private static int getNextLineIndex(int currLineIndex, int maxWidth, String[] words) {
        int n = words.length;
        int width = 0;
        while (currLineIndex < n && width < maxWidth) {
            width += words[currLineIndex++].length() + 1;
        }
        if (width > maxWidth + 1)
            currLineIndex--;
        return currLineIndex;
    }

    private static String getFullJustifiedString(String line, int maxWidth) {
        StringBuilder justifiedLine = new StringBuilder();
        String[] words = line.split(" ");
        int occupiedCharLength = 0;
        for (String word : words) {
            occupiedCharLength += word.length();
        }
        int remainingSpace = maxWidth - occupiedCharLength;
        int spaceForEachWordSeparation = words.length > 1 ? remainingSpace / (words.length - 1) : remainingSpace;
        int extraSpace = remainingSpace - spaceForEachWordSeparation * (words.length - 1);
        for (int j = 0; j < words.length - 1; j++) {
            justifiedLine.append(words[j]);
            justifiedLine.append(" ".repeat(Math.max(0, spaceForEachWordSeparation)));
            if (extraSpace > 0) {
                justifiedLine.append(" ");
                extraSpace--;
            }
        }
        justifiedLine.append(words[words.length - 1]);
        justifiedLine.append(" ".repeat(Math.max(0, extraSpace)));
        return justifiedLine.toString();
    }

    private static String getLeftJustifiedLine(String line, int maxWidth) {
        int lineWidth = line.length();
        return line + " ".repeat(Math.max(0, maxWidth - lineWidth));
    }

    private static void initG2DHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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

        leftLoop2:
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                if (new Color(img.getRGB(j, i), true).getAlpha() != 0) {
                    break leftLoop2;
                }
            }
        }
        y0 = i;
        rightLoop2:
        for (i = height - 1; i >= 0; i--) {
            for (j = 0; j < width; j++) {
                if (new Color(img.getRGB(j, i), true).getAlpha() != 0) {
                    break rightLoop2;
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
