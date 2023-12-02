/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.*;

public class FileSystem {
	private Mount m_rootMount;
	private Map<String, Mount> m_mounts;

	public FileSystem(File rootMount, boolean readOnly) throws FileSystemException {
		if (!rootMount.exists() || !rootMount.isDirectory()) {
			throw new FileSystemException("No such directory");
		}
		this.m_rootMount = new Mount("", rootMount, readOnly);
		this.m_mounts = new HashMap();
	}

	public void mount(String path, File realPath, boolean readOnly) throws FileSystemException {
		if (realPath.exists() && realPath.isDirectory()) {
			if ((path = this.sanitizePath(path)).indexOf("..") != -1) {
				throw new FileSystemException("Cannot mount below the root");
			}
			if (this.m_mounts.containsKey(path)) {
				this.m_mounts.remove(path);
			}
		} else {
			throw new FileSystemException("No such directory");
		}
		this.m_mounts.put(path, new Mount(path, realPath, readOnly));
	}

	public void unmount(String path) {
		if (this.m_mounts.containsKey(path = this.sanitizePath(path))) {
			this.m_mounts.remove(path);
		}
	}

	public String combine(String path, String childPath) {
		path = this.sanitizePath(path);
		childPath = this.sanitizePath(childPath);
		if (path.length() == 0) {
			return childPath;
		}
		if (childPath.length() == 0) {
			return path;
		}
		return this.sanitizePath(path + '/' + childPath);
	}

	public boolean contains(String pathA, String pathB) {
		return this._contains(this.sanitizePath(pathA), this.sanitizePath(pathB));
	}

	public String getName(String path) {
		if ((path = this.sanitizePath(path)).length() == 0) {
			return "root";
		}
		int lastSlash = path.lastIndexOf(47);
		if (lastSlash >= 0) {
			return path.substring(lastSlash + 1);
		}
		return path;
	}

	public String[] list(String path) throws FileSystemException {
		File dir = this.getRealPath(path = this.sanitizePath(path));
		if (!dir.exists() || dir.isHidden() || !dir.isDirectory()) {
			throw new FileSystemException("Not a directory");
		}
		String[] paths = dir.list();
		ArrayList<String> filtered = new ArrayList<String>(paths.length);
		for (int i = 0; i < paths.length; ++i) {
			File item = new File(dir, paths[i]);
			if (!item.exists() || item.isHidden()) continue;
			filtered.add(paths[i]);
		}
		for (Mount mount : this.m_mounts.values()) {
			if (!mount.parentLocation.equals(path)) continue;
			filtered.add(mount.name);
		}
		return filtered.toArray(new String[0]);
	}

	public boolean exists(String path) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(path));
		return file.exists() && !file.isHidden();
	}

	public boolean isDir(String path) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(path));
		return file.exists() && !file.isHidden() && file.isDirectory();
	}

	public boolean isReadOnly(String path) throws FileSystemException {
		Mount mount = this.getMount(this.sanitizePath(path));
		if (mount == null) {
			throw new FileSystemException("Invalid path");
		}
		if (path.equals(mount.location)) {
			return true;
		}
		return mount.readOnly;
	}

	public void makeDir(String path) throws FileSystemException {
		if (this.isReadOnly(path)) {
			throw new FileSystemException("Access denied");
		}
		File file = this.getRealPath(this.sanitizePath(path));
		if (file.exists()) {
			if (file.isDirectory()) {
				return;
			}
			throw new FileSystemException("File exists");
		}
		boolean success = file.mkdirs();
		if (!success) {
			throw new FileSystemException("Access denied");
		}
	}

	private void recurseDelete(File file) throws FileSystemException {
		boolean success;
		if (file.isDirectory()) {
			String[] children = file.list();
			for (int i = 0; i < children.length; ++i) {
				this.recurseDelete(new File(file, children[i]));
			}
		}
		if (!(success = file.delete())) {
			throw new FileSystemException("Access denied");
		}
	}

	public void delete(String path) throws FileSystemException {
		if (this.isReadOnly(path = this.sanitizePath(path))) {
			throw new FileSystemException("Access denied");
		}
		File file = this.getRealPath(path);
		if (file.exists() && !file.isHidden()) {
			this.recurseDelete(file);
		}
	}

	public void move(String sourcePath, String destPath) throws FileSystemException {
		sourcePath = this.sanitizePath(sourcePath);
		destPath = this.sanitizePath(destPath);
		if (this.isReadOnly(sourcePath) || this.isReadOnly(destPath)) {
			throw new FileSystemException("Access denied");
		}
		File source = this.getRealPath(sourcePath);
		File dest = this.getRealPath(destPath);
		if (!source.exists() || source.isHidden()) {
			throw new FileSystemException("No such file");
		}
		if (dest.exists()) {
			throw new FileSystemException("File exists");
		}
		if (this._contains(sourcePath, destPath)) {
			throw new FileSystemException("Can't move a directory inside itself");
		}
		boolean success = source.renameTo(dest);
		if (!success) {
			throw new FileSystemException("Access denied");
		}
	}

	private void recurseCopy(File source, File dest) throws FileSystemException {
		assert (source.exists());
		if (source.isDirectory()) {
			boolean success = dest.mkdirs();
			if (!success) {
				throw new FileSystemException("Access denied");
			}
			String[] children = source.list();
			for (int i = 0; i < children.length; ++i) {
				this.recurseCopy(new File(source, children[i]), new File(dest, children[i]));
			}
		} else {
			FileChannel sourceChannel = null;
			AbstractInterruptibleChannel destChannel = null;
			try {
				sourceChannel = new FileInputStream(source).getChannel();
				destChannel = new FileOutputStream(dest).getChannel();
				((FileChannel) destChannel).transferFrom(sourceChannel, 0L, sourceChannel.size());
			} catch (IOException e) {
				throw new FileSystemException("Access denied");
			} finally {
				try {
					if (sourceChannel != null) {
						sourceChannel.close();
					}
					if (destChannel != null) {
						destChannel.close();
					}
				} catch (IOException e) {
				}
			}
		}
	}

	public void copy(String sourcePath, String destPath) throws FileSystemException {
		sourcePath = this.sanitizePath(sourcePath);
		if (this.isReadOnly(destPath = this.sanitizePath(destPath))) {
			throw new FileSystemException("Access denied");
		}
		File source = this.getRealPath(sourcePath);
		File dest = this.getRealPath(destPath);
		if (!source.exists() || source.isHidden()) {
			throw new FileSystemException("No such file");
		}
		if (dest.exists()) {
			throw new FileSystemException("File exists");
		}
		if (this._contains(sourcePath, destPath)) {
			throw new FileSystemException("Can't copy a directory inside itself");
		}
		this.recurseCopy(source, dest);
	}

	public BufferedReader openForRead(String path) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(path));
		if (!file.exists() || file.isHidden()) {
			throw new FileSystemException("File not found");
		}
		if (file.isDirectory()) {
			throw new FileSystemException("Cannot read from directory");
		}
		try {
			return new BufferedReader(new FileReader(file));
		} catch (IOException e) {
			throw new FileSystemException("Access denied");
		}
	}

	public BufferedWriter openForWrite(String path, boolean append) throws FileSystemException {
		if (this.isReadOnly(path = this.sanitizePath(path))) {
			throw new FileSystemException("Access denied");
		}
		File file = this.getRealPath(path);
		if (file.exists()) {
			if (file.isHidden()) {
				throw new FileSystemException("Access denied");
			}
			if (file.isDirectory()) {
				throw new FileSystemException("Cannot write to directory");
			}
		}
		try {
			return new BufferedWriter(new FileWriter(file.toString(), append));
		} catch (IOException e) {
			throw new FileSystemException("Access denied");
		}
	}

	public BufferedInputStream openForBinaryRead(String path) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(path));
		if (!file.exists() || file.isHidden()) {
			throw new FileSystemException("File not found");
		}
		if (file.isDirectory()) {
			throw new FileSystemException("Cannot read from directory");
		}
		try {
			return new BufferedInputStream(new FileInputStream(file));
		} catch (IOException e) {
			throw new FileSystemException("Access denied");
		}
	}

	public BufferedOutputStream openForBinaryWrite(String path, boolean append) throws FileSystemException {
		if (this.isReadOnly(path = this.sanitizePath(path))) {
			throw new FileSystemException("Access denied");
		}
		File file = this.getRealPath(path);
		if (file.exists()) {
			if (file.isHidden()) {
				throw new FileSystemException("Access denied");
			}
			if (file.isDirectory()) {
				throw new FileSystemException("Cannot write to directory");
			}
		}
		try {
			return new BufferedOutputStream(new FileOutputStream(file.toString(), append));
		} catch (IOException e) {
			throw new FileSystemException("Access denied");
		}
	}

	private String sanitizePath(String path) {
		path = path.replace('\\', '/');
		String[] parts = path.split("/");
		Stack<String> outputParts = new Stack<String>();
		for (int n = 0; n < parts.length; ++n) {
			String part = parts[n];
			if (part.length() == 0 || part.equals(".")) continue;
			if (part.equals("..")) {
				if (!outputParts.empty()) {
					String top = (String) outputParts.peek();
					if (!top.equals("..")) {
						outputParts.pop();
						continue;
					}
					outputParts.push("..");
					continue;
				}
				outputParts.push("..");
				continue;
			}
			outputParts.push(part);
		}
		StringBuilder result = new StringBuilder("");
		Iterator it = outputParts.iterator();
		while (it.hasNext()) {
			String part = (String) it.next();
			result.append(part);
			if (!it.hasNext()) continue;
			result.append('/');
		}
		return result.toString();
	}

	private Mount getMount(String path) {
		for (Mount mount : this.m_mounts.values()) {
			if (!this._contains(mount.location, path)) continue;
			return mount;
		}
		if (this._contains(this.m_rootMount.location, path)) {
			return this.m_rootMount;
		}
		return null;
	}

	private File getRealPath(String path) throws FileSystemException {
		Mount mount = this.getMount(path);
		if (mount == null) {
			throw new FileSystemException("Invalid path.");
		}
		return new File(mount.realPath, mount.toLocal(path));
	}

	private boolean _contains(String pathA, String pathB) {
		if (pathB.indexOf("..") >= 0) {
			return false;
		}
		if (pathB.equals(pathA)) {
			return true;
		}
		if (pathA.length() == 0) {
			return true;
		}
		return pathB.startsWith(pathA + "/");
	}

	private class Mount {
		String name;
		String location;
		String parentLocation;
		File realPath;
		boolean readOnly;

		Mount(String _location, File _realPath, boolean _readOnly) {
			this.location = _location;
			this.realPath = _realPath;
			this.readOnly = _readOnly;
			int lastSlash = this.location.lastIndexOf(47);
			if (lastSlash >= 0) {
				this.name = this.location.substring(lastSlash + 1);
				this.parentLocation = this.location.substring(0, lastSlash);
			} else {
				this.name = this.location;
				this.parentLocation = "";
			}
		}

		private String toLocal(String path) {
			assert (FileSystem.this._contains(this.location, path));
			String local = path.substring(this.location.length());
			if (local.startsWith("/")) {
				return local.substring(1);
			}
			return local;
		}
	}
}
