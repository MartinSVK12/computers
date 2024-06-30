/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;


import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import sunsetsatellite.catalyst.core.util.ICustomDescription;
import sunsetsatellite.computers.Computers;
import sunsetsatellite.computers.packets.PacketComputers;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemDisk
	extends Item implements ICustomDescription {
	public static WeakReference labelWorld = null;
	private static Map labels = new HashMap();
	private static Map serverLabelRequests = new HashMap();
	private static boolean labelsChanged = false;

	public ItemDisk(String name, int id) {
		super(name, id);
	}

	private static int getNewDiskID(World world) {
		File baseUserDir = new File(Computers.getWorldDir(world), "/computer/disk");
		int id = 1;
		while (new File(baseUserDir, Integer.toString(id)).exists()) {
			++id;
		}
		File userDir = new File(baseUserDir, Integer.toString(id));
		userDir.mkdirs();
		return id;
	}

	public static int getDiskID(ItemStack itemstack, World world) {
		if (itemstack.itemID == Computers.disk.id) {
			int damage = itemstack.getMetadata();
			if (damage == 0 && world != null) {
				damage = ItemDisk.getNewDiskID(world);
				itemstack.setMetadata(damage);
			}
			return damage;
		}
		return -1;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public static void loadLabelsIfWorldChanged(World world) {
		Map strin = labels;
		synchronized (strin) {
			World currentWorld = null;
			if (labelWorld != null) {
				currentWorld = (World) labelWorld.get();
			}
			if (world != currentWorld) {
				labels.clear();
				serverLabelRequests.clear();
				labelWorld = null;
				labelsChanged = false;
				if (world != null) {
					labelWorld = new WeakReference<World>(world);
				}
				ComputerThread.start();
				ComputerThread.queueTask(new ITask() {

											 @Override
											 public Computer getOwner() {
												 return null;
											 }

											 @Override
											 public void execute() {
												 //ItemDisk.loadLabels((World) labelWorld.get());
											 }
										 }, null
				);

			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public static void loadLabels(World world) {
		Map strin = labels;
		synchronized (strin) {
			block12:
			{
				labels.clear();
				serverLabelRequests.clear();
				labelWorld = null;
				labelsChanged = false;
				if (world == null) {
					return;
				}
				labelWorld = new WeakReference<World>(world);
				labelsChanged = false;
				BufferedReader reader = null;
				try {
					File labelFile = new File(Computers.getWorldDir(world), "/computer/disk/labels.txt");
					if (!labelFile.exists()) break block12;
					reader = new BufferedReader(new FileReader(labelFile));
					String line = null;
					while ((line = reader.readLine()) != null) {
						int number;
						int space = line.indexOf(32);
						if (space <= 0) continue;
						try {
							number = Integer.parseInt(line.substring(0, space));
						} catch (NumberFormatException e) {
							continue;
						}
						String label = line.substring(space + 1).trim();
						labels.put(number, label);
					}
					reader.close();
				} catch (IOException e) {
					System.out.println("ComputerCraft: Failed to write to labels file");
					try {
						if (reader != null) {
							reader.close();
						}
					} catch (IOException e2) {
// empty catch block
					}
				}
			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	private static void saveLabels() {
		Map strin = labels;
		synchronized (strin) {
			if (labelWorld == null) {
				return;
			}
			World world = (World) labelWorld.get();
			if (world == null) {
				labelWorld = null;
				return;
			}
			if (labelsChanged) {
				BufferedWriter writer = null;
				try {
					File labelFile = new File(Computers.getWorldDir(world), "/computer/disk/labels.txt");
					writer = new BufferedWriter(new FileWriter(labelFile));
					Set<Map.Entry> entries = labels.entrySet();
					for (Map.Entry entry : entries) {
						writer.write(entry.getKey() + " " + (String) entry.getValue());
						writer.newLine();
					}
					writer.close();
				} catch (IOException e) {
					System.out.println("ComputerCraft: Failed to write to labels file");
					try {
						if (writer != null) {
							writer.close();
						}
					} catch (IOException e2) {
// empty catch block
					}
				} finally {
					labelsChanged = false;
				}
			}
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public static String getDiskLabel(int diskID) {
		if (diskID > 0) {
			Map strin = labels;
			synchronized (strin) {
				String label = (String) labels.get(diskID);
				if (label != null) {
					return label;
				}
			}
		}
		return null;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public static void setDiskLabel(int diskID, String label) {
		if (diskID > 0) {
			Map strin = labels;
			synchronized (strin) {
				if (label != null && label.length() == 0) {
					label = null;
				}
				boolean changed = false;
				String existing = (String) labels.get(diskID);
				if (label != null) {
					if ((label = label.trim().replaceAll("[\r\n\t]+", "")).length() > 25) {
						label = label.substring(0, 25);
					}
					if (!label.equals(existing)) {
						labels.put(diskID, label);
						changed = true;
					}
				} else if (existing != null) {
					labels.remove(diskID);
					changed = true;
				}
				if (changed) {
					if (!labelsChanged) {
						labelsChanged = true;
						ComputerThread.queueTask(new ITask() {

													 @Override
													 public Computer getOwner() {
														 return null;
													 }

													 @Override
													 public void execute() {
														 ItemDisk.saveLabels();
													 }
												 }, null
						);
					}
					PacketComputers packet = ItemDisk.buildDiskLabelPacket(diskID, label);
					Computers.sendToAllPlayers(packet);
				}
			}
		}
	}

	public static String getDiskLabel(ItemStack itemstack, World world) {
		int diskID = ItemDisk.getDiskID(itemstack, world);
		return ItemDisk.getDiskLabel(diskID);
	}

	@Override
	public String getDescription(ItemStack itemStack) {
		String label = ItemDisk.getDiskLabel(itemStack, null);
		if (label != null && !label.isEmpty()) {
			return label;
		}
		return "No label.";
	}

	public static void sendDiskLabelToPlayer(EntityPlayer player, int diskID) {
		String label = ItemDisk.getDiskLabel(diskID);
		if (label != null) {
			PacketComputers packet = ItemDisk.buildDiskLabelPacket(diskID, label);
			Computers.sendToPlayer(player, packet);
		}
	}

	private static PacketComputers buildDiskLabelPacket(int diskID, String label) {
		PacketComputers packet = new PacketComputers();
		packet.packetType = 10;
		packet.dataInt = new int[]{diskID};
		packet.dataString = new String[]{label != null ? label : ""};
		return packet;
	}
}
