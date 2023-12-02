/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.client;

import dan200.shared.ContainerDiskDrive;
import dan200.shared.TileEntityDiskDrive;
import net.minecraft.client.gui.GuiContainer;
import net.minecraft.core.player.inventory.InventoryPlayer;
import org.lwjgl.opengl.GL11;

public class GuiDiskDrive
	extends GuiContainer {
	private TileEntityDiskDrive m_diskDrive;

	public GuiDiskDrive(InventoryPlayer inventoryplayer, TileEntityDiskDrive tileentitydiskdrive) {
		super(new ContainerDiskDrive(inventoryplayer, tileentitydiskdrive));
		this.m_diskDrive = tileentitydiskdrive;
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Disk Drive", (this.xSize - this.fontRenderer.getStringWidth("Disk Drive")) / 2, 6, 0x404040);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f) {
		int i = mc.renderEngine.getTexture("/gui/diskdrive.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(i);
		int j = (width - xSize) / 2;
		int k = (height - ySize) / 2;
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
	}
}
