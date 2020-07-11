package me.ipodtouch0218.jackboxdrawer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
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
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.ipodtouch0218.jackboxdrawer.SupportedGames.ImageType;

public class JackboxDrawer {

	private static final int CANVAS_WIDTH = 240, CANVAS_HEIGHT = 300, VECTOR_IMPORT_SCALE_FACTOR = 5;
	private final BufferedImage transparentTexture = new BufferedImage(2,2,BufferedImage.TYPE_BYTE_GRAY);
	{
		Graphics2D g = transparentTexture.createGraphics();
		g.setColor(new Color(244,244,244));
		g.fillRect(0, 0, 2, 2);
		g.setColor(new Color(224,224,224));
		g.fillRect(1, 0, 2, 1);
		g.fillRect(0, 1, 1, 2);
		g.dispose();
	}
	
	JFrame window;
	WebsocketServer websocketServer;
	SupportedGames currentGame = SupportedGames.DRAWFUL_2;
	EnumMap<SupportedGames, JRadioButtonMenuItem> gameSelectionButtons = new EnumMap<>(SupportedGames.class);
	JColorChooser teeKOBackgroundColorPicker;
	private JMenuItem mntmRedo, mntmUndo;
	private JLabel sketchpad, lblTeeKo;
	BufferedImage drawnToScreenImage = new BufferedImage(CANVAS_WIDTH*2,CANVAS_HEIGHT*2, BufferedImage.TYPE_INT_RGB), rasterBackgroundImage, actualImage;
	private boolean drawing, erasing;
	int importLines, currentLine;
	List<Line> lines = new ArrayList<>();
	
	public static void main(String[] args) {
		new JackboxDrawer();
	}
	
	public JackboxDrawer() {
		initialize();
		websocketServer = new WebsocketServer(this);
		websocketServer.start();
		window.setVisible(true);
		changeGame(SupportedGames.DRAWFUL_2);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		window = new JFrame();
		window.setTitle("Jackbox Drawer v1.0.0");
		window.setBounds(100, 100, 925, 598);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		window.getContentPane().setLayout(gridBagLayout);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		window.getContentPane().add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		panel.setLayout(gbl_panel);
		
		JMenuBar menuBar = new JMenuBar();
		window.setJMenuBar(menuBar);
		
		JMenu mnFile_1 = new JMenu("File");
		menuBar.add(mnFile_1);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmImportFromImage = new JMenuItem("Import from Image");
		mntmImportFromImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mntmImportFromImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
//				if (!lines.isEmpty() && JOptionPane.showConfirmDialog(window, "This action will clear the current canvas. Continue?", "Continue?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
//					return;
//				}
				
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Images", "png", "jpg", "jpeg");
				chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(window);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	File file = chooser.getSelectedFile();
//			    	lines.clear();
//			    	currentLine = 0;
			    	
			    	try {
			    		BufferedImage loadedImage = ImageIO.read(file);
			    		importFromImage(loadedImage);
			    		rasterBackgroundImage = loadedImage;
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			    	sketchpad.repaint();
			    }
			}
		});
		mnFile_1.add(mntmImportFromImage);
		
		JMenuItem mntmExportToGame = new JMenuItem("Export to Game");
		mntmExportToGame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				currentGame.export(JackboxDrawer.this);
			}
		});
		mntmExportToGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		mnFile_1.add(mntmExportToGame);
		
		
		JSeparator separator = new JSeparator();
		mnEdit.add(separator);
		
		JMenuItem mntmClearCanvas = new JMenuItem("Clear Canvas");
		mntmClearCanvas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ((lines.isEmpty() || currentLine <= 0) && rasterBackgroundImage != null) {
					return;
				}
				if (JOptionPane.showConfirmDialog(window, "Clear the entire canvas?", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					currentLine = 0;
					importLines = 0;
					lines.clear();
					rasterBackgroundImage = null;
					sketchpad.repaint();
				}
			}
		});
		
		mntmUndo = new JMenuItem("Undo");
		mntmUndo.setEnabled(false);
		mntmUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (drawing || erasing) return;
				if (currentLine > 0 && currentLine > importLines) {
					currentLine = Math.max(importLines, currentLine - 1);
					mntmRedo.setEnabled(true);
				}
				mntmUndo.setEnabled(currentLine != 0);
				sketchpad.repaint();
			}
		});
		mntmUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
		mnEdit.add(mntmUndo);
		
		mntmRedo = new JMenuItem("Redo");
		mntmRedo.setEnabled(false);
		mntmRedo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
		});
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
		mnEdit.add(mntmRedo);
		
		JSeparator separator_2 = new JSeparator();
		mnEdit.add(separator_2);
		mnEdit.add(mntmClearCanvas);
		
		JMenu mnSelectGame = new JMenu("Select Game");
		menuBar.add(mnSelectGame);
		
		ButtonGroup game = new ButtonGroup();
		
		JRadioButtonMenuItem rdbtnmntmDrawful_1 = new JRadioButtonMenuItem("Drawful 1");
		gameSelectionButtons.put(SupportedGames.DRAWFUL_1, rdbtnmntmDrawful_1);
		rdbtnmntmDrawful_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.DRAWFUL_1);
			}
		});
		game.add(rdbtnmntmDrawful_1);
		mnSelectGame.add(rdbtnmntmDrawful_1);
		
		JRadioButtonMenuItem rdbtnmntmDrawful_2 = new JRadioButtonMenuItem("Drawful 2");
		gameSelectionButtons.put(SupportedGames.DRAWFUL_2, rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				changeGame(SupportedGames.DRAWFUL_2);
			}
		});
		game.add(rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.setSelected(true);
		mnSelectGame.add(rdbtnmntmDrawful_2);
		
		JRadioButtonMenuItem rdbtnmntmBidiots = new JRadioButtonMenuItem("Bidiots");
		gameSelectionButtons.put(SupportedGames.BIDIOTS, rdbtnmntmBidiots);
		rdbtnmntmBidiots.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.BIDIOTS);
			}
		});
		mnSelectGame.add(rdbtnmntmBidiots);
		game.add(rdbtnmntmBidiots);
		
		JRadioButtonMenuItem rdbtnmntmTeeKo = new JRadioButtonMenuItem("Tee K.O.");
		gameSelectionButtons.put(SupportedGames.TEE_KO, rdbtnmntmTeeKo);
		rdbtnmntmTeeKo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.TEE_KO);
			}
		});
		game.add(rdbtnmntmTeeKo);
		mnSelectGame.add(rdbtnmntmTeeKo);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_1 = new JRadioButtonMenuItem("Trivia Murder Party 1");
		rdbtnmntmTriviaMurderParty_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.TRIVIA_MURDER_PARTY_1);
			}
		});
		mnSelectGame.add(rdbtnmntmTriviaMurderParty_1);
		game.add(rdbtnmntmTriviaMurderParty_1);
		
		JRadioButtonMenuItem rdbtnmntmPatentlyStupid = new JRadioButtonMenuItem("Patently Stupid");
		rdbtnmntmPatentlyStupid.setEnabled(false);
		mnSelectGame.add(rdbtnmntmPatentlyStupid);
		game.add(rdbtnmntmPatentlyStupid);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_2 = new JRadioButtonMenuItem("Trivia Murder Party 2");
		rdbtnmntmTriviaMurderParty_2.setEnabled(false);
		mnSelectGame.add(rdbtnmntmTriviaMurderParty_2);
		game.add(rdbtnmntmTriviaMurderParty_2);
		
		JRadioButtonMenuItem rdbtnmntmPushTheButton = new JRadioButtonMenuItem("Push the Button");
		gameSelectionButtons.put(SupportedGames.PUSH_THE_BUTTON, rdbtnmntmPushTheButton);
		rdbtnmntmPushTheButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.PUSH_THE_BUTTON);
			}
		});
		mnSelectGame.add(rdbtnmntmPushTheButton);
		game.add(rdbtnmntmPushTheButton);
		
		JLabel lblBrushSettings = new JLabel("Brush Settings");
		GridBagConstraints gbc_lblBrushSettings = new GridBagConstraints();
		gbc_lblBrushSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblBrushSettings.gridwidth = 2;
		gbc_lblBrushSettings.gridx = 1;
		gbc_lblBrushSettings.gridy = 0;
		panel.add(lblBrushSettings, gbc_lblBrushSettings);
		
		JPanel drawPanel = new JPanel();
		drawPanel.setBorder(null);
		GridBagConstraints gbc_drawPanel = new GridBagConstraints();
		gbc_drawPanel.gridheight = 6;
		gbc_drawPanel.insets = new Insets(0, 0, 5, 5);
		gbc_drawPanel.fill = GridBagConstraints.BOTH;
		gbc_drawPanel.gridx = 0;
		gbc_drawPanel.gridy = 0;
		panel.add(drawPanel, gbc_drawPanel);
		drawPanel.setLayout(new BorderLayout(0, 0));
		
		
		JLabel lblBrushThickness = new JLabel("Brush Thickness");
		GridBagConstraints gbc_lblBrushThickness = new GridBagConstraints();
		gbc_lblBrushThickness.anchor = GridBagConstraints.NORTH;
		gbc_lblBrushThickness.insets = new Insets(0, 0, 5, 5);
		gbc_lblBrushThickness.gridx = 1;
		gbc_lblBrushThickness.gridy = 2;
		panel.add(lblBrushThickness, gbc_lblBrushThickness);
		
		JSpinner thicknessSpinner = new JSpinner();
		thicknessSpinner.setModel(new SpinnerNumberModel(6, 1, null, 1));
		GridBagConstraints gbc_thicknessSpinner = new GridBagConstraints();
		gbc_thicknessSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_thicknessSpinner.ipadx = 20;
		gbc_thicknessSpinner.anchor = GridBagConstraints.NORTH;
		gbc_thicknessSpinner.gridx = 2;
		gbc_thicknessSpinner.gridy = 2;
		panel.add(thicknessSpinner, gbc_thicknessSpinner);
		
		JColorChooser brushChooser = new JColorChooser();
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
		gbc_brushChooser.insets = new Insets(0, 5, 5, 0);
		gbc_brushChooser.gridx = 1;
		gbc_brushChooser.gridy = 1;
		panel.add(brushChooser, gbc_brushChooser);
		
		sketchpad = new JLabel("") {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				repaintImage();
				super.paintComponent(g);
			}
		};
		sketchpad.setHorizontalAlignment(SwingConstants.LEFT);
		sketchpad.setVerticalAlignment(SwingConstants.TOP);
		StretchIcon si = new StretchIcon(drawnToScreenImage, true);
		sketchpad.setIcon(si);
		sketchpad.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * 240);
				y = (int) (((double) y / (double) si.getH()) * 300);
				
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
				if (e.getButton() == MouseEvent.BUTTON1) {
					drawing = false;
					int x = e.getX() - si.getXOffset();
					int y = e.getY() - si.getYOffset();
					x = (int) (((double) x / (double) si.getW()) * 240);
					y = (int) (((double) y / (double) si.getH()) * 300);
					
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
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * 240);
				y = (int) (((double) y / (double) si.getH()) * 300);
				
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
					int half = line.getThickness()/2;
					if (x-half < 0 || x+half >= 240 || y-half < 0 || y+half >= 300) {
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
		
		lblTeeKo = new JLabel("Tee K.O. Background Color");
		GridBagConstraints gbc_lblTeeKo = new GridBagConstraints();
		gbc_lblTeeKo.insets = new Insets(0, 0, 5, 0);
		gbc_lblTeeKo.gridwidth = 2;
		gbc_lblTeeKo.gridx = 1;
		gbc_lblTeeKo.gridy = 3;
		lblTeeKo.setVisible(false);
		panel.add(lblTeeKo, gbc_lblTeeKo);
		
		teeKOBackgroundColorPicker = new JColorChooser();
		teeKOBackgroundColorPicker.setColor(Color.BLACK);
		teeKOBackgroundColorPicker.setPreviewPanel(new JPanel());
		teeKOBackgroundColorPicker.setChooserPanels(new AbstractColorChooserPanel[]{teeKOBackgroundColorPicker.getChooserPanels()[1]});
		
		for (Component comp : teeKOBackgroundColorPicker.getChooserPanels()[0].getComponents()) {
			comp.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					sketchpad.repaint();
				}
			});
		}
		container = ((Container) teeKOBackgroundColorPicker.getChooserPanels()[0].getComponents()[0]);
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
		teeKOBackgroundColorPicker.setVisible(false);
		teeKOBackgroundColorPicker.setColor(Color.WHITE);
		GridBagConstraints gbc_teekoBackgroundChooser = new GridBagConstraints();
		gbc_teekoBackgroundChooser.insets = new Insets(0, 5, 5, 0);
		gbc_teekoBackgroundChooser.gridwidth = 2;
		gbc_teekoBackgroundChooser.gridx = 1;
		gbc_teekoBackgroundChooser.gridy = 4;
		panel.add(teeKOBackgroundColorPicker, gbc_teekoBackgroundChooser);
		
		
	}
	
	public void changeGame(SupportedGames game) {
		currentGame = game;
		teeKOBackgroundColorPicker.setVisible(currentGame == SupportedGames.TEE_KO);
		lblTeeKo.setVisible(currentGame == SupportedGames.TEE_KO);
		sketchpad.repaint();
	}
	
	private void importFromImage(BufferedImage loadedImage) {
		int scaling;
		if (loadedImage.getWidth()%CANVAS_WIDTH == 0 && loadedImage.getHeight()%CANVAS_HEIGHT == 0) {
			scaling = Image.SCALE_FAST;
		} else {
			scaling = Image.SCALE_SMOOTH;
		}
		Image tmp = loadedImage.getScaledInstance(CANVAS_WIDTH/VECTOR_IMPORT_SCALE_FACTOR, CANVAS_HEIGHT/VECTOR_IMPORT_SCALE_FACTOR, scaling);
		loadedImage = new BufferedImage(tmp.getWidth(null), tmp.getHeight(null), BufferedImage.TYPE_USHORT_555_RGB);
		
		lines = lines.subList(importLines, currentLine);
		
		Graphics2D g2d = loadedImage.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		int i = 0;
		for (int y = 0; y < loadedImage.getHeight(); y++) {
			for (int x = 0; x < loadedImage.getWidth(); x++) {
				int clr = loadedImage.getRGB(x, y);
				Color color = new Color(clr);
				Line newLine = new Line(VECTOR_IMPORT_SCALE_FACTOR+1, color);
				newLine.points.add(new Point(x*VECTOR_IMPORT_SCALE_FACTOR,y*VECTOR_IMPORT_SCALE_FACTOR+VECTOR_IMPORT_SCALE_FACTOR/2));
				
				int x1 = x+1;
				while (x1 < loadedImage.getWidth() && loadedImage.getRGB(x1, y) == clr) {
					x1++;
				}
				
				newLine.points.add(new Point(x1*VECTOR_IMPORT_SCALE_FACTOR,y*VECTOR_IMPORT_SCALE_FACTOR+VECTOR_IMPORT_SCALE_FACTOR/2));
				x = x1-1;
				
				lines.add(i++, newLine);
			}
		}
		currentLine = lines.size();
		importLines = i;
	}
	
	private void repaintImage() {
		Graphics2D g = drawnToScreenImage.createGraphics();
		g.setPaint(new TexturePaint(transparentTexture, new Rectangle(0,0,40,40)));
		g.fill(new Rectangle(0,0, 240*2, 300*2));
		
		actualImage = new BufferedImage(240, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g1 = actualImage.createGraphics();
		
		if (currentGame.getImageType() == ImageType.BITMAP && rasterBackgroundImage != null) {
			g1.drawImage(rasterBackgroundImage, 0, 0, 240, 300, null);
		}
		
		if (currentGame == SupportedGames.TEE_KO) {
			g1.setColor(teeKOBackgroundColorPicker.getColor());
			g1.fill(new Rectangle(0,0, 240, 300));
		}
		
		int linesDrawn = 0;
		lines:
		for (Line line : lines) {
			if (linesDrawn++ >= currentLine) break lines;
			if (linesDrawn < importLines && currentGame.getImageType() == ImageType.BITMAP) continue lines;
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
}
