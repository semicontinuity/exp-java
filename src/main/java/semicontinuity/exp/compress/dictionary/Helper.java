package semicontinuity.exp.compress.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import semicontinuity.exp.offheap.ByteArrayAsLongArrayAdapter;
import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.MappedByteArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;
import semicontinuity.exp.offheap.OffheapByteArrayAsLongArrayLsb;

class Helper {
    static void copyLongs(LongArray from, LongArray to) {
        for (long i = 0; i < from.length(); i++) {
            to.set(i, from.get(i));
        }
    }

    static ByteArrayAsLongArrayAdapter openBytes(File dataFile) {
        return new ByteArrayAsLongArrayAdapter(mapped(dataFile, FileChannel.MapMode.READ_ONLY), 0);
    }


    static OffheapByteArrayAsFiveByteLongArrayLsb openFiveByteLongs(File file) {
        return new OffheapByteArrayAsFiveByteLongArrayLsb(mapped(file, FileChannel.MapMode.READ_ONLY));
    }

    static OffheapByteArrayAsLongArrayLsb openLongs(File file) {
        return new OffheapByteArrayAsLongArrayLsb(mapped(file, FileChannel.MapMode.READ_ONLY));
    }


    static OffheapByteArrayAsFiveByteLongArrayLsb createFiveByteLongs(File file, long size) {
        createLongsFile(file, size, 5L);
        return new OffheapByteArrayAsFiveByteLongArrayLsb(mapped(file, FileChannel.MapMode.READ_WRITE));
    }

    static OffheapByteArrayAsLongArrayLsb createLongs(File file, long size) {
        createLongsFile(file, size, 8L);
        return new OffheapByteArrayAsLongArrayLsb(mapped(file, FileChannel.MapMode.READ_WRITE));
    }

    private static MappedByteArray mapped(File file, FileChannel.MapMode readWrite) {
        return MappedByteArray.fromFile(file, readWrite);
    }

    private static void createLongsFile(File file, long size, long unitLength) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            f.setLength(unitLength * size);
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
