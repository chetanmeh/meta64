package com.meta64.mobile.util;

/*
 * TODO: Apache commons has a class already like this one (even same name), and the POM is already pulling it in so USE IT!
 */

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream, which limits its data size. This stream is used, if the content length is
 * unknown.
 */
public class LimitedInputStream extends FilterInputStream {
	/**
	 * The maximum size of an item, in bytes.
	 */
	private long sizeMax;
	/**
	 * The current number of bytes.
	 */
	private long count;
	/**
	 * Whether this stream is already closed.
	 */
	private boolean closed;

	/**
	 * Creates a new instance.
	 * 
	 * @param pIn
	 *            The input stream, which shall be limited.
	 * @param pSizeMax
	 *            The limit; no more than this number of bytes shall be returned by the source
	 *            stream.
	 */
	public LimitedInputStream(InputStream pIn, long pSizeMax) {
		super(pIn);
		sizeMax = pSizeMax;
	}

	/**
	 * Reads the next byte of data from this input stream. The value byte is returned as an
	 * <code>int</code> in the range <code>0</code> to <code>255</code>. If no byte is available
	 * because the end of the stream has been reached, the value <code>-1</code> is returned. This
	 * method blocks until input data is available, the end of the stream is detected, or an
	 * exception is thrown.
	 * 
	 * This method simply performs <code>in.read()</code> and returns the result.
	 * 
	 * @return the next byte of data, or <code>-1</code> if the end of the stream is reached.
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.FilterInputStream#in
	 */
	@Override
	public int read() throws IOException {
		int res = super.read();
		if (res != -1) {
			count++;
			if (count > sizeMax) {
				throw new IOException("stream to large.");
			}
		}
		return res;
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		int res = super.read(bytes);
		if (res != -1) {
			count++;
			if (count > sizeMax) {
				throw new IOException("stream to large.");
			}
		}
		return res;
	}

	/**
	 * Reads up to <code>len</code> bytes of data from this input stream into an array of bytes. If
	 * <code>len</code> is not zero, the method blocks until some input is available; otherwise, no
	 * bytes are read and <code>0</code> is returned.
	 * 
	 * This method simply performs <code>in.read(b, off, len)</code> and returns the result.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param off
	 *            The start offset in the destination array <code>b</code>.
	 * @param len
	 *            the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
	 *         more data because the end of the stream has been reached.
	 * @exception NullPointerException
	 *                If <code>b</code> is <code>null</code>.
	 * @exception IndexOutOfBoundsException
	 *                If <code>off</code> is negative, <code>len</code> is negative, or
	 *                <code>len</code> is greater than <code>b.length - off</code>
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.FilterInputStream#in
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int res = super.read(b, off, len);
		if (res != -1) {
			count += res;
			if (count > sizeMax) {
				throw new IOException("stream to large.");
			}
		}
		return res;
	}

	/**
	 * Returns, whether this stream is already closed.
	 * 
	 * @return True, if the stream is closed, otherwise false.
	 * @throws IOException
	 *             An I/O error occurred.
	 */
	public boolean isClosed() throws IOException {
		return closed;
	}

	/**
	 * Closes this input stream and releases any system resources associated with the stream. This
	 * method simply performs <code>in.close()</code>.
	 * 
	 * @exception IOException
	 *                if an I/O error occurs.
	 * @see java.io.FilterInputStream#in
	 */
	@Override
	public void close() throws IOException {
		closed = true;
		super.close();
	}

	public long getCount() {
		return count;
	}
}