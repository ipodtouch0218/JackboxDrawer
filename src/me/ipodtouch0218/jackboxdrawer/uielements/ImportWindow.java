package me.ipodtouch0218.jackboxdrawer.uielements;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import me.ipodtouch0218.jackboxdrawer.JackboxDrawer;
import me.ipodtouch0218.jackboxdrawer.SupportedGames;
import me.ipodtouch0218.jackboxdrawer.obj.JackboxLine;
import me.ipodtouch0218.jackboxdrawer.util.ImageVectorizer;

public class ImportWindow extends JDialog {

	private static final long serialVersionUID = 7283260108551606453L;
	
	private final JPanel contentPanel = new JPanel();
	private JLabel lblColors, lblResolution, lblEstimatedLines, lblLag;
	private JSlider sliderColorDistance, sliderResolution, sliderAlpha;
	private JLabel lblTransparencyCutoff;

	public ImportWindow(BufferedImage image) {
		AtomicBoolean imported = new AtomicBoolean(false);
		
		JackboxDrawer drawer = JackboxDrawer.INSTANCE;
		int width = drawer.getCanvasWidth();
		int height = drawer.getCanvasHeight();
		
		ArrayList<JackboxLine> prevLines = drawer.getLines();
		BufferedImage prevImportImage = drawer.getImportedImage();
		int prevImportLines = drawer.getImportLines();
		
		drawer.setImportedImage(image);
		
		setTitle("Import Settings");
		try {
			setIconImage(ImageIO.read(getClass().getResource("/img/jbd-smol.png")));
		} catch (IOException e1) {
		}
		setModalityType(ModalityType.TOOLKIT_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		Rectangle bounds = JackboxDrawer.INSTANCE.getWindow().getBounds();
		setBounds((int) bounds.getCenterX() - 386/2, (int) bounds.getCenterY() - 210/2, 386, 242);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			lblColors = new JLabel("Color Resolution:");
			GridBagConstraints gbc_lblColors = new GridBagConstraints();
			gbc_lblColors.insets = new Insets(0, 5, 5, 5);
			gbc_lblColors.gridx = 0;
			gbc_lblColors.gridy = 1;
			contentPanel.add(lblColors, gbc_lblColors);
		}
		{
			sliderColorDistance = new JSlider();
			sliderColorDistance.setToolTipText("Higher means more colors");
			lblColors.setLabelFor(sliderColorDistance);
			sliderColorDistance.setMinorTickSpacing(64);
			sliderColorDistance.setMajorTickSpacing(128);
			sliderColorDistance.setPaintTicks(true);
			sliderColorDistance.setMaximum(256);
			sliderColorDistance.setValue(215);
			sliderColorDistance.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					ArrayList<JackboxLine> lines = ImageVectorizer.vectorizeImage(image, width, height, (8-sliderResolution.getValue()), (256-sliderColorDistance.getValue()), (255-sliderAlpha.getValue()));
					drawer.setLines(lines);
					drawer.setImportLines(lines.size());
					drawer.repaintImage(true);
					updateLinesLabel(lines.size());
				}
			});
			GridBagConstraints gbc_sliderColorDistance = new GridBagConstraints();
			gbc_sliderColorDistance.fill = GridBagConstraints.HORIZONTAL;
			gbc_sliderColorDistance.insets = new Insets(0, 0, 5, 0);
			gbc_sliderColorDistance.gridx = 1;
			gbc_sliderColorDistance.gridy = 1;
			contentPanel.add(sliderColorDistance, gbc_sliderColorDistance);
		}
		{
			lblResolution = new JLabel("Image Resolution:");
			GridBagConstraints gbc_lblResolution = new GridBagConstraints();
			gbc_lblResolution.insets = new Insets(0, 5, 5, 5);
			gbc_lblResolution.gridx = 0;
			gbc_lblResolution.gridy = 2;
			contentPanel.add(lblResolution, gbc_lblResolution);
		}
		{
			sliderResolution = new JSlider();
			sliderResolution.setMajorTickSpacing(3);
			sliderResolution.setMinimum(1);
			sliderResolution.setSnapToTicks(true);
			sliderResolution.setValue(5);
			sliderResolution.setMaximum(7);
			sliderResolution.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					ArrayList<JackboxLine> lines = ImageVectorizer.vectorizeImage(image, width, height, (8-sliderResolution.getValue()), (256-sliderColorDistance.getValue()), (255-sliderAlpha.getValue()));
					drawer.setLines(lines);
					drawer.setImportLines(lines.size());
					drawer.repaintImage(true);
					updateLinesLabel(lines.size());
				}
			});
			sliderResolution.setToolTipText("Higher means more pixels");
			lblResolution.setLabelFor(sliderResolution);
			sliderResolution.setMinorTickSpacing(1);
			sliderResolution.setPaintTicks(true);
			GridBagConstraints gbc_sliderResolution = new GridBagConstraints();
			gbc_sliderResolution.insets = new Insets(0, 0, 5, 0);
			gbc_sliderResolution.fill = GridBagConstraints.HORIZONTAL;
			gbc_sliderResolution.gridx = 1;
			gbc_sliderResolution.gridy = 2;
			contentPanel.add(sliderResolution, gbc_sliderResolution);
		}
		{
			lblTransparencyCutoff = new JLabel("Transparency Cutoff:");
			GridBagConstraints gbc_lblTransparencyCutoff = new GridBagConstraints();
			gbc_lblTransparencyCutoff.insets = new Insets(0, 5, 5, 5);
			gbc_lblTransparencyCutoff.gridx = 0;
			gbc_lblTransparencyCutoff.gridy = 3;
			contentPanel.add(lblTransparencyCutoff, gbc_lblTransparencyCutoff);
		}
		{
			sliderAlpha = new JSlider();
			sliderAlpha.setMinorTickSpacing(64);
			sliderAlpha.setMajorTickSpacing(128);
			sliderAlpha.setPaintTicks(true);
			sliderAlpha.setMaximum(256);
			sliderAlpha.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					ArrayList<JackboxLine> lines = ImageVectorizer.vectorizeImage(image, width, height, (8-sliderResolution.getValue()), (256-sliderColorDistance.getValue()), (255-sliderAlpha.getValue()));
					drawer.setLines(lines);
					drawer.setImportLines(lines.size());
					drawer.repaintImage(true);
					updateLinesLabel(lines.size());
				}
			});
			GridBagConstraints gbc_sliderAlpha = new GridBagConstraints();
			gbc_sliderAlpha.fill = GridBagConstraints.HORIZONTAL;
			gbc_sliderAlpha.insets = new Insets(0, 0, 5, 0);
			gbc_sliderAlpha.gridx = 1;
			gbc_sliderAlpha.gridy = 3;
			contentPanel.add(sliderAlpha, gbc_sliderAlpha);
		}
		{
			lblLag = new JLabel("");
			GridBagConstraints gbc_lblLag = new GridBagConstraints();
			gbc_lblLag.gridwidth = 2;
			gbc_lblLag.gridx = 0;
			gbc_lblLag.gridy = 4;
			contentPanel.add(lblLag, gbc_lblLag);
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			GridBagLayout gbl_buttonPane = new GridBagLayout();
			gbl_buttonPane.columnWidths = new int[]{162, 77, 71, 0};
			gbl_buttonPane.rowHeights = new int[]{25, 0};
			gbl_buttonPane.columnWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
			gbl_buttonPane.rowWeights = new double[]{0.0, Double.MIN_VALUE};
			buttonPane.setLayout(gbl_buttonPane);
			{
				lblEstimatedLines = new JLabel("Number of Lines: ");
				GridBagConstraints gbc_lblEstimatedLines = new GridBagConstraints();
				gbc_lblEstimatedLines.anchor = GridBagConstraints.WEST;
				gbc_lblEstimatedLines.insets = new Insets(5, 5, 5, 5);
				gbc_lblEstimatedLines.gridx = 0;
				gbc_lblEstimatedLines.gridy = 0;
				buttonPane.add(lblEstimatedLines, gbc_lblEstimatedLines);
			}
			{
				JButton okButton = new JButton("Confirm");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						imported.set(true);
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				GridBagConstraints gbc_okButton = new GridBagConstraints();
				gbc_okButton.anchor = GridBagConstraints.WEST;
				gbc_okButton.insets = new Insets(5, 5, 5, 5);
				gbc_okButton.gridx = 1;
				gbc_okButton.gridy = 0;
				buttonPane.add(okButton, gbc_okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				GridBagConstraints gbc_cancelButton = new GridBagConstraints();
				gbc_cancelButton.insets = new Insets(5, 5, 5, 5);
				gbc_cancelButton.anchor = GridBagConstraints.WEST;
				gbc_cancelButton.gridx = 2;
				gbc_cancelButton.gridy = 0;
				buttonPane.add(cancelButton, gbc_cancelButton);
			}
		}
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				if (imported.get()) {
					drawer.setImportedImage(image);
					drawer.setImportLines(drawer.getLines().size());
					drawer.clearHistory();
					JOptionPane.showMessageDialog(drawer.getWindow(), drawer.getLines().size() + " lines drawn.", "Image Loaded", JOptionPane.INFORMATION_MESSAGE);
				} else {
					drawer.setLines(prevLines);
					drawer.setImportedImage(prevImportImage);
					drawer.setImportLines(prevImportLines);
				}
				drawer.repaintImage(true);
			}
		});
		
		ArrayList<JackboxLine> lines = ImageVectorizer.vectorizeImage(image, width, height, (8-sliderResolution.getValue()), (256-sliderColorDistance.getValue()), (255-sliderAlpha.getValue()));
		drawer.setLines(lines);
		drawer.repaintImage(true);
		updateLinesLabel(lines.size());
		setVisible(true);
	}
	
	private void updateLinesLabel(int lines) {
		lblEstimatedLines.setText("Number of Lines: " + lines);
		
		if (JackboxDrawer.INSTANCE.getCurrentGame() == SupportedGames.TEE_KO) {
			if (lines < 1000) {
				lblLag.setText("Shouldn't be laggy, even with multiple users.");
				lblLag.setForeground(new Color(51, 204, 51));
			} else {
				lblLag.setText("Might break the game. Not recommended.");
				lblLag.setForeground(new Color(204, 0, 0));
			}
		} else {
			if (lines < 1250) {
				lblLag.setText("Shouldn't be laggy, even with multiple users.");
				lblLag.setForeground(new Color(51, 204, 51));
			} else if (lines < 3500) {
				lblLag.setText("Might be laggy with multiple users in certain games.");
				lblLag.setForeground(new Color(255, 153, 0));
			} else {
				lblLag.setText("Will be laggy, even with only one user.");
				lblLag.setForeground(new Color(204, 0, 0));
			}
		}
	}
	
}
