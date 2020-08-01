package com.igor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CardParser {
    private static Map<BufferedImage, String> templates = new HashMap<>();

    private static int colorDifference(int sample, int template, boolean sampleIsDark)
    {
        Color sampleColor = new Color(sample);
        Color templateColor = new Color(template);
        int divisor = sampleIsDark ? 2 : 1;

        return Math.abs(sampleColor.getBlue() - templateColor.getBlue() / divisor) +
                Math.abs(sampleColor.getGreen() - templateColor.getGreen() / divisor) +
                Math.abs(sampleColor.getRed() - templateColor.getRed() / divisor);
    }

    private static boolean colorCloseTo(int src, int dst)
    {
        return colorDifference(src, dst, false) < 30;
    }

    private static long imageDifference(BufferedImage sample, BufferedImage template, boolean sampleIsDark) {
        if (sample.getHeight() != template.getHeight() || sample.getWidth() != template.getWidth()) {
            throw new IllegalArgumentException("Images size must be equal");
        }

        if (sample.getHeight() * sample.getWidth() > 1000000) {
            throw new IllegalArgumentException("Image size is too large");
        }

        long difference = 0;
        for (int x = 0; x < sample.getWidth(); x++) {
            for (int y = 0; y < sample.getHeight(); y++) {
                difference += colorDifference(sample.getRGB(x, y), template.getRGB(x, y), sampleIsDark);
            }
        }

        return difference;
    }

    private static String parseCard(BufferedImage cardImage) {
        int probeColor = cardImage.getRGB(50, 10);
        if (colorCloseTo(0x2a2a2a, probeColor))
            return "";

        boolean isDark = (colorCloseTo(0x787878, probeColor));

        long minDistance = Long.MAX_VALUE;
        String minDistanceCard = "";
        for (Map.Entry<BufferedImage, String> entry : templates.entrySet()) {
            long distance = imageDifference(cardImage, entry.getKey(), isDark);
            if (distance < minDistance) {
                minDistance = distance;
                minDistanceCard = entry.getValue();
            }
        }

        return minDistanceCard;
    }

    private static void printCards(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null)
            return;

        int[] xOffsets = {147, 219, 290, 362, 434};
        StringBuilder cardsDescription = new StringBuilder();
        for (int xOffset: xOffsets)
            cardsDescription.append(parseCard(image.getSubimage(xOffset, 589, 56, 80)));

        System.out.println(file.getName() + " - " + cardsDescription.toString());
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Path to directory is not specified");
            return;
        }

        File resourceDir = new File("cards");
        File[] resources = resourceDir.listFiles();
        if (resources != null) {
            for (File resource : resources) {
                String name = resource.getName();
                templates.put(ImageIO.read(resource), name.substring(0, name.lastIndexOf('.')));
            }
        }

        File directory = new File(args[0]);
        File[] images = directory.listFiles();
        if (images != null) {
            for (File image : images) {
                printCards(image);
            }
        }
    }
}