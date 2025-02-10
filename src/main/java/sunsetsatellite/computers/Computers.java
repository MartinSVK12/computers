package sunsetsatellite.computers;

import dan200.client.FixedWidthFontRenderer;
import dan200.client.GuiComputer;
import dan200.client.GuiDiskDrive;
import dan200.shared.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.EntityPlayerSP;
import net.minecraft.client.render.block.model.BlockModelHorizontalRotation;
import net.minecraft.client.render.stitcher.AtlasStitcher;
import net.minecraft.client.render.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.data.DataLoader;
import net.minecraft.core.data.registry.Registries;
import net.minecraft.core.data.registry.recipe.RecipeGroup;
import net.minecraft.core.data.registry.recipe.RecipeNamespace;
import net.minecraft.core.data.registry.recipe.RecipeSymbol;
import net.minecraft.core.data.registry.recipe.entry.RecipeEntryCrafting;
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
import turniplabs.halplibe.helper.ItemBuilder;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.ClientStartEntrypoint;
import turniplabs.halplibe.util.RecipeEntrypoint;
import turniplabs.halplibe.util.TomlConfigHandler;
import turniplabs.halplibe.util.toml.Toml;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Computers implements ModInitializer, RecipeEntrypoint, ClientStartEntrypoint {
	public static final String MOD_ID = "computers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final TomlConfigHandler config;
	private static int availableBlockId = 1870;
	private static int availableItemId = 17300;

	public static RecipeNamespace COMPUTERS;
	public static RecipeGroup<RecipeEntryCrafting<?,?>> WORKBENCH;

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
		.setSideTextures("computers:block/1_computer")
		.setNorthTexture("computers:block/2_computer")
		.setTopBottomTextures("computers:block/3_computer")
		.setHardness(2)
		.setResistance(2)
		.setBlockModel(BlockModelHorizontalRotation::new)
		.build(new BlockComputer("computer",config.getInt("BlockIDs.computer")).withTags(BlockTags.MINEABLE_BY_PICKAXE));
	public static final BlockDiskDrive diskDrive = (BlockDiskDrive) new BlockBuilder(MOD_ID)
		.setSideTextures("computers:block/1_diskdrive")
		.setNorthTexture("computers:block/0_diskdrive")
		.setTopBottomTextures("computers:block/3_diskdrive")
		.setHardness(2)
		.setResistance(2)
		.setBlockModel(BlockModelHorizontalRotation::new)
		.build(new BlockDiskDrive("diskDrive",config.getInt("BlockIDs.diskDrive")).withTags(BlockTags.MINEABLE_BY_PICKAXE));
	public static final ItemDisk disk = new ItemBuilder(MOD_ID).setIcon("computers:item/floppy").build(new ItemDisk("computerDisk",config.getInt("ItemIDs.disk")));

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

	@Override
	public void onRecipesReady() {
		RecipeBuilder.Shaped(MOD_ID,"XXX","XYX","XZX")
			.addInput('X',"minecraft:stones")
			.addInput('Y',Item.dustRedstone)
			.addInput('Z',Block.glass)
			.create("computer",computer.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,"XXX","XYX","XYX")
			.addInput('X',"minecraft:stones")
			.addInput('Y',Item.dustRedstone)
			.create("computer",diskDrive.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,"XXX","XYX","XYX")
			.addInput('X',Item.paper)
			.addInput('Y',Item.dustRedstone)
			.create("computer",disk.getDefaultStack());
	}

	@Override
	public void initNamespaces() {
		COMPUTERS = new RecipeNamespace();
		WORKBENCH = new RecipeGroup<>(new RecipeSymbol(new ItemStack(Block.workbench)));
		COMPUTERS.register("workbench",WORKBENCH);
		Registries.RECIPES.register("computers",COMPUTERS);
	}

	//thanks kill05 ;) (and also why do i have to use this again?)
	public void loadTextures(AtlasStitcher stitcher){
		// This is awful, but required until 7.2-pre2 comes ou-- nuh uh, pre2 is here and this is still needed :abyss:
		String id = TextureRegistry.stitcherMap.entrySet().stream().filter((e)->e.getValue() == stitcher).map(Map.Entry::getKey).collect(Collectors.toSet()).stream().findFirst().orElse(null);
		if(id == null){
			throw new RuntimeException("Failed to load textures: invalid atlas provided!");
		}
		LOGGER.info("Loading "+id+" textures...");
		long start = System.currentTimeMillis();

		String path = String.format("%s/%s/%s", "/assets", MOD_ID, stitcher.directoryPath);
		URI uri;
		try {
			uri = DataLoader.class.getResource(path).toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		FileSystem fileSystem = null;
		Path myPath;
		if (uri.getScheme().equals("jar")) {
			try {
				fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			myPath = fileSystem.getPath(path);
		} else {
			myPath = Paths.get(uri);
		}

		Stream<Path> walk;
		try {
			walk = Files.walk(myPath, Integer.MAX_VALUE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Iterator<Path> it = walk.iterator();

		while (it.hasNext()) {
			Path file = it.next();
			String name = file.getFileName().toString();
			if (name.endsWith(".png")) {
				String path1 = file.toString().replace(file.getFileSystem().getSeparator(), "/");
				String cutPath = path1.split(path)[1];
				cutPath = cutPath.substring(0, cutPath.length() - 4);
				TextureRegistry.getTexture(MOD_ID + ":"+ id + cutPath);
			}
		}

		walk.close();
		if (fileSystem != null) {
			try {
				fileSystem.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			TextureRegistry.initializeAllFiles(MOD_ID, stitcher, true);
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException("Failed to load textures.", e);
		}
		LOGGER.info(String.format("Loaded "+id+" textures (took %sms).", System.currentTimeMillis() - start));
	}

	@Override
	public void beforeClientStart() {

	}

	@Override
	public void afterClientStart() {
		loadTextures(TextureRegistry.blockAtlas);
		loadTextures(TextureRegistry.itemAtlas);
		Minecraft.getMinecraft(Minecraft.class).renderEngine.refreshTextures(new ArrayList<>());
	}
}
