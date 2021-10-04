package com.glowfischdesignstudio.jsonte.utils;

import java.io.*;

public class AudioUtils {
    private static final byte[] RIFF_HEADER =
            {(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46};
    private static final byte[] WAVE_HEADER =
            {(byte) 0x57, (byte) 0x41, (byte) 0x56, (byte) 0x45};
    private static final byte[] DATA_HEADER =
            {(byte) 0x64, (byte) 0x61, (byte) 0x74, (byte) 0x61};
    private static final byte[] FMT_HEADER =
            {(byte) 0x66, (byte) 0x6D, (byte) 0x74};

    public static AudioInfo getAudioInfo(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            byte[] header = new byte[4];
            int read = in.read(header, 0, 4);
            if (read != 4) {
                throw new EOFException();
            }
            if (FileFormatUtils.compare(header, RIFF_HEADER)) {
                return getRiffInfo(in);
            }
            else {
                throw new IllegalArgumentException("Unsupported audio format!");
            }
        }
    }

    private static AudioInfo getRiffInfo(DataInputStream stream) throws IOException {
        ExtendedDataInputStream in = new ExtendedDataInputStream(stream, true);
        // Skip file length
        in.skipBytes(4);
        if (!FileFormatUtils.compare(in, WAVE_HEADER)) {
            throw new IllegalArgumentException("Unsupported RIFF file!");
        }
        if (!FileFormatUtils.compare(in, FMT_HEADER)) {
            throw new IllegalArgumentException("No fmt data!");
        }
        // Skip trailing space/null character and chunk length, since it's always 16
        in.skipBytes(5);
        int formatType = in.readShort();
        if (formatType != 1) {
            throw new IllegalArgumentException("Unsupported wav format!");
        }
        int channels = in.readShort();
        int sampleRate = in.readInt();
        int bytesPerSecond = in.readInt();
        // Skip "type" field, since it can be deduced from values
        // Following value is (BitsPerSample * Channels) / 8
        // 1 - 8 bit mono
        // 2 - 8 bit stereo/16 bit mono
        // 4 - 16 bit stereo
        in.skipBytes(2);
        int bitsPerSample = in.readShort();
        if (!FileFormatUtils.compare(in, DATA_HEADER)) {
            throw new IllegalArgumentException("No data chunk!");
        }
        int length = in.readInt();
        double duration = length / (double) bytesPerSecond;
        return new AudioInfo(channels, sampleRate, bitsPerSample, duration);
    }


    public static class AudioInfo {
        public int channels;
        public int sampleRate;
        public int bitsPerSample;
        public double duration;

        public AudioInfo(int channels, int sampleRate, int bitsPerSample, double duration) {
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.bitsPerSample = bitsPerSample;
            this.duration = duration;
        }
    }

}
