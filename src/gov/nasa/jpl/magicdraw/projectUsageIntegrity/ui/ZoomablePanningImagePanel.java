/**
 * Adapted from:
 * http://web.eecs.utk.edu/~bvz/teaching/guiSp10/notes/transforms/PanAndZoom.java
 * http://www.coderanch.com/t/338284/GUI/java/zoom-zoom-picture-swing
 * 
 */
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ZoomablePanningImagePanel extends JPanel {

	private static final long serialVersionUID = -7846965827918574719L;

	public ZoomablePanningImagePanel(BufferedImage image) {
		super(new BorderLayout());
		
		final ImagePanel imagePanel = new ImagePanel(image);  
		
		final JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 25, 225, 100);
		zoomSlider.setMajorTickSpacing(25);
		zoomSlider.setMinorTickSpacing(10);
		zoomSlider.setPaintTicks(true);
		zoomSlider.setPaintLabels(true);
		zoomSlider.addChangeListener(new ChangeListener() {  
			public void stateChanged(ChangeEvent e) {  

				double scale = Math.max(0.00001, zoomSlider.getValue() / 100.0);
				imagePanel.setScale(scale);  
			}  
		});  
		
		final PanningHandler panner = new PanningHandler(imagePanel);
		imagePanel.addMouseListener(panner);
		imagePanel.addMouseMotionListener(panner);

		add(zoomSlider, BorderLayout.NORTH);  
		add(new JScrollPane(imagePanel), BorderLayout.CENTER);  
	}  

	public static class ImagePanel extends JPanel {  

		private static final long serialVersionUID = 5983038356725167521L;
		final BufferedImage image;  
		double translateX;
		double translateY;
		double scale;  

		public ImagePanel(BufferedImage image) {  
			this.image = image;
			this.translateX = 0;
			this.translateY = 0;
			this.scale = 1.0;  
			setBackground(Color.black);  
		}  

		protected void paintComponent(Graphics g)  
		{  
			super.paintComponent(g);  
			Graphics2D g2 = (Graphics2D)g;  
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);  
			int w = getWidth();  
			int h = getHeight();  
			int imageWidth = image.getWidth();  
			int imageHeight = image.getHeight();  
			double x = (w - scale * imageWidth)/2;  
			double y = (h - scale * imageHeight)/2;  
			AffineTransform at = AffineTransform.getTranslateInstance(x,y);  
			at.scale(scale, scale);  
			at.translate(translateX, translateY);
			g2.drawRenderedImage(image, at);  
		}  

		/** 
		 * For the scroll pane. 
		 */  
		public Dimension getPreferredSize()  
		{  
			int w = (int)(scale * image.getWidth());  
			int h = (int)(scale * image.getHeight());  
			return new Dimension(w, h);  
		}  

		public void setScale(double s)  
		{  
			scale = s;  
			revalidate();      // update the scroll pane  
			repaint();  
		}  

		public void pan(int deltaX, int deltaY) {
			translateX += deltaX;
			translateY += deltaY;
			revalidate();
			repaint();
		}
	}  

	public static class PanningHandler implements MouseListener, MouseMotionListener {
		int referenceX;
		int referenceY;
		final ImagePanel canvas;

		public PanningHandler(ImagePanel canvas) {
			this.canvas = canvas;
		}

		public void mousePressed(MouseEvent e) {
			// capture the starting point
			referenceX = e.getX();
			referenceY = e.getY();
		}

		public void mouseDragged(MouseEvent e) {

			// the size of the pan translations 
			// are defined by the current mouse location subtracted
			// from the reference location
			int deltaX = e.getX() - referenceX;
			int deltaY = e.getY() - referenceY;

			// make the reference point be the new mouse point. 
			referenceX = e.getX();
			referenceY = e.getY();

			canvas.pan(deltaX, deltaY);
		}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseMoved(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}
}
