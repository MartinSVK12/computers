package sunsetsatellite.computers;

import dan200.client.FixedWidthFontRenderer;
import dan200.client.GuiComputer;
import dan200.client.GuiDiskDrive;
import dan200.shared.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.EntityPlayerSP;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.crafting.CraftingManager;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.tool.ItemToolPickaxe;
import net.minecraft.core.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sunsetsatellite.computers.packets.PacketComputers;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.helper.EntityHelper;
import turniplabs.halplibe.helper.ItemHelper;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Computers implements ModInitializer {
	public static final String MOD_ID = "computers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final TomlConfigHandler config;
	private static int availableBlockId = 1300;
	private static int availableItemId = 17300;

	static {
		Toml configToml = new Toml("Computers configuration file.");
		configToml.addCategory("BlockIDs");
		configToml.addCategory("ItemIDs");
		configToml.addCategory("Other");
		configToml.addEntry("Other.enableHTTP", 0);
		configToml.addEntry("Other.enableLuaJava", 0);
		configToml.addEntry("Other.terminalTextColor", 0xFFFFFF);

		List<Field> blockFields = Arrays.stream(Computers.class.getDeclaredFields()).filter((F) -> Block.class.isAssignableFrom(F.getType())).collect(Collectors.toList());
		for (Field blockField : blockFields) {
			configToml.addEntry("BlockIDs." + blockField.getName(), availableBlockId++);
		}
		List<Field> itemFields = Arrays.stream(Computers.class.getDeclaredFields()).filter((F) -> Item.class.isAssignableFrom(F.getType())).collect(Collectors.toList());
		for (Field itemField : itemFields) {
			configToml.addEntry("ItemIDs." + itemField.getName(), availableItemId++);
		}

		config = new TomlConfigHandler(MOD_ID, configToml);
	}

	public static final int terminal_defaultWidth = 50;
	public static final int terminal_defaultHeight = 18;
	public static int terminal_width = 50;
	public static int terminal_height = 18;
	public static int terminal_textColour_r = config.getInt("Other.terminalTextColor") >> 16 & 0xFF;
	public static int terminal_textColour_g = config.getInt("Other.terminalTextColor") >> 8 & 0xFF;
	public static int terminal_textColour_b = config.getInt("Other.terminalTextColor") & 0xFF;

	public static int enableAPI_http = config.getInt("Other.enableHTTP");
	public static int enableAPI_luajava = config.getInt("Other.enableLuaJava");

	public static FixedWidthFontRenderer fixedWidthFontRenderer;

	public static Computers instance;

	public static String luaFolder = "/mods/computers/lua";

	public static final BlockComputer computer = (BlockComputer) new BlockBuilder(MOD_ID)
		.setSideTextures("computer/1.png")
		.setNorthTexture("computer/2.png")
		.setTopBottomTexture("computer/3.png")
		.setHardness(2)
		.setResistance(2)
		.build(new BlockComputer("computer",config.getInt("BlockIDs.computer")).withTags(BlockTags.MINEABLE_BY_PICKAXE));
	public static final BlockDiskDrive diskDrive = (BlockDiskDrive) new BlockBuilder(MOD_ID)
		.setSideTextures("diskdrive/1.png")
		.setNorthTexture("diskdrive/0.png")
		.setTopBottomTexture("diskdrive/3.png")
		.setHardness(2)
		.setResistance(2)
		.build(new BlockDiskDrive("diskDrive",config.getInt("BlockIDs.diskDrive")).withTags(BlockTags.MINEABLE_BY_PICKAXE));
	public static final ItemDisk disk = (ItemDisk) ItemHelper.createItem(MOD_ID,new ItemDisk(config.getInt("ItemIDs.disk")),"computerDisk","floppy.png");

	public static int m_tickCount;

	public static void notifyBlockChange(World world, int x, int y, int z, int b) {
		world.notifyBlockChange(x, y, z, b);
	}

	public void load() {
		Minecraft mc = Minecraft.getMinecraft(Minecraft.class);
		fixedWidthFontRenderer = new FixedWidthFontRenderer(mc.gameSettings, "/assets/computers/font/default.png", mc.renderEngine);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Computers initialized.");
		instance = this;

		ItemToolPickaxe.miningLevels.put(computer,1);
		ItemToolPickaxe.miningLevels.put(diskDrive,1);

		CraftingManager.getInstance().addRecipe(new ItemStack(computer, 1), "XXX", "XYX", "XZX", 'X', Block.stone, 'Y', Item.dustRedstone, 'Z', Block.glass);
		CraftingManager.getInstance().addRecipe(new ItemStack(diskDrive, 1), "XXX", "XYX", "XYX", 'X', Block.stone, 'Y', Item.dustRedstone);
		CraftingManager.getInstance().addRecipe(new ItemStack(disk, 1), "X", "Y", 'X', Item.dustRedstone, 'Y', Item.paper);


		EntityHelper.createTileEntity(TileEntityComputer.class,"Computer");
		EntityHelper.createTileEntity(TileEntityDiskDrive.class,"Disk Drive");
	}

	public static boolean isMultiplayerClient() {
		World world = Minecraft.getMinecraft(Minecraft.class).theWorld;
		if (world != null) {
			return world.isClientSide;
		}
		return false;
	}

	public static boolean isMultiplayerServer() {
		return false;
	}

	public static void sendToPlayer(EntityPlayer player, PacketComputers packet) {
		if (player instanceof EntityPlayerSP) {
			instance.HandlePacket(packet);
		}
	}

	public static void sendToAllPlayers(PacketComputers packet) {
		instance.HandlePacket(packet);
	}

	public static void sendToServer(PacketComputers packet) {
		Minecraft.getMinecraft(Minecraft.class).getSendQueue().addToSendQueue(packet);
	}

	public void HandlePacket(PacketComputers packet) {
		World world = Minecraft.getMinecraft(Minecraft.class).theWorld;
		EntityPlayer player = Minecraft.getMinecraft(Minecraft.class).thePlayer;
		if (world != null) {
			if (packet.packetType == 10) {
				for (int n = 0; n < packet.dataInt.length; ++n) {
					int id = packet.dataInt[n];
					String label = packet.dataString[n];
					ItemDisk.setDiskLabel(id, label);
				}
			} else {
				int i = packet.dataInt[0];
				int j = packet.dataInt[1];
				int k = packet.dataInt[2];
				TileEntity entity = world.getBlockTileEntity(i, j, k);
				if (entity instanceof IComputerCraftEntity) {
					IComputerCraftEntity iComputerCraftEntity = (IComputerCraftEntity) entity;
					if (packet.packetType == 1) {
						Minecraft.getMinecraft(Minecraft.class).displayGuiScreen(new GuiComputer((TileEntityComputer)iComputerCraftEntity));
					} else {
						iComputerCraftEntity.handlePacket(packet, player);
					}
				}
			}
		}
	}

	public static File getModDir() {
		Minecraft mc = Minecraft.getMinecraft(Minecraft.class);
		return new File(mc.getMinecraftDir(), "mods/computers");
	}

	public static File getWorldDir(World world) {
		Minecraft mc = Minecraft.getMinecraft(Minecraft.class);
		return new File(mc.getMinecraftDir(), "saves/" + world.getLevelData().getWorldName());
	}

	public static void openDiskDriveGUI(EntityPlayer entityplayer, TileEntityDiskDrive drive) {
		GuiDiskDrive gui = new GuiDiskDrive(entityplayer.inventory, drive);
		Minecraft.getMinecraft(Minecraft.class).displayGuiScreen(gui);
	}

	public static boolean getCursorBlink() {
		return (m_tickCount / 6 % 2 == 0);
	}
}
