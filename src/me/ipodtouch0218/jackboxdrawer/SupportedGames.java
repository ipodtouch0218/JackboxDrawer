package me.ipodtouch0218.jackboxdrawer;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.ipodtouch0218.jackboxdrawer.obj.Line;
import me.ipodtouch0218.jackboxdrawer.obj.PushTheButtonLine;

public enum SupportedGames {
	
	DRAWFUL_1("Drawful 1", ImageType.BITMAP, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(jbd.actualImage, "PNG", stream);
			jbd.websocketServer.broadcast("var test = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "'; if (typeof (data.picture) !== 'undefined') { data.picture = test; } else { data.drawing = test; }");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	DRAWFUL_2("Drawful 2", ImageType.VECTOR, (jbd) -> {
		try {
			jbd.websocketServer.broadcast("var test = " + new ObjectMapper().writeValueAsString(jbd.lines.subList(0, jbd.currentLine)) +"; if (typeof (data.pictureLines) !== 'undefined') { data.pictureLines = test; } else { data.drawingLines = test; }");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	BIDIOTS("Bidiots", ImageType.BITMAP, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(jbd.actualImage, "PNG", stream);
			jbd.websocketServer.broadcast("data.drawing = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "';");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	TEE_KO("Tee K.O.", ImageType.VECTOR, (jbd) -> {
		try {
			Color bg = jbd.teeKOBackgroundColorPicker.getColor();
			jbd.websocketServer.broadcast("data.pictureLines = " + new ObjectMapper().writeValueAsString(jbd.lines.subList(0, jbd.currentLine)) + "; data.background = '" + String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue()) + "'");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	PUSH_THE_BUTTON("Push the Button", ImageType.VECTOR, (jbd) -> {
		ArrayList<PushTheButtonLine> list = new ArrayList<>();
		for (Line lines : jbd.lines.subList(0, jbd.currentLine)) {
			list.add(new PushTheButtonLine(lines));
		}
		
		try {
			jbd.websocketServer.broadcast("data.lines = " + new ObjectMapper().writeValueAsString(list.toArray(new PushTheButtonLine[]{})));
			return true;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return false;
	}),
	TRIVIA_MURDER_PARTY_1("Trivia Murder Party 1", ImageType.BITMAP, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(jbd.actualImage, "PNG", stream);
			jbd.websocketServer.broadcast("data.drawing = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "';");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	;

	private String prettyName;
	private ImageType type;
	private Function<JackboxDrawer,Boolean> exportConsumer;
	
	SupportedGames(String id, ImageType type, Function<JackboxDrawer, Boolean> exportConsumer) {
		this.prettyName = id;
		this.type = type;
		this.exportConsumer = exportConsumer;
	}
	
	public ImageType getImageType() { return type; }
	public String getName() { return prettyName; }
	
	public boolean export(JackboxDrawer jbd) {
		return exportConsumer.apply(jbd);
	}
	
	public enum ImageType {
		VECTOR, BITMAP;
	}
}
