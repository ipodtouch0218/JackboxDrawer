package me.ipodtouch0218.jackboxdrawer.obj;

public class PushTheButtonLine {

	public PushTheButtonLine(JackboxLine line) {
		this.thickness = line.getThickness();
		this.color = line.getColor();
		
		StringBuilder builder = new StringBuilder();
		for (LinePoint p : line.points) {
			builder.append(p.getX()).append(",").append(p.getY()).append("|");
		}
		points = builder.substring(0, builder.length()-1);
	}
	
	String points;
	int thickness;
	String color;
	
	public String getPoints() {
		return points;
	}
	
	public int getThickness() { 
		return thickness; 
	}
	
	public void setThickness(int thick) {
		thickness = thick;
	}
	
	public String getColor() {
		return color;
	}

	@Override
	public String toString() {
		return "PushTheButtonLine [points=" + points + ", thickness=" + thickness + ", color=" + color + "]";
	}
	
}
