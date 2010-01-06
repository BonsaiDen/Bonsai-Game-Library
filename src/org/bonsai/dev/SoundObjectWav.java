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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundObjectWav extends SoundObject {
	public String typeName = "WAV";

	@Override
	public final void initSound(final byte[] bytes) {
		if (byteData == null) {
			byteData = bytes;
		}
		if (audioInputStream != null) {
			try {
				audioInputStream.close();
				audioInputStream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		InputStream stream = new ByteArrayInputStream(bytes.clone());
		try {
			audioInputStream = AudioSystem.getAudioInputStream(stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void startSound() {
		AudioFormat format = audioInputStream.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format);
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
		int nBytesRead = 0;
		byte[] abData = new byte[1024];

		try {
			while (nBytesRead != -1) {
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
				}
				if (status == 2) {
					line.flush();
					break;
				}
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0) {
					line.write(abData, 0, nBytesRead);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			line.drain();
			line.close();
		}
		audioInputStream = null;
	}

	@Override
	public String getTypeName() {
		return "WAV";
	}
}
