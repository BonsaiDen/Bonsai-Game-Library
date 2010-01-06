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
 *  
 *  In order to be able to use OggVorbis files you need to include JOrbis in 
 *  your project <http://www.jcraft.com/jorbis/>.
 */

package org.bonsai.dev;

import java.io.*;

import com.jcraft.jorbis.*;
import com.jcraft.jogg.*;

import javax.sound.sampled.*;


/**
 * 
 * In order to use OGG Vorbis sound files you need to add the JOrbis Project 
 * onto your project build path.
 *
 */

public class SoundObjectOgg extends SoundObject {
	public String typeName = "OGG";
	InputStream bitStream = null;

	static final int BUFSIZE = 512;
	int convsize = BUFSIZE * 2;
	byte[] convbuffer = new byte[convsize];
	byte[] emptyBuffer = new byte[convsize];

	SyncState oy;
	StreamState os;
	Page og;
	Packet op;
	Info vi;
	Comment vc;
	DspState vd;
	Block vb;

	byte[] buffer = null;
	int bytes = 0;

	int rate = 0;
	int channels = 0;
	SourceDataLine outputLine = null;

	private void initJorbis() {
		oy = new SyncState();
		os = new StreamState();
		og = new Page();
		op = new Packet();

		vi = new Info();
		vc = new Comment();
		vd = new DspState();
		vb = new Block(vd);

		buffer = null;
		bytes = 0;
	}

	private SourceDataLine getOutputLine(final int c, final int r) {
		if (outputLine == null || rate != r || channels != c) {
			if (outputLine != null) {
				outputLine.drain();
				outputLine.stop();
				outputLine.close();
			}
			initAudio(c, r);
			outputLine.start();
		}
		return outputLine;
	}

	private void initAudio(final int c, final int r) {
		try {
			AudioFormat audioFormat = new AudioFormat(r, 16, c, true, // PCM_Signed
				false // littleEndian
					);
			DataLine.Info info =
					new DataLine.Info(SourceDataLine.class,
						audioFormat,
						AudioSystem.NOT_SPECIFIED);
			if (!AudioSystem.isLineSupported(info)) {
				return;
			}

			try {
				outputLine = (SourceDataLine) AudioSystem.getLine(info);
				outputLine.open(audioFormat);
			} catch (LineUnavailableException ex) {
				return;
			} catch (IllegalArgumentException ex) {
				return;
			}

			rate = r;
			channels = c;

		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

	@Override
	public final void initSound(final byte[] dataBytes) {
		if (byteData == null) {
			byteData = dataBytes;
		}
		if (bitStream != null) {
			try {
				bitStream.close();
				bitStream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		bitStream = new ByteArrayInputStream(dataBytes.clone());
	}

	@Override
	public final void startSound() {
		playSound();
		while (loop && status != 2) {
			initSound(byteData);
			playSound();
		}
		status = 0;
	}

	public final void playSound() {
		initJorbis();
		loop: while (true) {
			int eos = 0;

			int index = oy.buffer(BUFSIZE);
			buffer = oy.data;
			try {
				bytes = bitStream.read(buffer, index, BUFSIZE);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			oy.wrote(bytes);

			if (oy.pageout(og) != 1) {
				if (bytes < BUFSIZE)
					break;
				return;
			}
			os.init(og.serialno());
			os.reset();

			vi.init();
			vc.init();
			if (os.pagein(og) < 0) {
				return;
			}
			if (os.packetout(op) != 1) {
				break;
			}
			if (vi.synthesis_headerin(vc, op) < 0) {
				return;
			}
			int i = 0;
			while (i < 2) {
				while (i < 2) {
					int result = oy.pageout(og);
					if (result == 0)
						break; // Need more data
					if (result == 1) {
						os.pagein(og);
						while (i < 2) {
							result = os.packetout(op);
							if (result == 0)
								break;
							if (result == -1) {
								// return;
								break loop;
							}
							vi.synthesis_headerin(vc, op);
							i++;
						}
					}
				}

				index = oy.buffer(BUFSIZE);
				buffer = oy.data;
				try {
					bytes = bitStream.read(buffer, index, BUFSIZE);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				if (bytes == 0 && i < 2) {
					return;
				}
				oy.wrote(bytes);
			}

			convsize = BUFSIZE / vi.channels;

			vd.synthesis_init(vi);
			vb.init(vd);

			float[][][] _pcmf = new float[1][][];
			int[] _index = new int[vi.channels];

			getOutputLine(vi.channels, vi.rate);

			while (eos == 0) {
				while (eos == 0) {
					int result = oy.pageout(og);
					if (result == 0)
						break;
					if (result != -1) {
						os.pagein(og);
						if (og.granulepos() == 0) {
							eos = 1;
							break;
						}

						// Get Time Tick
						long lastTick = System.nanoTime();
						int loopCount = 1;
						long tick = 20;
						while (true) {
							long delta = System.nanoTime() - lastTick;
							if (delta > 10000000) {
								tick = delta / loopCount / 1000000;
								if (tick == 0) {
									tick = 1;
								}
							}
							loopCount++;

							// Stopped
							if (status == 2) {
								try {
									bitStream.close();
									outputLine.flush();
									outputLine.stop();
									outputLine.close();
									outputLine = null;
								} catch (Exception e) {
									e.printStackTrace();
								}
								return;

								// Paused
							} else if (status == 3) {
								outputLine.stop();
								outputLine.flush();
								while (status == 3) {
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										status = 2;
									}
									outputLine.start();
								}

								// Volume
							} else if (volumeChanged) {
								float add =
										toVolumeDifference
												/ (toVolumeTime / tick);
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

							// Packet
							result = os.packetout(op);
							if (result == 0)
								break;
							if (result != -1) {
								int samples;
								if (vb.synthesis(op) == 0) {
									vd.synthesis_blockin(vb);
								}

								while ((samples =
										vd.synthesis_pcmout(_pcmf, _index)) > 0) {

									float[][] pcmf = _pcmf[0];
									int bout =
											(samples < convsize ? samples
													: convsize);

									if (volume != 0) {
										for (i = 0; i < vi.channels; i++) {
											int ptr = i * 2;
											int mono = _index[i];
											for (int j = 0; j < bout; j++) {
												int val =
														(int) (pcmf[i][mono + j] * (32767. * volume));
												if (val > 32767) {
													val = 32767;
												}
												if (val < -32768) {
													val = -32768;
												}
												if (val < 0)
													val = val | 0x8000;

												convbuffer[ptr] = (byte) (val);
												convbuffer[ptr + 1] =
														(byte) (val >>> 8);

												ptr += 2 * (vi.channels);
											}
										}
										outputLine.write(convbuffer, 0, 2
												* vi.channels * bout);

									} else {
										outputLine.write(emptyBuffer, 0, 2
												* vi.channels * bout);
									}

									vd.synthesis_read(bout);
								}
							}
						}
						if (og.eos() != 0) {
							eos = 1;
						}
					}
				}

				if (eos == 0) {
					index = oy.buffer(BUFSIZE);
					buffer = oy.data;
					try {
						bytes = bitStream.read(buffer, index, BUFSIZE);
					} catch (Exception e) {
						return;
					}
					if (bytes == -1) {
						break;
					}
					oy.wrote(bytes);
					if (bytes == 0) {
						eos = 1;
					}
				}
			}

			os.clear();
			vb.clear();
			vd.clear();
			vi.clear();
		}

		try {
			if (bitStream != null) {
				outputLine.drain();
				bitStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getTypeName() {
		return "OGG";
	}
}
