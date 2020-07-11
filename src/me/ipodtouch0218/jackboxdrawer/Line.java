package me.ipodtouch0218.jackboxdrawer;

import java.awt.Color;
import java.util.ArrayList;

public class Line {

	public Line(int thickness, Color color) {
		this.thickness = thickness;
		this.color = color;
	}
	
	ArrayList<Point> points = new ArrayList<>();
	int thickness;
	Color color;
	
	public Point[] getPoints() {
		return points.toArray(new Point[]{});
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
