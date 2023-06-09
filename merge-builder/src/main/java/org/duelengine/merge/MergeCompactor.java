package org.duelengine.merge;

import java.io.*;
import java.security.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MergeCompactor implements Compactor {

	private static final int BUFFER_SIZE = 4096;
	private static final String CHAR_ENCODING = "UTF-8";
	private static final String EXT = ".merge";
	private final Logger log = LoggerFactory.getLogger(MergeCompactor.class);
	private final Map<String, PlaceholderGenerator> placeholders;

	public MergeCompactor(PlaceholderGenerator... placeholders) {
		if (placeholders == null) {
			throw new NullPointerException("placeholders");
		}

		this.placeholders = new LinkedHashMap<String, PlaceholderGenerator>(placeholders.length);
		for (PlaceholderGenerator placeholder : placeholders) {
			this.placeholders.put(placeholder.getTargetExtension(), placeholder);
		}
	}
	
	@Override
	public String[] getSourceExtensions() {
		return new String[] { EXT };
	}

	@Override
	public String getTargetExtension(BuildManager manager, String path) {
		// merge file assumes the first non-empty extension
		for (String dependency : manager.getDependencies(path)) {
			String ext = BuildManager.getExtension(dependency);
			if (EXT.equalsIgnoreCase(ext)) {
				ext = getTargetExtension(manager, dependency);
			}
			if (ext.isEmpty()) {
				continue;
			}
			return ext;
		}

		return "";
	}

	@Override
	public void calcHash(BuildManager manager, MessageDigest hash, String path, File source)
			throws IOException, NoSuchAlgorithmException {

		BufferedReader reader = new BufferedReader(new FileReader(source));
		try {

			// calculate the hash for the merge file as a hash of the dependency hash paths
			// if any of the dependencies change this hash will also

			String dependency;
			while ((dependency = reader.readLine()) != null) {
				dependency = dependency.trim();
				if (dependency.isEmpty() || dependency.startsWith("#")) {
					// skip empty lines and comments
					continue;
				}

				manager.ensureProcessed(dependency);

				String dependencyPath = manager.getProcessedPath(dependency);
				if (dependencyPath == null) {
					// skip missing resources (will be reflected in hash when come available)
					log.warn("Missing merge reference: "+dependency);
					continue;
				}

				manager.addDependency(path, dependency);
				hash.update(dependencyPath.getBytes(CHAR_ENCODING));
			}

		} finally {
			reader.close();
		}
	}

	@Override
	public void compact(BuildManager manager, String path, File source, File target)
			throws IOException {

		this.buildMerge(manager, path, target);
		this.buildDebugPlaceholders(manager, path);
	}

	private void buildMerge(BuildManager manager, String path, File target)
		throws FileNotFoundException, IOException {
		
		log.info("Building "+path);
		String outputPath = manager.getProcessedPath(path);

		if (manager.isProcessed(path) && target.exists()) {
			log.info("- exists: "+outputPath);
			return;
		}
		log.info("- writing to "+outputPath);

		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);

		try {
			final char[] buffer = new char[BUFFER_SIZE];

			// concatenate children
			for (String child : manager.getDependencies(path)) {
				// insert child files into outputFile
				log.info("- adding "+child);
				File source = manager.getTargetFile(child);
				FileReader reader = new FileReader(source);
				try {
					int count;
					while ((count = reader.read(buffer)) > 0) {
						writer.write(buffer, 0, count);
					}
				} finally {
					reader.close();
				}
			}

		} finally {
			writer.flush();
			writer.close();
		}
	}

	private void buildDebugPlaceholders(BuildManager manager, String path)
		throws FileNotFoundException, IOException {
		
		String hashPath = manager.getProcessedPath(path);

		List<String> dependencies = manager.getDependencies(path);

		if (dependencies.size() == 1) {
			// if only one child then use source file as the debugPath
			manager.setProcessedPath(hashPath, manager.getPlaceholderPath(dependencies.get(0)));
			return;
		}

		// splice in the debug directory
		int slash = hashPath.lastIndexOf('/');
		String debugPath = hashPath.substring(0, slash)+"/debug"+hashPath.substring(slash);
		manager.setProcessedPath(hashPath, debugPath);

		for (String dependency : dependencies) {
			// in debug placeholders, merge dependencies become child links
			manager.addChildLink(debugPath, dependency);
		}

		File target = manager.getTargetFile(hashPath);
		if (manager.isProcessed(path) && target.exists()) {
			return;
		}

		String targetExt = BuildManager.getExtension(hashPath);
		PlaceholderGenerator generator = this.placeholders.get(targetExt);
		if (generator == null) {
			log.warn("No debug placeholder generator found for "+targetExt);
			return;
		}

		generator.build(manager, target, dependencies);
	}
}
