/**
 *  This file is part of the Bonsai Game Library.
 *
 *  The Bonsai Game Library is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The Bonsai Game Library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with the Bonsai Game Library.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package org.bonsai.dev;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

public class GameImage extends GameComponent {
	public GameImage(final Game game) {
		super(game);
	}

	public final BufferedImage getScreen() {
		final int width = game.width();
		final int height = game.height();
		final int scale = game.scale();
		final BufferedImage buffer = create(width * scale, height * scale);
		final Graphics2D g = (Graphics2D) buffer.getGraphics();
		if (scale != 1) {
			g.drawImage(game.getBackbuffer(), 0, 0, width * scale, height
					* scale, 0, 0, width, height, null);
		} else {
			g.drawImage(game.getBackbuffer(), 0, 0, null);
		}
		g.dispose();
		return buffer;
	}

	private URL getURL(final String filename) {
		return this.getClass().getResource(filename);
	}

	public final BufferedImage create(final int width, final int height) {
		return create(width, height, true);
	}

	public final BufferedImage create(final int width, final int height,
			final boolean alpha) {
		return game.getConfig().createCompatibleImage(width, height,
				alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
	}
	
	public final VolatileImage createVolatile(final int width, final int height) {
		return createVolatile(width, height, true);
	}

	public final VolatileImage createVolatile(final int width, final int height,
			final boolean alpha) {
		return game.getConfig().createCompatibleVolatileImage(width, height,
				alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
	}

	public final BufferedImage get(final String file) {
		final URL filename = getURL(file);
		if (filename == null) {
			return null;
		} else {
			try {
				return compatible(ImageIO.read(filename));
			} catch (IOException e) {
				return null;
			}
		}
	}

	private final BufferedImage compatible(BufferedImage image) {
		final GraphicsConfiguration config = game.getConfig();
		if (image.getColorModel().equals(config.getColorModel())) {
			return image;

		} else {
			BufferedImage newImage = config.createCompatibleImage(
					image.getWidth(), image.getHeight(), image.getColorModel()
							.getTransparency());

			Graphics2D g = (Graphics2D) newImage.getGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();
			return newImage;
		}
	}

	public final BufferedImage[] gets(final String filename, final int cols,
			final int rows) {
		BufferedImage image = get(filename);
		if (image == null) {
			return null;
		}
		return gets(image, cols, rows);
	}

	public final BufferedImage[] gets(final BufferedImage image,
			final int cols, final int rows) {
		BufferedImage[] buffer = new BufferedImage[cols * rows];
		final int width = image.getWidth() / cols;
		final int height = image.getHeight() / rows;
		int i = 0;
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				buffer[i] = game.getConfig().createCompatibleImage(width,
						height, image.getColorModel().getTransparency());
				final Graphics2D g = buffer[i].createGraphics();
				g.drawImage(image, 0, 0, width, height, x * width, y * height,
						(x + 1) * width, (y + 1) * height, null);
				g.dispose();
				i++;
			}
		}
		return buffer;
	}

	public final BufferedImage flip(final BufferedImage image, final boolean h,
			final boolean v) {
		final BufferedImage buffer = game.getConfig().createCompatibleImage(
				image.getWidth(), image.getHeight(),
				image.getColorModel().getTransparency());

		final Graphics2D g = buffer.createGraphics();
		g.drawImage(image, h ? image.getWidth() : 0, v ? image.getHeight() : 0,
				h ? 0 : image.getWidth(), v ? 0 : image.getHeight(), 0, 0,
				image.getWidth(), image.getHeight(), null);

		g.dispose();
		return buffer;
	}

	public final BufferedImage[] flips(final BufferedImage[] images,
			final boolean h, final boolean v) {
		final BufferedImage[] buffer = new BufferedImage[images.length];
		for (int i = 0; i < images.length; i++) {
			buffer[i] = flip(images[i], h, v);
		}
		return buffer;
	}
}