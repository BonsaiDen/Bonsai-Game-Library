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
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
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
import javax.swing.WindowConstants;

import org.bonsai.ext.Base64;

import netscape.javascript.JSObject;

public class Game extends Applet {
	// Applet
	private static final long serialVersionUID = -7860545086276629929L;

	// Graphics
	private GraphicsConfiguration config =
			GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice()
				.getDefaultConfiguration();

	private Canvas canvas;
	private BufferStrategy strategy;
	private BufferedImage background;
	private Graphics2D graphics;
	private Graphics2D backgroundGraphics;
	private int width;
	private int height;
	private int scale;

	// Game Stuff
	private boolean gameLoaded = false;
	private boolean gameSound = false;
	private boolean isRunning = true;
	protected boolean paused = false;
	private boolean focused = false;
	private boolean pausedOnFocus = false;
	private boolean animationPaused = false;

	private int currentFPS = 0;
	private long fpsWait;
	private long gameTime = 0;
	private boolean limitFPS = true;

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
				path =
						this.getClass()
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
	 * GUI ---------------------------------------------------------------------
	 */
	public final void frame(final String title, final int sizex,
			final int sizey, final boolean scaled, final boolean initMenu,
			final boolean gameMenu, final boolean gameConsole) {

		// Size
		if (scaled) {
			scale = 2;
		} else {
			scale = 1;
		}
		width = sizex;
		height = sizey;

		// Create frame
		frame = new JFrame(config);
		frame.setLayout(new BorderLayout(0, 0));
		frame.setResizable(false);
		frame.setTitle(title);
		menu = new GameMenu(this, initMenu, gameMenu);
		frame.addWindowListener(new FrameClose());
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);

		
		// Engine
		resizeFrame();
		initEngine(frame);
		if (gameConsole) {
			console = new GameConsole(this, width(), height() / 4);
		}

		initThreads();
		canvas.requestFocus();
	}

	public void setSize(final int width, final int height) {
		this.width = width;
		this.height = height;
		background = image.create(width, height, false);
		backgroundGraphics = (Graphics2D) background.getGraphics();
		setScale(1);
	}

	public void resizeFrame() {
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
		new Game().frame("Bonsai Game Library 1.00", 320, 240, false, true,
				true, true);
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
			width = getWidth() / scale;
			height = getHeight() / scale;
			setLayout(null);
			this.menu = new GameMenu(this, false, false);
			initEngine(this);
			applet = this;
			initThreads();
			stopped = false;
		}
		canvas.requestFocus();
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

	public final void setScale(final int scale) {
		try {
			canvas.setSize(width * scale, height * scale);
			canvas.createBufferStrategy(2);
			strategy = null;
			do {
				strategy = canvas.getBufferStrategy();
			} while (strategy == null);
			this.scale = scale;
			resizeFrame();
		} catch (IllegalStateException e) {
			setScale(scale);
		}
	}

	/*
	 * Gameloader --------------------------------------------------------------
	 */
	private void initEngine(final Container parent) {
		setFPS(30);

		// Components
		animation = new GameAnimation(this);
		sound = new GameSound(this);
		image = new GameImage(this);
		input = new GameInput(this);
		font = new GameFont(this);
		timer = new GameTimer(this);

		// Canvas
		canvas = new Canvas(config);
		canvas.setSize(width * scale, height * scale);
		parent.add(canvas, 0);

		// Add input listeners
		canvas.addMouseListener(input);
		canvas.addMouseMotionListener(input);
		canvas.addKeyListener(input);
		canvas.addFocusListener(input);
		canvas.setIgnoreRepaint(true);

		// Create the buffer strategy
		background = image.create(width, height, false);
		canvas.createBufferStrategy(2);
		do {
			strategy = canvas.getBufferStrategy();
		} while (strategy == null);

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
			gameSound = sound.init(); // This actually takes time!
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

	public void initGame(final boolean loaded) {
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
			int fpsCount = 0;
			long fpsTime = 0;

			backgroundGraphics = (Graphics2D) background.getGraphics();

			main: while (true) {
				// Pausing
				long renderStart = System.nanoTime();
				if (!consoleOpen && input.keyPressed(java.awt.event.KeyEvent.VK_P, true)) {
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
				do {
					Graphics2D bg = getBuffer();
					if (!isRunning) {
						break main;
					}
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
				} while (!updateScreen());

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
					long renderTime =
							(System.nanoTime() - renderStart) / 1000000;
					if (limitFPS) {
						try {
							Thread.sleep(Math.max(0, fpsWait - renderTime));
						} catch (InterruptedException e) {
							Thread.interrupted();
							break;
						}
					}
					renderTime = (System.nanoTime() - renderStart) / 1000000;
					if (gameLoaded) {
						gameTime += renderTime;
					}
					fpsTime += (System.nanoTime() - renderStart) / 1000000;
					fpsCount += 1;
					if (fpsTime > 1000 - fpsWait) {
						currentFPS = fpsCount;
						fpsCount = 0;
						fpsTime = 0;
					}

				} else {
					try {
						Thread.sleep(100);
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

	private Graphics2D getBuffer() {
		if (graphics == null) {
			try {
				graphics = (Graphics2D) strategy.getDrawGraphics();
			} catch (IllegalStateException e) {
				return null;
			}
		}
		return graphics;
	}

	private boolean updateScreen() {
		graphics.dispose();
		graphics = null;
		try {
			strategy.show();
			Toolkit.getDefaultToolkit().sync();
			return (!strategy.contentsLost());

		} catch (NullPointerException e) {
			return true;

		} catch (IllegalStateException e) {
			return true;
		}
	}

	public final GraphicsConfiguration getConfig() {
		return config;
	}

	public final BufferedImage getBackbuffer() {
		return background;
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

	public final void pause(final boolean mode) {
		paused = mode;
		menu.getItem("pause").setSelected(paused);
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
		return input.keyDown(java.awt.event.KeyEvent.VK_SHIFT, true) && input.keyPressed(java.awt.event.KeyEvent.VK_F1, true);
	}
	
	public final void setLimitFPS(final boolean limit) {
		limitFPS = limit;
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
				String data =
						cookiename
								+ "="
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
				JSObject myDocument =
						(JSObject) myBrowser.getMember("document");

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
