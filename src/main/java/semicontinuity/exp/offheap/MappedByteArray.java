package semicontinuity.exp.offheap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

import sun.nio.ch.FileChannelImpl;

public class MappedByteArray extends OffheapByteArray {
    private final RandomAccessFile f;

    private static final Method map;
    private static final Method unmap;

    static {
        Class<FileChannelImpl> clazz = FileChannelImpl.class;
        try {
            map = clazz.getDeclaredMethod("map0", int.class, long.class, long.class);
            map.setAccessible(true);
            unmap = clazz.getDeclaredMethod("unmap0", long.class, long.class);
            unmap.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }


    public static MappedByteArray fromFile(File file, FileChannel.MapMode mode) {
        String rafMode;
        int mapMode;
        if (mode == FileChannel.MapMode.READ_ONLY) {
            rafMode = "r";
            mapMode = 0;
        } else if (mode == FileChannel.MapMode.READ_WRITE) {
            rafMode = "rw";
            mapMode = 1;
        } else {
            throw new IllegalArgumentException(String.valueOf(mode));
        }

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, rafMode);
            return new MappedByteArray(randomAccessFile, mapMode);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.valueOf(file));
        }
    }


    @SuppressWarnings("WeakerAccess")
    public MappedByteArray(RandomAccessFile f, int mapMode) {
        this(f, f.getChannel(), mapMode);
    }

    @SuppressWarnings("WeakerAccess")
    public MappedByteArray(RandomAccessFile f, FileChannel channel, int mapMode) {
        this(f, channel, channelSize(channel), mapMode);
    }


    private MappedByteArray(RandomAccessFile f, FileChannel channel, long size, int mapMode) {
        super(map(channel, size, mapMode), size);
        this.f = f;
    }

    private static long map(FileChannel channel, long size, int mapMode) {
        try {
            return (long) map.invoke(channel, mapMode, 0L, size);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long channelSize(FileChannel channel) {
        long size;
        try {
            size = channel.size();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (size == 0) {
            throw new IllegalArgumentException();
        }
        return size;
    }

    public void close() {
        try {
            unmap.invoke(null, address, size);
            f.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
