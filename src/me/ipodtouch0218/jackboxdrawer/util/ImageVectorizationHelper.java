package me.ipodtouch0218.jackboxdrawer.util;

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
	
}
