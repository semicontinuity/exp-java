package semicontinuity.exp.offheap;

public class LongArrayView implements LongArray {
    private final LongArray underlying;
    private final long offset;

    public LongArrayView(LongArray underlying, long offset) {
        this.underlying = underlying;
        this.offset = offset;
    }

    @Override
    public long get(long pos) {
        return underlying.get(offset + pos);
    }

    @Override
    public void set(long pos, long value) {
        underlying.set(offset + pos, value);
    }

    @Override
    public long length() {
        return underlying.length() - offset;
    }

    @Override
    public void close() {
        // do nothing
    }
}
