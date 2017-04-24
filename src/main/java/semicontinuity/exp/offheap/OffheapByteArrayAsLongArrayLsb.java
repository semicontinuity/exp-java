package semicontinuity.exp.offheap;

import java.io.Closeable;

public class OffheapByteArrayAsLongArrayLsb implements LongArray, Closeable {
    private final OffheapByteArray buffer;

    public OffheapByteArrayAsLongArrayLsb(OffheapByteArray buffer) {
        this.buffer = buffer;
    }

    public OffheapByteArrayAsLongArrayLsb(long size) {
        this.buffer = new OffheapByteArray(size * Long.BYTES);
        for (long i = 0; i < size; i++) {
             set(i, 0L);
        }
    }

    @Override
    public long get(long pos) {
        return UnsafeHelper.UNSAFE.getLong(buffer.address + pos * Long.BYTES);
    }

    @Override
    public void set(long pos, long value) {
        UnsafeHelper.UNSAFE.putLong(buffer.address + pos * Long.BYTES, value);
    }

    @Override
    public long length() {
        return buffer.size / Long.BYTES;
    }

    @Override
    public void close() {
        buffer.close();
    }
}
