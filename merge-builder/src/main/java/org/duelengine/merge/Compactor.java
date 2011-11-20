package org.duelengine.merge;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	String getTargetExtension(BuildManager manager, String path);

	/**
	 * Generate SHA-1 hash for the specified file
	 * @param manager
	 * @param hash
	 * @param path
	 * @param source
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	void calcHash(BuildManager manager, MessageDigest hash, String path, File source)
			throws IOException, NoSuchAlgorithmException;

	/**
	 * Perform compaction
	 * @param manager
	 * @param path URL path
	 * @param source input file
	 * @param target output file
	 */
	void compact(BuildManager manager, String path, File source, File target)
			throws IOException;
}
