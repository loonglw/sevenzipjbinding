package net.sf.sevenzipjbinding.junit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

public class ZipContentComparator {
	private class UniversalFileEntryInfo implements Comparable<UniversalFileEntryInfo> {
		public int itemId;
		public String itemIdString;
		public String filename;
		public long realSize;
		public Date fileLastModificationTime;

		public int compareTo(UniversalFileEntryInfo o) {
			return filename.compareTo(o.filename);
		}

	}

	private final ZipFile expectedZipFile;
	private Enumeration<? extends ZipEntry> expectedZipEntries;

	private final ISevenZipInArchive actualSevenZipArchive;
	List<String> errorMessages = null;
	private final boolean useSimpleInterface;
	private final String password;
	private final ArchiveFormat archiveFormat;

	public ZipContentComparator(ArchiveFormat archiveFormat, ISevenZipInArchive sevenZipArchive, ZipFile zipFile,
			boolean useSimpleInterface, String password) {
		this.archiveFormat = archiveFormat;
		this.actualSevenZipArchive = sevenZipArchive;
		this.expectedZipFile = zipFile;
		this.useSimpleInterface = useSimpleInterface;
		this.password = password;
		expectedZipEntries = zipFile.entries();
	}

	private void compare() {
		try {
			errorMessages = new ArrayList<String>();
			List<UniversalFileEntryInfo> expectedFileNames;
			List<UniversalFileEntryInfo> actualFileNames;

			expectedFileNames = getExpectedFileNameList();
			actualFileNames = getActualFileNameList();

			Collections.sort(expectedFileNames);
			Collections.sort(actualFileNames);

			if (expectedFileNames.size() != actualFileNames.size()) {
				errorMessages.add("Difference in count of files in archive: expected " + expectedFileNames.size()
						+ ", found " + actualFileNames.size());
				return;
			}

			for (int i = 0; i < actualFileNames.size(); i++) {
				UniversalFileEntryInfo actualInfo = actualFileNames.get(i);
				UniversalFileEntryInfo expectedInfo = expectedFileNames.get(i);

				String actualFilename = actualInfo.filename;
				if (archiveFormat == ArchiveFormat.TAR && actualFilename.startsWith("./")) {
					actualFilename = actualFilename.substring(2);
				}

				String expectedFilename = expectedInfo.filename;
				//				if (archiveFormat == ArchiveFormat.ISO) {
				//					expectedFilename = convertToISOFilename(expectedFilename);
				//				}
				if (!actualFilename.equalsIgnoreCase(expectedFilename)) {
					error("Filename missmatch: expected '" + expectedFilename + "', actual '" + actualFilename + "'");
				}

				if (actualInfo.realSize != expectedInfo.realSize) {
					error("Real file size missmatch for file '" + expectedFilename + "': expected "
							+ expectedInfo.realSize + ", actual " + actualInfo.realSize);
				}

				// TODO
				// if (!actualInfo.fileLastModificationTime
				// .equals(expectedInfo.fileLastModificationTime)) {
				// error("Last modification time missmatch for file '"
				// + expectedInfo.filename + "': expected "
				// + expectedInfo.fileLastModificationTime
				// + ", actual " + actualInfo.fileLastModificationTime);
				// }
			}

			String[] itemIdToItemName = new String[actualSevenZipArchive.getNumberOfItems()];

			for (int i = 0; i < actualFileNames.size(); i++) {
				UniversalFileEntryInfo actualInfo = actualFileNames.get(i);
				UniversalFileEntryInfo expectedInfo = expectedFileNames.get(i);

				itemIdToItemName[actualInfo.itemId] = expectedInfo.itemIdString;
			}

			long start = System.currentTimeMillis();
			IArchiveExtractCallback archiveExtractCallback;

			if (password == null) {
				archiveExtractCallback = new TestArchiveExtractCallback(itemIdToItemName);
			} else {
				archiveExtractCallback = new TestArchiveExtractCryptoCallback(itemIdToItemName);
			}

			int[] indices = new int[actualFileNames.size()];
			for (int i = 0; i < actualFileNames.size(); i++) {
				indices[i] = actualFileNames.get(i).itemId;
			}

			if (useSimpleInterface) {
				ISimpleInArchiveItem[] simpleInArchiveItems = actualSevenZipArchive.getSimpleInterface()
						.getArchiveItems();
				for (int index = 0; index < indices.length; index++) {
					if (index > 20) {
						break; // Test only first 20 indices. extractSlow() is slow indeed.
					}
					ISimpleInArchiveItem archiveItems = simpleInArchiveItems[indices[index]];
					ExtractOperationResult operationResult;
					if (password == null) {
						operationResult = archiveItems.extractSlow(archiveExtractCallback.getStream(indices[index],
								ExtractAskMode.EXTRACT));
					} else {
						operationResult = archiveItems.extractSlow(archiveExtractCallback.getStream(indices[index],
								ExtractAskMode.EXTRACT), password);
					}
					if (operationResult != ExtractOperationResult.OK) {
						error("Extract: operation result wasn't ok: " + operationResult);
					}
				}
			} else {
				actualSevenZipArchive.extract(indices, false, archiveExtractCallback);
			}
			System.out.println("Extraction in " + (System.currentTimeMillis() - start) / 1000.0 + " sec ("
					+ actualFileNames.size() + " items)");
		} catch (Exception e) {
			error("Exception occurs: " + e);
		}
	}

	//	private String convertToISOFilename(String expectedFilename) {
	//		if (true) {
	//			return expectedFilename;
	//		}
	//
	//		int nameCounter = 0;
	//		int extCounter = 0;
	//		boolean isInExtension = false;
	//		StringBuilder stringBuilder = new StringBuilder(expectedFilename.length());
	//		forloop: for (int i = 0, n = expectedFilename.length(); i < n; i++) {
	//			char c = expectedFilename.charAt(i);
	//			switch (c) {
	//			case '/':
	//				nameCounter = 0;
	//				isInExtension = false;
	//				break;
	//			case '.':
	//				extCounter = 0;
	//				isInExtension = true;
	//				break;
	//			default:
	//				if (isInExtension) {
	//					if (extCounter < 3) {
	//						extCounter++;
	//					} else {
	//						continue forloop;
	//					}
	//				} else {
	//					if (nameCounter < 8) {
	//						nameCounter++;
	//					} else {
	//						continue forloop;
	//					}
	//				}
	//
	//			}
	//			stringBuilder.append(c);
	//		}
	//		return stringBuilder.toString();
	//	}

	private void error(String message) {
		if (errorMessages.size() > 10) {
			return;
		}
		if (errorMessages.size() == 10) {
			errorMessages.add("...");
			return;
		}
		errorMessages.add(message);
	}

	private String getStringProperty(ISevenZipInArchive sevenZip, int index, PropID propID) throws SevenZipException {
		String string = (String) sevenZip.getProperty(index, propID);
		if (string == null) {
			return "";
		}
		return string;
	}

	private List<UniversalFileEntryInfo> getActualFileNameList() throws SevenZipException {
		List<UniversalFileEntryInfo> fileNames = new ArrayList<UniversalFileEntryInfo>();

		if (useSimpleInterface) {
			ISimpleInArchive simpleInArchive = actualSevenZipArchive.getSimpleInterface();

			ISimpleInArchiveItem[] archiveItems = simpleInArchive.getArchiveItems();

			for (ISimpleInArchiveItem simpleInArchiveItem : archiveItems) {
				if (simpleInArchiveItem.isFolder()) {
					continue;
				}

				UniversalFileEntryInfo info = new UniversalFileEntryInfo();
				info.filename = simpleInArchiveItem.getPath();
				info.filename = info.filename.replace('\\', '/');
				info.itemId = simpleInArchiveItem.getItemIndex();
				info.realSize = simpleInArchiveItem.getSize();
				info.fileLastModificationTime = simpleInArchiveItem.getLastWriteTime();
				fileNames.add(info);
			}
		} else {
			for (int i = 0; i < actualSevenZipArchive.getNumberOfItems(); i++) {
				if (((Boolean) actualSevenZipArchive.getProperty(i, PropID.IS_FOLDER)).booleanValue()) {
					continue;
				}
				UniversalFileEntryInfo info = new UniversalFileEntryInfo();
				info.filename = getStringProperty(actualSevenZipArchive, i, PropID.PATH)
						+ getStringProperty(actualSevenZipArchive, i, PropID.NAME)
						+ getStringProperty(actualSevenZipArchive, i, PropID.EXTENSION);
				info.filename = info.filename.replace('\\', '/');
				info.itemId = i;
				info.realSize = ((Long) actualSevenZipArchive.getProperty(i, PropID.SIZE)).longValue();
				info.fileLastModificationTime = (Date) actualSevenZipArchive.getProperty(i, PropID.LAST_WRITE_TIME);
				fileNames.add(info);
			}
		}
		return fileNames;
	}

	private List<UniversalFileEntryInfo> getExpectedFileNameList() {
		List<UniversalFileEntryInfo> fileNames = new ArrayList<UniversalFileEntryInfo>();

		while (expectedZipEntries.hasMoreElements()) {
			ZipEntry zipEntry = expectedZipEntries.nextElement();

			if (zipEntry.isDirectory()) {
				continue;
			}
			UniversalFileEntryInfo info = new UniversalFileEntryInfo();

			info.itemIdString = zipEntry.getName();
			info.filename = info.itemIdString.replace('\\', '/');
			info.realSize = zipEntry.getSize();
			if (zipEntry.getTime() != -1) {
				info.fileLastModificationTime = new Date(zipEntry.getTime());
			}
			fileNames.add(info);
		}

		return fileNames;
	}

	public boolean isEqual() {
		if (errorMessages == null) {
			compare();
		}
		return errorMessages.size() == 0;
	}

	public String getErrorMessage() {
		if (errorMessages == null) {
			compare();
		}

		StringBuffer stringBuffer = new StringBuffer();

		for (String errrorMessage : errorMessages) {
			if (stringBuffer.length() > 0) {
				stringBuffer.append('\n');
			}
			stringBuffer.append(errrorMessage);
		}

		return stringBuffer.toString();
	}

	public void print() throws SevenZipException {
		List<UniversalFileEntryInfo> expectedFileNames;
		List<UniversalFileEntryInfo> actualFileNames;

		expectedFileNames = getExpectedFileNameList();
		actualFileNames = getActualFileNameList();

		Collections.sort(expectedFileNames);
		Collections.sort(actualFileNames);

		for (int i = 0; i < Math.max(expectedFileNames.size(), actualFileNames.size()); i++) {
			if (i < expectedFileNames.size()) {
				UniversalFileEntryInfo universalFileEntryInfo1 = expectedFileNames.get(i);
				System.out.println("EX: " + universalFileEntryInfo1.filename);
			} else {
				System.out.println("EX: <none>");
			}

			if (i < actualFileNames.size()) {
				UniversalFileEntryInfo universalFileEntryInfo2 = actualFileNames.get(i);
				System.out.println("AC: " + universalFileEntryInfo2.filename);
			} else {
				System.out.println("AC: <none>");
			}
		}
	}

	public class TestSequentialOutStream implements ISequentialOutStream {
		private final InputStream inputStream;
		private final String filename;

		public TestSequentialOutStream(String filename, InputStream inputStream) {
			this.filename = filename;
			this.inputStream = inputStream;
		}

		public int write(byte[] data) {
			try {
				if (inputStream.available() < data.length) {
					error("More data as expected for file '" + filename + "'");
					throw new RuntimeException("More data as expected for file '" + filename + "'");
				} else {
					byte[] expectedData = new byte[data.length];
					int readBytes = 0;
					do {
						if (inputStream.available() == 0) {
							error("More data as expected for file '" + filename + "'");
							throw new RuntimeException("More data as expected for file '" + filename + "'");
						}

						readBytes += inputStream.read(expectedData, readBytes, data.length - readBytes);
					} while (readBytes < data.length);

					if (!Arrays.equals(expectedData, data)) {
						error("Extracted data missmatched for file '" + filename + "'");
						throw new RuntimeException("Extracted data missmatched for file '" + filename + "'");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				error("Unexpected exception: " + e);
				throw new RuntimeException("Unexpected exception: " + e, e);
			}
			return data.length;
		}
	}

	public class TestArchiveExtractCryptoCallback extends TestArchiveExtractCallback implements ICryptoGetTextPassword {

		public TestArchiveExtractCryptoCallback(String[] itemIdToItemName) {
			super(itemIdToItemName);
		}

		/**
		 * {@inheritDoc}
		 */

		public String cryptoGetTextPassword() throws SevenZipException {
			return password;
		}

	}

	public class TestArchiveExtractCallback implements IArchiveExtractCallback {
		private final String[] itemIdToItemName;

		public TestArchiveExtractCallback(String[] itemIdToItemName) {
			this.itemIdToItemName = itemIdToItemName;
		}

		/**
		 * {@inheritDoc}
		 */

		public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) {

			if (!extractAskMode.equals(ExtractAskMode.EXTRACT)) {
				return null;
			}

			try {
				String filename = itemIdToItemName[index];
				InputStream inputStream = expectedZipFile.getInputStream(expectedZipFile.getEntry(filename));
				return new TestSequentialOutStream(filename, inputStream);

			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		/**
		 * {@inheritDoc}
		 */

		public boolean prepareOperation(ExtractAskMode extractAskMode) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */

		public void setOperationResult(ExtractOperationResult extractOperationResult) {
			if (extractOperationResult != ExtractOperationResult.OK) {
				error("Extraction: operation result wan's ok  but " + extractOperationResult);
			}
		}

		/**
		 * {@inheritDoc}
		 */

		public void setCompleted(long completeValue) {
		}

		/**
		 * {@inheritDoc}
		 */

		public void setTotal(long total) {
		}
	}
}