/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockTileEntityRotatable;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.world.World;
import sunsetsatellite.computers.Computers;
import turniplabs.halplibe.helper.TextureHelper;

public class BlockDiskDrive
	extends BlockTileEntityRotatable {
	public int[] blockTextures = new int[]{
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/0.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/0.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/1.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/1.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/2.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/2.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/3.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"diskdrive/3.png")[1]),
	};

	public BlockDiskDrive(String id, int i) {
		super(id, i, Material.stone);
	}

	public void onBlockRemoval(World world, int i, int j, int k) {
		TileEntityDiskDrive drive = (TileEntityDiskDrive) world.getBlockTileEntity(i, j, k);
		if (drive != null) {
			drive.ejectContents(true);
		}
		super.onBlockRemoval(world, i, j, k);
	}

	@Override
	protected TileEntity getNewBlockEntity() {
		return new TileEntityDiskDrive();
	}

	/*public int getBlockTexture(WorldSource iblockaccess, int i, int j, int k, int l) {
		if (l == 1 || l == 0) {
			return this.blockTextures[3];
		}
		int i1 = iblockaccess.getBlockMetadata(i, j, k);
		if (l == i1) {
			TileEntityDiskDrive drive = (TileEntityDiskDrive) iblockaccess.getBlockTileEntity(i, j, k);
			if (drive != null && drive.hasAnything()) {
				if (drive.hasDisk()) {
					return this.blockTextures[2];
				}
				return this.blockTextures[4];
			}
			return this.blockTextures[0];
		}
		return this.blockTextures[1];
	}

	public int getBlockTextureFromSide(int i) {
		if (i == 1 || i == 0) {
			return this.blockTextures[3];
		}
		if (i == 3) {
			return this.blockTextures[0];
		}
		return this.blockTextures[1];
	}*/

	public boolean blockActivated(World world, int i, int j, int k, EntityPlayer entityplayer) {
		if (Computers.isMultiplayerClient()) {
			return true;
		}
		if (!entityplayer.isSneaking()) {
			TileEntityDiskDrive drive = (TileEntityDiskDrive) world.getBlockTileEntity(i, j, k);
			if (drive != null) {
				Computers.openDiskDriveGUI(entityplayer, drive);
			}
			return true;
		}
		return false;
	}

	public boolean canProvidePower() {
		return false;
	}
}
