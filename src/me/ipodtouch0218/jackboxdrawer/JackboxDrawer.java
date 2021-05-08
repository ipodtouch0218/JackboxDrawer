package me.ipodtouch0218.jackboxdrawer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SystemColor;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import lombok.Getter;
import lombok.Setter;
import me.ipodtouch0218.jackboxdrawer.SupportedGames.ImageType;
import me.ipodtouch0218.jackboxdrawer.obj.JackboxLine;
import me.ipodtouch0218.jackboxdrawer.obj.LinePoint;
import me.ipodtouch0218.jackboxdrawer.uielements.HintTextField;
import me.ipodtouch0218.jackboxdrawer.uielements.ImportWindow;
import me.ipodtouch0218.jackboxdrawer.uielements.JPanelDnD;
import me.ipodtouch0218.jackboxdrawer.uielements.StretchIcon;
import me.ipodtouch0218.jackboxdrawer.util.RandomUtils;
import me.ipodtouch0218.jackboxdrawer.util.SizeLimitedList;
import me.ipodtouch0218.jackboxdrawer.util.VolatileImageHelper;

public class JackboxDrawer {

	public static JackboxDrawer INSTANCE;
	
	//--Constants--//
	public static final String VERSION = "1.5.0";
	private static final String PROGRAM_NAME = "Jackbox Drawer v" + VERSION;
	private static final Color[] TEEKO_BG_COLORS = {new Color(40, 85, 135), new Color(95, 98, 103), new Color(8, 8, 8), new Color(117, 14, 30), new Color(98, 92, 74)};
	private static final BufferedImage TRANSPARENT_TEXTURE = new BufferedImage(2,2,BufferedImage.TYPE_BYTE_GRAY);
	static {
		Graphics2D g = TRANSPARENT_TEXTURE.createGraphics();
		g.setColor(new Color(244,244,244));
		g.fillRect(0, 0, 2, 2);
		g.setColor(new Color(224,224,224));
		g.fillRect(1, 0, 2, 1);
		g.fillRect(0, 1, 1, 2);
		g.dispose();
		TRANSPARENCY = new TexturePaint(TRANSPARENT_TEXTURE, new Rectangle(0,0,40,40));
	}
	private static final TexturePaint TRANSPARENCY;
	
	//--Variables--//
	
	@Getter private WebsocketServer websocketServer;
	@Getter private SupportedGames currentGame = SupportedGames.DRAWFUL_1;
	
	//Undo and Redo History
	private SizeLimitedList<ArrayList<JackboxLine>> undoRedoHistory = new SizeLimitedList<>(100);
	private int currentHistoryIndex = -1;
	
	//Components
	@Getter private JFrame window;
	private JPanel teekoPanel, champdUpPanel, drawPanel;
	@Getter private EnumMap<SupportedGames, JRadioButtonMenuItem> gameSelectionButtons = new EnumMap<>(SupportedGames.class);
	@Getter private JColorChooser shirtBackgroundColorPicker;
	private JColorChooser brushColorPicker;
	private JMenuItem mntmRedoButton, mntmUndoButton;
	@Getter private JLabel sketchpad;
	private JLabel lblShirtWarning, lblContrastWarning;
	private HintTextField txtChampdUpName;
	
	//Drawing vars
	@Getter @Setter private ArrayList<JackboxLine> lines = new ArrayList<>();
	private boolean drawing, erasing;
	@Getter @Setter private int importLines;
	Point previousMousePos;
	
	//Images
	@Getter @Setter private BufferedImage importedImage;
	@Getter private VolatileImage canvasImage = VolatileImageHelper.createVolatileImage(getCanvasWidth(), getCanvasHeight(), VolatileImage.TRANSLUCENT);
	private VolatileImage drawnToScreenImage  = VolatileImageHelper.createVolatileImage(getCanvasWidth(), getCanvasHeight(), VolatileImage.OPAQUE);
	
	//Game Specific
	private boolean sentChampdUpName;
	
	//--Callback Functions & Classes--//
	
	/**
	 * Called when File > Import from Image or Ctrl + I
	 */
	public void promptImportFromImage() { 
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Images", "png", "jpg", "jpeg");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
		
		//Default to "details" view, at least in windows.
		Action details = chooser.getActionMap().get("viewTypeDetails");
		if (details != null)
			details.actionPerformed(null);
		
	    int returnVal = chooser.showOpenDialog(window);
	    if (returnVal == JFileChooser.APPROVE_OPTION) 
	    	tryImportFile(chooser.getSelectedFile());
	}
	
	/**
	 * Called when File > Import from URL or Ctrl + U
	 */
	public void promptImportFromURL() {
		String path = JOptionPane.showInputDialog("Enter an image URL.");
		if (path == null || path.isEmpty()) 
			return;
		if (path.startsWith("data:image")) {
			//Shoot, this is base64. Oh well, handle it anyway.
			String data = path.split(",")[1];
			try {
				tryImportImage(decodeToImage(data));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(window, "URL could not be read.\n", "Read Error!", JOptionPane.ERROR_MESSAGE);
			}
			return;
		}
		tryImportFile(path);
	}
	
	/**
	 * Decode base64 string to image (in case someone copies a base64 string as an image from google or something...)
	 * @param imageString Base64 String to deocde
	 * @return Image (or null) of the given string
	 */
	public BufferedImage decodeToImage(String imageString) {
		BufferedImage image = null;
		byte[] imageByte;
		try {
			imageByte = Base64.getDecoder().decode(imageString);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
    }
	
	/**
	 * Called when File > Export to Game or Ctrl + E
	 * Sends the current drawing to the GreaseMonkey extension
	 */
	public void exportToGame() {
		if (drawing || erasing)
			return;
		
		if (currentGame == SupportedGames.CHAMPD_UP) {
			if (!sentChampdUpName) {
				JOptionPane.showMessageDialog(window, "You must submit a name first!\nUse the text box and \"Submit\" button under the color picker first!", "Enter Challenger Name", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		sentChampdUpName = false;
		currentGame.export(JackboxDrawer.this);
	}
	
	/**
	 * Clears the entire canvas, without resizing
	 * Called when Edit > Clear Canvas
	 * @return If the canvas was cleared.
	 */
	public boolean clearCanvas() {
		if ((lines.isEmpty()) && importedImage == null) {
			return true;
		}
		if (JOptionPane.showConfirmDialog(window, "Clear the entire canvas?\nThis action is irreversable.", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			
			lines.clear();
			clearHistory();
			
			repaintImage(true);
			return true;
		}
		return false;
	}
	
	/**
	 * Clears the entire canvas AND sets a new canvas size.
	 * Called when current game changes.
	 * @param newWidth The new width of the canvas, in pixels.
	 * @param newHeight The new height of the canvas, in pixels.
	 * @return If the canvas was cleared & resized.
	 */
	public boolean clearCanvas(int newWidth, int newHeight) {
		if ((lines.isEmpty() && importedImage == null) ||
			JOptionPane.showConfirmDialog(window, "You are changing to a different game.\nThe canvas must be cleared to continue.\nThis action is irreversable.\nClear the entire canvas?", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			
			lines.clear();
			clearHistory();
			
			return true;
		}
		return false;
	}
	
	/**
	 * Clears undo/redo history.
	 */
	public void clearHistory() {
		undoRedoHistory.clear();
		currentHistoryIndex = -1;
		saveHistory();
	}
	
	/**
	 * Called when Edit >  Undo or Ctrl + Z
	 * Undoes a single edit.
	 */
	@SuppressWarnings("unchecked")
	public void undo() { 
		if (drawing || erasing) return;
		if (currentHistoryIndex <= 0) return;
		
		lines = (ArrayList<JackboxLine>) undoRedoHistory.get(--currentHistoryIndex).clone();
		
		mntmUndoButton.setEnabled(currentHistoryIndex > 0);
		mntmRedoButton.setEnabled(currentHistoryIndex != undoRedoHistory.size()-1);
		
		repaintImage(true);
	}
	
	/**
	 * Called when Edit >  Redo or Ctrl + Y
	 * Redos a single edit.
	 */
	@SuppressWarnings("unchecked")
	public void redo() { 
		if (drawing || erasing) return;
		if (currentHistoryIndex >= undoRedoHistory.size());
		
		lines = (ArrayList<JackboxLine>) undoRedoHistory.get(++currentHistoryIndex).clone();
		
		mntmUndoButton.setEnabled(currentHistoryIndex > 0);
		mntmRedoButton.setEnabled(currentHistoryIndex != undoRedoHistory.size()-1);
		
		repaintImage(true);
	}
	
	/**
	 * Saves the current state of the canvas to the history.
	 */
	@SuppressWarnings("unchecked")
	public void saveHistory() {
		undoRedoHistory.removeAfter(currentHistoryIndex);
		
		undoRedoHistory.add((ArrayList<JackboxLine>) lines.clone());
		currentHistoryIndex = Math.min(currentHistoryIndex + 1, undoRedoHistory.getMaxSize());
		
		mntmUndoButton.setEnabled(currentHistoryIndex > 0);
		mntmRedoButton.setEnabled(false);
	}
	
	/**
	 * Called when any of the Select Game radio buttons are pressed
	 * @param newGame The new game to be set
	 */
	public void changeGame(SupportedGames newGame) {
		if (currentGame == newGame) {
			return;
		}
		
		if (!clearCanvas(newGame.getCanvasWidth(), newGame.getCanvasHeight())) {
			gameSelectionButtons.get(currentGame).setSelected(true);
			return;
		} else {
			currentGame = newGame;
			canvasImage = VolatileImageHelper.createVolatileImage(getCanvasWidth(), getCanvasHeight(), VolatileImage.TRANSLUCENT);
			drawnToScreenImage = VolatileImageHelper.createVolatileImage(getCanvasWidth(), getCanvasHeight(), VolatileImage.OPAQUE);
			sketchpad.setIcon(new StretchIcon(drawnToScreenImage, true));
		}
		currentGame = newGame;
		teekoPanel.setVisible(newGame == SupportedGames.TEE_KO);
		champdUpPanel.setVisible(newGame == SupportedGames.CHAMPD_UP);
		repaintImage(true);
	}
	
	//Helper Functions//
	
	/**
	 * Opens a URL in the OS' default browser
	 * @param url The URL to be opened
	 */
	public void openUrl(String url) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
		    try {
				Desktop.getDesktop().browse(new URL(url).toURI());
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Tries to import + vectorize an image
	 * @param loadedImage Image to be vectorized
	 * @throws IOException if the loaded image is null.
	 */
	public void tryImportImage(BufferedImage loadedImage) throws IOException {
		if (loadedImage == null)
			throw new IOException("read fail");
		
		new ImportWindow(loadedImage);
	}
	
	/**
	 * Imports and vectorizes an image from a File
	 * @param file File of the image to be imported
	 */
	public void tryImportFile(File file) {
		try {
    		tryImportImage(ImageIO.read(file));
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(window, "File could not be read.", "Read Error!", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
	}
	
	/**
	 * Imports and vectorizes an image from URL
	 * @param url URL of the image to be imported
	 */
	public void tryImportFile(String url) {
		try {
			URL urlObj = new URL(url);
			try {
				//see if its a file
				tryImportImage(ImageIO.read(urlObj));
			} catch (Exception e) {
				URLConnection conn = urlObj.openConnection();
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0");
				tryImportImage(ImageIO.read(conn.getInputStream()));
			}
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(window, "URL could not be read.", "Read Error!", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
	}
	
	/**
	 * Redraws the sketchpad image.
	 * @param fullRepaint If fast-draw optimizations should be ignored.
	 */
	public void repaintImage(boolean fullRepaint) {
		int canvasWidth = getCanvasWidth();
		int canvasHeight = getCanvasHeight();
		
		boolean fastdraw = drawing && !canvasImage.contentsLost() && !drawnToScreenImage.contentsLost() && !fullRepaint;
		if (!fastdraw) {
			drawnToScreenImage = VolatileImageHelper.clearImage(drawnToScreenImage);
		}
		Graphics2D g = drawnToScreenImage.createGraphics();
		
		do {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			if (fastdraw) {
				
				JackboxLine line = lines.get(lines.size()-1);
				Color color = Color.decode(line.getColor());
				g.setStroke(new BasicStroke(Math.max(1, line.getThickness() + (currentGame == SupportedGames.CHAMPD_UP ? 4 : 0)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				for (int i = Math.max(0, line.points.size()-4); i+1 < line.points.size(); i++) {
					LinePoint p1 = line.points.get(i);
					LinePoint p2 = line.points.get(i+1);
					g.setColor(color);
					g.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
				}
				
				g.dispose();
				sketchpad.repaint();
				return;
			}
			
			if (currentGame == SupportedGames.TEE_KO) {
				g.setColor(shirtBackgroundColorPicker.getColor());
				drawPanel.setBackground(g.getColor());
				lblShirtWarning.setVisible(!arrayContains(TEEKO_BG_COLORS, g.getColor()));
				lblContrastWarning.setVisible(RandomUtils.getContrastRatio(Color.WHITE, shirtBackgroundColorPicker.getColor()) < 2d);
				g.fill(new Rectangle(0,0, canvasWidth, canvasHeight));
				g.setColor(getContrastColor(g.getColor()));
				g.setStroke(new BasicStroke(3f));
				g.drawRect(0, 0, canvasWidth-1, canvasHeight-1);
			} else {
				drawPanel.setBackground(null);
				g.setPaint(TRANSPARENCY);
				g.fillRect(0, 0, canvasWidth, canvasHeight);
			}
			
			canvasImage = VolatileImageHelper.clearImage(canvasImage);
			if (((StretchIcon) sketchpad.getIcon()).getImage() != canvasImage) {
				sketchpad.setIcon(new StretchIcon(drawnToScreenImage, true));
			}
			Graphics2D canvasG = canvasImage.createGraphics();

			do {
				canvasG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				if (currentGame.getType() == ImageType.BITMAP && importedImage != null) {
					canvasG.drawImage(importedImage, 0, 0, canvasWidth, canvasHeight, null);
				}
				
				lines.stream()
					.skip(currentGame.getType() == ImageType.BITMAP ? importLines : 0)
					.forEach(line -> {
						canvasG.setColor(Color.decode(line.getColor()));
						canvasG.setStroke(new BasicStroke(Math.max(1, line.getThickness() + (currentGame == SupportedGames.CHAMPD_UP ? 4 : 0)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						for (int i = 0; i+1 < line.points.size(); i++) {
							LinePoint p1 = line.points.get(i);
							LinePoint p2 = line.points.get(i+1);
							canvasG.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
						}
					});
				
				canvasG.dispose();
			} while (canvasImage.contentsLost());
			
			g.drawImage(canvasImage, 0, 0, null);
			g.dispose();
		} while (drawnToScreenImage.contentsLost());
		
		sketchpad.repaint();
	}
    
	/**
	 * Returns if an array contains a given value
	 * @param arr The array to check
	 * @param obj Value to check if the array contains
	 * @return Boolean if the array contains the given value
	 */
	private <T> boolean arrayContains(T[] arr, T obj) {
		return Arrays.stream(arr).anyMatch(obj::equals);
	}
	
	/**
	 * Returns black or white, whichever best contrasts the input color.
	 * @param color Input color.
	 * @return Either {@link Color.BLACK} or {@link Color.WHITE}
	 */
	private Color getContrastColor(Color color) {
		double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
		return y >= 128 ? Color.BLACK : Color.WHITE;
	}

	/**
	 * Returns the current canvas width, according to the current game.
	 * @return The current canvas width
	 */
	public int getCanvasWidth() {
		return currentGame.getCanvasWidth();
	}
	/**
	 * Returns the current canvas height, according to the current game.
	 * @return The current canvas height
	 */
	public int getCanvasHeight() {
		return currentGame.getCanvasHeight();
	}
	
	
    //Initialization Functions//
    
    public static void main(String[] args) {
    	System.setProperty("sun.java2d.opengl", "true");
	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch(Exception ex) {
	        ex.printStackTrace();
	    }
		INSTANCE = new JackboxDrawer();
	}
	
	public JackboxDrawer() {
		initialize();
		websocketServer = new WebsocketServer(this);
		websocketServer.start();
		window.setVisible(true);
		drawPanel.grabFocus();
		saveHistory();
		repaintImage(true);
	}
	
	/**
	 * Initializes all components & the main window.
	 */
	private void initialize() {
		window = new JFrame();
		try {
			window.setIconImage(ImageIO.read(getClass().getResource("/img/jbd-smol.png")));
		} catch (IOException e1) {
		}
		window.setTitle(PROGRAM_NAME);
		window.setBounds(100, 100, 1062, 689);
		window.setMinimumSize(new Dimension(736, 612));
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		window.getContentPane().setLayout(gridBagLayout);
		
		JPanel mainPanel = new JPanelDnD();
		GridBagConstraints gbc_mainPanel = new GridBagConstraints();
		gbc_mainPanel.fill = GridBagConstraints.BOTH;
		gbc_mainPanel.gridx = 0;
		gbc_mainPanel.gridy = 0;
		window.getContentPane().add(mainPanel, gbc_mainPanel);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[] {0};
		gbl_mainPanel.rowHeights = new int[] {0};
		gbl_mainPanel.columnWeights = new double[]{1.0};
		gbl_mainPanel.rowWeights = new double[]{1.0};
		mainPanel.setLayout(gbl_mainPanel);
		
		JMenuBar menuBar = new JMenuBar();
		window.setJMenuBar(menuBar);
		
		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic('F');
		menuBar.add(menuFile);
		
		JMenu menuEdit = new JMenu("Edit");
		menuEdit.setMnemonic('E');
		menuBar.add(menuEdit);
		
		JMenuItem mntmImportFromImage = new JMenuItem("Import from Image");
		mntmImportFromImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mntmImportFromImage.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/import.png")));
		mntmImportFromImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				promptImportFromImage();
			}
		});
		menuFile.add(mntmImportFromImage);
		
		JMenuItem mntmImportFromUrl = new JMenuItem("Import from URL");
		mntmImportFromUrl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				promptImportFromURL();
			}
		});
		mntmImportFromUrl.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		mntmImportFromUrl.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/link.png")));
		menuFile.add(mntmImportFromUrl);
		
		JSeparator separator_2_1 = new JSeparator();
		menuFile.add(separator_2_1);
		
		JMenuItem mntmExportToGame = new JMenuItem("Export to Game");
		mntmExportToGame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exportToGame();
			}
		});	
		mntmExportToGame.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/export.png")));
		mntmExportToGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		menuFile.add(mntmExportToGame);
		
		JMenuItem mntmClearCanvas = new JMenuItem("Clear Canvas");
		mntmClearCanvas.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/clear.png")));
		mntmClearCanvas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearCanvas();
			}
		});
		
		mntmUndoButton = new JMenuItem("Undo");
		mntmUndoButton.setEnabled(false);
		mntmUndoButton.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/undo.png")));
		mntmUndoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				undo();
			}
		});
		mntmUndoButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
		menuEdit.add(mntmUndoButton);
		
		mntmRedoButton = new JMenuItem("Redo");
		mntmRedoButton.setEnabled(false);
		mntmRedoButton.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/redo.png")));
		mntmRedoButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				redo();
			}
		});
		mntmRedoButton.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
		menuEdit.add(mntmRedoButton);
		
		JSeparator separator_2 = new JSeparator();
		menuEdit.add(separator_2);
		menuEdit.add(mntmClearCanvas);
		
		JMenu menuGame = new JMenu("Select Game");
		menuGame.setMnemonic('G');
		menuBar.add(menuGame);
		
		ButtonGroup gameButtons = new ButtonGroup();
		
		ActionListener gameRadioButtonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gameSelectionButtons.entrySet().stream()
					.filter(entry -> entry.getValue().equals(e.getSource()))
					.findAny()
					.map(Entry::getKey)
					.ifPresent(JackboxDrawer.this::changeGame);
			}
		};
		
		JRadioButtonMenuItem rdbtnmntmDrawful_1 = new JRadioButtonMenuItem("Drawful 1");
		gameSelectionButtons.put(SupportedGames.DRAWFUL_1, rdbtnmntmDrawful_1);
		rdbtnmntmDrawful_1.addActionListener(gameRadioButtonListener);
		gameButtons.add(rdbtnmntmDrawful_1);
		menuGame.add(rdbtnmntmDrawful_1);
		
		JRadioButtonMenuItem rdbtnmntmDrawful_2 = new JRadioButtonMenuItem("Drawful 2 [PATCHED]");
		rdbtnmntmDrawful_2.setSelected(true);
		gameSelectionButtons.put(SupportedGames.DRAWFUL_2, rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.addActionListener(gameRadioButtonListener);
		gameButtons.add(rdbtnmntmDrawful_2);
		menuGame.add(rdbtnmntmDrawful_2);
		
		JRadioButtonMenuItem rdbtnmntmBidiots = new JRadioButtonMenuItem("Bidiots");
		gameSelectionButtons.put(SupportedGames.BIDIOTS, rdbtnmntmBidiots);
		rdbtnmntmBidiots.addActionListener(gameRadioButtonListener);
		menuGame.add(rdbtnmntmBidiots);
		gameButtons.add(rdbtnmntmBidiots);
		
		JRadioButtonMenuItem rdbtnmntmTeeKo = new JRadioButtonMenuItem("Tee K.O.");
		gameSelectionButtons.put(SupportedGames.TEE_KO, rdbtnmntmTeeKo);
		rdbtnmntmTeeKo.addActionListener(gameRadioButtonListener);
		gameButtons.add(rdbtnmntmTeeKo);
		menuGame.add(rdbtnmntmTeeKo);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_1 = new JRadioButtonMenuItem("Trivia Murder Party 1");
		gameSelectionButtons.put(SupportedGames.TRIVIA_MURDER_PARTY_1, rdbtnmntmTriviaMurderParty_1);
		rdbtnmntmTriviaMurderParty_1.addActionListener(gameRadioButtonListener);
		menuGame.add(rdbtnmntmTriviaMurderParty_1);
		gameButtons.add(rdbtnmntmTriviaMurderParty_1);
		
		JRadioButtonMenuItem rdbtnmntmPushTheButton = new JRadioButtonMenuItem("Push the Button");
		gameSelectionButtons.put(SupportedGames.PUSH_THE_BUTTON, rdbtnmntmPushTheButton);
		rdbtnmntmPushTheButton.addActionListener(gameRadioButtonListener);
		menuGame.add(rdbtnmntmPushTheButton);
		gameButtons.add(rdbtnmntmPushTheButton);
		
		/*
		JRadioButtonMenuItem rdbtnmntmPatentlyStupid = new JRadioButtonMenuItem("Patently Stupid");
		gameSelectionButtons.put(SupportedGames.PATENTLYSTUPID, rdbtnmntmPatentlyStupid);
		rdbtnmntmPatentlyStupid.addActionListener(gameRadioButtonListener);
		menuGame.add(rdbtnmntmPatentlyStupid);
		gameButtons.add(rdbtnmntmPatentlyStupid);
		*/
		
		JRadioButtonMenuItem rdbtnmntmChampdUp = new JRadioButtonMenuItem("Champ'd Up");
		gameSelectionButtons.put(SupportedGames.CHAMPD_UP, rdbtnmntmChampdUp);
		rdbtnmntmChampdUp.addActionListener(gameRadioButtonListener);
		gameButtons.add(rdbtnmntmChampdUp);
		menuGame.add(rdbtnmntmChampdUp);
		
		JMenu menuAbout = new JMenu("About");
		menuAbout.setMnemonic('A');
		menuBar.add(menuAbout);
		
		JMenuItem mntmGreasyforkPage = new JMenuItem("GreasyFork Page");
		mntmGreasyforkPage.setIcon(new ImageIcon(getClass().getResource("/img/greasyfork.png")));
		mntmGreasyforkPage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openUrl("https://greasyfork.org/en/scripts/406893-jackboxdrawer");
			}
		});
		menuAbout.add(mntmGreasyforkPage);
		
		JMenuItem mntmGithub = new JMenuItem("GitHub Page");
		mntmGithub.setIcon(new ImageIcon(getClass().getResource("/img/github.png")));
		mntmGithub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openUrl("https://github.com/ipodtouch0218/JackboxDrawer");
			}
		});
		menuAbout.add(mntmGithub);
		
		drawPanel = new JPanel();
		drawPanel.setLayout(new BorderLayout(0, 0));
		drawPanel.setMinimumSize(new Dimension(100,50));
		
		JPanel settingsPanel = new JPanel();
		GridBagLayout gbl_settingsPanel = new GridBagLayout();
		gbl_settingsPanel.columnWidths = new int[] {0, 0};
		gbl_settingsPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_settingsPanel.columnWeights = new double[]{0.0, 4.9E-324};
		gbl_settingsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		settingsPanel.setLayout(gbl_settingsPanel);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawPanel, settingsPanel);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(1.0);
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.insets = new Insets(0, 0, 0, 5);
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 0;
		mainPanel.add(splitPane, gbc_splitPane);
		
		JLabel lblBrushSettings = new JLabel("Brush Settings");
		GridBagConstraints gbc_lblBrushSettings = new GridBagConstraints();
		gbc_lblBrushSettings.gridwidth = 2;
		gbc_lblBrushSettings.insets = new Insets(0, 10, 5, 0);
		gbc_lblBrushSettings.gridx = 0;
		gbc_lblBrushSettings.gridy = 0;
		settingsPanel.add(lblBrushSettings, gbc_lblBrushSettings);
		
		brushColorPicker = new JColorChooser();
		GridBagConstraints gbc_brushColorPicker = new GridBagConstraints();
		gbc_brushColorPicker.anchor = GridBagConstraints.WEST;
		gbc_brushColorPicker.gridwidth = 2;
		gbc_brushColorPicker.insets = new Insets(0, 0, 5, 0);
		gbc_brushColorPicker.gridx = 0;
		gbc_brushColorPicker.gridy = 1;
		settingsPanel.add(brushColorPicker, gbc_brushColorPicker);
		brushColorPicker.setBackground(SystemColor.menu);
		brushColorPicker.setColor(Color.black);
		brushColorPicker.setPreviewPanel(new JPanel());
		brushColorPicker.setChooserPanels(new AbstractColorChooserPanel[]{brushColorPicker.getChooserPanels()[1]});
		removeAlphaSpinner(brushColorPicker);
		
		JSeparator sep2 = new JSeparator();
		GridBagConstraints gbc_sep2 = new GridBagConstraints();
		gbc_sep2.fill = GridBagConstraints.HORIZONTAL;
		gbc_sep2.gridwidth = 2;
		gbc_sep2.insets = new Insets(0, 0, 5, 0);
		gbc_sep2.gridx = 0;
		gbc_sep2.gridy = 2;
		settingsPanel.add(sep2, gbc_sep2);
		
		
		JLabel lblBrushThickness = new JLabel("Brush Thickness");
		GridBagConstraints gbc_lblBrushThickness = new GridBagConstraints();
		gbc_lblBrushThickness.fill = GridBagConstraints.VERTICAL;
		gbc_lblBrushThickness.insets = new Insets(0, 10, 5, 5);
		gbc_lblBrushThickness.gridx = 0;
		gbc_lblBrushThickness.gridy = 3;
		settingsPanel.add(lblBrushThickness, gbc_lblBrushThickness);
		
		JSpinner thicknessSpinner = new JSpinner();
		GridBagConstraints gbc_thicknessSpinner = new GridBagConstraints();
		gbc_thicknessSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_thicknessSpinner.anchor = GridBagConstraints.NORTH;
		gbc_thicknessSpinner.insets = new Insets(0, 25, 5, 25);
		gbc_thicknessSpinner.gridx = 1;
		gbc_thicknessSpinner.gridy = 3;
		settingsPanel.add(thicknessSpinner, gbc_thicknessSpinner);
		thicknessSpinner.setModel(new SpinnerNumberModel(6, 1, null, 1));
		
		champdUpPanel = new JPanel();
		GridBagConstraints gbc_champdUpPanel = new GridBagConstraints();
		gbc_champdUpPanel.fill = GridBagConstraints.BOTH;
		gbc_champdUpPanel.gridwidth = 2;
		gbc_champdUpPanel.insets = new Insets(0, 0, 0, 5);
		gbc_champdUpPanel.gridx = 0;
		gbc_champdUpPanel.gridy = 4;
		settingsPanel.add(champdUpPanel, gbc_champdUpPanel);
		champdUpPanel.setVisible(false);
		GridBagLayout gbl_champdUpPanel = new GridBagLayout();
		gbl_champdUpPanel.columnWidths = new int[] {0, 0};
		gbl_champdUpPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_champdUpPanel.columnWeights = new double[]{0.0, 1.0, 0.0};
		gbl_champdUpPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		champdUpPanel.setLayout(gbl_champdUpPanel);
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 3;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 0;
		champdUpPanel.add(separator, gbc_separator);
		
		JLabel lblChampdUpName = new JLabel("Champ'd Up");
		GridBagConstraints gbc_lblChampdUpName = new GridBagConstraints();
		gbc_lblChampdUpName.gridwidth = 3;
		gbc_lblChampdUpName.insets = new Insets(0, 0, 5, 0);
		gbc_lblChampdUpName.gridx = 0;
		gbc_lblChampdUpName.gridy = 1;
		champdUpPanel.add(lblChampdUpName, gbc_lblChampdUpName);
		
		JLabel lblChallengerName = new JLabel("Challenger Name");
		GridBagConstraints gbc_lblChallengerName = new GridBagConstraints();
		gbc_lblChallengerName.insets = new Insets(0, 10, 0, 5);
		gbc_lblChallengerName.anchor = GridBagConstraints.EAST;
		gbc_lblChallengerName.gridx = 0;
		gbc_lblChallengerName.gridy = 2;
		champdUpPanel.add(lblChallengerName, gbc_lblChallengerName);
		
		txtChampdUpName = new HintTextField("Enter Challenger Name");
		txtChampdUpName.setToolTipText("");
		GridBagConstraints gbc_txtChampdUpName = new GridBagConstraints();
		gbc_txtChampdUpName.insets = new Insets(0, 0, 0, 5);
		gbc_txtChampdUpName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtChampdUpName.gridx = 1;
		gbc_txtChampdUpName.gridy = 2;
		champdUpPanel.add(txtChampdUpName, gbc_txtChampdUpName);
		txtChampdUpName.setColumns(10);
		
		JButton btnSubmitName = new JButton("Submit Name");
		btnSubmitName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!txtChampdUpName.getText().isEmpty()) {
					String out = txtChampdUpName.getText();
					if (out.toLowerCase().startsWith("\u0073\u0061\u006E\u0073")) {
						BufferedImage newImg = new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
						Graphics2D g = newImg.createGraphics();
						g.drawImage(window.getIconImage(), 0, 0, null);
						g.setColor(new Color(0,255,255));
						g.fillOval(16, 5, 4, 4);
						g.dispose();
						window.setIconImage(newImg);
					}
					out = out.trim().replaceAll("['\"]", "\\$0");
					getWebsocketServer().broadcast("SUBMITNAME;data.val = '" + out + "'");
					
					sentChampdUpName = true;
				}
			}
		});
		GridBagConstraints gbc_btnSubmitName = new GridBagConstraints();
		gbc_btnSubmitName.insets = new Insets(0, 0, 0, 5);
		gbc_btnSubmitName.gridx = 2;
		gbc_btnSubmitName.gridy = 2;
		champdUpPanel.add(btnSubmitName, gbc_btnSubmitName);
		
		teekoPanel = new JPanel();
		GridBagConstraints gbc_teekoPanel = new GridBagConstraints();
		gbc_teekoPanel.fill = GridBagConstraints.BOTH;
		gbc_teekoPanel.gridwidth = 2;
		gbc_teekoPanel.gridx = 0;
		gbc_teekoPanel.gridy = 4;
		settingsPanel.add(teekoPanel, gbc_teekoPanel);
		teekoPanel.setVisible(false);
		GridBagLayout gbl_teekoPanel = new GridBagLayout();
		gbl_teekoPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_teekoPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_teekoPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_teekoPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		teekoPanel.setLayout(gbl_teekoPanel);
		
		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 6;
		gbc_separator_1.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 0;
		teekoPanel.add(separator_1, gbc_separator_1);
		
		JLabel lblTeeKoBackground = new JLabel("Tee K.O. Background Color");
		GridBagConstraints gbc_lblTeeKoBackground = new GridBagConstraints();
		gbc_lblTeeKoBackground.gridwidth = 6;
		gbc_lblTeeKoBackground.insets = new Insets(0, 0, 5, 0);
		gbc_lblTeeKoBackground.gridx = 0;
		gbc_lblTeeKoBackground.gridy = 1;
		teekoPanel.add(lblTeeKoBackground, gbc_lblTeeKoBackground);
		
		shirtBackgroundColorPicker = new JColorChooser();
		GridBagConstraints gbc_teeKOBackgroundColorPicker = new GridBagConstraints();
		gbc_teeKOBackgroundColorPicker.gridwidth = 6;
		gbc_teeKOBackgroundColorPicker.insets = new Insets(0, 0, 5, 0);
		gbc_teeKOBackgroundColorPicker.gridx = 0;
		gbc_teeKOBackgroundColorPicker.gridy = 2;
		teekoPanel.add(shirtBackgroundColorPicker, gbc_teeKOBackgroundColorPicker);
		shirtBackgroundColorPicker.setPreviewPanel(new JPanel());
		shirtBackgroundColorPicker.setChooserPanels(new AbstractColorChooserPanel[]{shirtBackgroundColorPicker.getChooserPanels()[1]});
		shirtBackgroundColorPicker.setColor(TEEKO_BG_COLORS[0]);
		removeAlphaSpinner(shirtBackgroundColorPicker);
		ActionListener teeKoButtonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				shirtBackgroundColorPicker.setColor(((Component) e.getSource()).getBackground());
			}
		};
		
		JButton btnBlue = new JButton("Blue");
		btnBlue.addActionListener(teeKoButtonListener);
		btnBlue.setBackground(TEEKO_BG_COLORS[0]);
		GridBagConstraints gbc_btnBlue = new GridBagConstraints();
		gbc_btnBlue.insets = new Insets(0, 10, 5, 5);
		gbc_btnBlue.gridx = 0;
		gbc_btnBlue.gridy = 3;
		teekoPanel.add(btnBlue, gbc_btnBlue);
		
		JButton btnGray = new JButton("Gray");
		btnGray.addActionListener(teeKoButtonListener);
		btnGray.setBackground(TEEKO_BG_COLORS[1]);
		GridBagConstraints gbc_btnGray = new GridBagConstraints();
		gbc_btnGray.insets = new Insets(0, 0, 5, 5);
		gbc_btnGray.gridx = 1;
		gbc_btnGray.gridy = 3;
		teekoPanel.add(btnGray, gbc_btnGray);
		
		JButton btnBlack = new JButton("Black");
		btnBlack.addActionListener(teeKoButtonListener);
		btnBlack.setBackground(TEEKO_BG_COLORS[2]);
		GridBagConstraints gbc_btnBlack = new GridBagConstraints();
		gbc_btnBlack.insets = new Insets(0, 0, 5, 5);
		gbc_btnBlack.gridx = 2;
		gbc_btnBlack.gridy = 3;
		teekoPanel.add(btnBlack, gbc_btnBlack);
		
		JButton btnRed = new JButton("Red");
		btnRed.addActionListener(teeKoButtonListener);
		btnRed.setBackground(TEEKO_BG_COLORS[3]);
		GridBagConstraints gbc_btnRed = new GridBagConstraints();
		gbc_btnRed.insets = new Insets(0, 0, 5, 5);
		gbc_btnRed.gridx = 3;
		gbc_btnRed.gridy = 3;
		teekoPanel.add(btnRed, gbc_btnRed);
		
		JButton btnOlive = new JButton("Olive");
		btnOlive.addActionListener(teeKoButtonListener);
		btnOlive.setBackground(TEEKO_BG_COLORS[4]);
		GridBagConstraints gbc_btnOlive = new GridBagConstraints();
		gbc_btnOlive.insets = new Insets(0, 0, 5, 5);
		gbc_btnOlive.anchor = GridBagConstraints.WEST;
		gbc_btnOlive.gridx = 4;
		gbc_btnOlive.gridy = 3;
		teekoPanel.add(btnOlive, gbc_btnOlive);
		
		lblShirtWarning = new JLabel("You cannot purchase this shirt because it has a custom background color!");
		lblShirtWarning.setForeground(new Color(204, 0, 0));
		lblShirtWarning.setVisible(false);
		GridBagConstraints gbc_lblShirtWarning = new GridBagConstraints();
		gbc_lblShirtWarning.insets = new Insets(0, 0, 5, 0);
		gbc_lblShirtWarning.gridwidth = 6;
		gbc_lblShirtWarning.gridx = 0;
		gbc_lblShirtWarning.gridy = 4;
		teekoPanel.add(lblShirtWarning, gbc_lblShirtWarning);
		
		lblContrastWarning = new JLabel("This background color might make the caption hard to read, or even unreadable!");
		lblContrastWarning.setForeground(new Color(204, 0, 0));
		lblContrastWarning.setVisible(false);
		GridBagConstraints gbc_lblThisBackgroundColor = new GridBagConstraints();
		gbc_lblThisBackgroundColor.gridwidth = 6;
		gbc_lblThisBackgroundColor.insets = new Insets(0, 0, 0, 5);
		gbc_lblThisBackgroundColor.gridx = 0;
		gbc_lblThisBackgroundColor.gridy = 5;
		teekoPanel.add(lblContrastWarning, gbc_lblThisBackgroundColor);
		
		sketchpad = new JLabel("");
		sketchpad.setHorizontalAlignment(SwingConstants.CENTER);
		sketchpad.setIcon(new StretchIcon(drawnToScreenImage, true));
		sketchpad.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * getCanvasWidth());
				y = (int) (((double) y / (double) si.getH()) * getCanvasHeight());
				
				previousMousePos = e.getPoint();
				
				if (x < 0 || x >= canvasImage.getWidth() || y < 0 || y >= canvasImage.getHeight()) {
					return;
				}
				
				if (e.getButton() == MouseEvent.BUTTON1 && !erasing) {
					drawing = true;
					previousMousePos = e.getPoint();
					
					undoRedoHistory.removeAfter(currentHistoryIndex);
					
					int thickness = (int) thicknessSpinner.getValue();
					
					JackboxLine newLine = new JackboxLine(thickness, brushColorPicker.getColor());
					lines.add(newLine);
					
					newLine.points.add(new LinePoint(x,y));
					newLine.points.add(new LinePoint(x,y));
				} else if (e.getButton() == MouseEvent.BUTTON3 && !drawing) {
					erasing = true;
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				if (e.getButton() == MouseEvent.BUTTON1 && drawing) {
					if (lines.isEmpty()) return;
					drawing = false;
					int x = e.getX() - si.getXOffset();
					int y = e.getY() - si.getYOffset();
					x = (int) (((double) x / (double) si.getW()) * getCanvasWidth());
					y = (int) (((double) y / (double) si.getH()) * getCanvasHeight());
					
					if (x < 0 || x >= canvasImage.getWidth() || y < 0 || y >= canvasImage.getHeight()) {
						return;
					}
					JackboxLine line = lines.get(lines.size()-1);
					line.points.add(new LinePoint(x,y));
					
					saveHistory();
					repaintImage(true);
				} else if (e.getButton() == MouseEvent.BUTTON3 && erasing) {
					erasing = false;
					
					if (lines.size() != undoRedoHistory.get(currentHistoryIndex).size())
						saveHistory();
				}
			}
		});
		sketchpad.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * getCanvasWidth());
				y = (int) (((double) y / (double) si.getH()) * getCanvasHeight());
				
				
				if (erasing) {
					boolean removedOne = false;
					int count = 0;
					Iterator<JackboxLine> it = lines.iterator();
					while (it.hasNext()) {
						JackboxLine line = it.next();
						if (count++ < importLines) {
							continue;
						}
						
						for (int i = 0; i < line.getPoints().length-1; i++) {
							LinePoint pointA = line.getPoints()[i];
							LinePoint pointB = line.getPoints()[i+1];
							double halfThick = line.getThickness()/2d;
							
							Line2D.Double line2d = new Line2D.Double(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
							
							if (line2d.intersects(x-halfThick, y-halfThick, line.getThickness(), line.getThickness())) {
								it.remove();
								removedOne = true;
								break;
							}
						}
					}
					if (removedOne) {
						repaintImage(true);
					}
				} else if (drawing) {
					JackboxLine line = lines.get(lines.size()-1);
					if (x < 0 || x >= getCanvasWidth() || y < 0 || y >= getCanvasHeight()) {
						return;
					}
					LinePoint newPoint = new LinePoint(x,y);
					if (!line.points.get(line.points.size()-1).equals(newPoint)) {
						line.points.add(newPoint);
					}
					repaintImage(false);
				}
				previousMousePos = e.getPoint();
			}
		});
		sketchpad.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		drawPanel.add(sketchpad, BorderLayout.CENTER);
	}
	
	private void removeAlphaSpinner(JColorChooser chooser) {
		Container container = ((Container) chooser.getChooserPanels()[0].getComponents()[0]);
		int spinners = 0;
		int sliders = 0;
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component comp = container.getComponent(i);
			if (comp instanceof JSlider) {
				if (sliders++ >= 3) {
					container.remove(comp);
					//Remove alpha slider
					i--;
					continue;
				}
				((JSlider) comp).addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent arg0) {
						repaintImage(true);
					}
				});
			} else if (comp instanceof JSpinner) {
				if (spinners++ >= 3) {
					container.remove(comp);
					i--;
					continue;
				}
				((JSpinner) comp).addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent arg0) {
						repaintImage(true);
					}
				});
			} else if (comp instanceof JLabel) {
				container.remove(comp);
				i--;
			} 
		}
	}
}
