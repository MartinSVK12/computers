/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.Container;
import net.minecraft.core.player.inventory.IInventory;
import net.minecraft.core.player.inventory.slot.Slot;

import java.util.List;

public class ContainerDiskDrive extends Container {
	private TileEntityDiskDrive diskDrive;

	public ContainerDiskDrive(IInventory iinventory, TileEntityDiskDrive tileentitydiskdrive) {
		this.diskDrive = tileentitydiskdrive;

		//disk drive slot
		this.addSlot(new Slot((IInventory) this.diskDrive, 0, 80, 35));

		//inventory
		for (int j = 0; j < 3; ++j) {
			for (int i1 = 0; i1 < 9; ++i1) {
				this.addSlot(new Slot(iinventory, i1 + j * 9 + 9, 8 + i1 * 18, 84 + j * 18));
			}
		}

		//hotbar
		for (int k = 0; k < 9; ++k) {
			this.addSlot(new Slot(iinventory, k, 8 + k * 18, 142));
		}
	}

	@Override
	public List<Integer> getMoveSlots(InventoryAction inventoryAction, Slot slot, int i, EntityPlayer entityPlayer) {
		return null;
	}

	@Override
	public List<Integer> getTargetSlots(InventoryAction inventoryAction, Slot slot, int i, EntityPlayer entityPlayer) {
		return null;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityplayer) {
		return this.diskDrive.canInteractWith(entityplayer);
	}

	public ItemStack getStackInSlot(int i) {
		ItemStack itemstack = null;
		Slot slot = (Slot) this.inventorySlots.get(i);
		if (slot != null && slot.hasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			//can't figure out what these methods were supposed to be, one matching method but doesn't return a boolean
			//UPDATE: possible fix
			if (i == 0 ? !this.mergeItemStack(itemstack1, 1, 37, true) : !this.mergeItemStack(itemstack1, 0, 1, false)) {
				return null;
			}
			if (itemstack1.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
			if (itemstack1.stackSize != itemstack.stackSize) {
				slot.onPickupFromSlot(itemstack1);
			} else {
				return null;
			}
		}
		return itemstack;
	}

	protected boolean mergeItemStack(ItemStack itemstack, int i, int j, boolean flag) {
		boolean flag1 = false;
		int k = i;
		if (flag) {
			k = j - 1;
		}
		if (itemstack.isStackable()) {
			while (itemstack.stackSize > 0 && (!flag && k < j || flag && k >= i)) {
				Slot slot = (Slot) inventorySlots.get(k);
				ItemStack itemstack1 = slot.getStack();
				if (itemstack1 != null && itemstack1.itemID == itemstack.itemID && (!itemstack.getHasSubtypes() || itemstack.getMetadata() == itemstack1.getMetadata())) {
					int i1 = itemstack1.stackSize + itemstack.stackSize;
					if (i1 <= itemstack.getMaxStackSize()) {
						itemstack.stackSize = 0;
						itemstack1.stackSize = i1;
						slot.onSlotChanged();
						flag1 = true;
					} else if (itemstack1.stackSize < itemstack.getMaxStackSize()) {
						itemstack.stackSize -= itemstack.getMaxStackSize() - itemstack1.stackSize;
						itemstack1.stackSize = itemstack.getMaxStackSize();
						slot.onSlotChanged();
						flag1 = true;
					}
				}
				if (flag) {
					k--;
				} else {
					k++;
				}
			}
		}
		if (itemstack.stackSize > 0) {
			int l;
			if (flag) {
				l = j - 1;
			} else {
				l = i;
			}
			do {
				if ((flag || l >= j) && (!flag || l < i)) {
					break;
				}
				Slot slot1 = inventorySlots.get(l);
				ItemStack itemstack2 = slot1.getStack();
				if (itemstack2 == null) {
					slot1.putStack(itemstack.copy());
					slot1.onSlotChanged();
					itemstack.stackSize = 0;
					flag1 = true;
					break;
				}
				if (flag) {
					l--;
				} else {
					l++;
				}
			} while (true);
		}
		return flag1;
	}
}
