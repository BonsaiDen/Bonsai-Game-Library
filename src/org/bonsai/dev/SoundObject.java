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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;

public abstract class SoundObject extends Thread {
	public float volume = 1.0f;
	public float toVolume = 0.0f;
	public int toVolumeTime = 1000;
	public float toVolumeDifference = 1.0f;
	public boolean volumeChanged = false;
	public int status = 1;
	public boolean loop = false;
	protected byte[] byteData = null;
	public SourceDataLine line = null;
	public AudioInputStream audioInputStream = null;

	public abstract String getTypeName();
	
	public abstract void initSound(final byte[] bytes);

	public abstract void startSound();

	@Override
	public final void run() {
		startSound();
	}
}
