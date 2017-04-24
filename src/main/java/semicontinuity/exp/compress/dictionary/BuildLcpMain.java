package semicontinuity.exp.compress.dictionary;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import semicontinuity.exp.offheap.ByteArrayAsLongArrayAdapter;
import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;

import semicontinuity.exp.suffixarrays.SuffixArrays;

/**
 * Builds LCP array from data and suffix array.
 */
public class BuildLcpMain {
    private static final Logger logger = LogManager.getLogger(BuildRsaMain.class);

    public static void main(String[] args) {
        new BuildLcpMain().run(new File(args[0]));
    }

    private void run(File folder) {
        logger.info("Generating");
        ByteArrayAsLongArrayAdapter input = Helper.openBytes(new File(folder, "data"));
        LongArray sa = Helper.openLongs(new File(folder, "sa"));
        LongArray lcp = new OffheapByteArrayAsFiveByteLongArrayLsb(input.length());
        SuffixArrays.computeLCP(input, 0, input.length(), sa, lcp, OffheapByteArrayAsFiveByteLongArrayLsb::new);
        input.close();
        sa.close();

        logger.info("Saving");
        LongArray lcpPersistent = Helper.createLongs(new File(folder, "lcp"), input.length());
        Helper.copyLongs(lcp, lcpPersistent);
        lcp.close();
        lcpPersistent.close();
    }
}
