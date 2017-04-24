package semicontinuity.exp.offheap;


import org.junit.Assert;
import org.junit.Test;

public class ByteArrayAsLongArrayAdapterTest {

    @Test
    public void setGet() {
        ByteArrayAsLongArrayAdapter a = new ByteArrayAsLongArrayAdapter(4);

        a.set(0, 0);
        Assert.assertEquals(0, a.get(0));

        a.set(1, 1);
        Assert.assertEquals(1, a.get(1));

        a.set(2, 255);
        Assert.assertEquals(255, a.get(2));
    }
}
