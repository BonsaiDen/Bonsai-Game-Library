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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameAnimation extends GameComponent {
	private final Map<String, Animation> animations = new HashMap<String, Animation>();
	private final List<Animation> animationList = new ArrayList<Animation>();

	public GameAnimation(final Game game) {
		super(game);
	}

	public final void update() {
		for (int i = 0; i < animationList.size(); i++) {
			animationList.get(i).update();
		}
	}

	public final Animation add(final String id, final int[] frames,
			final int frameTime, final boolean loop) {
		return new Animation(id, frames, frameTime, loop);
	}

	public final void set(final String id, final int frame) {
		if (animations.containsKey(id)) {
			animations.get(id).set(frame);
		}
	}

	public final int get(final String id) {
		if (animations.containsKey(id)) {
			return animations.get(id).get();
		} else {
			return 0;
		}
	}

	public final boolean delete(final String id) {
		if (animations.containsKey(id)) {
			animationList.remove(animations.remove(id));
			return true;
		} else {
			return false;
		}
	}

	public class Animation {
		private int[] frames;
		private int pos;
		private int frameTime;
		private long lastTime;
		private boolean loop;

		public Animation(final String id, final int[] f, final int time,
				final boolean l) {
			frames = f;
			pos = 0;
			loop = l;
			frameTime = time;
			lastTime = -1;
			animations.put(id, this);
			animationList.add(this);
		}

		public final void update() {
			if (lastTime == -1) {
				lastTime = getTime();

			} else if (getTime() > lastTime + frameTime) {
				final long delta = getTime() - lastTime;
				final int frameCount = (int) (delta / frameTime);
				pos += frameCount;
				if (!loop) {
					if (pos > frames.length - 1) {
						pos = frames.length - 1;
					}
				} else {
					pos = pos % frames.length;
				}
				lastTime = getTime();
			}
		}

		public final void set(final int frame) {
			pos = frame;
		}

		public final int get() {
			return frames[pos];
		}
	}
}
