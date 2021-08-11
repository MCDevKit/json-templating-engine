package com.stirante.json.utils;

import java.io.*;
import java.util.AbstractMap;

public class ImageUtils {

    private static final byte[] PNG_HEADER =
            {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
    private static final byte[] PNG_HEADER_ENDING =
            {(byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

    private static final byte[] JPG_EXIF_HEADER =
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1};
    private static final byte[] EXIF_CONSTANT =
            {(byte) 0x45, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x00, (byte) 0x00};

    private static final byte[] JPG_HEADER =
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] JFIF_CONSTANT =
            {(byte) 0x4A, (byte) 0x46, (byte) 0x49, (byte) 0x46, (byte) 0x00};

    public static Pair<Integer, Integer> getBounds(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            byte[] header = new byte[4];
            int read = in.read(header, 0, 4);
            if (read != 4) {
                throw new EOFException();
            }
            if (FileFormatUtils.compare(header, PNG_HEADER)) {
                return getPngBounds(in);
            }
            else if (FileFormatUtils.compare(header, JPG_HEADER)) {
                return getJpgBounds(in);
            }
            else if (FileFormatUtils.compare(header, JPG_EXIF_HEADER)) {
                return getJpgExifBounds(in);
            }
            else {
                throw new IllegalArgumentException("Unsupported image format!");
            }
        }
    }

    private static Pair<Integer, Integer> getPngBounds(DataInputStream in) throws IOException {
        byte[] header = new byte[4];
        int read = in.read(header, 0, 4);
        if (read != 4) {
            throw new EOFException();
        }
        if (!FileFormatUtils.compare(header, PNG_HEADER_ENDING)) {
            throw new IllegalArgumentException("File is not in a PNG format!");
        }
        in.skipBytes(4);
        read = in.read(header, 0, 4);
        if (read != 4) {
            throw new EOFException();
        }
        if (!new String(header).equals("IHDR")) {
            throw new IllegalArgumentException("File is not in a PNG format!");
        }
        int width = in.readInt();
        int height = in.readInt();
        return new Pair<>(width, height);
    }

    private static Pair<Integer, Integer> getJpgBounds(DataInputStream in) throws IOException {
        int id;
        do {
            int i = in.readUnsignedShort();
            in.skipBytes(i - 2);
            id = in.readUnsignedShort();
        } while (id != 0xFFC2 && id != 0xFFC0);
        in.skipBytes(3);
        int height = in.readUnsignedShort();
        int width = in.readUnsignedShort();
        return new Pair(width, height);
    }

    private static Pair<Integer, Integer> getJpgExifBounds(DataInputStream in) throws IOException {
        in.skipBytes(2);
        // Exif constant
        byte[] buffer = new byte[6];
        int read = in.read(buffer);
        if (read != 6) {
            throw new EOFException();
        }
        if (!FileFormatUtils.compare(buffer, EXIF_CONSTANT)) {
            throw new IllegalArgumentException("File is not in a JPG format!");
        }
        // Endianess
        ExtendedDataInputStream ein;
        char endianess = (char) in.read();
        in.skipBytes(1);
        if (endianess == 'M') {
            ein = new ExtendedDataInputStream(in, false);
        }
        else if (endianess == 'I') {
            ein = new ExtendedDataInputStream(in, true);
        }
        else {
            throw new IllegalArgumentException("File is not in a JPG format!");
        }
        // Check whether endianess is correct
        short i1 = ein.readShort();
        if (i1 != 0x002A) {
            throw new IllegalArgumentException("File is not in a JPG format!");
        }
        // Read offset to the first IFD
        int offset = ein.readInt();
        // Skip to the first IFD
        ein.skipBytes(offset - 8);
        // Skip first IFD
        int tagCount = ein.readShort();
        if (tagCount == 0) {
            in.skipBytes(8);
            return getJpgBounds(in);
        }
        int position = 10;
        int endOffset = -1;
        for (int i = 0; i < tagCount; i++) {
            AbstractMap.SimpleEntry<Integer, Integer> entry = readIFDEntry(ein);
            position += 12;
            if (entry.getKey() == 0x8769) {
                endOffset = entry.getValue();
            }
        }
        ein.skipBytes(endOffset - position);
        position = endOffset;
        // Read SubIFD
        int width = -1;
        int height = -1;
        tagCount = ein.readShort();
        for (int i = 0; i < tagCount; i++) {
            AbstractMap.SimpleEntry<Integer, Integer> entry = readIFDEntry(ein);
            position += 12;
            if (entry.getKey() == 0xa002) {
                width = entry.getValue();
            }
            else if (entry.getKey() == 0xa003) {
                height = entry.getValue();
            }
        }
        return new Pair(width, height);
    }

    private static AbstractMap.SimpleEntry<Integer, Integer> readIFDEntry(ExtendedDataInputStream ein) throws IOException {
        int id = ein.readUnsignedShort();
        short format = ein.readShort();
        ein.skipBytes(4);
//        int length = ein.readInt();
        int value;
        if (format == 3) {
            value = ein.readShort();
            ein.skipBytes(2);
        }
        else {
            value = ein.readInt();
        }
        return new AbstractMap.SimpleEntry<>(id, value);
    }

}
