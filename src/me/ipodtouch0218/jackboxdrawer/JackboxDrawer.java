package me.ipodtouch0218.jackboxdrawer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import me.ipodtouch0218.jackboxdrawer.SupportedGames.ImageType;
import me.ipodtouch0218.jackboxdrawer.obj.Line;
import me.ipodtouch0218.jackboxdrawer.obj.Point;
import me.ipodtouch0218.jackboxdrawer.uielements.HintTextField;
import me.ipodtouch0218.jackboxdrawer.uielements.JPanelDnD;
import me.ipodtouch0218.jackboxdrawer.uielements.StretchIcon;

public class JackboxDrawer {

	public static JackboxDrawer INSTANCE;
	
	//Constants//
	
	public static final String VERSION = "1.3.0";
	public static final String PROGRAM_NAME = "Jackbox Drawer v" + VERSION;
	private static final List<Color> TEEKO_BG_COLORS = Arrays.asList(new Color[]{new Color(40, 85, 135), new Color(95, 98, 103), new Color(8, 8, 8), new Color(117, 14, 30), new Color(98, 92, 74)});
	private static int CANVAS_WIDTH = 240, CANVAS_HEIGHT = 300;
	private static final double 
	VECTOR_IMPORT_SCALE_FACTOR = 3, 
	COLOR_WEIGHTING = 1.0,
	DISTANCE_WEIGHTING = 0,
	STRIP_MATCH = 0.5,
	MIN_COLOR_DIST = 55;
	private static final BufferedImage TRANSPARENT_TEXTURE = new BufferedImage(2,2,BufferedImage.TYPE_BYTE_GRAY);
	static {
		Graphics2D g = TRANSPARENT_TEXTURE.createGraphics();
		g.setColor(new Color(244,244,244));
		g.fillRect(0, 0, 2, 2);
		g.setColor(new Color(224,224,224));
		g.fillRect(1, 0, 2, 1);
		g.fillRect(0, 1, 1, 2);
		g.dispose();
	}
	
	//Global Variables//
	
	JFrame window;
	JPanel teekoPanel, champdUpPanel, drawPanel, mainPanel;
	private JSeparator sep1, sep2;
	WebsocketServer websocketServer;
	SupportedGames currentGame = SupportedGames.DRAWFUL_2;
	EnumMap<SupportedGames, JRadioButtonMenuItem> gameSelectionButtons = new EnumMap<>(SupportedGames.class);
	JColorChooser brushChooser, teeKOColorPicker;
	private JMenuBar menuBar;
	private JMenu menuFile, menuEdit, menuGame, menuAbout;
	private JMenuItem mntmRedo, mntmUndo;
	public JLabel sketchpad, lblBrushThickness, lblBrushSettings;
	private JLabel lblShirtWarning;
	BufferedImage drawnToScreenImage = new BufferedImage(CANVAS_WIDTH*2,CANVAS_HEIGHT*2, BufferedImage.TYPE_INT_RGB), rasterBackgroundImage, actualImage;
	private boolean drawing, erasing;
	int importLines, currentLine;
	List<Line> lines = new ArrayList<>();
	HintTextField txtChampdUpName;
	
	//Callback Functions & Classes//
	
	public void promptImportFromImage() { //Called when File > Import from Image or Ctrl + I
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Images", "png", "jpg", "jpeg");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
		Action details = chooser.getActionMap().get("viewTypeDetails");
		if (details != null)
			details.actionPerformed(null);
	    int returnVal = chooser.showOpenDialog(window);
	    if (returnVal == JFileChooser.APPROVE_OPTION) 
	    	tryImportFile(chooser.getSelectedFile());
	}
	
	public void promptImportFromURL() {
		String path = JOptionPane.showInputDialog("Enter an image URL.");
		if (path == null || path.isEmpty()) 
			return;
		if (path.startsWith("data:image")) {
			//Shoot, this is base64. Oh well, handle it anyway.
			String data = path.split(",")[1];
			try {
				tryImportFile(decodeToImage(data));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(window, "URL could not be read.\n", "Read Error!", JOptionPane.ERROR_MESSAGE);
			}
			return;
		}
		tryImportFile(path);
	}
	
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
	
	public void exportToGame() { //Called when File > Export to Game or Ctrl + E
		if (currentGame == SupportedGames.CHAMPD_UP) {
			if (txtChampdUpName.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(window, "You must enter a Challenger Name!", "Export Error!", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		currentGame.export(JackboxDrawer.this);
	}
	
	public boolean clearCanvas() { //Called when Edit > Clear Canvas
		if ((lines.isEmpty() || currentLine <= 0) && rasterBackgroundImage == null) {
			return true;
		}
		if (JOptionPane.showConfirmDialog(window, "Clear the entire canvas?", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			currentLine = 0;
			importLines = 0;
			lines.clear();
			rasterBackgroundImage = null;
			sketchpad.repaint();
			return true;
		}
		return false;
	}
	
	public boolean clearCanvas(int newWidth, int newHeight) {
		if (((lines.isEmpty() || currentLine <= 0) && rasterBackgroundImage == null) || (CANVAS_WIDTH == newWidth && CANVAS_HEIGHT == newHeight)) {
			CANVAS_WIDTH = newWidth;
			CANVAS_HEIGHT = newHeight;
			drawnToScreenImage = new BufferedImage(CANVAS_WIDTH*2,CANVAS_HEIGHT*2, BufferedImage.TYPE_INT_RGB);
			StretchIcon si = new StretchIcon(drawnToScreenImage, true);
			sketchpad.setIcon(si);
			sketchpad.repaint();
			return true;
		}
		if (JOptionPane.showConfirmDialog(window, "The game you are changing to has a differnet canvas size?\nThe canvas must be cleared to continue.\nClear the entire canvas?", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			currentLine = 0;
			importLines = 0;
			lines.clear();
			rasterBackgroundImage = null;
			CANVAS_WIDTH = newWidth;
			CANVAS_HEIGHT = newHeight;
			drawnToScreenImage = new BufferedImage(CANVAS_WIDTH*2,CANVAS_HEIGHT*2, BufferedImage.TYPE_INT_RGB);
			StretchIcon si = new StretchIcon(drawnToScreenImage, true);
			sketchpad.setIcon(si);
			sketchpad.repaint();
			return true;
		}
		return false;
	}
	
	public void undoDraw() { //Called when Edit >  Undo or Ctrl + Z
		if (drawing || erasing) return;
		if (currentLine > 0 && currentLine > importLines) {
			currentLine = Math.max(importLines, currentLine - 1);
			mntmRedo.setEnabled(true);
		}
		mntmUndo.setEnabled(currentLine != 0);
		sketchpad.repaint();
	}
	
	public void redoDraw() { //Called when Edit >  Redo or Ctrl + Y
		if (drawing || erasing || currentLine >= lines.size()) return;
		currentLine++;
		if (currentLine == lines.size()) {
			mntmRedo.setEnabled(false);
		} else {
			mntmRedo.setEnabled(true);
		}
		mntmUndo.setEnabled(true);
		sketchpad.repaint();
	}
	
	public void changeGame(SupportedGames game) { //Called when any of the Select Game radio buttons are pressed
		if (currentGame == game) {
			return;
		}
		if (game == SupportedGames.CHAMPD_UP) {
			if (!clearCanvas(640, 640)) {
				gameSelectionButtons.get(currentGame).setSelected(true);
				return;
			}
		} else {
			if (!clearCanvas(240, 300)) {
				gameSelectionButtons.get(currentGame).setSelected(true);
				return;
			}
		}
		currentGame = game;
		
		teekoPanel.setVisible(game == SupportedGames.TEE_KO);
		champdUpPanel.setVisible(game == SupportedGames.CHAMPD_UP);
		sketchpad.repaint();
	}
	
	//Helper Functions//
	
	public void openUrl(String url) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
		    try {
				Desktop.getDesktop().browse(new URL(url).toURI());
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void tryImportFile(BufferedImage loadedImage) throws IOException {
		if (loadedImage == null)
			throw new IOException("read fail");
		vectorizeImage(loadedImage);
		sketchpad.repaint();
		rasterBackgroundImage = loadedImage;
		JOptionPane.showMessageDialog(window, importLines + " lines drawn.", "Image Loaded", JOptionPane.INFORMATION_MESSAGE); 
	}
	
	public void tryImportFile(File file) {
		try {
    		BufferedImage loadedImage = ImageIO.read(file);
    		tryImportFile(loadedImage);
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(window, "File could not be read.", "Read Error!", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
	}
	
	public void tryImportFile(String url) {
		try {
			File file = new File(new URL(url).toURI());
    		BufferedImage loadedImage = ImageIO.read(file);
    		tryImportFile(loadedImage);
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(window, "URL could not be read.", "Read Error!", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
	}
	
	public int vectorizeImage(BufferedImage loadedImage) {
		
		double sfw = (VECTOR_IMPORT_SCALE_FACTOR / 240f * CANVAS_WIDTH);
		double sfh = (VECTOR_IMPORT_SCALE_FACTOR / 300f * CANVAS_HEIGHT);
		Image tmp = loadedImage.getScaledInstance((int) (CANVAS_WIDTH/sfw), (int) (CANVAS_HEIGHT/sfh), Image.SCALE_SMOOTH);
		loadedImage = new BufferedImage(tmp.getWidth(null), tmp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		
		lines = lines.subList(importLines, currentLine);
		
		Graphics2D g2d = loadedImage.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		
		int lineCount = 0;
		for (int x = 0; x < loadedImage.getWidth(); x++) {
			ArrayList<Line> linePyramid = new ArrayList<Line>();
			int yStart = 0;
			int pixelsInLine = 0;
			int startClr = loadedImage.getRGB(x, 0);
			int[] avgClr = {0,0,0};
			//LOOP START
			for(int y = 0; y < loadedImage.getHeight(); y++) {
				int clr = loadedImage.getRGB(x,y);
				boolean match = COLOR_WEIGHTING*Math.sqrt(colorDistance(clr, startClr)) +
								DISTANCE_WEIGHTING*(y-yStart)/loadedImage.getHeight() <
								MIN_COLOR_DIST;
				if(!match || y+1 == loadedImage.getHeight()) { //significant color change or EOL
					//compute average color
					avgClr[0] /= pixelsInLine;
					avgClr[1] /= pixelsInLine;
					avgClr[2] /= pixelsInLine;
					
					//create new line from last terminal point to current point
					Line strip = new Line( (int) Math.ceil(Math.max(sfw, sfh)), new Color(avgClr[0],avgClr[1],avgClr[2]));
					strip.points.add(
							new Point( (int) (x*sfw+sfw),(int) (yStart*sfh+sfh/2) )
						);
					strip.points.add(
							new Point( (int) (x*sfw+sfw),(int) (y*sfh+sfh/2) )
						);
					linePyramid.add(strip);
					//reset variables to new start
					yStart = y;
					pixelsInLine = 1;
					startClr = clr;
					avgClr[0] = sepRed(clr);
					avgClr[1] = sepGreen(clr);
					avgClr[2] = sepBlue(clr);
				}else { //insignificant color change
					pixelsInLine++;
					avgClr[0] += sepRed(clr);
					avgClr[1] += sepGreen(clr);
					avgClr[2] += sepBlue(clr);
				}
			}
			//LOOP END
			
			//Note: Do not try to optimize be moving variable declerations around.
			
			int i = 0;
			while(i < linePyramid.size()) {
				int j = i + 2;
				while(j < linePyramid.size()) {
					int ci = rgbStringToInt(linePyramid.get(i).color);
					int cj = rgbStringToInt(linePyramid.get(j).color);
					double dist = Math.sqrt(colorDistance(ci,cj));
					if(Math.sqrt(dist)  < STRIP_MATCH) { //colors are similar, combine strips
						Line si = linePyramid.get(i);
						Line sj = linePyramid.get(j);
						int di = si.points.get(1).y - si.points.get(0).y;
						int dj = sj.points.get(1).y - sj.points.get(0).y;
						//create merged line and mix colors
						Line sm = new Line( (int) Math.ceil(VECTOR_IMPORT_SCALE_FACTOR), mixColors(ci,cj, (double)di/(double)(dj+di) ) );
						sm.points.add(si.points.get(0));
						sm.points.add(sj.points.get(1));
						//replace the line in slot i with the merged line
						linePyramid.set(i, sm);
						//move next element to j
						linePyramid.remove(j);
					}else { //move j to next element
						j++;	
					}
				}
				i++;
			}
			
			for(Line l : linePyramid) { //could be done with addAll; done like this for debug count
				lines.add(lineCount++,l);
			}
		}
		currentLine = lines.size();
		importLines = lineCount;
		return importLines;
	}
	
	//given something that contains a hex color, return the int value of the hex color
	private static int rgbStringToInt(String rgb) throws IllegalStateException {
		Matcher m = Pattern.compile("[a-fA-F0-9]+").matcher(rgb);
		m.find();
		return Integer.parseInt(m.group(0), 16);
	}
	//get individual channels
	private static int sepRed(int rgb) {
		return (rgb&0x00FF0000) >> 0x10;
	}
	private static int sepGreen(int rgb) {
		return (rgb&0x0000FF00) >> 0x08;
	}
	private static int sepBlue(int rgb) {
		return rgb&0x000000FF;
	}
	
	//interpolate colors: c = a*t + (t-1)*b
	private static Color mixColors(Color c1, Color c2, double t) {
		return new Color((int) (t*c1.getRed() + (1.0-t)*c2.getRed()),(int) (t*c1.getGreen() + (1.0-t)*c2.getGreen()),(int) (t*c1.getBlue() + (1.0-t)*c2.getBlue()));
	}
	private static Color mixColors(int c1, int c2, double t) {
		return mixColors(new Color(c1), new Color(c2),t);
	}
	
	
	private void repaintImage() {
		Graphics2D g = drawnToScreenImage.createGraphics();
		actualImage = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g1 = actualImage.createGraphics();
		
		if (currentGame == SupportedGames.TEE_KO) {
			g1.setColor(teeKOColorPicker.getColor());
			drawPanel.setBackground(g1.getColor());
			lblShirtWarning.setVisible(!TEEKO_BG_COLORS.contains(g1.getColor()));
			g1.fill(new Rectangle(0,0, CANVAS_WIDTH, CANVAS_HEIGHT));
			g1.setColor(getContrastColor(g1.getColor()));
			g1.setStroke(new BasicStroke(3f));
			g1.drawRect(0, 0, CANVAS_WIDTH-1, CANVAS_HEIGHT-1);
		} else {
			drawPanel.setBackground(null);
			g.setPaint(new TexturePaint(TRANSPARENT_TEXTURE, new Rectangle(0,0,40,40)));
			g.fill(new Rectangle(0,0, CANVAS_WIDTH*2, CANVAS_HEIGHT*2));
		}
		
		if (currentGame.getImageType() == ImageType.BITMAP && rasterBackgroundImage != null) {
			g1.drawImage(rasterBackgroundImage, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
		}
		
		int linesDrawn = 0;
		lines:
		for (Line line : lines) {
			if (linesDrawn++ >= currentLine) break lines;
			if (linesDrawn <= importLines && currentGame.getImageType() == ImageType.BITMAP) continue lines;
			g1.setColor(Color.decode(line.color));
			g1.setStroke(new BasicStroke(line.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (int i = 0; i+1 < line.points.size(); i++) {
				Point p1 = line.points.get(i);
				Point p2 = line.points.get(i+1);
				g1.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
		g.drawImage(actualImage, 0, 0, drawnToScreenImage.getWidth(), drawnToScreenImage.getHeight(), null);
		g.dispose();
		g1.dispose();
	}
	
	private Color getContrastColor(Color color) {
		double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
		return y >= 128 ? Color.black : Color.white;
	}
	
    public static double colorDistance(int c1, int c2) {
        int red1 = (c1 & 0xff0000) >> 16;
        int red2 = (c2 & 0xff0000) >> 16;
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = ((c1 & 0xff00) >> 8) - ((c2 & 0xff00) >> 8);
        int b = (c1 & 0xff) - (c2 & 0xff);
        return (((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8);
    }
    
    //Initialization Functions//
    
    public static void main(String[] args) {
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
	}
	
	private void initialize() {
		window = new JFrame();
		window.setTitle(PROGRAM_NAME);
		window.setBounds(100, 100, 1062, 676);
		window.setMinimumSize(new Dimension(736, 612));
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		window.getContentPane().setLayout(gridBagLayout);
		
		mainPanel = new JPanelDnD();
		GridBagConstraints gbc_mainPanel = new GridBagConstraints();
		gbc_mainPanel.fill = GridBagConstraints.BOTH;
		gbc_mainPanel.gridx = 0;
		gbc_mainPanel.gridy = 0;
		window.getContentPane().add(mainPanel, gbc_mainPanel);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_mainPanel.rowHeights = new int[] {0, 0, 0, 0, 0};
		gbl_mainPanel.columnWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_mainPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
		mainPanel.setLayout(gbl_mainPanel);
		
		menuBar = new JMenuBar();
		window.setJMenuBar(menuBar);
		
		menuFile = new JMenu("File");
		menuFile.setMnemonic('F');
		menuBar.add(menuFile);
		
		menuEdit = new JMenu("Edit");
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
		
		mntmUndo = new JMenuItem("Undo");
		mntmUndo.setEnabled(false);
		mntmUndo.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/undo.png")));
		mntmUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				undoDraw();
			}
		});
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
		menuEdit.add(mntmUndo);
		
		mntmRedo = new JMenuItem("Redo");
		mntmRedo.setEnabled(false);
		mntmRedo.setIcon(new ImageIcon(getClass().getResource("/img/teenyicons/redo.png")));
		mntmRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				redoDraw();
			}
		});
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
		menuEdit.add(mntmRedo);
		
		JSeparator separator_2 = new JSeparator();
		menuEdit.add(separator_2);
		menuEdit.add(mntmClearCanvas);
		
		menuGame = new JMenu("Select Game");
		menuGame.setMnemonic('G');
		menuBar.add(menuGame);
		
		ButtonGroup gameButtons = new ButtonGroup();
		
		JRadioButtonMenuItem rdbtnmntmDrawful_1 = new JRadioButtonMenuItem("Drawful 1");
		gameSelectionButtons.put(SupportedGames.DRAWFUL_1, rdbtnmntmDrawful_1);
		rdbtnmntmDrawful_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.DRAWFUL_1);
			}
		});
		gameButtons.add(rdbtnmntmDrawful_1);
		menuGame.add(rdbtnmntmDrawful_1);
		
		JRadioButtonMenuItem rdbtnmntmDrawful_2 = new JRadioButtonMenuItem("Drawful 2");
		gameSelectionButtons.put(SupportedGames.DRAWFUL_2, rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				changeGame(SupportedGames.DRAWFUL_2);
			}
		});
		gameButtons.add(rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.setSelected(true);
		menuGame.add(rdbtnmntmDrawful_2);
		
		JRadioButtonMenuItem rdbtnmntmBidiots = new JRadioButtonMenuItem("Bidiots");
		gameSelectionButtons.put(SupportedGames.BIDIOTS, rdbtnmntmBidiots);
		rdbtnmntmBidiots.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.BIDIOTS);
			}
		});
		menuGame.add(rdbtnmntmBidiots);
		gameButtons.add(rdbtnmntmBidiots);
		
		JRadioButtonMenuItem rdbtnmntmTeeKo = new JRadioButtonMenuItem("Tee K.O.");
		gameSelectionButtons.put(SupportedGames.TEE_KO, rdbtnmntmTeeKo);
		rdbtnmntmTeeKo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.TEE_KO);
			}
		});
		gameButtons.add(rdbtnmntmTeeKo);
		menuGame.add(rdbtnmntmTeeKo);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_1 = new JRadioButtonMenuItem("Trivia Murder Party 1");
		gameSelectionButtons.put(SupportedGames.TRIVIA_MURDER_PARTY_1, rdbtnmntmTriviaMurderParty_1);
		rdbtnmntmTriviaMurderParty_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.TRIVIA_MURDER_PARTY_1);
			}
		});
		menuGame.add(rdbtnmntmTriviaMurderParty_1);
		gameButtons.add(rdbtnmntmTriviaMurderParty_1);
		
		JRadioButtonMenuItem rdbtnmntmPushTheButton = new JRadioButtonMenuItem("Push the Button");
		gameSelectionButtons.put(SupportedGames.PUSH_THE_BUTTON, rdbtnmntmPushTheButton);
		rdbtnmntmPushTheButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.PUSH_THE_BUTTON);
			}
		});
		menuGame.add(rdbtnmntmPushTheButton);
		gameButtons.add(rdbtnmntmPushTheButton);
		
		JRadioButtonMenuItem rdbtnmntmChampdUp = new JRadioButtonMenuItem("Champ'd Up");
		gameSelectionButtons.put(SupportedGames.CHAMPD_UP, rdbtnmntmChampdUp);
		rdbtnmntmChampdUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.CHAMPD_UP);
			}
		});
		gameButtons.add(rdbtnmntmChampdUp);
		menuGame.add(rdbtnmntmChampdUp);
		
		menuAbout = new JMenu("About");
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
		
		sep1 = new JSeparator();
		sep1.setOrientation(SwingConstants.VERTICAL);
		GridBagConstraints gbc_separator_3 = new GridBagConstraints();
		gbc_separator_3.fill = GridBagConstraints.VERTICAL;
		gbc_separator_3.gridheight = 5;
		gbc_separator_3.insets = new Insets(0, 0, 0, 5);
		gbc_separator_3.gridx = 1;
		gbc_separator_3.gridy = 0;
		mainPanel.add(sep1, gbc_separator_3);
		
		lblBrushSettings = new JLabel("Brush Settings");
		GridBagConstraints gbc_lblBrushSettings = new GridBagConstraints();
		gbc_lblBrushSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblBrushSettings.gridwidth = 2;
		gbc_lblBrushSettings.gridx = 2;
		gbc_lblBrushSettings.gridy = 0;
		mainPanel.add(lblBrushSettings, gbc_lblBrushSettings);
		
		drawPanel = new JPanel();
//		drawPanel.setBorder(null);
		GridBagConstraints gbc_drawPanel = new GridBagConstraints();
		gbc_drawPanel.gridheight = 5;
		gbc_drawPanel.insets = new Insets(0, 0, 0, 5);
		gbc_drawPanel.fill = GridBagConstraints.BOTH;
		gbc_drawPanel.gridx = 0;
		gbc_drawPanel.gridy = 0;
		mainPanel.add(drawPanel, gbc_drawPanel);
		drawPanel.setLayout(new BorderLayout(0, 0));
		
		sep2 = new JSeparator();
		GridBagConstraints gbc_separator_4 = new GridBagConstraints();
		gbc_separator_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_4.gridwidth = 2;
		gbc_separator_4.insets = new Insets(0, 0, 5, 0);
		gbc_separator_4.gridx = 2;
		gbc_separator_4.gridy = 2;
		mainPanel.add(sep2, gbc_separator_4);
		
		
		lblBrushThickness = new JLabel("Brush Thickness");
		GridBagConstraints gbc_lblBrushThickness = new GridBagConstraints();
		gbc_lblBrushThickness.anchor = GridBagConstraints.NORTH;
		gbc_lblBrushThickness.insets = new Insets(0, 0, 5, 5);
		gbc_lblBrushThickness.gridx = 2;
		gbc_lblBrushThickness.gridy = 3;
		mainPanel.add(lblBrushThickness, gbc_lblBrushThickness);
		
		JSpinner thicknessSpinner = new JSpinner();
		thicknessSpinner.setModel(new SpinnerNumberModel(6, 1, null, 1));
		GridBagConstraints gbc_thicknessSpinner = new GridBagConstraints();
		gbc_thicknessSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_thicknessSpinner.ipadx = 20;
		gbc_thicknessSpinner.anchor = GridBagConstraints.NORTH;
		gbc_thicknessSpinner.gridx = 3;
		gbc_thicknessSpinner.gridy = 3;
		mainPanel.add(thicknessSpinner, gbc_thicknessSpinner);
		
		brushChooser = new JColorChooser();
		brushChooser.setBackground(SystemColor.menu);
		brushChooser.setColor(Color.black);
		brushChooser.setPreviewPanel(new JPanel());
		brushChooser.setChooserPanels(new AbstractColorChooserPanel[]{brushChooser.getChooserPanels()[1]});
		Container container = ((Container) brushChooser.getChooserPanels()[0].getComponents()[0]);
		int spinners = 0, sliders = 0;
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component comp = container.getComponent(i);
			if (comp instanceof JSlider) {
				if (sliders++ >= 3) {
					container.remove(comp);
					i--;
					continue;
				}
			} else if (comp instanceof JSpinner) {
				if (spinners++ >= 3) {
					container.remove(comp);
					i--;
					continue;
				}
			} else if (comp instanceof JLabel) {
				container.remove(comp);
				i--;
			}
		}
		GridBagConstraints gbc_brushChooser = new GridBagConstraints();
		gbc_brushChooser.anchor = GridBagConstraints.WEST;
		gbc_brushChooser.gridwidth = 2;
		gbc_brushChooser.insets = new Insets(0, 0, 5, 0);
		gbc_brushChooser.gridx = 2;
		gbc_brushChooser.gridy = 1;
		mainPanel.add(brushChooser, gbc_brushChooser);
		
		sketchpad = new JLabel("") {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				repaintImage();
				super.paintComponent(g);
			}
		};
		sketchpad.setBackground(SystemColor.textHighlight);
		sketchpad.setHorizontalAlignment(SwingConstants.LEFT);
		sketchpad.setVerticalAlignment(SwingConstants.TOP);
		sketchpad.setIcon(new StretchIcon(drawnToScreenImage, true));
		sketchpad.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * CANVAS_WIDTH);
				y = (int) (((double) y / (double) si.getH()) * CANVAS_HEIGHT);
				
				if (x < 0 || x >= drawnToScreenImage.getWidth() || y < 0 || y >= drawnToScreenImage.getHeight()) {
					return;
				}
				
				if (e.getButton() == MouseEvent.BUTTON1 && !erasing) {
					drawing = true;
					if (currentLine == -1) {
						lines.clear();
						currentLine = 0;
					}
					while (currentLine < lines.size()) {
						int i = lines.size()-1;
						lines.remove(i);
					}
					currentLine++;
					
					int thickness = (int) thicknessSpinner.getValue();
					
					Line newLine = new Line(thickness, brushChooser.getColor());
					lines.add(newLine);
					
					newLine.points.add(new Point(x,y));
					sketchpad.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3 && !drawing) {
					erasing = true;
					Point point = new Point(x,y);
					int count = 0;
					Iterator<Line> it = lines.iterator();
					while (it.hasNext()) {
						Line line = it.next();
						if (count++ < importLines) continue;
						if (line.points.contains(point)) {
							it.remove();
							sketchpad.repaint();
							break;
						}
					}
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				if (e.getButton() == MouseEvent.BUTTON1) {
					drawing = false;
					int x = e.getX() - si.getXOffset();
					int y = e.getY() - si.getYOffset();
					x = (int) (((double) x / (double) si.getW()) * CANVAS_WIDTH);
					y = (int) (((double) y / (double) si.getH()) * CANVAS_HEIGHT);
					
					if (x < 0 || x >= drawnToScreenImage.getWidth() || y < 0 || y >= drawnToScreenImage.getHeight()) {
						return;
					}
					Line line = lines.get(lines.size()-1);
					line.points.add(new Point(x,y));
					
					mntmUndo.setEnabled(true);
					mntmRedo.setEnabled(false);
					sketchpad.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					erasing = false;
					mntmUndo.setEnabled(true);
				}
			}
		});
		sketchpad.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				StretchIcon si = (StretchIcon) sketchpad.getIcon();
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * CANVAS_WIDTH);
				y = (int) (((double) y / (double) si.getH()) * CANVAS_HEIGHT);
				
				if (erasing) {
					Point point = new Point(x,y);
					Iterator<Line> it = lines.iterator();
					int count = 0;
					while (it.hasNext()) {
						Line line = it.next();
						if (count++ < importLines) continue;
						if (line.points.contains(point)) {
							it.remove();
							sketchpad.repaint();
							break;
						}
					}
				} else if (drawing) {
					Line line = lines.get(lines.size()-1);
					if (x < 0 || x >= CANVAS_WIDTH || y < 0 || y >= CANVAS_HEIGHT) {
						return;
					}
					Point newPoint = new Point(x,y);
					if (!line.points.get(line.points.size()-1).equals(newPoint)) {
						line.points.add(newPoint);
					}
					sketchpad.repaint();
				}
			}
		});
		drawPanel.add(sketchpad, BorderLayout.CENTER);
		
		
		teekoPanel = new JPanel();
		teekoPanel.setVisible(false);
		GridBagConstraints gbc_teekoPanel = new GridBagConstraints();
		gbc_teekoPanel.gridwidth = 2;
		gbc_teekoPanel.fill = GridBagConstraints.BOTH;
		gbc_teekoPanel.gridx = 2;
		gbc_teekoPanel.gridy = 4;
		mainPanel.add(teekoPanel, gbc_teekoPanel);
		GridBagLayout gbl_teekoPanel = new GridBagLayout();
		gbl_teekoPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_teekoPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_teekoPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_teekoPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		teekoPanel.setLayout(gbl_teekoPanel);
		
		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 6;
		gbc_separator_1.insets = new Insets(0, 0, 5, 5);
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
		
		teeKOColorPicker = new JColorChooser();
		GridBagConstraints gbc_teeKOBackgroundColorPicker = new GridBagConstraints();
		gbc_teeKOBackgroundColorPicker.gridwidth = 6;
		gbc_teeKOBackgroundColorPicker.insets = new Insets(0, 0, 5, 0);
		gbc_teeKOBackgroundColorPicker.gridx = 0;
		gbc_teeKOBackgroundColorPicker.gridy = 2;
		teekoPanel.add(teeKOColorPicker, gbc_teeKOBackgroundColorPicker);
		teeKOColorPicker.setPreviewPanel(new JPanel());
		teeKOColorPicker.setChooserPanels(new AbstractColorChooserPanel[]{teeKOColorPicker.getChooserPanels()[1]});
		teeKOColorPicker.setColor(TEEKO_BG_COLORS.get(0));
		
		JButton btnBlue = new JButton("Blue");
		btnBlue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				teeKOColorPicker.setColor(btnBlue.getBackground());
				sketchpad.repaint();
			}
		});
		btnBlue.setForeground(Color.WHITE);
		btnBlue.setBackground(TEEKO_BG_COLORS.get(0));
		GridBagConstraints gbc_btnBlue = new GridBagConstraints();
		gbc_btnBlue.insets = new Insets(0, 10, 5, 5);
		gbc_btnBlue.gridx = 0;
		gbc_btnBlue.gridy = 3;
		teekoPanel.add(btnBlue, gbc_btnBlue);
		
		JButton btnGray = new JButton("Gray");
		btnGray.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				teeKOColorPicker.setColor(btnGray.getBackground());
				sketchpad.repaint();
			}
		});
		btnGray.setForeground(Color.WHITE);
		btnGray.setBackground(TEEKO_BG_COLORS.get(1));
		GridBagConstraints gbc_btnGray = new GridBagConstraints();
		gbc_btnGray.insets = new Insets(0, 0, 5, 5);
		gbc_btnGray.gridx = 1;
		gbc_btnGray.gridy = 3;
		teekoPanel.add(btnGray, gbc_btnGray);
		
		JButton btnBlack = new JButton("Black");
		btnBlack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				teeKOColorPicker.setColor(btnBlack.getBackground());
				sketchpad.repaint();
			}
		});
		btnBlack.setForeground(Color.WHITE);
		btnBlack.setBackground(TEEKO_BG_COLORS.get(2));
		GridBagConstraints gbc_btnBlack = new GridBagConstraints();
		gbc_btnBlack.insets = new Insets(0, 0, 5, 5);
		gbc_btnBlack.gridx = 2;
		gbc_btnBlack.gridy = 3;
		teekoPanel.add(btnBlack, gbc_btnBlack);
		
		JButton btnRed = new JButton("Red");
		btnRed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				teeKOColorPicker.setColor(btnRed.getBackground());
				sketchpad.repaint();
			}
		});
		btnRed.setForeground(Color.WHITE);
		btnRed.setBackground(TEEKO_BG_COLORS.get(3));
		GridBagConstraints gbc_btnRed = new GridBagConstraints();
		gbc_btnRed.insets = new Insets(0, 0, 5, 5);
		gbc_btnRed.gridx = 3;
		gbc_btnRed.gridy = 3;
		teekoPanel.add(btnRed, gbc_btnRed);
		
		JButton btnOlive = new JButton("Olive");
		btnOlive.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				teeKOColorPicker.setColor(btnOlive.getBackground());
				sketchpad.repaint();
			}
		});
		btnOlive.setForeground(Color.WHITE);
		btnOlive.setBackground(TEEKO_BG_COLORS.get(4));
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
		gbc_lblShirtWarning.gridwidth = 6;
		gbc_lblShirtWarning.gridx = 0;
		gbc_lblShirtWarning.gridy = 4;
		teekoPanel.add(lblShirtWarning, gbc_lblShirtWarning);
		
		champdUpPanel = new JPanel();
		champdUpPanel.setVisible(false);
		GridBagConstraints gbc_champdUpPanel = new GridBagConstraints();
		gbc_champdUpPanel.gridwidth = 2;
		gbc_champdUpPanel.fill = GridBagConstraints.BOTH;
		gbc_champdUpPanel.gridx = 2;
		gbc_champdUpPanel.gridy = 4;
		mainPanel.add(champdUpPanel, gbc_champdUpPanel);
		GridBagLayout gbl_champdUpPanel = new GridBagLayout();
		gbl_champdUpPanel.columnWidths = new int[] {0, 0};
		gbl_champdUpPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_champdUpPanel.columnWeights = new double[]{0.0, 1.0, 0.0};
		gbl_champdUpPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		champdUpPanel.setLayout(gbl_champdUpPanel);
		
		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 2;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 0;
		champdUpPanel.add(separator, gbc_separator);
		
		JLabel lblChampdUpName = new JLabel("Champ'd Up");
		GridBagConstraints gbc_lblChampdUpName = new GridBagConstraints();
		gbc_lblChampdUpName.gridwidth = 3;
		gbc_lblChampdUpName.insets = new Insets(0, 0, 5, 5);
		gbc_lblChampdUpName.gridx = 0;
		gbc_lblChampdUpName.gridy = 1;
		champdUpPanel.add(lblChampdUpName, gbc_lblChampdUpName);
		
		JLabel lblChallengerName = new JLabel("Challenger Name");
		GridBagConstraints gbc_lblChallengerName = new GridBagConstraints();
		gbc_lblChallengerName.insets = new Insets(0, 0, 0, 5);
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
		
		for (Component comp : teeKOColorPicker.getChooserPanels()[0].getComponents()) {
			comp.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					sketchpad.repaint();
				}
			});
		}
		container = ((Container) teeKOColorPicker.getChooserPanels()[0].getComponents()[0]);
		spinners = 0;
		sliders = 0;
		for (int i = 0; i < container.getComponentCount(); i++) {
			Component comp = container.getComponent(i);
			if (comp instanceof JSlider) {
				if (sliders++ >= 3) {
					container.remove(comp);
					i--;
					continue;
				}
				((JSlider) comp).addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent arg0) {
						sketchpad.repaint();
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
						sketchpad.repaint();
					}
				});
			} else if (comp instanceof JLabel) {
				container.remove(comp);
				i--;
			} else {
				comp.addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						sketchpad.repaint();
					}
				});
			}
		}
	}
}
