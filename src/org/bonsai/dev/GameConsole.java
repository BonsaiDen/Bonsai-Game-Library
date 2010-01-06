package org.bonsai.dev;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


public class GameConsole extends GameObject<Game> {
	private BufferedImage buffer;
	private BufferedImage[] font;
	private int maxLines = 128;
	private String[] lines = new String[maxLines];
	private String inputString = "";

	private int lineMax = 0;
	private int height = 0;
	private int width = 0;
	private boolean changed = true;

	private int scroll = 0;
	private int scrollOffset = 0;
	private int scrollHeight = 0;

	private int scrollOffsetOld = 0;
	private int scrollHeightOld = 0;

	private boolean autoScroll = true;

	private boolean submit = false;

	private boolean cursor = false;
	private boolean moveCursor = false;
	private int cursorPos = 0;

	private float transparency = 0.75f;
	
	public GameConsole(final Game game, final int width, final int height) {
		super(game);
		this.width = width - 8;
		this.height = height;
		buffer = image.create(width, height, false);
		font = image.gets("/images/console.png", 32, 4);
		timer.add("consoleCursor", 250);
		timer.add("consoleRepeatLeft", 35);
		timer.add("consoleRepeatStartLeft", 500);
		timer.add("consoleRepeatRight", 35);
		timer.add("consoleRepeatStartRight", 500);
	}

	public void print(String text) {
		if (lineMax < maxLines) {
			lines[lineMax] = text;
			lineMax++;
		} else {
			for (int i = 0; i < lineMax - 1; i++) {
				lines[i] = lines[i + 1];
			}
			lines[lineMax - 1] = text;
		}

		if (autoScroll) {
			scrollOffset = 0;
		}
		changed = true;
	}

	public void control() {
		// Cursor
		if (!input.keyDown(java.awt.event.KeyEvent.VK_LEFT, true)) {
			timer.set("consoleRepeatStartLeft");
		}
		if (input.keyPressed(java.awt.event.KeyEvent.VK_LEFT, true)
				|| (input.keyDown(java.awt.event.KeyEvent.VK_LEFT, true)
						&& timer.expired("consoleRepeatStartLeft") && timer.expired("consoleRepeatLeft"))) {
			cursorPos -= 1;
			if (cursorPos < 0) {
				cursorPos = 0;
			}
			moveCursor = true;
			changed = true;
			timer.set("consoleRepeatLeft");

		}

		if (!input.keyDown(java.awt.event.KeyEvent.VK_RIGHT, true)) {
			timer.set("consoleRepeatStartRight");
		}
		if (input.keyPressed(java.awt.event.KeyEvent.VK_RIGHT, true)
				|| (input.keyDown(java.awt.event.KeyEvent.VK_RIGHT, true)
						&& timer.expired("consoleRepeatStartRight") && timer.expired("consoleRepeatRight"))) {
			cursorPos += 1;
			if (cursorPos > inputString.length()) {
				cursorPos = inputString.length();
			}
			moveCursor = true;
			changed = true;
			timer.set("consoleRepeatRight");
		}

		if (!input.keyDown(java.awt.event.KeyEvent.VK_LEFT, true)
				&& !input.keyDown(java.awt.event.KeyEvent.VK_RIGHT, true)) {
			moveCursor = false;
		}

		// Scrolling
		if (input.keyDown(java.awt.event.KeyEvent.VK_PAGE_UP, true)) {
			scrollOffset += 12;
			changed = true;
		}
		if (input.keyDown(java.awt.event.KeyEvent.VK_PAGE_DOWN, true)) {
			scrollOffset -= 12;
			changed = true;
		}

		if (scrollOffset < 0) {
			scrollOffset = 0;
		}
		if (scrollOffset > scrollHeight - height) {
			scrollOffset = scrollHeight - height;
		}
		if (submit == true) {
			inputString = inputString.trim();
			print(">>" + inputString);
			onSubmit(inputString);
			inputString = "";
			cursorPos = 0;
			submit = false;
		}

		if (timer.expired("consoleCursor")) {
			cursor = !cursor;
			timer.set("consoleCursor");
		}
	}

	public void onSubmit(String input) {
		if (input.equals("clear") || input.equals("cls")) {
			scrollOffset = 0;
			lineMax = 0;
			lines = new String[maxLines];
			scrollHeight = 0;
			scroll = 0;

		} else if (input.equals("pause")) {
			game.pause(!game.paused);
			
		} else {
			game.onConsole(input);
		}
	}

	public void draw(Graphics2D g, int x, int y) {
		// Draw
		Graphics2D bg = (Graphics2D) buffer.getGraphics();
		String drawInputString =
				inputString + (cursorPos == inputString.length() ? " " : "");
		int inputHeight = textHeight(drawInputString) + 2;
		if (changed || scrollOffsetOld != scrollOffset
				|| scrollHeightOld != scrollHeight) {
			scrollOffsetOld = scrollOffset;
			scrollHeightOld = scrollHeight;

			bg.setColor(Color.BLACK);
			bg.fillRect(0, 0, width, height);

			// Cache
			int oy = 0;
			for (int i = 0; i < lineMax; i++) {
				// if (i < lineMax) {
				oy +=
						drawText(bg, 0, oy - scroll + scrollOffset, lines[i],
								false);
				if (oy - scroll > height - inputHeight) {
					int dec = (oy - scroll) - (height - inputHeight);
					scroll += dec;
					oy -= dec;

					if (!autoScroll && scrollOffset != 0) {
						scrollOffset += dec;
					}
				}
				// }
			}

			// Scrollbars
			scrollHeight = scroll + height;
			double windowHeight = height / (scrollHeight + 0.0);
			bg.setColor(Color.DARK_GRAY);
			bg.fillRect(width, 0, 8, height);
			bg.setColor(Color.WHITE);
			bg.fillRect(
					width,
					(int) ((height - height * windowHeight) - (scrollOffset * windowHeight)),
					8, (int) (height * windowHeight));
			changed = false;
		}

		// Input
		bg.setColor(Color.BLACK);
		bg.fillRect(0, height - inputHeight + 2, width, inputHeight + 2);
		bg.setColor(Color.WHITE);
		bg.fillRect(0, height - inputHeight, width, 2);
		drawText(bg, 0, height - inputHeight + 2, drawInputString, true);

		// fade
		Composite tmp = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				transparency));
		g.drawImage(buffer, x, y, null);
		g.setComposite(tmp);
	}

	public int textHeight(String text) {
		String empty = text.replace("\n", "").replace("\n", "");
		int lineheight = ((empty.length() - 1) / (width / 8)) * 12 + 12;
		int lines = text.length() - empty.length();
		lineheight += lines * 12;
		return lineheight;
	}

	public int drawText(Graphics2D g, int x, int y, String text,
			boolean drawCursor) {
		int length = text.length();
		int lineheight = textHeight(text);

		int ox = 0;
		int oy = 0;
		if (y < height && y + height > 0) {
			for (int i = 0; i < length; i++) {
				int c = (int) text.charAt(i);
				if (c < 128) {
					if (c == 13 || c == 10) {
						oy += 12;
						ox = 0;
					} else if (y + oy >= 0) {

						if (c == 9) {
							ox += 4;
						} else {
							g.drawImage(font[c], x + ox * 8, y + oy, null);
							if (drawCursor && i == cursorPos
									&& (moveCursor || cursor)) {
								g.drawImage(font[8], x + ox * 8, y + oy, null);
							}
						}
					}

					ox += 1;
					if (ox * 8 >= width) {
						y += 12;
						ox = 0;
					}
				}
			}
		}
		return lineheight;
	}

	public synchronized void onKey(int key) {
		if (key == 8) {
			if (!inputString.equals("")) {
				inputString =
						inputString.substring(0, cursorPos - 1)
								+ inputString.substring(cursorPos);
				cursorPos -= 1;
			}
		} else if (key == 10) {
			if (!inputString.equals("")) {
				submit = true;
			}
		} else {
			// if (key == 32 && (cursorPos == inputString.length() || cursorPos
			// == 0)) {

			// } else {
			inputString =
					inputString.substring(0, cursorPos) + (char) key
							+ inputString.substring(cursorPos);

			cursorPos += 1;
			// }
		}
		cursor = true;
		changed = true;
	}
}
