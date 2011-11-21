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
	 * Perform build
	 * @param manager
	 * @param target output file
	 * @param children
	 * @throws IOException
	 */
	void build(BuildManager manager, File target, List<String> children) throws IOException;
}
