package me.ipodtouch0218.jackboxdrawer;

import java.net.InetSocketAddress;

import javax.swing.JOptionPane;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WebsocketServer extends WebSocketServer {

	private JackboxDrawer drawer;
	
	public WebsocketServer(JackboxDrawer drawer) {
		super(new InetSocketAddress(2460));
		this.drawer = drawer;
	}
	
	@Override
	public void onClose(WebSocket socket, int code, String reason, boolean remote) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(WebSocket socket, Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(WebSocket socket, String message) {
		if (message.startsWith("updategame")) {
			SupportedGames previous = drawer.getCurrentGame();
			drawer.changeGame(SupportedGames.valueOf(message.split(":")[1].toUpperCase()));
			drawer.getGameSelectionButtons().forEach((game,button) -> {
				button.setSelected(game == drawer.getCurrentGame());
			});
			if (previous == drawer.getCurrentGame()) {
				return;
			}
			JOptionPane.showMessageDialog(drawer.getWindow(), "Selected game updated to " + drawer.getCurrentGame().getName(), "Selected Game Updated", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}

	@Override
	public void onOpen(WebSocket socket, ClientHandshake handshake) {
		broadcast("version:" + JackboxDrawer.VERSION.replace(".", ""));
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		
	}
}