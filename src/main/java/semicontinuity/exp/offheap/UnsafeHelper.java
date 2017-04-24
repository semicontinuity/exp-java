package semicontinuity.exp.offheap;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeHelper {
    public static final Unsafe UNSAFE = getUnsafe();

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static byte getByte(long address) {
        return UnsafeHelper.UNSAFE.getByte(address);
    }

    public static void setByte(long address, byte b) {
        UnsafeHelper.UNSAFE.putByte(address, b);
    }
}
