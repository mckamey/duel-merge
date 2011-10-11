package org.duelengine.merge;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface Compactor {

	/**
	 * Gets the extension which this compaction consumes
	 * @return
	 */
	String[] getSourceExtensions();

	/**
	 * Gets the extension which this compaction emits
	 * @return
	 */
	String getTargetExtension();

	/**
	 * Perform compaction
	 * @param source input file
	 * @param target output file
	 * @param path URL path
	 */
	void compact(Map<String, String> fileHashes, File source, File target, String path) throws IOException;
}
