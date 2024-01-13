/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.client;

import dan200.shared.Terminal;
import dan200.shared.TileEntityComputer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import sunsetsatellite.computers.Computers;

public class GuiComputer extends GuiScreen {
	private TileEntityComputer m_computer;

	private float m_terminateTimer;

	private float m_rebootTimer;

	private float m_shutdownTimer;

	public GuiComputer(TileEntityComputer tileentitycomputer) {
		this.m_computer = tileentitycomputer;
		this.m_terminateTimer = 0.0F;
		this.m_rebootTimer = 0.0F;
	}

	public void init() {
		super.init();
		Keyboard.enableRepeatEvents(true);
		this.m_terminateTimer = 0.0F;
		this.m_rebootTimer = 0.0F;
	}

	public void onClosed() {
		super.onClosed();
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean pausesGame() {
		return false;
	}

	public void tick() {
		super.tick();
		if (this.m_computer.isDestroyed()) {
			this.mc.displayGuiScreen(null);
			return;
		}
		if (Keyboard.isKeyDown(29) || Keyboard.isKeyDown(157)) {
			if (Keyboard.isKeyDown(20)) {
				if (this.m_terminateTimer < 1.0F) {
					this.m_terminateTimer += 0.05F;
					if (this.m_terminateTimer >= 1.0F)
						this.m_computer.terminate();
				}
			} else {
				this.m_terminateTimer = 0.0F;
			}
			if (Keyboard.isKeyDown(19)) {
				if (this.m_rebootTimer < 1.0F) {
					this.m_rebootTimer += 0.05F;
					if (this.m_rebootTimer >= 1.0F)
						this.m_computer.reboot();
				}
			} else {
				this.m_rebootTimer = 0.0F;
			}
			if (Keyboard.isKeyDown(31)) {
				if (this.m_shutdownTimer < 1.0F) {
					this.m_shutdownTimer += 0.05F;
					if (this.m_shutdownTimer >= 1.0F)
						this.m_computer.shutdown();
				}
			} else {
				this.m_shutdownTimer = 0.0F;
			}
		} else {
			this.m_terminateTimer = 0.0F;
			this.m_rebootTimer = 0.0F;
			this.m_shutdownTimer = 0.0F;
		}
	}

	@Override
	public void keyTyped(char c, int key, int mouseX, int mouseY) {
		if(key != 14){
			super.keyTyped(c, key, mouseX, mouseY);
		}
		if (this.m_terminateTimer < 0.5F && this.m_rebootTimer < 0.5F && this.m_shutdownTimer < 0.5F)
			this.m_computer.keyTyped(c, key);
	}

	public void drawScreen(int i, int j, float f) {
		Terminal terminal = this.m_computer.getTerminal();
		synchronized (terminal) {
			boolean tblink = terminal.getCursorBlink();
			int tw = terminal.getWidth();
			int th = terminal.getHeight();
			int tx = terminal.getCursorX();
			int ty = terminal.getCursorY();
			String[] tlines = terminal.getLines();
			drawDefaultBackground();
			int termWidth = 4 + tw * Computers.fixedWidthFontRenderer.FONT_WIDTH;
			int termHeight = 4 + th * Computers.fixedWidthFontRenderer.FONT_HEIGHT;
			int term = this.mc.renderEngine.getTexture("/assets/computers/gui/terminal.png");
			int corners = this.mc.renderEngine.getTexture("/assets/computers/gui/corners.png");
			int vfix = this.mc.renderEngine.getTexture("/assets/computers/gui/vertical_bar_fix.png");
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			int startX = (this.width - termWidth) / 2;
			int startY = (this.height - termHeight) / 2;
			int endX = startX + termWidth;
			int endY = startY + termHeight;
			this.mc.renderEngine.bindTexture(term);
			drawTexturedModalRect(startX, startY, 0, 0, termWidth, termHeight); //bg
			this.mc.renderEngine.bindTexture(corners);

			//drawing border
			drawTexturedModalRect(startX - 12, startY - 12, 12, 28, 12, 12); //top left corner
			drawTexturedModalRect(startX - 12, endY, 12, 40, 12, 16); //bottom left corner
			drawTexturedModalRect(endX, startY - 12, 24, 28, 12, 12); //top right corner
			drawTexturedModalRect(endX, endY, 24, 40, 12, 16); //bottom right corner

			drawTexturedModalRect(startX, startY - 12, 0, 0, termWidth, 12); //top bar
			drawTexturedModalRect(startX, endY, 0, 12, termWidth, 16); //bottom bar

			this.mc.renderEngine.bindTexture(vfix);
			drawTexturedModalRect(startX - 12, startY, 0, 28, 12, termHeight); //left bar
			drawTexturedModalRect(endX, startY, 36, 28, 12, termHeight); //right bar


			int textColour = (Computers.terminal_textColour_r << 16) + (Computers.terminal_textColour_g << 8) + (Computers.terminal_textColour_b << 0);
			int x = startX + 2;
			int y = startY + 2;
			for (int line = 0; line < th; line++) {
				String text = tlines[line];
				if (tblink && ty == line && Computers.getCursorBlink() &&
					tx >= 0 && tx < tw)
					Computers.fixedWidthFontRenderer.drawString("_", x + Computers.fixedWidthFontRenderer.FONT_WIDTH * tx, y, textColour);
				Computers.fixedWidthFontRenderer.drawString(text, x, y, textColour);
				y += Computers.fixedWidthFontRenderer.FONT_HEIGHT;
			}
		}
		super.drawScreen(i, j, f);
	}
}
