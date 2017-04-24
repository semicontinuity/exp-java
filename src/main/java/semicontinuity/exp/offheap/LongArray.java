package semicontinuity.exp.offheap;

import java.io.Closeable;

public interface LongArray extends Closeable {
    long get(long pos);
    void set(long pos, long value);
    long length();

    @Override
    void close();

    default long update(long pos, long value) {
        long newValue = get(pos) + value;
        set(pos, newValue);
        return newValue;
    }
}
