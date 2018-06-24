package xml.benchmark.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipReader {

	public static List<String> getZippedFileNames(InputStream zipFile) throws IOException {
		List<String> fileNames = new ArrayList<>();
		
		ZipInputStream is = new ZipInputStream(new BufferedInputStream(zipFile));
		ZipEntry entry;
		while ((entry = is.getNextEntry()) != null) {
			fileNames.add(entry.getName());
		}
		
		return fileNames;
	}

	public static List<String> readZipFile(InputStream zipFile, String outputDir) throws IOException {
		List<String> fileNames = new ArrayList<>();
		
		
		
		ZipInputStream is = new ZipInputStream(new BufferedInputStream(zipFile));
		ZipEntry entry;
		while ((entry = is.getNextEntry()) != null) {
			File unpacked = new File(outputDir, entry.getName());
			fileNames.add(unpacked.getAbsolutePath());
			FileChannel channel = new FileOutputStream(unpacked).getChannel();
			byte[] bytes = new byte[2048];
			try {
				int len;
				long size = entry.getSize();
				while (size > 0 && (len = is.read(bytes, 0, 2048)) > 0) {
					ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, len);
					int j = channel.write(buffer);
					size -= len;
				}
			} finally {
				channel.close();
			}
		}
		return fileNames;
	}

}
