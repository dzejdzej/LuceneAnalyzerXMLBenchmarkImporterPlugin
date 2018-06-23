package xml.benchmark.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipReader {
	
	
	public static void readZipFile(InputStream zipFile) throws IOException {
		 final ZipInputStream zfile = new ZipInputStream(zipFile);
		 
		
		    ZipInputStream is = new ZipInputStream(new BufferedInputStream(zipFile));
		    ZipEntry entry;
		    while ((entry = is.getNextEntry()) != null) {
		        File unpacked = new File("test-zip", entry.getName());
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
		    ZipEntry e = is.getNextEntry();
		 
	}

}
