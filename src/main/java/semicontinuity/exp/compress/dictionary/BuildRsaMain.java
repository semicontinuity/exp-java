package semicontinuity.exp.compress.dictionary;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import semicontinuity.exp.offheap.LongArray;
import semicontinuity.exp.offheap.OffheapByteArrayAsFiveByteLongArrayLsb;

/**
 * Builds reverse suffix array for the given suffix array file.
 */
public class BuildRsaMain {
    private static final Logger logger = LogManager.getLogger(BuildRsaMain.class);

    public static void main(String[] args) {
        new BuildRsaMain().run(new File(args[0]));
    }

    private void run(File folder) {
        logger.info("Generating");
        LongArray sa = Helper.openLongs(new File(folder, "sa"));
        LongArray rsa = new OffheapByteArrayAsFiveByteLongArrayLsb(sa.length());

        for (long i = 0; i < sa.length(); i++) {
            rsa.set(sa.get(i), i);
        }
        sa.close();

        logger.info("Saving");
        LongArray rsaPersistent = Helper.createLongs(new File(folder, "rsa"), sa.length());
        Helper.copyLongs(rsa, rsaPersistent);
        rsa.close();
        rsaPersistent.close();
    }
}
