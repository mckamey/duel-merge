package org.duelengine.merge;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface PlaceholderGenerator {

	/**
	 * Gets the extension which this compaction emits
	 * @return
	 */
	String getTargetExtension();

	/**
	 * Perform compaction
	 * @param target output file
	 * @param source input file
	 */
	void build(File target, List<String> children) throws IOException;
}
