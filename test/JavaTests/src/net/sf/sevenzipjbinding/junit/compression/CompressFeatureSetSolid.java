package net.sf.sevenzipjbinding.junit.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IOutCreateArchive;
import net.sf.sevenzipjbinding.IOutFeatureSetSolid;
import net.sf.sevenzipjbinding.IOutItemAllFormats;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.junit.tools.VirtualContent;
import net.sf.sevenzipjbinding.junit.tools.VirtualContent.FilenameGenerator;
import net.sf.sevenzipjbinding.junit.tools.VirtualContent.VirtualContentConfiguration;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

import org.junit.Test;

/**
 * Tests setting solid.
 *
 * @author Boris Brodski
 * @version 9.13-2.00
 */
public abstract class CompressFeatureSetSolid extends CompressFeatureAbstractMultpleFiles {
    private static final int DELTA_FILE_LENGTH = 100;
    private static final int AVERAGE_FILE_LENGTH = 100;
    private static final int MAX_SUBDIRECTORIES = 5;
    private static final int DIRECTORIES_DEPTH = 1;
    private static final int COUNT_OF_FILES = 30;

    public static class CompressionFeatureSetSolidSevenZip extends CompressFeatureSetSolid {
        @Override
        protected ArchiveFormat getArchiveFormat() {
            return ArchiveFormat.SEVEN_ZIP;
        }
    }

    private interface FeatureSetSolidTester {
        public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException;
    }

    private static ThreadLocal<VirtualContent> virtualContentThreadLocal = new ThreadLocal<VirtualContent>();

    public void initVirtualContentForThread() {
        FilenameGenerator filenameGenerator = new FilenameGenerator() {
            int count = 0;

            public String nextFilename() {
                count++;
                return "file" + count + ".ext" + (4 * count / (COUNT_OF_FILES + 1));
            }
        };

        VirtualContent virtualContent = new VirtualContent(new VirtualContentConfiguration());
        virtualContentThreadLocal.set(virtualContent);
        virtualContent.fillRandomly(COUNT_OF_FILES, DIRECTORIES_DEPTH, MAX_SUBDIRECTORIES, AVERAGE_FILE_LENGTH,
                DELTA_FILE_LENGTH, filenameGenerator);
    }

    @Test
    public void testCompressionFeatureSetSolid() throws Exception {
        testSingleOrMultithreaded(false, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolid();
            }
        });
    }

    @Test
    public void testCompressionFeatureSetSolidMultithreaded() throws Exception {
        testSingleOrMultithreaded(true, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolid();
            }
        });
    }

    private void doTestCompressionFeatureSetSolid() throws Exception {
        initVirtualContentForThread();

        int solidFalseSize2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolid(false);
            }
        });

        int solidTrueSize1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolid(true);
            }
        });

        int solidFalseSize1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolid(true);
                outArchive.setSolid(false);
            }
        });

        int solidTrueSize2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolid(false);
                outArchive.setSolid(true);
            }
        });

        int solidNullSize = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) {
            }
        });
        assertEquals(solidFalseSize1, solidFalseSize2);
        assertEquals(solidTrueSize1, solidTrueSize2);
        assertTrue(solidFalseSize1 > solidTrueSize1);
        assertEquals(solidTrueSize1, solidNullSize);
    }

    @Test
    public void testCompressionFeatureSetSolidFiles() throws Exception {
        testSingleOrMultithreaded(false, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidFiles();
            }
        });
    }

    @Test
    public void testCompressionFeatureSetSolidFilesMultithreaded() throws Exception {
        testSingleOrMultithreaded(true, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidFiles();
            }
        });
    }

    private void doTestCompressionFeatureSetSolidFiles() throws Exception {
        initVirtualContentForThread();

        int solidFilesSingle1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(1);
            }
        });
        int solidFilesSingle2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(COUNT_OF_FILES);
                outArchive.setSolidFiles(1);
            }
        });

        int solidFilesQuartal1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(COUNT_OF_FILES / 4);
            }
        });
        int solidFilesQuartal2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(1);
                outArchive.setSolidFiles(COUNT_OF_FILES / 4);
            }
        });

        int solidFilesFull1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(COUNT_OF_FILES);
            }
        });
        int solidFilesFull2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(1);
                outArchive.setSolidFiles(COUNT_OF_FILES);
            }
        });

        int solidFilesDefault1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) {
            }
        });
        int solidFilesDefault2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(COUNT_OF_FILES);
                outArchive.setSolidFiles(-1);
            }
        });

        assertEquals(solidFilesSingle1, solidFilesSingle2);
        assertEquals(solidFilesQuartal1, solidFilesQuartal2);
        assertEquals(solidFilesFull1, solidFilesFull2);
        assertEquals(solidFilesDefault1, solidFilesDefault2);

        assertEquals(solidFilesDefault1, solidFilesFull1);
        assertTrue(solidFilesSingle1 > solidFilesQuartal1);
        assertTrue(solidFilesQuartal1 > solidFilesFull1);
    }

    @Test
    public void testCompressionFeatureSetSolidSize() throws Exception {
        testSingleOrMultithreaded(false, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidSize();
            }
        });
    }

    @Test
    public void testCompressionFeatureSetSolidSizeMultithreaded() throws Exception {
        testSingleOrMultithreaded(true, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidSize();
            }
        });
    }

    private void doTestCompressionFeatureSetSolidSize() throws Exception {
        initVirtualContentForThread();

        final int averageSize = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(2 * COUNT_OF_FILES * AVERAGE_FILE_LENGTH * DELTA_FILE_LENGTH);
            }
        }) * 2;

        int solidSizeSingle1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(1);
            }
        });
        int solidSizeSingle2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(averageSize);
                outArchive.setSolidSize(1);
            }
        });

        int solidSizeQuartal1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(averageSize / 8);
            }
        });
        int solidSizeQuartal2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(1);
                outArchive.setSolidSize(averageSize / 8);
            }
        });

        int solidSizeFull1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(averageSize);
            }
        });
        int solidSizeFull2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(1);
                outArchive.setSolidSize(averageSize);
            }
        });

        int solidSizeDefault1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) {
            }
        });
        int solidSizeDefault2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidSize(averageSize);
                outArchive.setSolidSize(-1);
            }
        });

        assertEquals(solidSizeSingle1, solidSizeSingle2);
        assertEquals(solidSizeQuartal1, solidSizeQuartal2);
        assertEquals(solidSizeFull1, solidSizeFull2);
        assertEquals(solidSizeDefault1, solidSizeDefault2);

        assertTrue(solidSizeSingle1 > solidSizeQuartal1);
        assertTrue(solidSizeQuartal1 > solidSizeFull1);
    }

    @Test
    public void testCompressionFeatureSetSolidExtension() throws Exception {
        testSingleOrMultithreaded(false, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidExtension();
            }
        });
    }

    @Test
    public void testCompressionFeatureSetSolidExtensionMultithreaded() throws Exception {
        testSingleOrMultithreaded(true, new RunnableThrowsException() {
            public void run() throws Exception {
                doTestCompressionFeatureSetSolidExtension();
            }
        });
    }

    private void doTestCompressionFeatureSetSolidExtension() throws Exception {
        initVirtualContentForThread();

        int solidExtensionDefault = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) {
            }
        });
        int solidExtensionTrue1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidExtension(true);
            }
        });
        int solidExtensionTrue2 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidExtension(false);
                outArchive.setSolidExtension(true);
            }
        });
        int solidFile1 = testCompressionFeatureSetSolid(new FeatureSetSolidTester() {
            public void applyFeatures(IOutFeatureSetSolid outArchive) throws SevenZipException {
                outArchive.setSolidFiles(1);
            }
        });
        assertEquals(solidExtensionTrue1, solidExtensionTrue2);
        assertTrue(solidExtensionDefault < solidExtensionTrue1);
        assertTrue(solidExtensionTrue1 < solidFile1);
    }

    private int testCompressionFeatureSetSolid(FeatureSetSolidTester tester) throws Exception {
        ByteArrayStream outputByteArrayStream = new ByteArrayStream(2 * COUNT_OF_FILES * AVERAGE_FILE_LENGTH
                * DELTA_FILE_LENGTH);

        IOutCreateArchive<IOutItemAllFormats> outArchive = createArchive();

        assertTrue(outArchive instanceof IOutFeatureSetSolid);
        IOutFeatureSetSolid featureOutArchive = (IOutFeatureSetSolid) outArchive;

        tester.applyFeatures(featureOutArchive);

        VirtualContent virtualContent = virtualContentThreadLocal.get();
        virtualContent.createOutArchive(outArchive, outputByteArrayStream);
        closeArchive(outArchive);

        outputByteArrayStream.rewind();
        IInArchive inArchive = SevenZip.openInArchive(getArchiveFormat(), outputByteArrayStream);
        addCloseable(inArchive);
        virtualContent.verifyInArchive(inArchive);
        closeArchive(inArchive);

        // System.out.println(outputByteArrayStream.getSize());
        return outputByteArrayStream.getSize();
    }
}
