package semicontinuity.exp.offheap;

/**
 * LongArray backed by OffheapByteArray.
 * Only values in the range [0-255] can be stored in this structure.
 */
public class ByteArrayAsLongArrayAdapter implements LongArray {
    private final ByteArray buffer;
    private final int extraZeroes;

    public ByteArrayAsLongArrayAdapter(long size) {
        this.buffer = new OffheapByteArray(size);
        this.extraZeroes = 0;
    }

    public ByteArrayAsLongArrayAdapter(OffheapByteArray buffer, int extraZeroes) {
        this.buffer = buffer;
        this.extraZeroes = extraZeroes;
    }

    @Override
    public long get(long pos) {
        if (pos >= buffer.length()) {
            return 0;
        } else {
            return (256 + buffer.get(pos)) & 0xFFL;   // convert to non-negative ints
        }
    }

    @Override
    public void set(long pos, long value) {
        buffer.set(pos, (byte) value);
    }

    @Override
    public long length() {
        return buffer.length() + extraZeroes;
    }

    @Override
    public void close() {
        buffer.close();
    }
}
