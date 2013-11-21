package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

public class BufferedImageFile {

	public final BufferedImage image;
	public final File imageFile;

	public BufferedImageFile(@Nonnull File imageFile) throws IOException {
		this.imageFile = imageFile;
		this.image = ImageIO.read(imageFile);
	}
}