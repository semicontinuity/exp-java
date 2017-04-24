package semicontinuity.exp.offheap;

public class OffheapByteArray implements ByteArray {
    final long address;
    final long size;

    @SuppressWarnings("WeakerAccess")
    public OffheapByteArray(long size) {
        this(UnsafeHelper.UNSAFE.allocateMemory(size), size);
    }

    @SuppressWarnings("WeakerAccess")
    public OffheapByteArray(long address, long size) {
        if (address == 0) {
            throw new IllegalArgumentException(String.valueOf(address));
        }
        this.address = address;
        this.size = size;
    }

    @Override
    public byte get(long index) {
        return UnsafeHelper.getByte(address + index);
    }

    @Override
    public void set(long index, byte value) {
        UnsafeHelper.setByte(address + index, value);
    }

    @Override
    public long length() {
        return size;
    }

    @Override
    public void close() {
        UnsafeHelper.UNSAFE.freeMemory(address);
    }
}
