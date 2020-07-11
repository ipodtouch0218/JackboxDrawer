package me.ipodtouch0218.jackboxdrawer;

import java.awt.Color;

public class PushTheButtonLine {

	public PushTheButtonLine(Line line) {
		this.thickness = line.thickness;
		this.color = line.color;
		
		StringBuilder builder = new StringBuilder();
		for (Point p : line.points) {
			builder.append(p.x).append(",").append(p.y).append("|");
		}
		points = builder.substring(0, builder.length()-1);
	}
	
	String points;
	int thickness;
	Color color;
	
	public String getPoints() {
		return points;
	}
	
	public int getThickness() { 
		return thickness; 
	}
	
	public String getColor() {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
	@Override
	public String toString() {
		return "Line [points=" + points + ", thickness=" + thickness + ", color=" + color + "]";
	}
}
