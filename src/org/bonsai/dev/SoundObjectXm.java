package org.bonsai.dev;

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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import org.xm.lib.FastTracker2;
import org.xm.lib.IBXM;

public class SoundObjectXm extends SoundObject {
	public String typeName = "XM";
	private IBXM ibxm;

	@Override
	public final void initSound(final byte[] bytes) {
		if (byteData == null) {
			byteData = bytes;
		}
		ibxm = new IBXM(22050);

		// Create Stream
		InputStream stream = new ByteArrayInputStream(bytes);
		DataInputStream data_input_stream = new DataInputStream(stream);
		byte[] xm_header = new byte[60];
		try {
			data_input_stream.readFully(xm_header);
			ibxm.set_module(FastTracker2.load_xm(xm_header, data_input_stream));
			stream.close();
			data_input_stream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void startSound() {
		try {
			line = AudioSystem.getSourceDataLine(new AudioFormat(22050, 16, 2,
					true, true));
			line.open();
		} catch (LineUnavailableException e) {
			return;
		} catch (Exception e) {
			return;
		}
		line.start();
		playSound();
		while (loop && status != 2) {
			initSound(byteData);
			playSound();
		}
		status = 0;
	}

	private void playSound() {
		int song_duration = ibxm.calculate_song_duration();
		byte[] output_buffer = new byte[1024 * 4];
		int play_position = 0;

		// Get Time Tick
		long lastTick = System.nanoTime();
		long tick;
		int loopCount = 1;
		while (true) {
			long delta = System.nanoTime() - lastTick;
			tick = delta / loopCount / 1000000;
			if (tick == 0) {
				tick = 1;
			}
			loopCount++;
			if (loopCount > 256) {
				loopCount = 1;
				lastTick = System.nanoTime();
			}

			// Pause
			if (status == 3) {
				line.stop();
				line.flush();
				while (status == 3) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						status = 2;
					}
					line.start();
				}

				// Stop
			} else if (status == 2) {
				line.flush();
				break;

				// Volume
			} else if (volumeChanged) {
				float add = toVolumeDifference / (toVolumeTime / tick);
				if (volume > toVolume) {
					volume = volume - add;
					if (volume < toVolume) {
						volume = toVolume;
						volumeChanged = false;
					}

				} else if (volume < toVolume) {
					volume = volume + add;
					if (volume > toVolume) {
						volumeChanged = false;
						volume = toVolume;
					}
				}
			}

			int frames = song_duration - play_position;
			if (frames > 1024)
				frames = 1024;
			ibxm.get_audio(output_buffer, frames, volume);
			line.write(output_buffer, 0, frames * 4);
			play_position += frames;
			if (play_position >= song_duration) {
				play_position = 0;
				if (!loop) {
					break;
				}
			}
		}
		line.drain();
		line.close();
	}

	@Override
	public String getTypeName() {
		return "XM";
	}
}
