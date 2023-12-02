/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;

import net.minecraft.client.Minecraft;
import net.minecraft.core.util.helper.ChatAllowedCharacters;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import sunsetsatellite.computers.Computers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Computer {
	private TileEntityComputer m_owner;
	private int m_id;
	private Terminal m_terminal;
	private FileSystem m_fileSystem;
	private boolean m_on;
	private boolean m_onDesired;
	private boolean m_rebootDesired;
	private boolean m_stopped;
	private boolean m_aborted;
	private boolean m_blinking;
	private ArrayList<Timer> m_timers;
	private ArrayList<Alarm> m_alarms;
	private ArrayList<HTTPRequest> m_httpRequests;
	private LuaValue m_mainFunction;
	private LuaValue m_globals;
	private boolean[] m_output;
	private int[] m_bundledOutput;
	private boolean m_outputChanged;
	private boolean[] m_input;
	private boolean[] m_pendingInput;
	private int[] m_bundledInput;
	private int[] m_pendingBundledInput;
	private boolean m_inputChanged;
	private DriveInfo[] m_drives;
	private int m_queuedDisc;
	private int m_playingDisc;
	private boolean m_restartDisc;
	private double m_clock;
	private double m_time;
	private static final String[] sides = new String[]{"top", "bottom", "front", "back", "left", "right"};

	public Computer(TileEntityComputer owner, Terminal terminal) {
		this.m_owner = owner;
		ComputerThread.start();
		this.m_id = -1;
		this.m_terminal = terminal;
		this.m_fileSystem = null;
		this.m_on = false;
		this.m_onDesired = false;
		this.m_rebootDesired = false;
		this.m_stopped = false;
		this.m_aborted = false;
		this.m_blinking = false;
		this.m_timers = new ArrayList();
		this.m_alarms = new ArrayList();
		this.m_httpRequests = new ArrayList();
		this.m_mainFunction = null;
		this.m_globals = null;
		this.m_output = new boolean[6];
		this.m_bundledOutput = new int[6];
		this.m_outputChanged = false;
		this.m_input = new boolean[6];
		this.m_bundledInput = new int[6];
		this.m_inputChanged = false;
		this.m_drives = new DriveInfo[6];
		for (int i = 0; i < 6; ++i) {
			this.m_drives[i] = new DriveInfo();
		}
		this.m_queuedDisc = -1;
		this.m_playingDisc = -1;
		this.m_restartDisc = false;
		this.m_clock = 0.0;
		this.m_time = 0.0;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void turnOn() {
		Computer computer = this;
		synchronized (computer) {
			this.m_onDesired = true;
			this.m_rebootDesired = false;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void turnOff() {
		Computer computer = this;
		synchronized (computer) {
			this.m_onDesired = false;
			this.m_rebootDesired = false;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void reboot() {
		Computer computer = this;
		synchronized (computer) {
			this.m_onDesired = false;
			this.m_rebootDesired = true;
		}
	}

	public boolean isOn() {
		return this.m_on;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void abort() {
		Computer computer = this;
		synchronized (computer) {
			if (this.m_on) {
				System.out.println("aborting");
				this.m_aborted = true;
			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void destroy() {
		Computer computer = this;
		synchronized (computer) {
			if (this.m_on) {
				this.m_onDesired = false;
				this.m_rebootDesired = false;
				this.stopComputer();
			}
			DriveInfo[] arrdriveInfo = this.m_drives;
			synchronized (this.m_drives) {
				if (this.m_playingDisc >= 0) {
					this.m_playingDisc = -1;
					this.m_queuedDisc = -1;
					this.m_owner.playRecord(null);
				}// ** MonitorExit[var2_2] (shouldn't be in output)

			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public String getUserDir() {
		Computer computer = this;
		synchronized (computer) {
			if (this.m_id >= 0) {
				return Integer.toString(this.m_id);
			}
			return null;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void setUserDir(String string) {
		Computer computer = this;
		synchronized (computer) {
			int id;
			try {
				id = Integer.parseInt(string);
			} catch (NumberFormatException e) {
				return;
			}
			assert (this.m_id == -1 || this.m_id == id);
			this.m_id = id;
		}
	}

	public void pressKey(final char ch, final int key) {
		if (this.m_on) {
			this.queueLuaEvent(new Event() {

								   @Override
								   public LuaValue[] getArguments() {
									   return new LuaValue[]{LuaValue.valueOf("key"), LuaValue.valueOf(key)};
								   }
							   }
			);
			if (ChatAllowedCharacters.ALLOWED_CHARACTERS.indexOf(ch) >= 0) {
				this.queueLuaEvent(new Event() {

									   @Override
									   public LuaValue[] getArguments() {
										   return new LuaValue[]{LuaValue.valueOf("char"), LuaValue.valueOf("" + ch)};
									   }
								   }
				);
			}
		}
	}

	public void terminate() {
		if (this.m_on) {
			this.queueLuaEvent(new Event() {

								   @Override
								   public LuaValue[] getArguments() {
									   return new LuaValue[]{LuaValue.valueOf("terminate")};
								   }
							   }
			);
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
// Enabled aggressive block sorting
// Enabled unnecessary exception pruning
// Enabled aggressive exception aggregation
	 */
	public void advance(double _dt) {
		Object object = this;
		synchronized (this) {
			if (!this.m_on && this.m_rebootDesired) {
				this.turnOn();
			}
			if (!this.m_on && this.m_onDesired) {
				this.startComputer();
			}
			if (this.m_on && !this.m_onDesired) {
				this.stopComputer();
			}// ** MonitorExit[var3_2 /* !! */ ] (shouldn't be in output)

			object = this.m_drives;
			synchronized (this.m_drives) {
				if (this.m_queuedDisc != this.m_playingDisc || this.m_restartDisc) {
					this.m_playingDisc = this.m_queuedDisc;
					this.m_restartDisc = false;
					if (this.m_queuedDisc >= 0) {
						this.m_owner.playRecord(this.m_drives[this.m_playingDisc].recordName);
					} else {
						this.m_owner.playRecord(null);
					}
				}
				for (int i = 0; i < 6; ++i) {
					if (!this.m_drives[i].eject) continue;
					this.m_owner.ejectDisk(i);
					this.m_drives[i].eject = false;
				}// ** MonitorExit[var3_2 /* !! */ ] (shouldn't be in output)

				object = this;
				synchronized (this) {
					if (this.m_on) {
						boolean[] i = this.m_input;
						synchronized (i) {
							if (this.m_inputChanged) {
								if (this.m_id == 42) {
									System.out.println("QUEUE " + this.m_bundledInput[3]);
								}
								this.queueLuaEvent(new Event() {

													   @Override
													   public LuaValue[] getArguments() {
														   if (Computer.this.m_id == 42) {
															   System.out.println("SERVE " + Computer.this.m_bundledInput[3]);
														   }
														   return new LuaValue[]{LuaValue.valueOf("redstone")};
													   }
												   }
								);
								this.m_inputChanged = false;
							}
						}
						this.m_clock += _dt;
						ArrayList<Timer> j = this.m_timers;
						synchronized (j) {
							Iterator it = this.m_timers.iterator();
							while (it.hasNext()) {
								Timer t = (Timer) it.next();
								t.timeLeft -= _dt;
								if (!(t.timeLeft <= 0.0)) continue;
								final LuaValue token = t.token;
								this.queueLuaEvent(new Event() {

													   @Override
													   public LuaValue[] getArguments() {
														   return new LuaValue[]{LuaValue.valueOf("timer"), token};
													   }
												   }
								);
								it.remove();
							}
						}
						ArrayList<Alarm> k = this.m_alarms;
						synchronized (k) {
							double prevTime = this.m_time;
							double time = (double) ((this.m_owner.worldObj.getWorldTime() + 6000L) % 24000L) / 1000.0;
							double timeLast = prevTime;
							double timeNow = time;
							if (timeNow < timeLast) {
								timeNow += 24.0;
							}
							ArrayList<Alarm> finishedAlarms = null;
							Iterator it = this.m_alarms.iterator();
							while (it.hasNext()) {
								Alarm al = (Alarm) it.next();
								double t = al.time;
								if (t < timeLast) {
									t += 24.0;
								}
								if (!(timeNow >= t)) continue;
								if (finishedAlarms == null) {
									finishedAlarms = new ArrayList<Alarm>();
								}
								finishedAlarms.add(al);
								it.remove();
							}
							if (finishedAlarms != null) {
								Collections.sort(finishedAlarms);
								it = finishedAlarms.iterator();
								while (it.hasNext()) {
									final LuaValue token = ((Alarm) it.next()).token;
									this.queueLuaEvent(new Event() {

														   @Override
														   public LuaValue[] getArguments() {
															   return new LuaValue[]{LuaValue.valueOf("alarm"), token};
														   }
													   }
									);
								}
							}
							this.m_time = time;
						}
						ArrayList<HTTPRequest> l = this.m_httpRequests;
						synchronized (l) {
							Iterator it = this.m_httpRequests.iterator();
							while (it.hasNext()) {
								HTTPRequest h = (HTTPRequest) it.next();
								if (!h.isComplete()) continue;
								final String url = h.getURL();
								if (h.wasSuccessful()) {
									final BufferedReader contents = h.getContents();
									this.queueLuaEvent(new Event() {

														   @Override
														   public LuaValue[] getArguments() {
															   LuaValue result = Computer.this.wrapBufferedReader(contents);
															   return new LuaValue[]{LuaValue.valueOf("http_success"), LuaValue.valueOf(url), result};
														   }
													   }
									);
								} else {
									this.queueLuaEvent(new Event() {

														   @Override
														   public LuaValue[] getArguments() {
															   return new LuaValue[]{LuaValue.valueOf("http_failure"), LuaValue.valueOf(url)};
														   }
													   }
									);
								}
								it.remove();
							}
						}
					}// ** MonitorExit[var3_2 /* !! */ ] (shouldn't be in output)

					object = this.m_terminal;
					synchronized (object) {
						boolean blinking = this.m_terminal.getCursorBlink() && this.m_terminal.getCursorX() >= 0 && this.m_terminal.getCursorX() < this.m_terminal.getWidth() && this.m_terminal.getCursorY() >= 0 && this.m_terminal.getCursorY() < this.m_terminal.getHeight();
						if (blinking == this.m_blinking) return;
						boolean[] arrbl = this.m_output;
						synchronized (this.m_output) {
							this.m_outputChanged = true;
							this.m_blinking = blinking;
// ** MonitorExit[var5_7] (shouldn't be in output)
							return;
						}
					}
				}
			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public boolean pollChanged() {
		boolean[] arrbl = this.m_output;
		synchronized (this.m_output) {
			if (this.m_outputChanged) {
				this.m_outputChanged = false;
				//m_owner.worldObj.markBlocksDirty(m_owner.xCoord, m_owner.yCoord, m_owner.zCoord, m_owner.xCoord, m_owner.yCoord, m_owner.zCoord);
				//m_owner.worldObj.notifyBlocksOfNeighborChange(m_owner.xCoord, m_owner.yCoord, m_owner.zCoord, BetterThanComputers.computer.blockID);
// ** MonitorExit[var1_1] (shouldn't be in output)
				return true;
			}// ** MonitorExit[var1_1] (shouldn't be in output)

			return false;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public boolean isBlinking() {
		Terminal terminal = this.m_terminal;
		synchronized (terminal) {
			return this.isOn() && this.m_blinking;
		}
	}

	private File getUserDir(boolean _create) {
		File baseUserDir = new File(Computers.getWorldDir(this.m_owner.worldObj), "/computer/");
		if (this.m_id < 0) {
			if (!_create) {
				return null;
			}
			this.m_id = 0;
			while (new File(baseUserDir, Integer.toString(this.m_id)).exists()) {
				++this.m_id;
			}
		}
		File userDir = new File(baseUserDir, Integer.toString(this.m_id));
		userDir.mkdirs();
		return userDir;
	}

	private void initFileSystem() {
		File romDir = new File(getBiosFolder(), "rom");
		File userDir = this.getUserDir(true);
		try {
			this.m_fileSystem = new FileSystem(userDir, false);
			this.m_fileSystem.mount("rom", romDir, true);
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}

	private void destroyFileSystem() {
		File userDir = this.getUserDir(false);
		if (userDir != null) {
			userDir.delete();
			this.m_id = -1;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void setBundledOutput(int side, int combination) {
		boolean[] arrbl = this.m_output;
		synchronized (this.m_output) {
			if (this.m_bundledOutput[side] != combination) {
				this.m_bundledOutput[side] = combination;
				this.m_outputChanged = true;
			}// ** MonitorExit[var3_3] (shouldn't be in output)

			return;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public int getBundledOutput(int side) {
		boolean[] arrbl = this.m_output;
		synchronized (this.m_output) {
			if (this.isOn()) {
// ** MonitorExit[var2_2] (shouldn't be in output)
				return this.m_bundledOutput[side];
			}// ** MonitorExit[var2_2] (shouldn't be in output)

			return 0;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void setOutput(int side, boolean onOff) {
		boolean[] arrbl = this.m_output;
		synchronized (this.m_output) {
			if (this.m_output[side] != onOff) {
				this.m_output[side] = onOff;
				this.m_outputChanged = true;
			}// ** MonitorExit[var3_3] (shouldn't be in output)

			return;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public boolean getOutput(int side) {
		boolean[] arrbl = this.m_output;
		synchronized (this.m_output) {
// ** MonitorExit[var2_2] (shouldn't be in output)
			return this.isOn() && this.m_output[side];
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void setBundledInput(int side, int mask) {
		boolean[] arrbl = this.m_input;
		synchronized (this.m_input) {
			if (this.m_bundledInput[side] != mask) {
				this.m_bundledInput[side] = mask;
				this.m_inputChanged = true;
			}// ** MonitorExit[var3_3] (shouldn't be in output)

			return;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private int getBundledInput(int side) {
		boolean[] arrbl = this.m_input;
		synchronized (this.m_input) {
// ** MonitorExit[var2_2] (shouldn't be in output)
			return this.m_bundledInput[side];
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void setInput(int side, boolean onOff) {
		boolean[] arrbl = this.m_input;
		synchronized (this.m_input) {
			if (this.m_input[side] != onOff) {
				this.m_input[side] = onOff;
				this.m_inputChanged = true;
			}// ** MonitorExit[var3_3] (shouldn't be in output)

			return;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private boolean getInput(int side) {
		boolean[] arrbl = this.m_input;
		synchronized (this.m_input) {
// ** MonitorExit[var2_2] (shouldn't be in output)
			return this.m_input[side];
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void setDiskInfo(final int side, boolean occupied, int dataID, String recordName) {
		DriveInfo[] arrdriveInfo = this.m_drives;
		synchronized (this.m_drives) {
			DriveInfo drive = this.m_drives[side];
			if (occupied != drive.occupied || drive.dataID != dataID || drive.recordName != recordName) {
				if (drive.mountPath != null) {
					this.queueUnmount(drive.mountPath);
					drive.mountPath = null;
				}
				if (side == this.m_queuedDisc) {
					this.m_queuedDisc = -1;
				}
				if (drive.occupied) {
					this.queueLuaEvent(new Event() {

										   @Override
										   public LuaValue[] getArguments() {
											   return new LuaValue[]{LuaValue.valueOf("disk_eject"), LuaValue.valueOf(sides[side])};
										   }
									   }
					);
				}
				drive.occupied = occupied;
				drive.dataID = dataID;
				drive.recordName = recordName;
				if (drive.dataID >= 0) {
					this.queueMount(side);
				}
				if (drive.occupied) {
					this.queueLuaEvent(new Event() {

										   @Override
										   public LuaValue[] getArguments() {
											   return new LuaValue[]{LuaValue.valueOf("disk"), LuaValue.valueOf(sides[side])};
										   }
									   }
					);
				}
			}// ** MonitorExit[var5_5] (shouldn't be in output)

			return;
		}
	}

	private void tryAbort() {
		LuaValue coroutine;
		if (this.m_stopped && (coroutine = this.m_globals.get("coroutine")) != null) {
			do {
				System.out.println("yielding");
				coroutine.get("yield").call();
			} while (true);
		}
		if (this.m_aborted) {
			this.m_aborted = false;
			throw new LuaError("Too long without yielding");
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void initLua() {
		LuaTable globals = JsePlatform.standardGlobals();
		LuaValue loadfile = globals.get("loadfile");
		globals.set("io", LuaValue.NIL);
		globals.set("dofile", LuaValue.NIL);
		globals.set("loadfile", LuaValue.NIL);
		globals.set("package", LuaValue.NIL);
		globals.set("module", LuaValue.NIL);
		globals.set("debug", LuaValue.NIL);
		globals.set("print", LuaValue.NIL);
		if (Computers.enableAPI_luajava <= 0) {
			globals.set("luajava", LuaValue.NIL);
		}
		LuaTable term = new LuaTable();
		term.set("write", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					String text = "";
					if (!_arg.isnil()) {
						text = _arg.toString();
					}
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.write(text);
						Computer.this.m_terminal.setCursorPos(Computer.this.m_terminal.getCursorX() + text.length(), Computer.this.m_terminal.getCursorY());
					}
					return LuaValue.NIL;
				}
			}
		);
		term.set("scroll", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					int y = _arg.checkint();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.scroll(y);
					}
					return LuaValue.NIL;
				}
			}
		);
		term.set("setCursorPos", (LuaValue) new TwoArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _x, LuaValue _y) {
					Computer.this.tryAbort();
					int x = _x.checkint() - 1;
					int y = _y.checkint() - 1;
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.setCursorPos(x, y);
					}
					return LuaValue.NIL;
				}
			}
		);
		term.set("setCursorBlink", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					boolean b = _arg.checkboolean();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.setCursorBlink(b);
					}
					return LuaValue.NIL;
				}
			}
		);
		term.set("getCursorPos", (LuaValue) new VarArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public Varargs invoke(Varargs _args) {
					int y;
					int x;
					Computer.this.tryAbort();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						x = Computer.this.m_terminal.getCursorX();
						y = Computer.this.m_terminal.getCursorY();
					}
					return LuaValue.varargsOf(new LuaValue[]{LuaValue.valueOf(x + 1), LuaValue.valueOf(y + 1)});
				}
			}
		);
		term.set("getSize", (LuaValue) new VarArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public Varargs invoke(Varargs _args) {
					int height;
					int width;
					Computer.this.tryAbort();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						width = Computer.this.m_terminal.getWidth();
						height = Computer.this.m_terminal.getHeight();
					}
					return LuaValue.varargsOf(new LuaValue[]{LuaValue.valueOf(width), LuaValue.valueOf(height)});
				}
			}
		);
		term.set("clear", (LuaValue) new ZeroArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.clear();
					}
					return LuaValue.NIL;
				}
			}
		);
		term.set("clearLine", (LuaValue) new ZeroArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Terminal terminal = Computer.this.m_terminal;
					synchronized (terminal) {
						Computer.this.m_terminal.clearLine();
					}
					return LuaValue.NIL;
				}
			}
		);
		globals.set("term", (LuaValue) term);
		LuaTable redstone = new LuaTable();
		redstone.set("getSides", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					LuaTable results = new LuaTable();
					for (int i = 0; i < 6; ++i) {
						((LuaValue) results).set(i + 1, (LuaValue) LuaValue.valueOf(sides[i]));
					}
					return results;
				}
			}
		);
		redstone.set("setOutput", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _port, LuaValue _onOff) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					Computer.this.setOutput(side, _onOff.checkboolean());
					return LuaValue.NIL;
				}
			}
		);
		redstone.set("getInput", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _port) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					return LuaValue.valueOf(Computer.this.getInput(side));
				}
			}
		);
		redstone.set("setBundledOutput", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _port, LuaValue _combination) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					Computer.this.setBundledOutput(side, _combination.checkint());
					return LuaValue.NIL;
				}
			}
		);
		redstone.set("getBundledOutput", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _port) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					return LuaValue.valueOf(Computer.this.getBundledOutput(side));
				}
			}
		);
		redstone.set("getBundledInput", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _port) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					return LuaValue.valueOf(Computer.this.getBundledInput(side));
				}
			}
		);
		redstone.set("testBundledInput", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _port, LuaValue _combination) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_port.checkstring().toString());
					int combo = _combination.checkint();
					int input = Computer.this.getBundledInput(side);
					return LuaValue.valueOf((combo & input) == combo);
				}
			}
		);
		globals.set("redstone", (LuaValue) redstone);
		globals.set("rs", (LuaValue) redstone);
		LuaTable fs = new LuaTable();
		fs.set("list", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						String[] results = Computer.this.m_fileSystem.list(path);
						LuaTable table = new LuaTable();
						for (int i = 0; i < results.length; ++i) {
							((LuaValue) table).set(i + 1, (LuaValue) LuaValue.valueOf(results[i]));
						}
						return table;
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		fs.set("combine", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _pathA, LuaValue _pathB) {
					Computer.this.tryAbort();
					String pathA = _pathA.checkstring().toString();
					String pathB = _pathB.checkstring().toString();
					return LuaValue.valueOf(Computer.this.m_fileSystem.combine(pathA, pathB));
				}
			}
		);
		fs.set("getName", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					return LuaValue.valueOf(Computer.this.m_fileSystem.getName(path));
				}
			}
		);
		fs.set("exists", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						return LuaValue.valueOf(Computer.this.m_fileSystem.exists(path));
					} catch (FileSystemException e) {
						return LuaValue.FALSE;
					}
				}
			}
		);
		fs.set("isDir", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						return LuaValue.valueOf(Computer.this.m_fileSystem.isDir(path));
					} catch (FileSystemException e) {
						return LuaValue.FALSE;
					}
				}
			}
		);
		fs.set("isReadOnly", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						return LuaValue.valueOf(Computer.this.m_fileSystem.isReadOnly(path));
					} catch (FileSystemException e) {
						return LuaValue.FALSE;
					}
				}
			}
		);
		fs.set("makeDir", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						Computer.this.m_fileSystem.makeDir(path);
						return LuaValue.NIL;
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		fs.set("move", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _path, LuaValue _dest) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					String dest = _dest.checkstring().toString();
					try {
						Computer.this.m_fileSystem.move(path, dest);
						return LuaValue.NIL;
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		fs.set("copy", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _path, LuaValue _dest) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					String dest = _dest.checkstring().toString();
					try {
						Computer.this.m_fileSystem.copy(path, dest);
						return LuaValue.NIL;
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		fs.set("delete", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						Computer.this.m_fileSystem.delete(path);
						return LuaValue.NIL;
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		fs.set("open", (LuaValue) new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue _path, LuaValue _mode) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					String mode = _mode.checkstring().toString();
					try {
						if (mode.equals("r")) {
							BufferedReader reader = Computer.this.m_fileSystem.openForRead(path);
							return Computer.this.wrapBufferedReader(reader);
						}
						if (mode.equals("w")) {
							BufferedWriter writer = Computer.this.m_fileSystem.openForWrite(path, false);
							return Computer.this.wrapBufferedWriter(writer);
						}
						if (mode.equals("a")) {
							BufferedWriter writer = Computer.this.m_fileSystem.openForWrite(path, true);
							return Computer.this.wrapBufferedWriter(writer);
						}
						if (mode.equals("rb")) {
							BufferedInputStream reader = Computer.this.m_fileSystem.openForBinaryRead(path);
							return Computer.this.wrapInputStream(reader);
						}
						if (mode.equals("wb")) {
							BufferedOutputStream writer = Computer.this.m_fileSystem.openForBinaryWrite(path, false);
							return Computer.this.wrapOutputStream(writer);
						}
						if (mode.equals("ab")) {
							BufferedOutputStream writer = Computer.this.m_fileSystem.openForBinaryWrite(path, true);
							return Computer.this.wrapOutputStream(writer);
						}
						throw new LuaError("Unsupported mode");
					} catch (FileSystemException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		fs.set("getDrive", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _path) {
					Computer.this.tryAbort();
					String path = _path.checkstring().toString();
					try {
						if (!Computer.this.m_fileSystem.exists(path)) {
							return LuaValue.NIL;
						}
						if (Computer.this.m_fileSystem.contains("rom", path)) {
							return LuaValue.valueOf("rom");
						}
						DriveInfo[] arrdriveInfo = Computer.this.m_drives;
						synchronized (arrdriveInfo) {
							for (int i = 0; i < 6; ++i) {
								DriveInfo drive = Computer.this.m_drives[i];
								if (drive.dataID < 0 || drive.mountPath == null || !Computer.this.m_fileSystem.contains(drive.mountPath, path))
									continue;
								return LuaValue.valueOf(sides[i]);
							}
						}
						return LuaValue.valueOf("hdd");
					} catch (FileSystemException e) {
						throw new LuaError(e.getMessage());
					}
				}
			}
		);
		globals.set("fs", (LuaValue) fs);
		LuaTable disk = new LuaTable();
		disk.set("isPresent", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.occupied) {
							return LuaValue.TRUE;
						}
					}
					return LuaValue.FALSE;
				}
			}
		);
		disk.set("getLabel", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.dataID >= 0) {
							String label = ItemDisk.getDiskLabel(info.dataID);
							if (label != null) {
								return LuaValue.valueOf(label);
							}
							return LuaValue.NIL;
						}
						if (info.recordName != null) {
							return LuaValue.valueOf("C418 - " + info.recordName);
						}
					}
					return LuaValue.NIL;
				}
			}
		);
		disk.set("setLabel", (LuaValue) new TwoArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side, LuaValue _label) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					String label = null;
					if (!_label.isnil()) {
						label = _label.checkstring().toString();
					}
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.dataID >= 0) {
							ItemDisk.setDiskLabel(info.dataID, label);
							return LuaValue.NIL;
						}
					}
					throw new LuaError("No data disk in drive");
				}
			}
		);
		disk.set("hasData", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.dataID >= 0) {
							return LuaValue.TRUE;
						}
					}
					return LuaValue.FALSE;
				}
			}
		);
		disk.set("getMountPath", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.dataID >= 0 && info.mountPath != null) {
							return LuaValue.valueOf(info.mountPath);
						}
					}
					return LuaValue.NIL;
				}
			}
		);
		disk.set("hasAudio", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.recordName != null) {
							return LuaValue.TRUE;
						}
					}
					return LuaValue.FALSE;
				}
			}
		);
		disk.set("getAudioTitle", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.recordName != null) {
							return LuaValue.valueOf("C418 - " + info.recordName);
						}
					}
					return LuaValue.NIL;
				}
			}
		);
		disk.set("playAudio", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						if (info.recordName != null) {
							Computer.this.m_queuedDisc = side;
							if (Computer.this.m_queuedDisc == Computer.this.m_playingDisc) {
								Computer.this.m_restartDisc = true;
							}
						}
					}
					return LuaValue.NIL;
				}
			}
		);
		disk.set("stopAudio", (LuaValue) new ZeroArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						Computer.this.m_queuedDisc = -1;
					}
					return LuaValue.NIL;
				}
			}
		);
		disk.set("eject", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _side) {
					Computer.this.tryAbort();
					int side = Computer.this.parseSide(_side.checkstring().toString());
					DriveInfo[] arrdriveInfo = Computer.this.m_drives;
					synchronized (arrdriveInfo) {
						DriveInfo info = Computer.this.m_drives[side];
						info.eject = true;
					}
					return LuaValue.NIL;
				}
			}
		);
		globals.set("disk", (LuaValue) disk);
		LuaTable os = new LuaTable();
		os.set("queueEvent", (LuaValue) new VarArgFunction() {

				@Override
				public Varargs invoke(Varargs _args) {
					Computer.this.tryAbort();
					LuaString eventName = _args.checkstring(1);
					final LuaValue[] args = new LuaValue[6];
					args[0] = eventName;
					for (int i = 0; i < 5; ++i) {
						args[i + 1] = _args.arg(i + 2);
					}
					Computer.this.queueLuaEvent(new Event() {

													@Override
													public LuaValue[] getArguments() {
														return args;
													}
												}
					);
					return LuaValue.varargsOf(new LuaValue[0]);
				}
			}
		);
		os.set("startTimer", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _time) {
					Computer.this.tryAbort();
					double timer = Math.max(_time.checkdouble(), 0.0);
					LuaTable token = new LuaTable();
					List list = Computer.this.m_timers;
					synchronized (list) {
						Computer.this.m_timers.add(new Timer(timer, token));
					}
					return token;
				}
			}
		);
		os.set("setAlarm", (LuaValue) new OneArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call(LuaValue _time) {
					Computer.this.tryAbort();
					double time = _time.checkdouble();
					if (time < 0.0 || time > 24.0) {
						throw new LuaError("Out of range");
					}
					LuaTable token = new LuaTable();
					List list = Computer.this.m_alarms;
					synchronized (list) {
						Computer.this.m_alarms.add(new Alarm(time, token));
					}
					return token;
				}
			}
		);
		os.set("shutdown", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Computer.this.turnOff();
					return LuaValue.NIL;
				}
			}
		);
		os.set("reboot", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Computer.this.reboot();
					return LuaValue.NIL;
				}
			}
		);
		ZeroArgFunction computerID = new ZeroArgFunction() {

			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				return LuaValue.valueOf(Computer.this.m_id);
			}
		};
		os.set("computerID", (LuaValue) computerID);
		os.set("getComputerID", (LuaValue) computerID);
		os.set("clock", (LuaValue) new ZeroArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Object var1_1 = this;
					synchronized (var1_1) {
						return LuaValue.valueOf((float) Computer.this.m_clock);
					}
				}
			}
		);
		os.set("time", (LuaValue) new ZeroArgFunction() {

				/*
	// WARNING - Removed try catching itself - possible behaviour change.
				 */
				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					Object var1_1 = this;
					synchronized (var1_1) {
						return LuaValue.valueOf(Computer.this.m_time);
					}
				}
			}
		);
		globals.set("os", (LuaValue) os);
		if (Computers.enableAPI_http > 0) {
			LuaTable http = new LuaTable();
			http.set("request", (LuaValue) new OneArgFunction() {

					/*
	// WARNING - Removed try catching itself - possible behaviour change.
					 */
					@Override
					public LuaValue call(LuaValue _url) {
						Computer.this.tryAbort();
						String urlString = _url.checkstring().toString();
						try {
							HTTPRequest request = new HTTPRequest(urlString);
							List list = Computer.this.m_httpRequests;
							synchronized (list) {
								Computer.this.m_httpRequests.add(request);
							}
							return LuaValue.NIL;
						} catch (HTTPRequestException e) {
							throw new LuaError(e.getMessage());
						}
					}
				}
			);
			globals.set("http", (LuaValue) http);
		}
		try {
			File bios = new File(getBiosFolder(), "bios.lua");
			LuaValue program = globals.get("assert").call(loadfile.call(LuaValue.valueOf(bios.toString())));
			LuaValue coroutine = globals.get("coroutine");
			this.m_mainFunction = coroutine.get("create").call(program);
			this.m_globals = globals;
		} catch (LuaError e) {
			Terminal terminal = this.m_terminal;
			synchronized (terminal) {
				this.m_terminal.write("Failed to " + Computers.luaFolder + "/bios.lua");
				this.m_terminal.setCursorPos(0, this.m_terminal.getCursorY() + 1);
				this.m_terminal.write("Check you have installed ComputerCraft correctly.");
			}
			e.printStackTrace();
			this.m_mainFunction = null;
			this.m_globals = null;
		}
	}

	private File getBiosFolder() {
		File biosFolder = new File(Minecraft.getMinecraft(Minecraft.class).getMinecraftDir(), Computers.luaFolder);
		File bios = new File(biosFolder, "bios.lua");

		if (!biosFolder.exists()) {
			biosFolder.mkdirs();
		}
		if (!bios.exists()) {
			String[] files = new String[]{
				"bios.lua",
				"rom/startup",
				"rom/apis/bit",
				"rom/apis/colors",
				"rom/apis/help",
				"rom/apis/io",
				"rom/apis/parallel",
				"rom/apis/rednet",
				"rom/apis/textutils",
				"rom/help/adventure",
				"rom/help/alias",
				"rom/help/apis",
				"rom/help/bit",
				"rom/help/cd",
				"rom/help/clear",
				"rom/help/colors",
				"rom/help/colours",
				"rom/help/copy",
				"rom/help/coroutine",
				"rom/help/credits",
				"rom/help/delete",
				"rom/help/disk",
				"rom/help/dj",
				"rom/help/drive",
				"rom/help/edit",
				"rom/help/eject",
				"rom/help/events",
				"rom/help/exit",
				"rom/help/fs",
				"rom/help/hello",
				"rom/help/help",
				"rom/help/helpapi",
				"rom/help/http",
				"rom/help/id",
				"rom/help/intro",
				"rom/help/io",
				"rom/help/label",
				"rom/help/list",
				"rom/help/lua",
				"rom/help/math",
				"rom/help/mkdir",
				"rom/help/move",
				"rom/help/os",
				"rom/help/parallel",
				"rom/help/programming",
				"rom/help/reboot",
				"rom/help/rednet",
				"rom/help/redpower",
				"rom/help/redprobe",
				"rom/help/redpulse",
				"rom/help/redset",
				"rom/help/redstone",
				"rom/help/rename",
				"rom/help/rs",
				"rom/help/shell",
				"rom/help/shellapi",
				"rom/help/shutdown",
				"rom/help/sleep",
				"rom/help/string",
				"rom/help/table",
				"rom/help/term",
				"rom/help/textutils",
				"rom/help/time",
				"rom/help/type",
				"rom/help/whatsnew",
				"rom/help/worm",
				"rom/programs/adventure",
				"rom/programs/alias",
				"rom/programs/apis",
				"rom/programs/cd",
				"rom/programs/clear",
				"rom/programs/copy",
				"rom/programs/delete",
				"rom/programs/dj",
				"rom/programs/drive",
				"rom/programs/edit",
				"rom/programs/eject",
				"rom/programs/exit",
				"rom/programs/hello",
				"rom/programs/help",
				"rom/programs/id",
				"rom/programs/label",
				"rom/programs/list",
				"rom/programs/lua",
				"rom/programs/mkdir",
				"rom/programs/move",
				"rom/programs/programs",
				"rom/programs/reboot",
				"rom/programs/redprobe",
				"rom/programs/redpulse",
				"rom/programs/redset",
				"rom/programs/rename",
				"rom/programs/shell",
				"rom/programs/shutdown",
				"rom/programs/sleep",
				"rom/programs/time",
				"rom/programs/type",
				"rom/programs/worm"
			};
			for (String file : files) {
				new File(biosFolder, file).mkdirs();
				copy(getClass().getResourceAsStream("/lua/" + file), biosFolder.getPath() + "/" + file);
			}
			copy(this.getClass().getResourceAsStream("/lua/bios.lua"), bios.getPath());

		}
		return biosFolder;
	}

	private boolean copy(InputStream source, String destination) {
		boolean succeess = true;
		try {
			Files.copy(source, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			ex.printStackTrace();
			succeess = false;
		}
		return succeess;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void startComputer() {
		Computer computer = this;
		synchronized (computer) {
			boolean[] arrbl = this.m_output;
			synchronized (this.m_output) {
				if (this.m_on) {
// ** MonitorExit[var2_2] (shouldn't be in output)
					return;
				}
				this.m_on = true;
				this.m_outputChanged = true;
				this.m_stopped = false;
				this.m_aborted = false;
				this.m_clock = 0.0;
// ** MonitorExit[var2_2] (shouldn't be in output)
			}
		}
		{
			final Computer computer2 = this;
			ComputerThread.queueTask(new ITask() {

										 @Override
										 public Computer getOwner() {
											 return computer2;
										 }

										 /*
						 // WARNING - Removed try catching itself - possible behaviour change.
										  */
										 @Override
										 public void execute() {
											 Object var1_1 = this;
											 synchronized (var1_1) {
												 Terminal terminal = Computer.this.m_terminal;
												 synchronized (terminal) {
													 Computer.this.m_terminal.clear();
													 Computer.this.m_terminal.setCursorPos(0, 0);
													 Computer.this.m_terminal.setCursorBlink(false);
												 }
												 Computer.this.initFileSystem();
												 Computer.this.initLua();
											 }
										 }
									 }, this
			);
			for (int i = 0; i < 6; ++i) {
				this.queueMount(i);
			}
			this.queueLuaEvent(new Event() {

								   @Override
								   public LuaValue[] getArguments() {
									   return new LuaValue[0];
								   }
							   }
			);
			return;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void stopComputer() {
		Computer computer = this;
		synchronized (computer) {
			if (this.m_stopped) {
				return;
			}
			this.m_stopped = true;
			System.out.println("stopping");
		}
		final Computer computer2 = this;
		ComputerThread.queueTask(new ITask() {

									 @Override
									 public Computer getOwner() {
										 return computer2;
									 }

									 /*
						 // WARNING - Removed try catching itself - possible behaviour change.
									  */
									 @Override
									 public void execute() {
										 Object var1_1 = this;
										 synchronized (var1_1) {
											 int i;
											 System.out.println("really stopping");
											 Object object = Computer.this.m_terminal;
											 synchronized (object) {
												 Computer.this.m_terminal.clear();
												 Computer.this.m_terminal.setCursorPos(0, 0);
												 Computer.this.m_terminal.setCursorBlink(false);
											 }
											 Computer.this.m_fileSystem = null;
											 object = Computer.this.m_timers;
											 synchronized (object) {
												 Computer.this.m_timers.clear();
											 }
											 object = Computer.this.m_alarms;
											 synchronized (object) {
												 Computer.this.m_alarms.clear();
											 }
											 object = Computer.this.m_httpRequests;
											 synchronized (object) {
												 for (HTTPRequest r : Computer.this.m_httpRequests) {
													 r.cancel();
												 }
												 Computer.this.m_httpRequests.clear();
											 }
											 object = Computer.this.m_output;
											 synchronized (object) {
												 for (i = 0;
													  i < 6; ++i) {
													 ((Computer) Computer.this).m_output[i] = false;
													 ((Computer) Computer.this).m_bundledOutput[i] = 0;
												 }
												 Computer.this.m_outputChanged = true;
											 }
											 object = Computer.this.m_drives;
											 synchronized (object) {
												 Computer.this.m_queuedDisc = -1;
												 for (i = 0;
													  i < 6; ++i) {
													 ((Computer) Computer.this).m_drives[i].mountPath = null;
												 }
											 }
											 object = this;
											 synchronized (object) {
												 Computer.this.m_on = false;
												 Computer.this.m_stopped = false;
												 Computer.this.m_mainFunction = null;
												 Computer.this.m_globals = null;
											 }
										 }
									 }
								 }, this
		);
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void queueLuaEvent(final Event _event) {
		Computer computer = this;
		synchronized (computer) {
			if (!this.m_on || this.m_stopped) {
				return;
			}
		}
		final Computer computer2 = this;
		ComputerThread.queueTask(new ITask() {

									 @Override
									 public Computer getOwner() {
										 return computer2;
									 }

									 /*
						 // WARNING - Removed try catching itself - possible behaviour change.
									  */
									 @Override
									 public void execute() {
										 Object var1_1 = this;
										 synchronized (var1_1) {
											 if (!Computer.this.m_on || Computer.this.m_stopped) {
												 return;
											 }
										 }
										 try {
											 LuaValue[] args = _event.getArguments();
											 if (Computer.this.m_mainFunction != null) {
												 LuaThread thread = Computer.this.m_mainFunction.checkthread();
												 Varargs results = thread.resume(LuaValue.varargsOf(args));
												 if (Computer.this.m_aborted) {
													 Computer.this.m_aborted = false;
												 }
												 //System.out.println("finishing");
												 if (!results.arg1().checkboolean()) {
													 throw new LuaError(results.arg(2).checkstring().toString());
												 }
												 if (thread.getStatus().equals("dead")) {
													 Computer.this.m_mainFunction = null;
													 Computer.this.m_globals = null;
													 Computer.this.turnOff();
												 }
											 }
										 } catch (LuaError e) {
											 Computer.this.m_mainFunction = null;
											 Computer.this.m_globals = null;
											 Terminal terminal = Computer.this.m_terminal;
											 synchronized (terminal) {
												 Computer.this.m_terminal.write(e.getMessage());
												 Computer.this.m_terminal.setCursorBlink(false);
												 Computer.this.m_terminal.setCursorPos(0, Computer.this.m_terminal.getCursorY() + 1);
												 if (Computer.this.m_terminal.getCursorY() >= Computer.this.m_terminal.getHeight()) {
													 Computer.this.m_terminal.scroll(1);
													 Computer.this.m_terminal.setCursorPos(0, Computer.this.m_terminal.getHeight() - 1);
												 }
											 }
											 e.printStackTrace();
										 }
									 }
								 }, this
		);
	}

	private String findFreeDiskMount() {
		try {
			if (!this.m_fileSystem.exists("disk")) {
				return "disk";
			}
			int n = 2;
			while (this.m_fileSystem.exists("disk" + n)) {
				++n;
			}
			return "disk" + n;
		} catch (FileSystemException e) {
			return null;
		}
	}

	private File getRealDiskPath(int dataID) {
		File baseUserDir = new File(Computers.getWorldDir(this.m_owner.worldObj), "/computer/disk");
		File userDir = new File(baseUserDir, Integer.toString(dataID));
		userDir.mkdirs();
		return userDir;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void queueMount(final int _side) {
		Computer computer = this;
		synchronized (computer) {
			if (!this.m_on || this.m_stopped) {
				return;
			}
		}
		final Computer computer2 = this;
		ComputerThread.queueTask(new ITask() {

									 @Override
									 public Computer getOwner() {
										 return computer2;
									 }

									 /*
						 // WARNING - Removed try catching itself - possible behaviour change.
									  */
									 @Override
									 public void execute() {
										 DriveInfo[] arrdriveInfo = Computer.this.m_drives;
										 synchronized (this) {
											 if (!Computer.this.m_on || Computer.this.m_stopped) {
// ** MonitorExit[var1_1] (shouldn't be in output)
												 return;
											 }// ** MonitorExit[var1_1] (shouldn't be in output)

											 arrdriveInfo = Computer.this.m_drives;
											 synchronized (arrdriveInfo) {
												 DriveInfo info = Computer.this.m_drives[_side];
												 if (info.dataID >= 0 && info.mountPath == null) {
													 String mountPath = Computer.this.findFreeDiskMount();
													 File realPath = Computer.this.getRealDiskPath(info.dataID);
													 if (mountPath != null && realPath != null) {
														 try {
															 Computer.this.m_fileSystem.mount(mountPath, realPath, false);
															 info.mountPath = mountPath;
														 } catch (FileSystemException e) {
// empty catch block
														 }
													 }
												 }
											 }
											 return;
										 }
									 }
								 }, this
		);
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private void queueUnmount(final String _path) {
		Computer computer = this;
		synchronized (computer) {
			if (!this.m_on || this.m_stopped) {
				return;
			}
		}
		final Computer computer2 = this;
		ComputerThread.queueTask(new ITask() {

									 @Override
									 public Computer getOwner() {
										 return computer2;
									 }

									 /*
						 // WARNING - Removed try catching itself - possible behaviour change.
									  */
									 @Override
									 public void execute() {
										 DriveInfo[] arrdriveInfo = Computer.this.m_drives;
										 synchronized (this) {
											 if (!Computer.this.m_on || Computer.this.m_stopped) {
// ** MonitorExit[var1_1] (shouldn't be in output)
												 return;
											 }// ** MonitorExit[var1_1] (shouldn't be in output)

											 arrdriveInfo = Computer.this.m_drives;
											 synchronized (arrdriveInfo) {
												 Computer.this.m_fileSystem.unmount(_path);
											 }
											 return;
										 }
									 }
								 }, this
		);
	}

	private int parseSide(String side) {
		for (int n = 0; n < 6; ++n) {
			if (!side.equals(sides[n])) continue;
			return n;
		}
		throw new LuaError("Invalid side.");
	}

	private LuaValue wrapBufferedReader(final BufferedReader _reader) {
		LuaTable result = new LuaTable();
		result.set("readLine", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						String line = _reader.readLine();
						if (line != null) {
							return LuaValue.valueOf(line);
						}
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("readAll", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						StringBuilder result = new StringBuilder("");
						String line = _reader.readLine();
						while (line != null) {
							result.append(line);
							line = _reader.readLine();
							if (line == null) continue;
							result.append("\n");
						}
						return LuaValue.valueOf(result.toString());
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("close", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						_reader.close();
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		return result;
	}

	private LuaValue wrapBufferedWriter(final BufferedWriter _writer) {
		LuaTable result = new LuaTable();
		result.set("write", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					try {
						String text = "";
						if (!_arg.isnil()) {
							text = _arg.toString();
						}
						_writer.write(text, 0, text.length());
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("writeLine", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					try {
						String text = "";
						if (!_arg.isnil()) {
							text = _arg.toString();
						}
						_writer.write(text, 0, text.length());
						_writer.newLine();
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("close", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						_writer.close();
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		return result;
	}

	private LuaValue wrapInputStream(final InputStream _reader) {
		LuaTable result = new LuaTable();
		result.set("read", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						int b = _reader.read();
						if (b != -1) {
							return LuaValue.valueOf(b);
						}
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("close", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						_reader.close();
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		return result;
	}

	private LuaValue wrapOutputStream(final OutputStream _writer) {
		LuaTable result = new LuaTable();
		result.set("write", (LuaValue) new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue _arg) {
					Computer.this.tryAbort();
					try {
						int number = _arg.checkint();
						_writer.write(number);
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		result.set("close", (LuaValue) new ZeroArgFunction() {

				@Override
				public LuaValue call() {
					Computer.this.tryAbort();
					try {
						_writer.close();
						return LuaValue.NIL;
					} catch (IOException e) {
						return LuaValue.NIL;
					}
				}
			}
		);
		return result;
	}

	private class DriveInfo {
		boolean occupied = false;
		String recordName = null;
		int dataID = -1;
		String mountPath = null;
		boolean eject = false;
	}

	private class Alarm
		implements Comparable {
		double time;
		LuaValue token;

		Alarm(double _time, LuaValue _token) {
			this.time = _time;
			this.token = _token;
		}

		public int compareTo(Alarm o) {
			double ot;
			double t = this.time;
			if (t < Computer.this.m_time) {
				t += 24.0;
			}
			if ((ot = o.time) < Computer.this.m_time) {
				ot += 24.0;
			}
			if (this.time < o.time) {
				return -1;
			}
			if (this.time > o.time) {
				return 1;
			}
			return 0;
		}

		@Override
		public int compareTo(Object arg0) {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	private class Timer {
		double timeLeft;
		LuaValue token;

		Timer(double _timeLeft, LuaValue _token) {
			this.timeLeft = _timeLeft;
			this.token = _token;
		}
	}

	private static interface Event {
		public LuaValue[] getArguments();
	}
}
