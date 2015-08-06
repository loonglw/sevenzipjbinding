package net.sf.sevenzipjbinding.junit.compression;

import static org.junit.Assert.assertEquals;
import net.sf.sevenzipjbinding.IOutCreateArchive;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItemBase;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.junit.tools.CallbackTester;
import net.sf.sevenzipjbinding.junit.tools.RandomContext;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

/**
 * Tests compression and extraction of a single file interface.
 *
 * @author Boris Brodski
 * @version 9.13-2.0
 */
public abstract class CompressNonGenericSingleFileAbstractTest<T extends IOutItemBase> extends
        CompressSingleFileAbstractTest<T> {
    public abstract class SingleFileOutItemCallback extends SingleFileCreateArchiveCallback {

    }

    protected abstract SingleFileCreateArchiveCallback getSingleFileCreateArchiveCallback();

    protected abstract IOutCreateArchive<T> openOutArchive() throws SevenZipException;

    @SuppressWarnings("unchecked")
    @Override
    protected long doTest(int dataSize, int entropy) throws Exception {
        SingleFileCreateArchiveCallback createArchiveCallback = getSingleFileCreateArchiveCallback();

        TestContext testContext = testContextThreadContext.get();
        testContext.callbackTester = new CallbackTester<IOutCreateCallback<T>>(createArchiveCallback);
        testContext.randomContext = new RandomContext(dataSize, entropy);

        int maxStreamSize = dataSize + MINIMUM_STREAM_LENGTH;
        ByteArrayStream outputByteArrayStream = new ByteArrayStream(maxStreamSize);

        IOutCreateArchive<T> outArchive = openOutArchive();

        try {
            outArchive.createArchive(outputByteArrayStream, 1,
                    (IOutCreateCallback<T>) testContext.callbackTester.getProxyInstance());
        } finally {
            outArchive.close();
        }

        //        System.out.println("Length: " + dataSize + ", entropy: " + entropy + ": compressed size: "
        //                + outputByteArrayStream.getSize());

        verifyCompressedArchive(testContext.randomContext, outputByteArrayStream);
        if (dataSize > 100000) {
            assertEquals(IOutCreateCallback.class.getMethods().length,
                    testContext.callbackTester.getDifferentMethodsCalled());
        }

        return outputByteArrayStream.getSize();
    }


}
