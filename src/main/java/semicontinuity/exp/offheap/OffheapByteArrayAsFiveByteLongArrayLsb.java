package semicontinuity.exp.offheap;

import java.io.Closeable;

public class OffheapByteArrayAsFiveByteLongArrayLsb implements LongArray, Closeable {

    private static final int ITEM_SIZE = 5;
    private final OffheapByteArray buffer;

    @SuppressWarnings("WeakerAccess")
    public OffheapByteArrayAsFiveByteLongArrayLsb(OffheapByteArray buffer) {
        this.buffer = buffer;
    }

    @SuppressWarnings("WeakerAccess")
    public OffheapByteArrayAsFiveByteLongArrayLsb(long size) {
        this.buffer = new OffheapByteArray(size * ITEM_SIZE);
    }

    @Override
    public long get(long pos) {
        if (pos < 0 || pos >= length()) {
            throw new IllegalArgumentException(String.valueOf(pos));
        }
        int i = UnsafeHelper.UNSAFE.getInt(buffer.address + pos * ITEM_SIZE);
        byte b = UnsafeHelper.UNSAFE.getByte(buffer.address + pos * ITEM_SIZE + 4);

        long rl = 0x00000000FFFFFFFFL & ((long) i);
        long rh = ((long)b << 32) & 0xFFFFFFFF00000000L;
        return rh | rl;
    }

    @Override
    public void set(long pos, long value) {
        if (pos < 0 || pos >= length()) {
            throw new IllegalArgumentException(String.valueOf(pos) + ", length: " + length());
        }
//        if (value < 0) {
//            throw new IllegalArgumentException(String.valueOf(value));
//        }
        int i = (int) value;
        byte b = (byte) (value >> 32);

        UnsafeHelper.UNSAFE.putInt(buffer.address + pos * ITEM_SIZE, i);
        UnsafeHelper.UNSAFE.putByte(buffer.address + pos * ITEM_SIZE + 4, b);
    }

    @Override
    public long length() {
        return buffer.length() / ITEM_SIZE;
    }

    @Override
    public void close() {
        buffer.close();
    }
}
