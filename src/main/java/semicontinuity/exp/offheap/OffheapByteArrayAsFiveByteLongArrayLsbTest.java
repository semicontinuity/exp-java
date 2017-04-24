package semicontinuity.exp.offheap;

import org.junit.Assert;
import org.junit.Test;

public class OffheapByteArrayAsFiveByteLongArrayLsbTest {

    @Test
    public void setGetUpdate() {
        OffheapByteArrayAsLongArrayLsb a = new OffheapByteArrayAsLongArrayLsb(4);

        a.set(0, 0);
        Assert.assertEquals(0, a.get(0));

        a.set(1, 1);
        Assert.assertEquals(1, a.get(1));
        Assert.assertEquals(-1, a.update(1, -2));
        Assert.assertEquals(-1, a.get(1));

        a.set(2, -1);
        Assert.assertEquals(-1, a.get(2));
        Assert.assertEquals(1, a.update(2, 2));
        Assert.assertEquals(1, a.get(2));

        a.set(3, 0x40FFFFFFFFL);
        Assert.assertEquals(0x40FFFFFFFFL, a.get(3));
    }
}
