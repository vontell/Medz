package org.vontech.medz;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;

public class SplitFileInputStream extends InputStream {

    private String baseName;
    private String ext;
    private AssetManager am;
    private int numberOfChunks;
    private int currentChunk = 1;
    private InputStream currentIs = null;

    public SplitFileInputStream(String baseName, String ext, int numberOfChunks, AssetManager am) throws IOException {
        this.baseName = baseName;
        this.am = am;
        this.numberOfChunks = numberOfChunks;
        this.ext = ext;
        currentIs = am.open(baseName + currentChunk + ext, AssetManager.ACCESS_STREAMING);
    }

    @Override
    public int read() throws IOException {
        int read = currentIs.read();
        if (read == -1 && currentChunk < numberOfChunks) {
            currentIs.close();
            currentIs = am.open(baseName + ++currentChunk + ext, AssetManager.ACCESS_STREAMING);
            return read();
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        return currentIs.available();
    }

    @Override
    public void close() throws IOException {
        currentIs.close();
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int read = currentIs.read(b, offset, length);
        if (read < length && currentChunk < numberOfChunks) {
            currentIs.close();
            currentIs = am.open(baseName + ++currentChunk + ext, AssetManager.ACCESS_STREAMING);
            read += read(b, offset + read, length - read);
        }
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized void reset() throws IOException {
        if (currentChunk == 1) {
            currentIs.reset();
        } else {
            currentIs.close();
            currentIs = am.open(baseName + currentChunk + ext, AssetManager.ACCESS_STREAMING);
            currentChunk = 1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = currentIs.skip(n);
        if (skipped < n && currentChunk < numberOfChunks) {
            currentIs.close();
            currentIs = am.open(baseName + ++currentChunk + ext, AssetManager.ACCESS_STREAMING);
            skipped += skip(n - skipped);
        }
        return skipped;
    }
}
