package me.ipodtouch0218.jackboxdrawer.obj;

import java.awt.Color;
import java.util.ArrayList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @ToString
public class Line {

	public Line(int thickness, Color color) {
		this.thickness = thickness;
		this.color = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public final ArrayList<Point> points = new ArrayList<>();
	@Getter private int thickness;
	@Getter private String color;
	
	public Point[] getPoints() {
		return points.toArray(new Point[]{});
	}
	
//	@JsonIgnore
//	public boolean intersectsPoint(Point point) {
//		
//	}
}
