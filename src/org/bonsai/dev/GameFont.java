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
import java.awt.image.BufferedImage;

public class GameFont extends GameComponent {
	private BufferedImage[] fontImages;
	private String fontChars;
	private int fontWidth;
	private int fontHeight;

	public GameFont(final Game game) {
		super(game);
	}

	public final boolean load(final String filename, final int cols,
			final int rows, final String chars) {
		fontImages = game.image.gets(filename, cols, rows);
		if (fontImages == null) {
			return false;
		}
		fontChars = chars;
		fontWidth = fontImages[0].getWidth() + 1;
		fontHeight = fontImages[0].getHeight() + 2;
		return true;
	}

	public final void draw(final Graphics2D g, final String text, final int x,
			final int y) {
		int xp = x;
		int yp = y;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int p = fontChars.indexOf(c);
			if (p != -1) {
				g.drawImage(fontImages[p], xp, yp, null);
			}
			if (c == ' ' || c == '.' || c == ':' || c == '|') {
				xp += 4;
			} else if (c == '\n') {
				xp = x;
				yp += fontHeight;
			} else {
				xp += fontWidth;
			}
		}
	}

	public final int width(final String text) {
		int size = 0;
		int xp = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == ' ' || c == '.' || c == ':' || c == '|') {
				xp += 4;
			} else if (c == '\n') {
				xp = 0;
			} else {
				xp += fontWidth;
			}
			if (xp > size) {
				size = xp;
			}
		}
		return size;
	}
}
