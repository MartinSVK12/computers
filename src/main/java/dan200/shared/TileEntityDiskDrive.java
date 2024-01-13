/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import com.mojang.nbt.CompoundTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemRecord;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.Packet;
import net.minecraft.core.player.inventory.IInventory;
import sunsetsatellite.computers.Computers;
import sunsetsatellite.computers.packets.PacketComputers;

public class TileEntityDiskDrive
	extends TileEntity
	implements IInventory,
	IComputerCraftEntity {
	private ItemStack diskStack = null;
	private boolean m_firstFrame = true;
	private int m_clientDiskLight = 0;

	@Override
	public void validate() {
		super.validate();
		if (Computers.isMultiplayerClient()) {
			PacketComputers packet = new PacketComputers();
			packet.packetType = 5;
			packet.dataInt = new int[]{this.x, this.y, this.z};
			Computers.sendToServer(packet);
		}
	}

	public Packet getDescriptionPacket() {
		return this.createDiskLightPacket();
	}

	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return this.diskStack;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		ItemStack disk = this.diskStack;
		this.setInventorySlotContents(i, null);
		return disk;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		boolean hadDisk = this.hasDisk();
		this.diskStack = itemstack;
		if (!Computers.isMultiplayerClient()) {
			Computers.notifyBlockChange(this.worldObj, this.x, this.y, this.z, Computers.diskDrive.id);
			if (Computers.isMultiplayerServer()) {
				int newDiskLight = 0;
				if (this.hasAnything()) {
					int n = newDiskLight = this.hasDisk() ? 1 : 2;
				}
				if (newDiskLight != this.m_clientDiskLight) {
					this.m_clientDiskLight = newDiskLight;
					PacketComputers diskLight = this.createDiskLightPacket();
					Computers.sendToAllPlayers(diskLight);
				}
			}
		}
	}

	@Override
	public String getInvName() {
		return "Disk Drive";
	}

	public void readFromNBT(CompoundTag nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		CompoundTag item = nbttagcompound.getCompound("item");
		this.diskStack = ItemStack.readItemStackFromNbt(item);
		//System.out.println("Disk Drive Read: Item ID " + diskStack.itemID);
	}

	public void writeToNBT(CompoundTag nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		CompoundTag item = new CompoundTag();
		if (this.diskStack != null) {
			//System.out.println("Disk Drive Save: Item ID " + diskStack.itemID);
			item = this.diskStack.writeToNBT(item);
		}

		nbttagcompound.putCompound("item", item);
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer) {
		if (this.worldObj.getBlockTileEntity(this.x, this.y, this.z) != this) {
			return false;
		}
		return entityplayer.distanceTo((double) this.x + 0.5, (double) this.y + 0.5, (double) this.z + 0.5) <= 64.0;
	}

	@Override
	public void sortInventory() {

	}

	public boolean hasAnything() {
		if (!Computers.isMultiplayerClient()) {
			return this.diskStack != null;
		}
		return this.m_clientDiskLight > 0;
	}

	public boolean hasDisk() {
		if (!Computers.isMultiplayerClient()) {
			return this.getDataDiskID() >= 0 || this.getAudioDiscRecordName() != null;
		}
		return this.m_clientDiskLight == 1;
	}

	public void ejectContents(boolean destroyed) {
		if (Computers.isMultiplayerClient()) {
			return;
		}
		if (this.diskStack != null) {
			ItemStack disks = this.diskStack;
			this.setInventorySlotContents(0, null);
			int xOff = 0;
			int zOff = 0;
			if (!destroyed) {
				int metaData = this.worldObj.getBlockMetadata(this.x, this.y, this.z);
				switch (metaData) {
					case 2: {
						zOff = -1;
						break;
					}
					case 3: {
						zOff = 1;
						break;
					}
					case 4: {
						xOff = -1;
						break;
					}
					case 5: {
						xOff = 1;
						break;
					}
				}
			}
			double x = (double) this.x + 0.5 + (double) xOff * 0.5;
			double y = (double) this.y + 0.75;
			double z = (double) this.z + 0.5 + (double) zOff * 0.5;
			EntityItem entityitem = new EntityItem(this.worldObj, x, y, z, disks);
			entityitem.xd = (double) xOff * 0.15;
			entityitem.yd = 0.0;
			entityitem.zd = (double) zOff * 0.15;
			this.worldObj.entityJoinedWorld(entityitem);
			if (!destroyed) {
				this.worldObj.playSoundEffect(1000, this.x, this.y, this.z, 0);
			}
		}
	}

	public int getDataDiskID() {
		if (this.diskStack != null && this.diskStack.itemID == Computers.disk.id) {
			return ItemDisk.getDiskID(this.diskStack, this.worldObj);
		}
		return -1;
	}

	public String getAudioDiscRecordName() {
		Item item;
		if (this.diskStack != null && (item = Item.itemsList[this.diskStack.itemID]) instanceof ItemRecord) {
			ItemRecord record = (ItemRecord) item;
			return record.recordName;
		}
		return null;
	}

	public void tick() {
		if (this.m_firstFrame) {
			if (!Computers.isMultiplayerClient()) {
				this.m_clientDiskLight = 0;
				if (this.hasAnything()) {
					this.m_clientDiskLight = this.hasDisk() ? 1 : 2;
				}
				Computers.notifyBlockChange(this.worldObj, this.x, this.y, this.z, Computers.diskDrive.id);
			}
			this.m_firstFrame = false;
		}
	}

	private PacketComputers createDiskLightPacket() {
		PacketComputers packet = new PacketComputers();
		packet.packetType = 8;
		packet.dataInt = new int[]{this.x, this.y, this.z, this.m_clientDiskLight};
		return packet;
	}

	private void updateClient(EntityPlayer player) {
		if (Computers.isMultiplayerServer()) {
			PacketComputers diskLight = this.createDiskLightPacket();
			Computers.sendToPlayer(player, diskLight);
		}
	}

	@Override
	public void handlePacket(PacketComputers packet, EntityPlayer player) {
		if (Computers.isMultiplayerServer()) {
			switch (packet.packetType) {
				case 5: {
					this.updateClient(player);
					break;
				}
			}
		} else {
			switch (packet.packetType) {
				case 8: {
					this.m_clientDiskLight = packet.dataInt[3];
					Computers.notifyBlockChange(this.worldObj, this.x, this.y, this.z, Computers.diskDrive.id);
					break;
				}
			}
		}
	}
}
