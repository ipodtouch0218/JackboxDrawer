package me.ipodtouch0218.jackboxdrawer.uielements;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import me.ipodtouch0218.jackboxdrawer.JackboxDrawer;

public class JPanelDnD extends JPanel implements DropTargetListener {

	private static final long serialVersionUID = -2753388319195303825L;

    public JPanelDnD() {
        new DropTarget(
                this,
                DnDConstants.ACTION_COPY_OR_MOVE,
                this,
                true);
    }
    
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dragExit(DropTargetEvent dtde) {
		// TODO Auto-generated method stub
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent dtde) {
		dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		Transferable t = dtde.getTransferable();
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                Object td = t.getTransferData(DataFlavor.javaFileListFlavor);
                if (td instanceof List) {
                    for (Object value : ((List<Object>) td)) {
                        if (value instanceof File) {
                            File file = (File) value;
                            JackboxDrawer.INSTANCE.tryImportFile(file);
                        }
                    }
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            }
		} else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				String td = (String) t.getTransferData(DataFlavor.stringFlavor);
				JackboxDrawer.INSTANCE.tryImportImage(ImageIO.read(new URL(td)));
			} catch (UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
			}
		} 
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
	}

}
