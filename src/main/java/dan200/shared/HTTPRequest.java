/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.shared;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPRequest {
	Object m_lock = new Object();
	URL m_url;
	Thread m_thread;
	String m_urlString;
	boolean m_complete;
	boolean m_cancelled;
	boolean m_success;
	String m_result;

	public HTTPRequest(String _url) throws HTTPRequestException {
		this.m_urlString = _url;
		try {
			this.m_url = new URL(_url);
			if (!this.m_url.getProtocol().toLowerCase().equals("http")) {
				throw new HTTPRequestException("Not an HTTP URL");
			}
		} catch (MalformedURLException e) {
			throw new HTTPRequestException("Invalid URL");
		}
		this.m_cancelled = false;
		this.m_complete = false;
		this.m_success = false;
		this.m_result = null;
		this.m_thread = new Thread(new Runnable() {

			/*
// WARNING - Removed try catching itself - possible behaviour change.
			 */
			@Override
			public void run() {
				try {
					Object object;
					InputStream is = HTTPRequest.this.m_url.openStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader reader = new BufferedReader(isr);
					StringBuilder result = new StringBuilder();
					do {
						object = HTTPRequest.this.m_lock;
						synchronized (object) {
							if (HTTPRequest.this.m_cancelled) {
								break;
							}
						}
						String line = reader.readLine();
						if (line == null) break;
						result.append(line);
						result.append('\n');
					} while (true);
					reader.close();
					object = HTTPRequest.this.m_lock;
					synchronized (object) {
						if (HTTPRequest.this.m_cancelled) {
							HTTPRequest.this.m_complete = true;
							HTTPRequest.this.m_success = false;
							HTTPRequest.this.m_result = null;
						} else {
							HTTPRequest.this.m_complete = true;
							HTTPRequest.this.m_success = true;
							HTTPRequest.this.m_result = result.toString();
						}
					}
				} catch (IOException e) {
					Object object = HTTPRequest.this.m_lock;
					synchronized (object) {
						HTTPRequest.this.m_complete = true;
						HTTPRequest.this.m_success = false;
						HTTPRequest.this.m_result = null;
					}
				}
			}
		}
		);
		this.m_thread.start();
	}

	String getURL() {
		return this.m_urlString;
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	void cancel() {
		Object object = this.m_lock;
		synchronized (object) {
			this.m_cancelled = true;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public boolean isComplete() {
		Object object = this.m_lock;
		synchronized (object) {
			return this.m_complete;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public boolean wasSuccessful() {
		Object object = this.m_lock;
		synchronized (object) {
			return this.m_success;
		}
	}

	/*
// WARNING - Removed try catching itself - possible behaviour change.
	 */
	public BufferedReader getContents() {
		String result = null;
		Object object = this.m_lock;
		synchronized (object) {
			result = this.m_result;
		}
		if (result != null) {
			return new BufferedReader(new StringReader(result));
		}
		return null;
	}
}
