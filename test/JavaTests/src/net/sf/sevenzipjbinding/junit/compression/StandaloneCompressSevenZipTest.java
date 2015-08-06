package net.sf.sevenzipjbinding.junit.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IOutCreateArchive7z;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.junit.JUnitNativeTestBase;
import net.sf.sevenzipjbinding.junit.tools.CallbackTester;
import net.sf.sevenzipjbinding.junit.tools.VirtualContent;
import net.sf.sevenzipjbinding.junit.tools.VirtualContent.VirtualContentConfiguration;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

import org.junit.Test;


/**
 * Create 7-Zip archive using specific {@link IOutCreateCallback}&lt;{@link IOutItemCallback7z}&gt; interface.
 *
 * @author Boris Brodski
 * @version 9.13-2.00
 */
public class StandaloneCompressSevenZipTest extends JUnitNativeTestBase {
    private class OutCreateArchive7z implements IOutCreateCallback<IOutItem7z> {
        public void setTotal(long total) throws SevenZipException {
        }

        public void setCompleted(long complete) throws SevenZipException {
        }

        public void setOperationResult(boolean operationResultOk) {
            assertTrue(operationResultOk);
        }

        public IOutItem7z getItemInformation(int index, OutItemFactory<IOutItem7z> outItemFactory)
                throws SevenZipException {
            ByteArrayStream byteArrayStream = virtualContent.getItemStream(index);
            byteArrayStream.rewind();

            IOutItem7z outItem = outItemFactory.createOutItem();

            outItem.setDataStream(byteArrayStream);
            outItem.setPropertySize((long) byteArrayStream.getSize());
            outItem.setPropertyPath(virtualContent.getItemPath(index));

            return outItem;
        }

        public void freeResources(int index, IOutItem7z outItem) throws SevenZipException {
            try {
                outItem.getDataStream().close();
            } catch (IOException e) {
                throw new SevenZipException("Error closing data stream for item (index: " + index + ")");
            }
        }
    }

    static final Date DATE = new Date();

    VirtualContent virtualContent;
    CallbackTester<OutCreateArchive7z> callbackTesterCreateArchive = new CallbackTester<OutCreateArchive7z>(
            new OutCreateArchive7z());

    @Test
    public void testCompression7z() throws Exception {
        virtualContent = new VirtualContent(new VirtualContentConfiguration());
        virtualContent.fillRandomly(100, 3, 3, 100, 50, null);

        ByteArrayStream byteArrayStream = new ByteArrayStream(100000);

        IOutCreateArchive7z outNewArchive7z = closeLater(SevenZip.openOutArchive7z());

        outNewArchive7z.setLevel(5);
        outNewArchive7z.setSolid(true);
        outNewArchive7z.setSolidExtension(true);
        outNewArchive7z.setSolidFiles(3);
        outNewArchive7z.setSolidSize(1024);
        outNewArchive7z.setThreadCount(4);

        assertEquals(ArchiveFormat.SEVEN_ZIP, outNewArchive7z.getArchiveFormat());

        outNewArchive7z.createArchive(byteArrayStream, virtualContent.getItemCount(),
                callbackTesterCreateArchive.getProxyInstance());

        // No setCompleted call
        assertEquals(4, callbackTesterCreateArchive.getDifferentMethodsCalled());

        byteArrayStream.rewind();

        IInArchive inArchive = closeLater(SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, byteArrayStream));
        virtualContent.verifyInArchive(inArchive);
    }

    Date substructDate(Date date, int day) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -day);
        return calendar.getTime();
    }
}
