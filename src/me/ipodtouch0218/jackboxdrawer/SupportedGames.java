package me.ipodtouch0218.jackboxdrawer;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.ipodtouch0218.jackboxdrawer.obj.Line;
import me.ipodtouch0218.jackboxdrawer.obj.PushTheButtonLine;
import me.ipodtouch0218.jackboxdrawer.util.VolatileImageHelper;

@Getter @AllArgsConstructor
public enum SupportedGames {
	
	//TODO CHECK
	DRAWFUL_1("Drawful 1", ImageType.BITMAP, 240, 300, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(VolatileImageHelper.toBufferedImage(jbd.getCanvasImage()), "PNG", stream);
			jbd.getWebsocketServer().broadcast("var test = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "'; if (typeof (data.body.picture) !== 'undefined') { data.body.picture = test; } else { data.body.drawing = test; }");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	DRAWFUL_2("Drawful 2", ImageType.VECTOR, 240, 300, (jbd) -> {
		try {
			jbd.getWebsocketServer().broadcast("var test = " + new ObjectMapper().writeValueAsString(jbd.getLines()) +"; if (typeof (data.body.pictureLines) !== 'undefined') { data.body.pictureLines = test; } else { data.body.drawingLines = test; }");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	//TODO Check
	BIDIOTS("Bidiots", ImageType.BITMAP, 240, 300, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(VolatileImageHelper.toBufferedImage(jbd.getCanvasImage()), "PNG", stream);
			jbd.getWebsocketServer().broadcast("data.drawing = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "';");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	//TODO CHECK
	TEE_KO("Tee K.O.", ImageType.VECTOR, 240, 300, (jbd) -> {
		try {
			Color bg = jbd.getShirtBackgroundColorPicker().getColor();
			jbd.getWebsocketServer().broadcast("data.pictureLines = " + new ObjectMapper().writeValueAsString(jbd.getLines()) + "; data.background = '" + String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue()) + "'");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	//TODO CHECK
	PUSH_THE_BUTTON("Push the Button", ImageType.VECTOR, 240, 300, (jbd) -> {
		ArrayList<PushTheButtonLine> list = new ArrayList<>();
		for (Line lines : jbd.getLines()) {
			list.add(new PushTheButtonLine(lines));
		}
		
		try {
			jbd.getWebsocketServer().broadcast("data.lines = " + new ObjectMapper().writeValueAsString(list.toArray(new PushTheButtonLine[]{})));
			return true;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return false;
	}),
	//TODO CHECK
	TRIVIA_MURDER_PARTY_1("Trivia Murder Party 1", ImageType.BITMAP, 240, 300, (jbd) -> {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(VolatileImageHelper.toBufferedImage(jbd.getCanvasImage()), "PNG", stream);
			jbd.getWebsocketServer().broadcast("data.drawing = '" + Base64.getEncoder().encodeToString(stream.toByteArray()) + "';");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	CHAMPD_UP("Champ'd Up", ImageType.VECTOR, 640, 640, (jbd) -> {
		ArrayList<PushTheButtonLine> list = new ArrayList<>();
		for (Line lines : jbd.getLines()) {
			list.add(new PushTheButtonLine(lines));
		}
		list.forEach(ptbl -> ptbl.setThickness(Math.max(1,ptbl.getThickness()-4)));
		
		try {			
			jbd.getWebsocketServer().broadcast("data.val.lines = " + new ObjectMapper().writeValueAsString(list.toArray(new PushTheButtonLine[]{})));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}),
	;

	private String name;
	private ImageType type;
	private int canvasWidth, canvasHeight;
	private Function<JackboxDrawer,Boolean> exportConsumer;
	
	public boolean export(JackboxDrawer jbd) {
		return exportConsumer.apply(jbd);
	}
	
	public enum ImageType {
		VECTOR, BITMAP;
	}
}
