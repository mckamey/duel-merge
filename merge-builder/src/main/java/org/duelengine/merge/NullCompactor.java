package org.duelengine.merge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Very basic "compactor" which simply copies the bits from source to target
 */
class NullCompactor implements Compactor {

	private static final int BUFFER_SIZE = 4096;
	private final String[] extensions;

	public NullCompactor(String... extensions) {
		this.extensions = (extensions != null) ? extensions : new String[0];
	}
	
	@Override
	public String[] getSourceExtensions() {
		return this.extensions;
	}

	@Override
	public String getTargetExtension() {
		return null;
	}

	@Override
	public void compact(Map<String, String> fileHashes, File source, File target, String path) throws IOException {
		target.getParentFile().mkdirs();

		FileInputStream inStream = new FileInputStream(source);
		FileOutputStream outStream = new FileOutputStream(target);
		try {
			final byte[] buffer = new byte[BUFFER_SIZE];

			int count;
			while ((count = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, count);
			}

		} finally {
			outStream.flush();
			outStream.close();
			inStream.close();
		}
	}
}
