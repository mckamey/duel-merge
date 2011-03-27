package org.duelengine.merge;

import java.io.File;
import java.io.IOException;

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
	 */
	void compact(File source, File target) throws IOException;
}
