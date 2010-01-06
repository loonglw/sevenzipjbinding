package net.sf.sevenzipjbinding.junit.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import net.sf.sevenzipjbinding.ISeekableStream;
import net.sf.sevenzipjbinding.util.ByteArrayStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class ByteArrayStreamTest {
    public static class ByteArrayStreamTestWithEmptyBuffer extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return new byte[0];
        }
    }

    public static class ByteArrayStreamTestWithBufferLength1 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "0".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength2 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "01".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength3 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "012".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength4 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "0123".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength5 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "01234".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength7 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "0123456".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength8 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "01234567".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength9 extends ByteArrayStreamTest {
        @Override
        protected byte[] getTestBuffer() {
            return "012345678".getBytes();
        }
    }

    public static class ByteArrayStreamTestWithBufferLength100 extends ByteArrayStreamTest {
        private byte[] buffers = new byte[100];
        {
            random.nextBytes(buffers);
        }

        @Override
        protected byte[] getTestBuffer() {
            return buffers;
        }
    }

    private static final int MAX_SIZE = 8 * 1024 * 1024;
    private static Random random = new Random(0);

    private static Field chunkListField;
    private static Field currentPositionField;
    private static Field currentPositionInChunkField;
    private static Field currentChunkIndexField;
    private static Field sizeField;

    private byte[] testBuffer;
    private ByteArrayStream byteArrayStream;

    @Before
    public void init() throws Exception {
        testBuffer = getTestBuffer();
        byteArrayStream = new ByteArrayStream(4, MAX_SIZE);
        checkInvariant();
    }

    @Test
    public void testSimpleWrite() throws Exception {
        write(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testSimpleWriteAfterTruncateEmpty() throws Exception {
        truncate();
        write(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testSimpleWriteAfterTruncateNonEmpty() throws Exception {
        write("ABCDEF".getBytes());
        truncate();
        write(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite1ByteFirst() throws Exception {
        write("A".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("A".getBytes(), testBuffer);
    }

    @Test
    public void testWrite2ByteFirst() throws Exception {
        write("AB".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("AB".getBytes(), testBuffer);
    }

    @Test
    public void testWrite3ByteFirst() throws Exception {
        write("ABC".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABC".getBytes(), testBuffer);
    }

    @Test
    public void testWrite4ByteFirst() throws Exception {
        write("ABCD".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABCD".getBytes(), testBuffer);
    }

    @Test
    public void testWrite5ByteFirst() throws Exception {
        write("ABCDE".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABCDE".getBytes(), testBuffer);
    }

    @Test
    public void testWrite7ByteFirst() throws Exception {
        write("ABCDEFG".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABCDEFG".getBytes(), testBuffer);
    }

    @Test
    public void testWrite8ByteFirst() throws Exception {
        write("ABCDEFGH".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABCDEFGH".getBytes(), testBuffer);
    }

    @Test
    public void testWrite9ByteFirst() throws Exception {
        write("ABCDEFGHI".getBytes());
        write(testBuffer);
        compareWithExpectedBuffer("ABCDEFGHI".getBytes(), testBuffer);
    }

    @Test
    public void testWrite50ByteFirst() throws Exception {
        byte[] firstBytes = new byte[50];
        random.nextBytes(firstBytes);
        write(firstBytes);
        write(testBuffer);
        compareWithExpectedBuffer(firstBytes, testBuffer);
    }

    // ----------------------------------------------------------------
    @Test
    public void testWrite1SetBytes() throws Exception {
        setBytes("A".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite2SetBytes() throws Exception {
        setBytes("AB".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite3SetBytes() throws Exception {
        setBytes("ABC".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite4SetBytes() throws Exception {
        setBytes("ABCD".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite5SetBytes() throws Exception {
        setBytes("ABCDE".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite7SetBytes() throws Exception {
        setBytes("ABCDEFG".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite8SetBytes() throws Exception {
        setBytes("ABCDEFGH".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite9SetBytes() throws Exception {
        setBytes("ABCDEFGHI".getBytes());
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWrite50SetBytes() throws Exception {
        byte[] firstBytes = new byte[50];
        random.nextBytes(firstBytes);
        setBytes(firstBytes);
        write(testBuffer);
        truncateToCurrentPosition();
        compareWithExpectedBuffer(testBuffer);
    }

    // ----------------------------------------------------------------

    @Test
    public void testSimpleWriteFromInputStream() throws Exception {
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testSimpleWriteFromInputStreamAfterTruncateEmpty() throws Exception {
        truncate();
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testSimpleWriteFromInputStreamAfterTruncateNonEmpty() throws Exception {
        writeFromInputStream("ABCDEF".getBytes());
        truncate();
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer(testBuffer);
    }

    @Test
    public void testWriteFromInputStream1ByteFirst() throws Exception {
        writeFromInputStream("A".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("A".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream2ByteFirst() throws Exception {
        writeFromInputStream("AB".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("AB".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream3ByteFirst() throws Exception {
        writeFromInputStream("ABC".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABC".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream4ByteFirst() throws Exception {
        writeFromInputStream("ABCD".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABCD".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream5ByteFirst() throws Exception {
        writeFromInputStream("ABCDE".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABCDE".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream7ByteFirst() throws Exception {
        writeFromInputStream("ABCDEFG".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABCDEFG".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream8ByteFirst() throws Exception {
        writeFromInputStream("ABCDEFGH".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABCDEFGH".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream9ByteFirst() throws Exception {
        writeFromInputStream("ABCDEFGHI".getBytes());
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer("ABCDEFGHI".getBytes(), testBuffer);
    }

    @Test
    public void testWriteFromInputStream50ByteFirst() throws Exception {
        byte[] firstBytes = new byte[50];
        random.nextBytes(firstBytes);
        writeFromInputStream(firstBytes);
        writeFromInputStream(testBuffer);
        compareWithExpectedBuffer(firstBytes, testBuffer);
    }

    @Test
    public void testSetSizeEmptyArray() throws Exception {
        write(testBuffer);
        setSize(0);
        compareWithExpectedBuffer(); // Expect empty byte array stream
    }

    @Test
    public void testSetSize1ByteArray() throws Exception {
        write(testBuffer);
        setSize(1);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 1));
    }

    @Test
    public void testSetSize2ByteArray() throws Exception {
        write(testBuffer);
        setSize(2);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 2));
    }

    @Test
    public void testSetSize3ByteArray() throws Exception {
        write(testBuffer);
        setSize(3);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 3));
    }

    @Test
    public void testSetSize4ByteArray() throws Exception {
        write(testBuffer);
        setSize(4);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 4));
    }

    @Test
    public void testSetSize5ByteArray() throws Exception {
        write(testBuffer);
        setSize(5);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 5));
    }

    @Test
    public void testSetSize7ByteArray() throws Exception {
        write(testBuffer);
        setSize(7);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 7));
    }

    @Test
    public void testSetSize8ByteArray() throws Exception {
        write(testBuffer);
        setSize(8);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 8));
    }

    @Test
    public void testSetSize9ByteArray() throws Exception {
        write(testBuffer);
        setSize(9);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 9));
    }

    @Test
    public void testSetSize200ByteArray() throws Exception {
        write(testBuffer);
        setSize(200);
        compareWithExpectedBuffer(subarray(testBuffer, 0, 200));
    }

    @Test
    public void testSeekTo0() throws Exception {
        testSeekTo(0);
    }

    @Test
    public void testSeekTo1() throws Exception {
        testSeekTo(1);
    }

    @Test
    public void testSeekTo2() throws Exception {
        testSeekTo(2);
    }

    @Test
    public void testSeekTo3() throws Exception {
        testSeekTo(3);
    }

    @Test
    public void testSeekTo4() throws Exception {
        testSeekTo(4);
    }

    @Test
    public void testSeekTo5() throws Exception {
        testSeekTo(5);
    }

    @Test
    public void testSeekTo7() throws Exception {
        testSeekTo(7);
    }

    @Test
    public void testSeekTo8() throws Exception {
        testSeekTo(8);
    }

    @Test
    public void testSeekTo9() throws Exception {
        testSeekTo(9);
    }

    @Test
    public void testSeekTo50() throws Exception {
        testSeekTo(50);
    }

    private void testSeekTo(int pos) throws Exception {
        write(testBuffer);
        if (testBuffer.length > 2) {
            for (int i = byteArrayStream.getCurrentPosition(); i != -1; i--) {
                seek(i);
            }

            assertEquals(Integer.valueOf(1), Integer.valueOf(byteArrayStream.read(new byte[1])));
        }
        seek(pos);
        write("x".getBytes());
        if (testBuffer.length <= pos) {
            byte[] newTestBuffer = new byte[pos + 1];
            if (testBuffer.length != 0) {
                System.arraycopy(testBuffer, 0, newTestBuffer, 0, testBuffer.length);
            }
            testBuffer = newTestBuffer;
        }
        testBuffer[pos] = 'x';
        compareWithExpectedBuffer(testBuffer);
    }

    private byte[] subarray(byte[] data, int startPosition, int length) {
        byte[] array = new byte[length];
        int lengthToCopy = data.length < length ? data.length : length;
        System.arraycopy(data, startPosition, array, 0, lengthToCopy);
        return array;
    }

    protected abstract byte[] getTestBuffer();

    private void write(byte[] buffer) throws Exception {
        int currentPosition = byteArrayStream.getCurrentPosition();
        int wrote = byteArrayStream.write(buffer);
        assertEquals(Integer.valueOf(buffer.length), Integer.valueOf(wrote));
        checkInvariant();
        assertEquals("Current position in the stream is incorrect. Initial position: " + currentPosition
                + ", buffer length: " + buffer.length, Integer.valueOf(currentPosition + buffer.length), Integer
                .valueOf(byteArrayStream.getCurrentPosition()));
    }

    private void setBytes(byte[] buffer) throws Exception {
        byteArrayStream.setBytes(buffer, true);
        checkInvariant();
    }

    private void truncate() throws Exception {
        byteArrayStream.truncate();
        checkInvariant();
    }

    private void truncateToCurrentPosition() throws Exception {
        byteArrayStream.setSize(byteArrayStream.getCurrentPosition());
        checkInvariant();
    }

    private void setSize(int newSize) throws Exception {
        byteArrayStream.setSize(newSize);
        checkInvariant();
    }

    private void seek(int position) throws Exception {
        long newPosition = byteArrayStream.seek(position, ISeekableStream.SEEK_SET);
        assertEquals(position, (int) newPosition);
        assertEquals(position, byteArrayStream.getCurrentPosition());
        checkInvariant();
    }

    private void writeFromInputStream(byte[] buffer) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
        byteArrayStream.writeFromInputStream(byteArrayInputStream, false);
        checkInvariant();
    }

    private void compareWithExpectedBuffer(byte[]... buffers) throws Exception {
        ByteArrayOutputStream expectedByteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] buffer : buffers) {
            expectedByteArrayOutputStream.write(buffer);
        }
        byte[] expected = expectedByteArrayOutputStream.toByteArray();
        checkInvariant();
        assertArrayEquals(expected, byteArrayStream.getBytes());
        checkInvariant();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayStream.writeToOutputStream(byteArrayOutputStream, true);
        checkInvariant();
        assertArrayEquals(expected, byteArrayOutputStream.toByteArray());
    }

    @BeforeClass
    public static void initByteArrayStreamPrivateMethods() throws Exception {
        chunkListField = ByteArrayStream.class.getDeclaredField("chunkList");
        currentPositionField = ByteArrayStream.class.getDeclaredField("currentPosition");
        currentPositionInChunkField = ByteArrayStream.class.getDeclaredField("currentPositionInChunk");
        currentChunkIndexField = ByteArrayStream.class.getDeclaredField("currentChunkIndex");
        sizeField = ByteArrayStream.class.getDeclaredField("size");

        chunkListField.setAccessible(true);
        currentPositionField.setAccessible(true);
        currentPositionInChunkField.setAccessible(true);
        currentChunkIndexField.setAccessible(true);
        sizeField.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private void checkInvariant() throws Exception {
        List<byte[]> chunkList = (List<byte[]>) chunkListField.get(byteArrayStream);
        int currentPosition = currentPositionField.getInt(byteArrayStream);
        int currentPositionInChunk = currentPositionInChunkField.getInt(byteArrayStream);
        int currentChunkIndex = currentChunkIndexField.getInt(byteArrayStream);
        int size = sizeField.getInt(byteArrayStream);

        if (chunkList.size() == 0) {
            assertTrue(currentPosition == 0);
            assertTrue(currentPositionInChunk == 0);
            assertTrue(currentChunkIndex == -1);
            assertTrue(size == 0);
            return;
        }

        assertTrue(currentPositionInChunk >= 0);
        assertTrue(currentPosition >= 0);
        assertTrue(currentChunkIndex >= 0);
        assertTrue(size >= 0);

        int sizeBeforeLastChunk = 0;
        int sizeBeforeCurrentChunk = 0;
        for (int i = 0; i < chunkList.size() - 1; i++) {
            if (i < currentChunkIndex) {
                sizeBeforeCurrentChunk += chunkList.get(i).length;
            }
            if (i == currentChunkIndex) {
                if (currentChunkIndex < chunkList.size() - 1) {
                    assertTrue(currentPositionInChunk < chunkList.get(i).length);
                } else {
                    assertTrue(currentPositionInChunk <= chunkList.get(i).length);
                }
            }
            sizeBeforeLastChunk += chunkList.get(i).length;
        }
        assertTrue((sizeBeforeCurrentChunk + currentPositionInChunk) == currentPosition);
        assertTrue(sizeBeforeLastChunk <= size);
        assertTrue(sizeBeforeLastChunk + chunkList.get(chunkList.size() - 1).length >= size);
    }
}