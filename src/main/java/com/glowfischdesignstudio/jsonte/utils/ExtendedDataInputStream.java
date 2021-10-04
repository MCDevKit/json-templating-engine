package com.glowfischdesignstudio.jsonte.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExtendedDataInputStream extends FilterInputStream implements DataInput {

    private final ByteBuffer littleEndianBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    private final boolean littleEndian;
    private final DataInputStream data;

    /**
     * Creates an ExtendedDataInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public ExtendedDataInputStream(InputStream in, boolean littleEndian) {
        super(in);
        data = new DataInputStream(in);
        this.littleEndian = littleEndian;
    }

    private ByteBuffer readLittleEndianBuffer(int len) throws IOException {
        littleEndianBuffer.clear();
        byte[] bytes = new byte[len];
        int i = read(bytes);
        if (i < len) {
            throw new EOFException();
        }
        littleEndianBuffer.put(bytes);
        littleEndianBuffer.rewind();
        return littleEndianBuffer;
    }

    private int readLittleEndianInt() throws IOException {
        return readLittleEndianBuffer(4).getInt();
    }

    private long readLittleEndianLong() throws IOException {
        return readLittleEndianBuffer(8).getLong();
    }

    private float readLittleEndianFloat() throws IOException {
        return readLittleEndianBuffer(4).getFloat();
    }

    private double readLittleEndianDouble() throws IOException {
        return readLittleEndianBuffer(8).getDouble();
    }

    private short readLittleEndianShort() throws IOException {
        return readLittleEndianBuffer(2).getShort();
    }

    private int readLittleEndianUShort() throws IOException {
        return readLittleEndianBuffer(2).getShort();
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        data.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        data.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return data.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return data.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return data.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return data.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return littleEndian ? readLittleEndianShort() : data.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return littleEndian ? readLittleEndianUShort() : data.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return data.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return littleEndian ? readLittleEndianInt() : data.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return littleEndian ? readLittleEndianLong() : data.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return littleEndian ? readLittleEndianFloat() : data.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return littleEndian ? readLittleEndianDouble() : data.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return data.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return data.readUTF();
    }
}
