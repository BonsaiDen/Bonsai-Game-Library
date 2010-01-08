package org.bonsai.dev;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class GameMenu extends GameComponent implements ActionListener {
	private transient boolean active = false;
	private transient JMenuBar menuBar = null;
	private transient final Map<String, JMenu> menus = new HashMap<String, JMenu>();
	private transient final Map<String, JMenuItem> menuItems = new HashMap<String, JMenuItem>();
	private transient final Map<String, ButtonGroup> menuGroups = new HashMap<String, ButtonGroup>();

	public GameMenu(final Game game, final boolean init, final boolean gameMenu) {
		super(game);
		if (init) {
			active = true;
			menuBar = new JMenuBar();
			game.getFrame().setJMenuBar(menuBar);
			if (gameMenu) {
				add("Game");
				addCheckItem("Game", "Pause", "pause");
				addCheckItem("Game", "Double", "scale");
				addCheckItem("Game", "Limit FPS", "limit");
				select("scale", game.scale() == 2);
				select("limit", game.isLimitFPS());
				get("Game").addSeparator();
				addItem("Game", "Exit", "exit");
			}
		}
	}

	public final int getSize() {
		return active ? menuBar.getHeight() : 0;
	}

	public final void addRadioItem(final String menuID, final String name,
			final String cmd, final String group) {
		if (active) {
			ButtonGroup bGroup;
			if (menuGroups.containsKey(group)) {
				bGroup = menuGroups.get(group);
			} else {
				bGroup = new ButtonGroup();
				menuGroups.put(group, bGroup);
			}
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
			bGroup.add(item);
			addItems(menuID, item, cmd);
		}
	}

	public final void addCheckItem(final String menuID, final String name,
			final String cmd) {
		addItems(menuID, new JCheckBoxMenuItem(name), cmd);
	}

	public final void addItem(final String menuID, final String name,
			final String cmd) {
		addItems(menuID, new JMenuItem(name), cmd);
	}

	private void addItems(final String menuID, final JMenuItem item,
			final String cmd) {
		if (active) {
			final JMenu menu = get(menuID);
			if (menu != null && !menuItems.containsKey(cmd)) {
				item.setActionCommand(cmd);
				item.addActionListener(this);
				menu.add(item);
				menuItems.put(cmd, item);
			}
		}
	}

	public final void enable(final String menuID, final boolean enable) {
		if (active) {
			get(menuID).setEnabled(enable);
		}
	}

	public final void enable(final boolean enable) {
		if (active) {
			for (JMenu menu : menus.values()) {
				menu.setEnabled(enable);
			}
		}
	}

	public final JMenu add(final String name) {
		JMenu menu = null;
		if (active && !menus.containsKey(name)) {
			menu = new JMenu(name);
			menus.put(name, menu);
			menuBar.add(menu);
			menuBar.validate();
			menu.setEnabled(false);
		}
		return menu;
	}

	public void remove(final String name) {
		if (active && menus.containsKey(name)) {
			menuBar.remove(menus.get(name));
			menus.remove(name);
		}
	}

	public final JMenu get(final String name) {
		if (active && menus.containsKey(name)) {
			return menus.get(name);
		}
		return null;
	}

	public final JMenuItem getItem(final String name) {
		if (active && menuItems.containsKey(name)) {
			return menuItems.get(name);
		}
		return null;
	}

	public final void select(final String name, final boolean selected) {
		if (active) {
			getItem(name).setSelected(selected);
		}
	}

	public final void actionPerformed(final ActionEvent event) {
		final String cmd = event.getActionCommand();
		if ("exit".equals(cmd)) {
			game.exitGame();

		} else if ("pause".equals(cmd)) {
			game.pause(!game.isPaused());

		} else if ("scale".equals(cmd)) {
			game.setScale(game.scale() == 1 ? 2 : 1);

		} else if("limit".equals(cmd)) {
			game.setLimitFPS(!game.isLimitFPS());
			
		} else {
			game.onMenu(cmd);
		}
	}
}
