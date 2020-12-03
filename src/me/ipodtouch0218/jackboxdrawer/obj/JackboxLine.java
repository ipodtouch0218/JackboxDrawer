package me.ipodtouch0218.jackboxdrawer.obj;

import java.awt.Color;
import java.util.ArrayList;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor @ToString
public class JackboxLine {

	public JackboxLine(int thickness, Color color) {
		this.thickness = thickness;
		setColor(color);
	}
	
	public final ArrayList<LinePoint> points = new ArrayList<>();
	@Getter private int thickness;
	@Getter private String color;
	
	public LinePoint[] getPoints() {
		return points.toArray(new LinePoint[]{});
	}
	
	public void setColor(Color color) {
		this.color = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
	
}
