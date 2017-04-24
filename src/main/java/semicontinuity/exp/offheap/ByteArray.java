package semicontinuity.exp.offheap;

import java.io.Closeable;

public interface ByteArray extends Closeable {
    byte get(long pos);
    void set(long pos, byte value);
    long length();

    @Override
    void close();
}
