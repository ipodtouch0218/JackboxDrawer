package me.ipodtouch0218.jackboxdrawer.obj;

import java.awt.Color;
import java.util.ArrayList;

public class Line {

	public Line() {
		
	}
	
	public Line(int thickness, Color color) {
		this.thickness = thickness;
		this.color = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public ArrayList<Point> points = new ArrayList<>();
	public int thickness;
	public String color;
	
	public Point[] getPoints() {
		return points.toArray(new Point[]{});
	}
	
	public int getThickness() { 
		return thickness; 
	}
	
	public String getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return "Line [points=" + points + ", thickness=" + thickness + ", color=" + color + "]";
	}
	
}
