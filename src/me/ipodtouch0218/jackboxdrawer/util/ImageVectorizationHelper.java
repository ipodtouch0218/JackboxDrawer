package me.ipodtouch0218.jackboxdrawer.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.ArrayList;

import me.ipodtouch0218.jackboxdrawer.JackboxDrawer;
import me.ipodtouch0218.jackboxdrawer.obj.Line;
import me.ipodtouch0218.jackboxdrawer.obj.Point;

public class ImageVectorizationHelper {

	private ImageVectorizationHelper() {}
	
	public static double colorDistanceSquared(int c1, int c2) {
		//https://en.wikipedia.org/wiki/Color_difference#sRGB
		int r1 = sepRed(c1), r2 = sepRed(c2);
		int g1 = sepGreen(c1), g2 = sepGreen(c2);
		int b1 = sepBlue(c1), b2 = sepBlue(c2);
		
		double redAvg = (r1 + r2) / 2d;
		
		if (redAvg < 128) {
			//  2*(r2-r1)^2 + 4*(g2-g1)^2 + 3(b2-b1)^2
			return 2*Math.pow(r2-r1, 2) + 4*Math.pow(g2-g1, 2) + 3*Math.pow(b2-b1, 2);
		} else {
			//  3*(r2-r1)^2 + 4*(g2-g1)^2 + 2(b2-b1)^2
			return 3*Math.pow(r2-r1, 2) + 4*Math.pow(g2-g1, 2) + 2*Math.pow(b2-b1, 2);
		}
	}
	
	public static int sepRed(int rgb) {
		return (rgb&0x00FF0000) >> 0x10;
	}
	
	public static int sepGreen(int rgb) {
		return (rgb&0x0000FF00) >> 0x08;
	}
	
	public static int sepBlue(int rgb) {
		return rgb&0x000000FF;
	}
	
	//interpolate colors: c = a*t + (t-1)*b
	public static Color mixColors(Color c1, Color c2, double t) {
		return new Color((int) (t*c1.getRed() + (1.0-t)*c2.getRed()),(int) (t*c1.getGreen() + (1.0-t)*c2.getGreen()),(int) (t*c1.getBlue() + (1.0-t)*c2.getBlue()));
	}
	
	public static ArrayList<Line> vectorizeImage(BufferedImage image, int canvasWidth, int canvasHeight, double scaling, double colorDistanceGroupingMax, int alphaCutoff) {
		
		colorDistanceGroupingMax *= colorDistanceGroupingMax;
		int scaledWidth = (int) (canvasWidth / scaling);
		int scaledHeight = (int) (canvasHeight / scaling);
		
		int xFactor = canvasWidth / scaledWidth;
		int yFactor = canvasHeight / scaledHeight;
		
		ArrayList<Line> lines = new ArrayList<>();
		
		//Rescale image
		Image scaled = image.getScaledInstance((int) (canvasWidth / scaling), (int) (canvasHeight / scaling), Image.SCALE_AREA_AVERAGING);
		image = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.drawImage(scaled, 0, 0, null);
		g.dispose();
		
		Color averageBgColor = null;
		int colors = 1;
		
		//Loop time
		for (int x = 0; x < scaledWidth; x++) {
			Line currentLine = null;
			int currentLineColor = -1;
			for (int y = 0; y < scaledHeight; y++) {
				Color color = new Color(image.getRGB(x, y), true);
				Point thisPoint = new Point(x * xFactor, y * yFactor);
				
				if (color.getAlpha() < alphaCutoff) {
					if (currentLine != null) {
						currentLine.points.add(new Point(x * xFactor, (y-1) * yFactor));
						currentLine = null;
					}
					continue;
				}
				
				if (x == 0 || x == scaledWidth-1 || y == 0 || y == scaledHeight-1) {
					if (averageBgColor == null) {
						averageBgColor = color;
					} else {
						averageBgColor = mixColors(color, averageBgColor, (1/colors++));
					}
				}
				
				//First line of a row.
				if (currentLine == null) {
					currentLine = new Line(xFactor + 1, color);
					currentLine.points.add(thisPoint);
					lines.add(currentLine);
					currentLineColor = color.getRGB();
					continue;
				}
				
				double colorDistanceSquared = colorDistanceSquared(color.getRGB(), currentLineColor);
				if (colorDistanceSquared > colorDistanceGroupingMax) {
					//Too different to be grouped. Add a point above this point, and start a new line.
					currentLine.points.add(thisPoint);
					currentLine.setColor(mixColors(Color.decode(currentLine.getColor()), new Color(image.getRGB(x, y-1)), 0.5));
					
					currentLine = new Line(xFactor + 1, color);
					currentLine.points.add(thisPoint);
					lines.add(currentLine);
					currentLineColor = color.getRGB();
					continue;
				}
			}
			//Finish the line
			if (currentLine != null) {
				currentLine.points.add(new Point(x * xFactor, (scaledHeight) * yFactor));
				currentLine = null;
			}
		}
		
		JackboxDrawer.INSTANCE.getShirtBackgroundColorPicker().setColor(averageBgColor);
		
		return lines;
	}
}
