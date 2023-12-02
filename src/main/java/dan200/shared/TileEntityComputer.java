/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import com.mojang.nbt.CompoundTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.net.packet.Packet;
import sunsetsatellite.computers.Computers;
import sunsetsatellite.computers.packets.PacketComputers;

public class TileEntityComputer extends TileEntity implements IComputerCraftEntity {
	private Terminal m_terminal;
	private Computer m_computer;
	private ClientData m_clientData;
	private boolean m_destroyed;

	public TileEntityComputer() {
		if (!Computers.isMultiplayerClient()) {
			this.m_terminal = new Terminal(Computers.terminal_width, Computers.terminal_height);
			this.m_computer = new Computer(this, this.m_terminal);
			this.m_clientData = null;
		} else {
			this.m_terminal = new Terminal(50, 18);
			this.m_computer = null;
			this.m_clientData = new ClientData();
		}
		this.m_destroyed = false;
	}



	@Override
	public void validate() {
		super.validate();
		if (Computers.isMultiplayerClient()) {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 5;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord};
			Computers.sendToServer(packet);
		}
	}

	public Packet getDescriptionPacket() {
		return this.createOutputChangedPacket();
	}

	public void destroy() {
		if (!this.m_destroyed) {
			if (!Computers.isMultiplayerClient()) {
				this.m_computer.destroy();
			}
			this.m_destroyed = true;
		}
	}

	public boolean isDestroyed() {
		return this.m_destroyed;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public void updateEntity() {
		double dt = 0.05;
		if (!Computers.isMultiplayerClient()) {
			PacketComputers packet;
			this.m_computer.advance(dt);
			if (this.m_computer.pollChanged()) {
				Computers.notifyBlockChange(this.worldObj, this.xCoord, this.yCoord, this.zCoord, Computers.computer.id);
				if (Computers.isMultiplayerServer()) {
					packet = this.createOutputChangedPacket();
					Computers.sendToAllPlayers(packet);
				}
			}
			if (Computers.isMultiplayerServer()) {
				packet = null;
				Terminal terminal = this.m_terminal;
				synchronized (terminal) {
					if (this.m_terminal.getChanged()) {
						packet = this.createTerminalChangedPacket(false);
						this.m_terminal.clearChanged();
					}
				}
				if (packet != null) {
					Computers.sendToAllPlayers(packet);
				}
			}
		}
	}

	public void writeToNBT(CompoundTag nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		if (!Computers.isMultiplayerClient()) {
			nbttagcompound.putBoolean("on", this.isSwitchedOn());
			String userDir = this.m_computer.getUserDir();
			if (userDir != null) {
				nbttagcompound.putString("userDir", userDir);
			}
		} else {
			nbttagcompound.putBoolean("on", this.m_clientData.on);
		}
	}

	public void readFromNBT(CompoundTag nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		if (!Computers.isMultiplayerClient()) {
			this.setSwitchedOn(nbttagcompound.getBoolean("on"));
			String userDir = nbttagcompound.getString("userDir");
			if (userDir != null && userDir.length() > 0) {
				this.m_computer.setUserDir(userDir);
			}
		} else {
			this.m_clientData.on = nbttagcompound.getBoolean("on");
		}
	}

	public void keyTyped(char ch, int key) {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.pressKey(ch, key);
		} else {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 2;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord, key};
			packet.dataString = new String[]{"" + ch};
			Computers.sendToServer(packet);
		}
	}

	public void terminate() {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.terminate();
		} else {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 6;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord};
			Computers.sendToServer(packet);
		}
	}

	public void reboot() {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.reboot();
		} else {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 9;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord};
			Computers.sendToServer(packet);
		}
	}

	public void shutdown() {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.turnOff();
		} else {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 12;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord};
			Computers.sendToServer(packet);
		}
	}

	public boolean isSwitchedOn() {
		if (!Computers.isMultiplayerClient()) {
			return this.m_computer.isOn();
		}
		return this.m_clientData.on;
	}

	public void setSwitchedOn(boolean on) {
		if (!Computers.isMultiplayerClient()) {
			if (on) {
				this.m_computer.turnOn();
			} else {
				this.m_computer.turnOff();
			}
		}
	}

	public boolean isCursorVisible() {
		if (!Computers.isMultiplayerClient()) {
			return this.m_computer.isBlinking();
		}
		return this.m_clientData.on && this.m_clientData.blinking;
	}

	public Terminal getTerminal() {
		return this.m_terminal;
	}

	public boolean isPowering(int side) {
		if (!Computers.isMultiplayerClient()) {
			return this.m_computer.getOutput(side);
		}
		return this.m_clientData.output[side];
	}

	public void providePower(int side, boolean onOff) {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.setInput(side, onOff);
		}
	}

	public int getBundledPowerOutput(int side) {
		if (!Computers.isMultiplayerClient()) {
			return this.m_computer.getBundledOutput(side);
		}
		return this.m_clientData.bundledOutput[side];
	}

	public void setBundledPowerInput(int side, int combination) {
		if (!Computers.isMultiplayerClient()) {
			this.m_computer.setBundledInput(side, combination);
		}
	}

	public void updateDiskInfo(int side, TileEntityDiskDrive diskDrive) {
		if (!Computers.isMultiplayerClient()) {
			if (diskDrive != null && diskDrive.hasAnything()) {
				this.m_computer.setDiskInfo(side, true, diskDrive.getDataDiskID(), diskDrive.getAudioDiscRecordName());
			} else {
				this.m_computer.setDiskInfo(side, false, -1, null);
			}
		}
	}

	public void playRecord(String record) {
		if (Computers.isMultiplayerServer()) {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 7;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord};
			if (record != null) {
				packet.dataString = new String[]{record};
			}
			Computers.sendToAllPlayers(packet);
		} else {
			this.worldObj.playRecord(record, this.xCoord, this.yCoord, this.zCoord);
		}
	}

	private void tryEjectDisk(int targetSide, int testSide, int i, int j, int k) {
		TileEntityDiskDrive drive;
		if (targetSide == testSide && (drive = BlockComputer.getDiskDriveAt(this.worldObj, i, j, k)) != null) {
			drive.ejectContents(false);
		}
	}

	public void ejectDisk(int side) {
		if (!Computers.isMultiplayerClient()) {
			int i = this.xCoord;
			int j = this.yCoord;
			int k = this.zCoord;
			int m = this.worldObj.getBlockMetadata(i, j, k);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(0, m), i, j + 1, k);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(1, m), i, j - 1, k);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(2, m), i, j, k + 1);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(3, m), i, j, k - 1);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(4, m), i + 1, j, k);
			this.tryEjectDisk(side, BlockComputer.getLocalSide(5, m), i - 1, j, k);
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private PacketComputers createTerminalChangedPacket(boolean _includeAllText) {
		Terminal terminal = this.m_terminal;
		synchronized (terminal) {
			boolean[] lineChanged = this.m_terminal.getLinesChanged();
			int lineChangeMask = this.m_terminal.getCursorBlink() ? 1 : 0;
			int lineChangeCount = 0;
			for (int y = 0; y < this.m_terminal.getHeight(); ++y) {
				if (!lineChanged[y] && !_includeAllText) continue;
				lineChangeMask += 1 << y + 1;
				++lineChangeCount;
			}
			PacketComputers packet = new PacketComputers();
			packet.packetType = 3;
			packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord, this.m_terminal.getCursorX(), this.m_terminal.getCursorY(), lineChangeMask};
			packet.dataString = new String[lineChangeCount];
			int n = 0;
			for (int y = 0; y < this.m_terminal.getHeight(); ++y) {
				if (!lineChanged[y] && !_includeAllText) continue;
				packet.dataString[n++] = this.m_terminal.getLine(y).replaceAll(" +$", "");
			}
			return packet;
		}
	}

	private PacketComputers createOutputChangedPacket() {
		PacketComputers packet = new PacketComputers();
		packet.packetType = 4;
		int flags = 0;
		if (this.m_computer.isOn()) {
			++flags;
		}
		if (this.m_computer.isBlinking()) {
			flags += 2;
		}
		for (int i = 0; i < 6; ++i) {
			if (!this.m_computer.getOutput(i)) continue;
			flags += 1 << i + 2;
		}
		packet.dataInt = new int[]{this.xCoord, this.yCoord, this.zCoord, flags, this.m_computer.getBundledOutput(0), this.m_computer.getBundledOutput(1), this.m_computer.getBundledOutput(2), this.m_computer.getBundledOutput(3), this.m_computer.getBundledOutput(3), this.m_computer.getBundledOutput(5)};
		return packet;
	}

	public void updateClient(EntityPlayer player) {
		if (Computers.isMultiplayerServer()) {
			PacketComputers terminalChanged = this.createTerminalChangedPacket(true);
			Computers.sendToPlayer(player, terminalChanged);
			PacketComputers outputChanged = this.createOutputChangedPacket();
			Computers.sendToPlayer(player, outputChanged);
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public void handlePacket(PacketComputers packet, EntityPlayer player) {
		if (Computers.isMultiplayerServer()) {
			switch (packet.packetType) {
				case 2: {
					int key = packet.dataInt[3];
					char ch = packet.dataString[0].charAt(0);
					this.keyTyped(ch, key);
					break;
				}
				case 6: {
					this.terminate();
					break;
				}
				case 9: {
					this.reboot();
					break;
				}
				case 12: {
					this.shutdown();
					break;
				}
				case 5: {
					this.updateClient(player);
					break;
				}
			}
		} else {
			switch (packet.packetType) {
				case 3: {
					Terminal key = this.m_terminal;
					synchronized (key) {
						int n = 0;
						int lineChangeMask = packet.dataInt[5];
						for (int y = 0; y < this.m_terminal.getHeight(); ++y) {
							if ((lineChangeMask & 1 << y + 1) <= 0) continue;
							this.m_terminal.setCursorPos(0, y);
							this.m_terminal.clearLine();
							this.m_terminal.write(packet.dataString[n++]);
						}
						this.m_terminal.setCursorPos(packet.dataInt[3], packet.dataInt[4]);
						this.m_terminal.setCursorBlink((lineChangeMask & 1) > 0);
						break;
					}
				}
				case 4: {
					int flags = packet.dataInt[3];
					this.m_clientData.on = (flags & 1) > 0;
					this.m_clientData.blinking = (flags & 2) > 0;
					for (int i = 0; i < 6; ++i) {
						this.m_clientData.output[i] = (flags & 1 << i + 2) > 0;
						this.m_clientData.bundledOutput[i] = packet.dataInt[4 + i];
					}
					Computers.notifyBlockChange(this.worldObj, this.xCoord, this.yCoord, this.zCoord, Computers.computer.id);
					break;
				}
				case 7: {
					if (packet.dataString != null && packet.dataString.length > 0) {
						this.playRecord(packet.dataString[0]);
						break;
					}
					this.playRecord(null);
					break;
				}
			}
		}
	}

	private class ClientData {
		boolean on = false;
		boolean blinking = false;
		boolean[] output = new boolean[6];
		int[] bundledOutput = new int[6];

		ClientData() {
		}
	}
}
