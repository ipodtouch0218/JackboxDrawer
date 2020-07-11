package me.ipodtouch0218.jackboxdrawer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
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

	JFrame frame;
	WebsocketServer server;
	SupportedGames currentGame = SupportedGames.DRAWFUL_2;
	EnumMap<SupportedGames, JRadioButtonMenuItem> gameButtons = new EnumMap<>(SupportedGames.class);
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JackboxDrawer window = new JackboxDrawer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public JackboxDrawer() {
		initialize();
		server = new WebsocketServer(this);
		server.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("Jackbox Drawer");
		frame.setBounds(100, 100, 925, 598);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		frame.getContentPane().setLayout(gridBagLayout);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		frame.getContentPane().add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
		panel.setLayout(gbl_panel);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile_1 = new JMenu("File");
		menuBar.add(mnFile_1);
		
		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);
		
		JMenuItem mntmImportFromImage = new JMenuItem("Import from Image");
		mntmImportFromImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		mntmImportFromImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane.showConfirmDialog(frame, "This action will clear the current canvas. Continue?", "Continue?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					return;
				}
				
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Supported Images", "png", "jpg", "jpeg");
				chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(frame);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	File file = chooser.getSelectedFile();
			    	lines.clear();
			    	currentLine = 0;
			    	
			    	try {
			    		BufferedImage loadedImage = ImageIO.read(file);
			    		switch (currentGame.getImageType()) {
			    		case VECTOR: {
				    		final int scaleFactor = (currentGame == SupportedGames.TEE_KO ? 5 : 4);
							Image tmp = loadedImage.getScaledInstance(240/scaleFactor, 300/scaleFactor, Image.SCALE_SMOOTH);
							tmp.getHeight(null);
							loadedImage = new BufferedImage(tmp.getWidth(null), tmp.getHeight(null), BufferedImage.TYPE_USHORT_555_RGB);
							
							Graphics2D g2d = loadedImage.createGraphics();
							g2d.drawImage(tmp, 0, 0, null);
							g2d.dispose();
							for (int y = 0; y < loadedImage.getHeight(); y++) {
								for (int x = 0; x < loadedImage.getWidth(); x++) {
									int clr = loadedImage.getRGB(x, y);
									Color color = new Color(clr);
									Line newLine = new Line(scaleFactor+1, color);
									newLine.points.add(new Point(x*scaleFactor,y*scaleFactor));
									
									int x1 = x+1;
									while (x1 < loadedImage.getWidth() && loadedImage.getRGB(x1, y) == clr) {
										x1++;
									}
									
									newLine.points.add(new Point(x1*scaleFactor,y*scaleFactor));
									x = x1-1;
									
									lines.add(newLine);
								}
							}
							currentLine = lines.size();	
							break;
			    		}
			    		case BITMAP: {
			    			rasterBackgroundImage = loadedImage;
			    			break;
			    		}
			    		}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			    	lblDrawImage.repaint();
			    }
			}
		});
		
		JMenuItem mntmSaveToClipboard = new JMenuItem("Save to Clipboard");
		mntmSaveToClipboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mnFile_1.add(mntmSaveToClipboard);
		
		JMenuItem mntmLoadFromText = new JMenuItem("Load from Text");
		mntmLoadFromText.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mnFile_1.add(mntmLoadFromText);
		
		JSeparator separator_1 = new JSeparator();
		mnFile_1.add(separator_1);
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
				if (JOptionPane.showConfirmDialog(frame, "Clear the entire canvas?", "Clear Canvas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					currentLine = -1;
					rasterBackgroundImage = null;
					lblDrawImage.repaint();
				}
			}
		});
		
		mntmUndo = new JMenuItem("Undo");
		mntmUndo.setEnabled(false);
		mntmUndo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (drawing || erasing) return;
				if (currentLine == -1) {
					currentLine = lines.size();
					mntmRedo.setEnabled(false);
				} else if (currentLine > 0) {
					currentLine--;
					mntmRedo.setEnabled(true);
				}
				mntmUndo.setEnabled(currentLine != 0);
				lblDrawImage.repaint();
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
				lblDrawImage.repaint();
			}
		});
		mntmRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK));
		mnEdit.add(mntmRedo);
		
		JSeparator separator_2 = new JSeparator();
		mnEdit.add(separator_2);
		mnEdit.add(mntmClearCanvas);
		
		JMenu mnCurrentGame = new JMenu("Current Game");
		menuBar.add(mnCurrentGame);
		
		ButtonGroup game = new ButtonGroup();
		
		JRadioButtonMenuItem rdbtnmntmDrawful_1 = new JRadioButtonMenuItem("Drawful 1");
		gameButtons.put(SupportedGames.DRAWFUL_1, rdbtnmntmDrawful_1);
		rdbtnmntmDrawful_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.DRAWFUL_1);
			}
		});
		game.add(rdbtnmntmDrawful_1);
		mnCurrentGame.add(rdbtnmntmDrawful_1);
		
		JRadioButtonMenuItem rdbtnmntmDrawful_2 = new JRadioButtonMenuItem("Drawful 2");
		gameButtons.put(SupportedGames.DRAWFUL_2, rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				changeGame(SupportedGames.DRAWFUL_2);
			}
		});
		game.add(rdbtnmntmDrawful_2);
		rdbtnmntmDrawful_2.setSelected(true);
		mnCurrentGame.add(rdbtnmntmDrawful_2);
		
		JRadioButtonMenuItem rdbtnmntmBidiots = new JRadioButtonMenuItem("Bidiots");
		gameButtons.put(SupportedGames.BIDIOTS, rdbtnmntmBidiots);
		rdbtnmntmBidiots.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.BIDIOTS);
			}
		});
		mnCurrentGame.add(rdbtnmntmBidiots);
		game.add(rdbtnmntmBidiots);
		
		JRadioButtonMenuItem rdbtnmntmTeeKo = new JRadioButtonMenuItem("Tee K.O.");
		gameButtons.put(SupportedGames.TEE_KO, rdbtnmntmTeeKo);
		rdbtnmntmTeeKo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.TEE_KO);
			}
		});
		game.add(rdbtnmntmTeeKo);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_1 = new JRadioButtonMenuItem("Trivia Murder Party 1");
		rdbtnmntmTriviaMurderParty_1.setEnabled(false);
		mnCurrentGame.add(rdbtnmntmTriviaMurderParty_1);
		mnCurrentGame.add(rdbtnmntmTeeKo);
		game.add(rdbtnmntmTriviaMurderParty_1);
		
		JRadioButtonMenuItem rdbtnmntmCivicDoodle = new JRadioButtonMenuItem("Civic Doodle");
		rdbtnmntmCivicDoodle.setEnabled(false);
		mnCurrentGame.add(rdbtnmntmCivicDoodle);
		game.add(rdbtnmntmCivicDoodle);
		
		JRadioButtonMenuItem rdbtnmntmPatentlyStupid = new JRadioButtonMenuItem("Patently Stupid");
		rdbtnmntmPatentlyStupid.setEnabled(false);
		mnCurrentGame.add(rdbtnmntmPatentlyStupid);
		game.add(rdbtnmntmPatentlyStupid);
		
		JRadioButtonMenuItem rdbtnmntmTriviaMurderParty_2 = new JRadioButtonMenuItem("Trivia Murder Party 2");
		rdbtnmntmTriviaMurderParty_2.setEnabled(false);
		mnCurrentGame.add(rdbtnmntmTriviaMurderParty_2);
		game.add(rdbtnmntmTriviaMurderParty_2);
		
		JRadioButtonMenuItem rdbtnmntmPushTheButton = new JRadioButtonMenuItem("Push the Button");
		gameButtons.put(SupportedGames.PUSH_THE_BUTTON, rdbtnmntmPushTheButton);
		rdbtnmntmPushTheButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeGame(SupportedGames.PUSH_THE_BUTTON);
			}
		});
		mnCurrentGame.add(rdbtnmntmPushTheButton);
		game.add(rdbtnmntmPushTheButton);
		
		JLabel lblBrushSettings = new JLabel("Brush Settings");
		GridBagConstraints gbc_lblBrushSettings = new GridBagConstraints();
		gbc_lblBrushSettings.gridwidth = 2;
		gbc_lblBrushSettings.insets = new Insets(0, 0, 5, 0);
		gbc_lblBrushSettings.gridx = 1;
		gbc_lblBrushSettings.gridy = 0;
		panel.add(lblBrushSettings, gbc_lblBrushSettings);
		
		JPanel drawPanel = new JPanel();
		drawPanel.setBorder(null);
		GridBagConstraints gbc_drawPanel = new GridBagConstraints();
		gbc_drawPanel.gridheight = 4;
		gbc_drawPanel.insets = new Insets(0, 0, 0, 5);
		gbc_drawPanel.fill = GridBagConstraints.BOTH;
		gbc_drawPanel.gridx = 0;
		gbc_drawPanel.gridy = 1;
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
		GridBagConstraints gbc_brushChooser = new GridBagConstraints();
		gbc_brushChooser.anchor = GridBagConstraints.WEST;
		gbc_brushChooser.gridwidth = 2;
		gbc_brushChooser.insets = new Insets(0, 5, 5, 0);
		gbc_brushChooser.gridx = 1;
		gbc_brushChooser.gridy = 1;
		panel.add(brushChooser, gbc_brushChooser);
		
		lblDrawImage = new JLabel("") {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				repaintImage();
				super.paintComponent(g);
			}
		};
		lblDrawImage.setHorizontalAlignment(SwingConstants.LEFT);
		lblDrawImage.setVerticalAlignment(SwingConstants.TOP);
		StretchIcon si = new StretchIcon(screenImage, true);
		lblDrawImage.setIcon(si);
		lblDrawImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * 240);
				y = (int) (((double) y / (double) si.getH()) * 300);
				
				if (x < 0 || x >= screenImage.getWidth() || y < 0 || y >= screenImage.getHeight()) {
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
					lblDrawImage.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3 && !drawing) {
					erasing = true;
					Point point = new Point(x,y);
					Iterator<Line> it = lines.iterator();
					while (it.hasNext()) {
						Line line = it.next();
						if (line.points.contains(point)) {
							it.remove();
							lblDrawImage.repaint();
							erasing = false;
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
					
					if (x < 0 || x >= screenImage.getWidth() || y < 0 || y >= screenImage.getHeight()) {
						return;
					}
					Line line = lines.get(lines.size()-1);
					line.points.add(new Point(x,y));
					
					mntmUndo.setEnabled(true);
					mntmRedo.setEnabled(false);
					lblDrawImage.repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					erasing = false;
					mntmUndo.setEnabled(true);
				}
			}
		});
		lblDrawImage.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int x = e.getX() - si.getXOffset();
				int y = e.getY() - si.getYOffset();
				x = (int) (((double) x / (double) si.getW()) * 240);
				y = (int) (((double) y / (double) si.getH()) * 300);
				
				if (erasing) {
					Point point = new Point(x,y);
					Iterator<Line> it = lines.iterator();
					while (it.hasNext()) {
						Line line = it.next();
						if (line.points.contains(point)) {
							it.remove();
							lblDrawImage.repaint();
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
					lblDrawImage.repaint();
				}
			}
		});
		drawPanel.add(lblDrawImage, BorderLayout.CENTER);
		
		lblTeeKo = new JLabel("Tee K.O. Background Color");
		GridBagConstraints gbc_lblTeeKo = new GridBagConstraints();
		gbc_lblTeeKo.gridwidth = 2;
		gbc_lblTeeKo.insets = new Insets(0, 0, 5, 0);
		gbc_lblTeeKo.gridx = 1;
		gbc_lblTeeKo.gridy = 3;
		lblTeeKo.setVisible(false);
		panel.add(lblTeeKo, gbc_lblTeeKo);
		
		teekoBackgroundChooser = new JColorChooser();
		teekoBackgroundChooser.setColor(Color.BLACK);
		teekoBackgroundChooser.setPreviewPanel(new JPanel());
		teekoBackgroundChooser.setChooserPanels(new AbstractColorChooserPanel[]{teekoBackgroundChooser.getChooserPanels()[1]});
		
		for (Component comp : teekoBackgroundChooser.getChooserPanels()[0].getComponents()) {
			comp.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					lblDrawImage.repaint();
				}
			});
		}
		Container container = ((Container) teekoBackgroundChooser.getChooserPanels()[0].getComponents()[0]);
		int spinners = 0, sliders = 0;
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
						lblDrawImage.repaint();
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
						lblDrawImage.repaint();
					}
				});
			} else if (comp instanceof JLabel) {
				container.remove(comp);
				i--;
			} else {
				comp.addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent e) {
						lblDrawImage.repaint();
					}
				});
			}
		}
		teekoBackgroundChooser.setVisible(false);
		GridBagConstraints gbc_teekoBackgroundChooser = new GridBagConstraints();
		gbc_teekoBackgroundChooser.gridwidth = 2;
		gbc_teekoBackgroundChooser.gridx = 1;
		gbc_teekoBackgroundChooser.gridy = 4;
		panel.add(teekoBackgroundChooser, gbc_teekoBackgroundChooser);
		
		
	}

	JColorChooser teekoBackgroundChooser;
	private JMenuItem mntmRedo, mntmUndo;
	private JLabel lblDrawImage, lblTeeKo;
	private BufferedImage screenImage = new BufferedImage(240*2,300*2,BufferedImage.TYPE_INT_RGB), rasterBackgroundImage;
	BufferedImage actualImage;
	private boolean drawing, erasing;
	int currentLine;
	ArrayList<Line> lines = new ArrayList<>();
	
	private static final BufferedImage TRANSPARENCY = new BufferedImage(2,2,BufferedImage.TYPE_BYTE_GRAY);
	static {
		Graphics2D g = TRANSPARENCY.createGraphics();
		g.setColor(new Color(244,244,244));
		g.fillRect(0, 0, 2, 2);
		g.setColor(new Color(224,224,224));
		g.fillRect(1, 0, 2, 1);
		g.fillRect(0, 1, 1, 2);
		g.dispose();
	}
	
	public void changeGame(SupportedGames game) {
		SupportedGames previous = currentGame;
		boolean resetBackground = currentGame.getImageType() != game.getImageType();
		currentGame = game;
		if (resetBackground) {
			JOptionPane.showMessageDialog(frame, String.format("%s and %s use different graphics formats.\nThe canvas will be cleared when switching between these games.", previous.getName(), game.getName()), "Warning", JOptionPane.WARNING_MESSAGE);
			lines.clear();
			currentLine = 0;
			rasterBackgroundImage = null;
		}
		teekoBackgroundChooser.setVisible(currentGame == SupportedGames.TEE_KO);
		lblTeeKo.setVisible(currentGame == SupportedGames.TEE_KO);
		lblDrawImage.repaint();
	}
	
	private void repaintImage() {
		Graphics2D g = screenImage.createGraphics();
		g.setPaint(new TexturePaint(TRANSPARENCY, new Rectangle(0,0,40,40)));
		g.fill(new Rectangle(0,0, 240*2, 300*2));
		
		actualImage = new BufferedImage(240, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g1 = actualImage.createGraphics();
		
		if (currentGame.getImageType() == ImageType.BITMAP && rasterBackgroundImage != null) {
			g1.drawImage(rasterBackgroundImage, 0, 0, 240, 300, null);
		}
		
		if (currentGame == SupportedGames.TEE_KO) {
			g1.setColor(teekoBackgroundChooser.getColor());
			g1.fill(new Rectangle(0,0, 240, 300));
		}
		
		int linesDrawn = 0;
		lines:
		for (Line line : lines) {
			if (linesDrawn++ >= currentLine) break lines;
			g1.setColor(line.color);
			g1.setStroke(new BasicStroke(line.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (int i = 0; i+1 < line.points.size(); i++) {
				Point p1 = line.points.get(i);
				Point p2 = line.points.get(i+1);
				g1.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
		}
		
		g.drawImage(actualImage, 0, 0, screenImage.getWidth(), screenImage.getHeight(), null);
		g.dispose();
		g1.dispose();
	}
}
