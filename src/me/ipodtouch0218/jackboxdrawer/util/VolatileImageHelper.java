package me.ipodtouch0218.jackboxdrawer.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class VolatileImageHelper {

	//Disable instances
	private VolatileImageHelper() {}
	
	private static final Color TRANSPARENCY = new Color(0,0,0,0);
	private static final GraphicsEnvironment GE = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private static final GraphicsConfiguration GC = GE.getDefaultScreenDevice().getDefaultConfiguration();
	
	
	public static VolatileImage createVolatileImage(int width, int height, int transparency) {
		VolatileImage image = GC.createCompatibleVolatileImage(width, height, transparency);
		
		if (image.validate(GC) == VolatileImage.IMAGE_INCOMPATIBLE) {
			return createVolatileImage(width, height, transparency);
		}
		return image;
	}
	
	public static VolatileImage createVolatileImage(Image image, int transparency) {
		VolatileImage volImage = createVolatileImage(image.getWidth(null), image.getHeight(null), transparency);
		
		do {
			Graphics2D g = volImage.createGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
		} while (volImage.contentsLost());

		return volImage;
	}
	
	public static VolatileImage clearImage(VolatileImage image) {
		if (image.validate(GC) == VolatileImage.IMAGE_INCOMPATIBLE) {
			image = createVolatileImage(image, image.getTransparency());
		}
		
		do {
			Graphics2D g = image.createGraphics();
			g.setColor(TRANSPARENCY);
			g.setComposite(AlphaComposite.Src);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			g.dispose();
		} while (image.contentsLost());
		
		return image;
	}
	
	public static BufferedImage toBufferedImage(VolatileImage image) {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getTransparency());
		Graphics2D g = newImage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}
}
