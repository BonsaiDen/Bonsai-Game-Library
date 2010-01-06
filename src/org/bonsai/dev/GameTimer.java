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

import java.util.HashMap;

public class GameTimer extends GameComponent {
	private HashMap<String, Timer> timers = new HashMap<String, Timer>();

	public GameTimer(final Game game) {
		super(game);
	}

	public final Timer add(final String id, final long timeout) {
		return new Timer(id, timeout);
	}

	public final boolean expired(final String id) {
		return expired(id, -1);
	}

	public final boolean expired(final String id, final long time) {
		if (timers.containsKey(id)) {
			return timers.get(id).expired(time);
		} else {
			return false;
		}
	}

	public final boolean pending(final String id) {
		return pending(id, -1);
	}

	public final boolean pending(final String id, final long time) {
		if (timers.containsKey(id)) {
			return timers.get(id).pending(time);
		} else {
			return false;
		}
	}

	public final void set(final String id) {
		set(id, 0);
	}

	public final void set(final String id, final long time) {
		if (timers.containsKey(id)) {
			timers.get(id).set(time);
		}
	}

	public final boolean delete(final String id) {
		if (timers.containsKey(id)) {
			timers.remove(id);
			return true;
		} else {
			return false;
		}
	}

	protected class Timer {
		private long timeout = 0;
		private long time = 0;

		public Timer(final String id, final long t) {
			timers.put(id, this);
			set(0);
			timeout = t;
		}

		public final void set(final long i) {
			time = game.getTime() + i;
		}

		public final boolean expired(final long i) {
			if (i == -1) {
				return game.getTime() > time + timeout;
			} else {
				return game.getTime() > time + i;
			}
		}

		public final boolean pending(final long i) {
			if (i == -1) {
				return game.getTime() < time + timeout;

			} else {
				return game.getTime() < time + i;
			}
		}
	}
}
