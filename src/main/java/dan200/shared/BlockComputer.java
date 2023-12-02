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
import net.minecraft.core.world.WorldSource;
import sunsetsatellite.computers.Computers;
import sunsetsatellite.computers.packets.PacketComputers;
import turniplabs.halplibe.helper.TextureHelper;

import java.util.Random;

public class BlockComputer extends BlockTileEntityRotatable {
	private Random random = new Random();

	public int[] blockTextures = new int[]{
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/0.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/0.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/1.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/1.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/2.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/2.png")[1]),
		Block.texCoordToIndex(TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/3.png")[0]
			,TextureHelper.getOrCreateBlockTexture(Computers.MOD_ID,"computer/3.png")[1]),
	};
	public int blinkTexture = 0;

	public BlockComputer(String id, int i) {
		super(id, i, Material.stone);
		setTickOnLoad(true);
	}

	public void onBlockAdded(World world, int i, int j, int k) {
		super.onBlockAdded(world, i, j, k);
		refreshInput(world, i, j, k);
		//world.scheduleBlockUpdate(i, j, k, blockID, tickRate());
	}

	private boolean isBlockProvidingPower(World world, int i, int j, int k, int l) {
		return world.isBlockIndirectlyProvidingPowerTo(i, j, k, l) || world.getBlockId(i, j, k) == Block.wireRedstone.id && world.getBlockMetadata(i, j, k) > 0;
	}

	private void refreshInput(World world, int i, int j, int k) {
		TileEntityComputer computer = (TileEntityComputer) world.getBlockTileEntity(i, j, k);
		if (computer != null) {
			int m = world.getBlockMetadata(i, j, k);
			computer.providePower(BlockComputer.getLocalSide(0, m), this.isBlockProvidingPower(world, i, j + 1, k, 1));
			computer.providePower(BlockComputer.getLocalSide(1, m), this.isBlockProvidingPower(world, i, j - 1, k, 0));
			computer.providePower(BlockComputer.getLocalSide(2, m), this.isBlockProvidingPower(world, i, j, k + 1, 3));
			computer.providePower(BlockComputer.getLocalSide(3, m), this.isBlockProvidingPower(world, i, j, k - 1, 2));
			computer.providePower(BlockComputer.getLocalSide(4, m), this.isBlockProvidingPower(world, i + 1, j, k, 5));
			computer.providePower(BlockComputer.getLocalSide(5, m), this.isBlockProvidingPower(world, i - 1, j, k, 4));
			computer.updateDiskInfo(BlockComputer.getLocalSide(0, m), BlockComputer.getDiskDriveAt(world, i, j + 1, k));
			computer.updateDiskInfo(BlockComputer.getLocalSide(1, m), BlockComputer.getDiskDriveAt(world, i, j - 1, k));
			computer.updateDiskInfo(BlockComputer.getLocalSide(2, m), BlockComputer.getDiskDriveAt(world, i, j, k + 1));
			computer.updateDiskInfo(BlockComputer.getLocalSide(3, m), BlockComputer.getDiskDriveAt(world, i, j, k - 1));
			computer.updateDiskInfo(BlockComputer.getLocalSide(4, m), BlockComputer.getDiskDriveAt(world, i + 1, j, k));
			computer.updateDiskInfo(BlockComputer.getLocalSide(5, m), BlockComputer.getDiskDriveAt(world, i - 1, j, k));
		}
	}

	public static TileEntityDiskDrive getDiskDriveAt(World world, int i, int j, int k) {
		TileEntity entity = world.getBlockTileEntity(i, j, k);
		if (entity instanceof TileEntityDiskDrive)
			return (TileEntityDiskDrive) entity;
		return null;
	}

	/*public int getBlockTexture(WorldSource iblockaccess, int i, int j, int k, int l) {
		if (l == 1 || l == 0) {
			return this.blockTextures[3];
		}
		int i1 = iblockaccess.getBlockMetadata(i, j, k);
		if (l == i1) {
			TileEntityComputer computer = (TileEntityComputer) iblockaccess.getBlockTileEntity(i, j, k);
			if (computer != null && computer.isSwitchedOn()) {
				if (computer.isCursorVisible()) {
					return this.blinkTexture;
				}
				return this.blockTextures[2];
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
			return this.blinkTexture;
		}
		return this.blockTextures[1];
	}*/

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer entityplayer) {
		if (Computers.isMultiplayerClient()) {
			return true;
		}
		if (!entityplayer.isSneaking()) {
			TileEntityComputer computer = (TileEntityComputer) world.getBlockTileEntity(x, y, z);
			if (computer != null) {
				PacketComputers packet = new PacketComputers();
				packet.packetType = 1;
				packet.dataInt = new int[]{x, y, z};
				Computers.sendToPlayer(entityplayer, packet);
				computer.setSwitchedOn(true);
				computer.updateClient(entityplayer);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	public boolean isPoweringTo(WorldSource iblockaccess, int i, int j, int k, int l) {
		//System.out.println("ComputerCraft: isPoweringTo");
		TileEntityComputer computer = (TileEntityComputer) iblockaccess.getBlockTileEntity(i, j, k);
		if (computer != null) {
			int side = BlockComputer.getLocalSide(l, iblockaccess.getBlockMetadata(i, j, k));
			return computer.isPowering(side);
		}
		return false;

	}

	public void updateTick(World world, int i, int j, int k, Random random) {
		this.refreshInput(world, i, j, k);
		//System.out.println("ComputerCraft: updateTick");

		//things don't update properly unless we do this terribleness
		//world.scheduleBlockUpdate(i, j, k, blockID, tickRate());
	}

	public void onNeighborBlockChange(World world, int i, int j, int k, int l) {
		this.refreshInput(world, i, j, k);
	}

	public int tickRate() {
		return 1;
	}

	public void onBlockRemoval(World world, int i, int j, int k) {
		TileEntityComputer computer = (TileEntityComputer) world.getBlockTileEntity(i, j, k);
		if (computer != null)
			computer.destroy();
		super.onBlockRemoval(world, i, j, k);
	}

	@Override
	protected TileEntity getNewBlockEntity() {
		return new TileEntityComputer();
	}

	public static int getLocalSide(int worldSide, int metadata) {
		int right;
		int left;
		int back;
		int front = metadata;
		switch (front) {
			case 2: {
				back = 3;
				left = 4;
				right = 5;
				break;
			}
			case 3: {
				back = 2;
				left = 5;
				right = 4;
				break;
			}
			case 4: {
				back = 5;
				left = 3;
				right = 2;
				break;
			}
			case 5: {
				back = 4;
				left = 2;
				right = 3;
				break;
			}
			default: {
				return worldSide;
			}
		}
		if (worldSide == front) {
			return 3;
		}
		if (worldSide == back) {
			return 2;
		}
		if (worldSide == left) {
			return 4;
		}
		if (worldSide == right) {
			return 5;
		}
		return worldSide;
	}

	public boolean canProvidePower() {
		return true;
	}
}
