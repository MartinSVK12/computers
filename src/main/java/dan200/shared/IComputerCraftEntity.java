/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import net.minecraft.core.entity.player.EntityPlayer;
import sunsetsatellite.computers.packets.PacketComputers;

public interface IComputerCraftEntity {
	public void handlePacket(PacketComputers var1, EntityPlayer var2);
}
