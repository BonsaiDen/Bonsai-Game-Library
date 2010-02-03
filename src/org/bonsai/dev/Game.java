/**
 *	Version 1.00
 *	Copyright (C) 2009 Ivo Wetzel
 *	<http://code.google.com/p/bonsaigamelibrary/>
 *
 *
 *  This file is part of the Bonsai Game Library.
 *
 *  The Bonsai Game Library is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The Bonsai Game Library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License along with
 *  the Bonsai Game Library. If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package org.bonsai.dev;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.bonsai.ext.Base64;

import netscape.javascript.JSObject;

public class Game extends Applet {
	// Applet
	private static final long serialVersionUID = -7860545086276629929L;

	// Graphics
	private GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getDefaultScreenDevice()
			.getDefaultConfiguration();

	private JPanel canvasPanel;
	private BufferedImage background;
	private Graphics2D backgroundGraphics;
	protected Color backgroundColor = Color.BLACK;
	private int width;
	private int height;
	private int scale;

	// Game Stuff
	private boolean gameLoaded = false;
	private boolean gameSound = true;
	private boolean isRunning = true;
	protected boolean paused = false;
	private boolean focused = false;
	private boolean pausedOnFocus = false;
	private boolean animationPaused = false;

	private int currentFPS = 0;
	private long fpsWait;
	private long gameTime = 0;
	private boolean limitFPS = true;
	private int maxFPS = 0;

	private boolean stopped = false;
	private transient Thread gameLoader = null;

	// GUI
	private JFrame frame = null;
	private Applet applet = null;

	// Classes
	protected GameAnimation animation = null;
	protected GameSound sound = null;
	protected GameImage image = null;
	protected GameInput input = null;
	protected GameFont font = null;
	protected GameTimer timer = null;
	protected GameMenu menu = null;

	// Console
	protected GameConsole console = null;
	protected boolean consoleOpen = false;

	// Builder
	private boolean buildScaled = false;
	private boolean buildSound = true;
	private int buildSizex = 320;
	private int buildSizey = 240;
	private String buildTitle = "Bonsai";
	private boolean buildInitMenu = false;
	private boolean buildGameMenu = false;

	/*
	 * Path --------------------------------------------------------------------
	 */
	public final boolean isJar() {
		boolean isJar = true;
		if (!isApplet()) {
			isJar = currentPath().toLowerCase().endsWith(".jar");
		}
		return isJar;
	}

	public final String getPath() {
		String path = "";
		if (!isApplet()) {
			path = currentPath();
			if (isJar()) {
				path = path.substring(0, path.lastIndexOf("/") + 1);
			}
		}
		return path;
	}

	private String currentPath() {
		String path = "";
		if (!isApplet()) {
			try {
				path = this.getClass()
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.toURI()
						.getPath();
			} catch (URISyntaxException e) {
				path = "";
			}
		}
		return path;
	}

	/*
	 * Builder Methods ---------------------------------------------------------
	 */
	public final Game title(final String title) {
		buildTitle = title;
		return this;
	}

	public final Game size(final int width, final int height) {
		buildSizex = width;
		buildSizey = height;
		return this;
	}

	public final Game scaled(final boolean scaled) {
		buildScaled = scaled;
		return this;
	}

	public final Game sound(final boolean sound) {
		buildSound = sound;
		return this;
	}

	public final Game menu(final boolean menu, final boolean def) {
		buildInitMenu = menu;
		buildGameMenu = def;
		return this;
	}

	public final Game background(final Color color) {
		backgroundColor = color;
		return this;
	}

	public final void create() {
		// Size
		scale = buildScaled ? 2 : 1;
		width = buildSizex;
		height = buildSizey;

		// Create frame
		frame = new JFrame(config);
		frame.setLayout(new BorderLayout(0, 0));

		// Init Engine
		gameSound = buildSound;
		initEngine(frame);

		// Setup frame
		frame.setResizable(false);
		frame.setTitle(buildTitle);
		menu = new GameMenu(this, buildInitMenu, buildGameMenu);
		frame.addWindowListener(new FrameClose());
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		resizeFrame();

		// Start threads
		initThreads();
	}

	/*
	 * General Methods ---------------------------------------------------------
	 */
	public final void setSize(final int width, final int height) {
		this.width = width;
		this.height = height;
		background = image.create(width, height, false);
		backgroundGraphics = (Graphics2D) background.getGraphics();
		setScale(1);
	}

	private final void resizeFrame() {
		frame.setSize((width * scale) + frame.getInsets().left
				+ frame.getInsets().right, (height * scale)
				+ frame.getInsets().top + frame.getInsets().bottom
				+ menu.getSize());
	}

	public void onMenu(final String menuID) {
	}

	public void onConsole(final String consoleInput) {
	}

	private class FrameClose extends WindowAdapter {
		@Override
		public void windowClosing(final WindowEvent e) {
			isRunning = false;
		}
	}

	public final JFrame getFrame() {
		return frame;
	}

	public final boolean hasMenu() {
		return menu != null;
	}

	public static void main(final String args[]) {
		new Game().title("Bonsai Game Library 1.00").size(320, 240).menu(true,
				true).create();
	}

	/*
	 * Applet ------------------------------------------------------------------
	 */

	@Override
	public final void init() {
		if (stopped) {
			isRunning = true;
		} else {
			scale = getParameter("scaled") != null ? 2 : 1;
			gameSound = getParameter("sound") != null ? getParameter("sound").equals(
					"true")
					: true;
			width = getWidth() / scale;
			height = getHeight() / scale;
			initApplet(this);
			setLayout(new BorderLayout(0, 0));
			initEngine(this);
			menu = new GameMenu(this, false, false);
			applet = this;
			initThreads();
			stopped = false;
		}
	}

	@Override
	public final void paint(Graphics g) {
		if (!gameLoaded) {
			g.setColor(backgroundColor);
			g.fillRect(0, 0, width() * scale, height() * scale);
			g.dispose();
		}
	}

	@Override
	public final void stop() {
		stopped = true;
	}

	@Override
	public final void destroy() {
		exitGame();
	}

	public final boolean isApplet() {
		return applet != null;
	}

	public final Applet getApplet() {
		return applet;
	}

	public final int height() {
		return height;
	}

	public final int width() {
		return width;
	}

	public final int scale() {
		return scale;
	}

	public final synchronized void setScale(final int scale) {
		canvasPanel.setPreferredSize(new Dimension(width * scale, height
				* scale));
		this.scale = scale;
		resizeFrame();
	}

	/*
	 * Gameloader --------------------------------------------------------------
	 */
	private void initEngine(final Container parent) {
		// We don't need double buffering here since we're already blitting all
		// stuff to our own buffer for scaling before actually updating the
		// screen.
		canvasPanel = new JPanel(false);
		canvasPanel.setPreferredSize(new Dimension(width * scale, height
				* scale));
		canvasPanel.setFocusable(false);
		canvasPanel.setOpaque(true);
		canvasPanel.setIgnoreRepaint(true);
		parent.add(canvasPanel, 0);

		// Components
		animation = new GameAnimation(this);
		sound = new GameSound(this);
		image = new GameImage(this);
		input = new GameInput(this);
		font = new GameFont(this);
		timer = new GameTimer(this);
		console = new GameConsole(this);

		// Add input listeners
		parent.addMouseListener(input);
		parent.addMouseMotionListener(input);
		parent.addKeyListener(input);
		parent.addFocusListener(input);
		
		// Our background for scaling which also acts as a replacement for
		// double buffering
		background = image.create(width, height, false);
		setFPS(30);
	}

	private void initThreads() {
		new GameLoop().start();
		gameLoader = new GameLoader();
		gameLoader.start();
	}

	private class GameLoader extends Thread {
		public GameLoader() {
			setDaemon(true);
			setName("Bonsai-GameLoader");
		}

		@Override
		public void run() {
			// Init Loading
			initGame(true);
			if (gameSound) {
				gameSound = sound.init(); // This actually takes time!
			}
			finishGame(false);
			menu.enable(true);
			gameLoaded = true;

			// Fix some of the graphical lag
			// This hack lowers the systems interrupt rate so that Thread.sleep
			// becomes more precise
			try {
				Thread.sleep(Integer.MAX_VALUE);

			} catch (InterruptedException e) {
				isRunning = false;
				Thread.interrupted();
			}
		}
	}

	/*
	 * Methods implemented by the game -----------------------------------------
	 */
	public void initGame(final boolean loaded) {
	}

	public void initApplet(final Applet applet) {
	}

	public void updateGame(final boolean loaded) {
	}

	public void renderGame(final boolean loaded, final Graphics2D g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width(), height());
	}

	public void finishGame(final boolean loaded) {
	}

	/*
	 * Gameloop ----------------------------------------------------------------
	 */
	private class GameLoop extends Thread {
		@Override
		public void run() {
			setName("Bonsai-GameLoop");
			initGame(false);

			// FPS
			long renderStart = System.nanoTime();
			final long[] renderStats = new long[10];
			final long[] renderStatsMax = new long[10];

			// Graphics
			backgroundGraphics = (Graphics2D) background.getGraphics();
			while (isRunning) {
				// Pausing
				if (!consoleOpen
						&& input.keyPressed(java.awt.event.KeyEvent.VK_P, true)) {
					pause(!paused);
				}

				// Console
				if (console != null && consoleKey()) {
					consoleOpen = !consoleOpen;
				}
				if (consoleOpen) {
					console.control();
				}

				// Update Game
				if (!paused) {
					updateGame(gameLoaded);
					if (!animationPaused) {
						animation.update();
					}
				}
				input.clearKeys();
				input.clearMouse();

				// Render
				Graphics2D bg = (Graphics2D) canvasPanel.getGraphics();
				renderGame(gameLoaded, backgroundGraphics);
				if (consoleOpen) {
					console.draw(backgroundGraphics, 0, 0);
				}
				if (scale != 1) {
					bg.drawImage(background, 0, 0, width * scale, height
							* scale, 0, 0, width, height, null);
				} else {
					bg.drawImage(background, 0, 0, null);
				}
				bg.dispose();

				// Limit FPS
				if (!paused) {
					// Use Nanoseconds instead of currentTimeMillis which
					// has a much lower resolution(based on the OS interrupt
					// rate) and would result in too high FPS.

					// Note: There is a way to set the interrupt rate lower
					// which is done by many programs, mostly media players.
					// That means if you use currentTimeMillis and play a
					// track, your FPS is okay, but without the music it's
					// too high.

					// More on this:
					// <http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks>
					long renderTime = (System.nanoTime() - renderStart) / 10000;
					if (limitFPS) {
						try {
							Thread.sleep(Math.max(0, fpsWait
									- (renderTime / 100)));
						} catch (InterruptedException e) {
							Thread.interrupted();
							break;
						}
					}
					long allRenderTime = (System.nanoTime() - renderStart) / 10000;
					if (gameLoaded) {
						gameTime += allRenderTime / 100;
					}

					// Average FPS over 10 frames
					final int frame = (int) (System.nanoTime() % 10);
					renderStats[frame] = allRenderTime;
					renderStatsMax[frame] = renderTime;
					if (frame == 9) {
						int time = 1;
						int max = 1;
						for (int i = 0; i < 10; i++) {
							time += renderStats[i];
							max += renderStatsMax[i];
						}
						currentFPS = (int) 1000000 / time;
						maxFPS = (int) 1000000 / max;
					}
					renderStart = System.nanoTime();

				} else {
					try {
						Thread.sleep(25);
					} catch (InterruptedException e) {
						Thread.interrupted();
						break;
					}
				}
			}

			// Clean up
			gameLoader.interrupt();
			finishGame(true);
			sound.stopAll();
			if (!isApplet()) {
				frame.dispose();
			} else {
				applet = null;
			}
		}
	}

	public final GraphicsConfiguration getConfig() {
		return config;
	}

	public final BufferedImage getBackbuffer() {
		return background;
	}

	public final JPanel getCanvas() {
		return canvasPanel;
	}

	/*
	 * Game methods ------------------------------------------------------------
	 */
	public final void exitGame() {
		isRunning = false;
	}

	// Setters & Getters
	public final boolean isRunning() {
		return isRunning;
	}

	public final boolean hasSound() {
		return gameSound;
	}

	public final long getTime() {
		return gameTime;
	}

	public final void setFPS(final int fps) {
		fpsWait = (long) (1.0 / fps * 1000);
	}

	public final int getFPS() {
		return currentFPS;
	}

	public final int getMaxFPS() {
		return maxFPS;
	}

	public final void pause(final boolean mode) {
		paused = mode;
		menu.select("pause", paused);
		sound.pauseAll(paused);
	}

	public final boolean isPaused() {
		return paused;
	}

	public final void animationTime(final boolean mode) {
		animationPaused = mode;
	}

	public final boolean isAnimationPaused() {
		return animationPaused;
	}

	public final void pauseOnFocus(final boolean mode) {
		pausedOnFocus = mode;
	}

	public final boolean isPausedOnFocus() {
		return pausedOnFocus;
	}

	public final boolean isFocused() {
		return focused;
	}

	public final void setFocused(final boolean focus) {
		focused = focus;
	}

	public final boolean isConsoleOpen() {
		return consoleOpen;
	}

	public boolean consoleKey() {
		return input.keyDown(java.awt.event.KeyEvent.VK_SHIFT, true)
				&& input.keyPressed(java.awt.event.KeyEvent.VK_F1, true);
	}

	public final void setLimitFPS(final boolean limit) {
		limitFPS = limit;
		menu.select("limit", limit);
	}

	public final boolean isLimitFPS() {
		return limitFPS;
	}

	/*
	 * Saving ------------------------------------------------------------------
	 */
	public final boolean saveGame(final String filename, final String cookiename) {
		try {
			if (!isApplet()) {
				OutputStream stream = new FileOutputStream(new File(filename));
				writeSave(stream);
				stream.close();

			} else {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				writeSave(stream);
				JSObject win = JSObject.getWindow(this);
				JSObject doc = (JSObject) win.getMember("document");
				String data = cookiename + "="
						+ Base64.encodeBytes(stream.toByteArray())
						+ "; path=/; expires=Thu, 31-Dec-2019 12:00:00 GMT";

				doc.setMember("cookie", data);
				stream.close();
			}

		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void writeSave(final OutputStream stream) throws IOException {
	}

	/*
	 * Loading -----------------------------------------------------------------
	 */
	public final boolean loadGame(final String filename, final String cookiename) {
		try {
			InputStream stream = null;
			if (!isApplet()) {
				stream = new FileInputStream(filename);

			} else {
				String data = null;
				JSObject myBrowser = JSObject.getWindow(this);
				JSObject myDocument = (JSObject) myBrowser.getMember("document");

				String myCookie = (String) myDocument.getMember("cookie");
				if (myCookie.length() > 0) {
					String[] cookies = myCookie.split(";");
					for (String cookie : cookies) {
						int pos = cookie.indexOf("=");
						if (cookie.substring(0, pos).trim().equals(cookiename)) {
							data = cookie.substring(pos + 1);
							break;
						}
					}
				}

				// Decode
				if (data != null) {
					byte[] buffer = Base64.decode(data);
					stream = new ByteArrayInputStream(buffer);
				}
			}

			// No Stream
			if (stream == null) {
				return false;
			}

			// Empty Stream
			if (stream.available() <= 0) {
				stream.close();
				return false;
			}

			// Read Save
			readSave(stream);
			stream.close();

		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
		return true;
	}

	public void readSave(final InputStream stream) throws IOException {
	}
}
