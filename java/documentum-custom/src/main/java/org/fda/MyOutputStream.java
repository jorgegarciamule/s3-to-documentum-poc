package org.fda;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

public class MyOutputStream extends ByteArrayOutputStream {

	Logger log = Logger.getGlobal();

	@Override
	public synchronized void write(int b) {
		System.out.println("write");
		super.write(b);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) {
		System.out.println("write");
		super.write(b, off, len);
	}

	@Override
	public synchronized void writeTo(OutputStream out) throws IOException {
		System.out.println("writeTo");
		super.writeTo(out);
	}

	@Override
	public synchronized void reset() {
		System.out.println("reset");
		super.reset();
	}

	@Override
	public synchronized byte[] toByteArray() {
		System.out.println("toByteArray");
		return super.toByteArray();
	}

	@Override
	public synchronized int size() {
		System.out.println("size");
		return super.size();
	}

	@Override
	public synchronized String toString() {
		System.out.println("toString");
		return super.toString();
	}

	@Override
	public synchronized String toString(String charsetName)
			throws UnsupportedEncodingException {
		System.out.println("charsetName");
		return super.toString(charsetName);
	}

	@Override
	public synchronized String toString(int hibyte) {
		System.out.println("toString");
		return super.toString(hibyte);
	}

	@Override
	public void close() throws IOException {
		System.out.println("close");
		super.close();
	}

	@Override
	public void write(byte[] b) throws IOException {
		System.out.println("write");
		super.write(b);
	}

	@Override
	public void flush() throws IOException {
		System.out.println("flush");
		super.flush();
	}

	@Override
	public int hashCode() {
		
		System.out.println("hashCode");
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		System.out.println("equals");
		return super.equals(obj);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		System.out.println("clone");
		return super.clone();
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.println("finalize");
		super.finalize();
	}

}
