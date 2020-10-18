package me.ipodtouch0218.jackboxdrawer.util;

import java.awt.Color;

public class RandomUtils {

	private RandomUtils() {}
	
	//https://stackoverflow.com/questions/9733288/how-to-programmatically-calculate-the-contrast-ratio-between-two-colors
	public static double getColorLuminance(Color color) {
		double r = color.getRed()/255d;
		double g = color.getGreen()/255d;
		double b = color.getBlue()/255d;
		
		//I would make this a function, but what the hell do i call it???
		r = (r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4));
		g = (g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4));
		b = (b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4));
		
	    return r * 0.2126 + g * 0.7152 + b * 0.0722;
	}
	
	public static double getContrastRatio(Color color1, Color color2) {
	    double lum1 = getColorLuminance(color1);
	    double lum2 = getColorLuminance(color2);
	    double brightest = Math.max(lum1, lum2);
	    double darkest = Math.min(lum1, lum2);
	    return (brightest + 0.05)
	         / (darkest + 0.05);
	}
	
}
