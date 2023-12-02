/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;

public class Terminal {
	private int m_cursorX;

	private int m_cursorY;

	private boolean m_cursorBlink;

	private int m_width;

	private int m_height;

	private String m_emptyLine;

	private String[] m_lines;

	private boolean m_changed;

	private boolean[] m_lineChanged;

	public Terminal(int width, int height) {
		this.m_width = width;
		this.m_height = height;
		StringBuilder emptyLine = new StringBuilder();
		int i;
		for (i = 0; i < this.m_width; i++)
			emptyLine.append(' ');
		this.m_emptyLine = emptyLine.toString();
		this.m_lines = new String[this.m_height];
		for (i = 0; i < this.m_height; i++)
			this.m_lines[i] = this.m_emptyLine;
		this.m_cursorX = 0;
		this.m_cursorY = 0;
		this.m_cursorBlink = false;
		this.m_changed = false;
		this.m_lineChanged = new boolean[this.m_height];
	}

	public int getWidth() {
		return this.m_width;
	}

	public int getHeight() {
		return this.m_height;
	}

	public void setCursorPos(int x, int y) {
		if (this.m_cursorX != x || this.m_cursorY != y) {
			this.m_cursorX = x;
			this.m_cursorY = y;
			this.m_changed = true;
		}
	}

	public void setCursorBlink(boolean blink) {
		if (this.m_cursorBlink != blink) {
			this.m_cursorBlink = blink;
			this.m_changed = true;
		}
	}

	public int getCursorX() {
		return this.m_cursorX;
	}

	public int getCursorY() {
		return this.m_cursorY;
	}

	public boolean getCursorBlink() {
		return this.m_cursorBlink;
	}

	public void write(String string) {
		if (this.m_cursorY >= 0 && this.m_cursorY < this.m_height) {
			string = string.replace('\t', ' ');
			string = string.replace('\r', ' ');
			int writeX = this.m_cursorX;
			int spaceLeft = this.m_width - this.m_cursorX;
			if (spaceLeft > this.m_width + string.length())
				return;
			if (spaceLeft > this.m_width) {
				writeX = 0;
				string = string.substring(spaceLeft - this.m_width);
				spaceLeft = this.m_width;
			}
			if (spaceLeft > 0) {
				String oldLine = this.m_lines[this.m_cursorY];
				StringBuilder newLine = new StringBuilder();
				newLine.append(oldLine.substring(0, writeX));
				if (string.length() < spaceLeft) {
					newLine.append(string);
					newLine.append(oldLine.substring(writeX + string.length()));
				} else {
					newLine.append(string.substring(0, spaceLeft));
				}
				this.m_lines[this.m_cursorY] = newLine.toString();
				this.m_changed = true;
				this.m_lineChanged[this.m_cursorY] = true;
			}
		}
	}

	public void scroll(int yDiff) {
		String[] newLines = new String[this.m_height];
		for (int y = 0; y < this.m_height; y++) {
			int oldY = y + yDiff;
			if (oldY >= 0 && oldY < this.m_height) {
				newLines[y] = this.m_lines[oldY];
			} else {
				newLines[y] = this.m_emptyLine;
			}
			if (!newLines[y].equals(this.m_lines[y]))
				this.m_lineChanged[y] = true;
		}
		this.m_lines = newLines;
		this.m_changed = true;
	}

	public void clear() {
		for (int y = 0; y < this.m_height; y++) {
			if (!this.m_lines[y].equals(this.m_emptyLine)) {
				this.m_lines[y] = this.m_emptyLine;
				this.m_lineChanged[y] = true;
				this.m_changed = true;
			}
		}
	}

	public void clearLine() {
		if (this.m_cursorY >= 0 && this.m_cursorY < this.m_height &&
			!this.m_lines[this.m_cursorY].equals(this.m_emptyLine)) {
			this.m_lines[this.m_cursorY] = this.m_emptyLine;
			this.m_lineChanged[this.m_cursorY] = true;
			this.m_changed = true;
		}
	}

	public String getLine(int y) {
		assert y >= 0 && y < this.m_height;
		return this.m_lines[y];
	}

	public String[] getLines() {
		return this.m_lines;
	}

	public boolean getChanged() {
		return this.m_changed;
	}

	public boolean[] getLinesChanged() {
		return this.m_lineChanged;
	}

	public void clearChanged() {
		if (this.m_changed) {
			this.m_changed = false;
			for (int y = 0; y < this.m_height; y++)
				this.m_lineChanged[y] = false;
		}
	}
}
