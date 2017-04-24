package semicontinuity.exp.compress.dictionary;

import org.junit.Assert;
import org.junit.Test;

public class BytesTest {

    @Test
    public void overlap() {
        // partial overlaps

        Assert.assertEquals(
                1,
                b(new byte[]{'b', 'c'}).overlapLength(b(new byte[]{'c', 'd', 'e'}))
        );
        Assert.assertEquals(
                2,
                b(new byte[]{'a', 'b', 'c', 'd'}).overlapLength(b(new byte[]{'c', 'd', 'e'}))
        );
        Assert.assertEquals(
                1,
                b(new byte[]{'a', 'b', 'c'}).overlapLength(b(new byte[]{'c', 'd', 'e'}))
        );
        Assert.assertEquals(
                2,
                b(new byte[]{'a', 'b', 'c'}).overlapLength(b(new byte[]{'b', 'c', 'd'}))
        );
        Assert.assertEquals(
                0,
                b(new byte[]{'a', 'b', 'c'}).overlapLength(b(new byte[]{'x', 'y', 'z'}))
        );

        // complete overlaps

        Assert.assertEquals(
                2,
                b(new byte[]{'a', 'b'}).overlapLength(b(new byte[]{'a', 'b', 'c'}))
        );
        Assert.assertEquals(
                2,
                b(new byte[]{'a', 'b', 'c'}).overlapLength(b(new byte[]{'b', 'c'}))
        );

        // no overlap
        Assert.assertEquals(
                0,
                b(new byte[]{'a', 'b', 'c', 'd'}).overlapLength(b(new byte[]{'b', 'c'}))
        );
    }

    @Test
    public void collapse() {
        CommonSuperStringFinder.Bytes first = b(new byte[]{'a', 'b', 'c', 'd'});
        CommonSuperStringFinder.Bytes second = b(new byte[]{'b', 'c', 'd', 'e'});
        CommonSuperStringFinder.Overlap overlap = new CommonSuperStringFinder.Overlap(3, first, second);
        Assert.assertEquals(
                b(new byte[]{'a', 'b', 'c', 'd', 'e'}),
                CommonSuperStringFinder.Bytes.collapse(overlap.first, overlap.second, overlap.value, 1)
        );
    }

    private static CommonSuperStringFinder.Bytes b(byte[] value) {
        return new CommonSuperStringFinder.Bytes(value, 1);
    }
}
