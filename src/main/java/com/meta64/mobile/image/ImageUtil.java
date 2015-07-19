package com.meta64.mobile.image;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageUtil {

	public static BufferedImage scaleImage(BufferedImage image, int width) {
		Image outImage = image.getScaledInstance(width, -1, Image.SCALE_SMOOTH);
		BufferedImage outBufferedImage = new BufferedImage(outImage.getWidth(null), outImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
		outBufferedImage.getGraphics().drawImage(outImage, 0, 0, null);
		return outBufferedImage;
	}

	public static boolean isImageMime(String mimeType) {
		return mimeType != null && mimeType.toLowerCase().startsWith("image/");
	}
}
