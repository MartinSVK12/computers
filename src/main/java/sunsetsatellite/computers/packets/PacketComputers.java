package sunsetsatellite.computers.packets;

import net.minecraft.core.net.handler.NetHandler;
import net.minecraft.core.net.packet.Packet;
import sunsetsatellite.computers.Computers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketComputers extends Packet {
	private static final int MAX_DATA_LENGTH = 65535;
	public int modId;
	public int packetType;
	public int[] dataInt = new int[0];
	public float[] dataFloat = new float[0];
	public String[] dataString = new String[0];

	public void readPacketData(DataInputStream datainputstream) throws IOException {
		this.modId = datainputstream.readInt();
		this.packetType = datainputstream.readInt();
		int i = datainputstream.readInt();
		if (i > 65535) {
			throw new IOException(String.format("Integer data size of %d is higher than the max (%d).", i, 65535));
		} else {
			this.dataInt = new int[i];

			int k;
			for (k = 0; k < i; ++k) {
				this.dataInt[k] = datainputstream.readInt();
			}

			k = datainputstream.readInt();
			if (k > 65535) {
				throw new IOException(String.format("Float data size of %d is higher than the max (%d).", k, 65535));
			} else {
				this.dataFloat = new float[k];

				int i1;
				for (i1 = 0; i1 < k; ++i1) {
					this.dataFloat[i1] = datainputstream.readFloat();
				}

				i1 = datainputstream.readInt();
				if (i1 > 65535) {
					throw new IOException(String.format("String data size of %d is higher than the max (%d).", i1, 65535));
				} else {
					this.dataString = new String[i1];

					for (int j1 = 0; j1 < i1; ++j1) {
						int k1 = datainputstream.readInt();
						if (k1 > 65535) {
							throw new IOException(String.format("String length of %d is higher than the max (%d).", k1, 65535));
						}

						byte[] abyte0 = new byte[k1];
						datainputstream.read(abyte0, 0, k1);
						this.dataString[j1] = new String(abyte0);
					}

				}
			}
		}
	}

	public void writePacketData(DataOutputStream dataoutputstream) throws IOException {
		if (this.dataInt != null && this.dataInt.length > 65535) {
			throw new IOException(String.format("Integer data size of %d is higher than the max (%d).", this.dataInt.length, 65535));
		} else if (this.dataFloat != null && this.dataFloat.length > 65535) {
			throw new IOException(String.format("Float data size of %d is higher than the max (%d).", this.dataFloat.length, 65535));
		} else if (this.dataString != null && this.dataString.length > 65535) {
			throw new IOException(String.format("String data size of %d is higher than the max (%d).", this.dataString.length, 65535));
		} else {
			dataoutputstream.writeInt(this.modId);
			dataoutputstream.writeInt(this.packetType);
			int k;
			if (this.dataInt == null) {
				dataoutputstream.writeInt(0);
			} else {
				dataoutputstream.writeInt(this.dataInt.length);

				for (k = 0; k < this.dataInt.length; ++k) {
					dataoutputstream.writeInt(this.dataInt[k]);
				}
			}

			if (this.dataFloat == null) {
				dataoutputstream.writeInt(0);
			} else {
				dataoutputstream.writeInt(this.dataFloat.length);

				for (k = 0; k < this.dataFloat.length; ++k) {
					dataoutputstream.writeFloat(this.dataFloat[k]);
				}
			}

			if (this.dataString == null) {
				dataoutputstream.writeInt(0);
			} else {
				dataoutputstream.writeInt(this.dataString.length);

				for (k = 0; k < this.dataString.length; ++k) {
					if (this.dataString[k].length() > 65535) {
						throw new IOException(String.format("String length of %d is higher than the max (%d).", this.dataString[k].length(), 65535));
					}

					dataoutputstream.writeInt(this.dataString[k].length());
					dataoutputstream.writeBytes(this.dataString[k]);
				}
			}

		}
	}

	public void processPacket(NetHandler nethandler) {
		Computers.instance.HandlePacket(this);
	}

	public int getPacketSize() {
		byte i = 1;
		int i3 = i + 1;
		++i3;
		i3 += this.dataInt != null ? this.dataInt.length * 32 : 0;
		++i3;
		i3 += this.dataFloat != null ? this.dataFloat.length * 32 : 0;
		++i3;
		if (this.dataString != null) {
			for (int j = 0; j < this.dataString.length; ++j) {
				++i3;
				i3 += this.dataString[j].length();
			}
		}

		return i3;
	}
}
