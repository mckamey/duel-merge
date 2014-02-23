package org.duelengine.merge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildManager {

	private static final String HASH_ALGORITHM = "SHA-1";
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String PROPERTY_LIST_DELIM = "|";
	private static final Logger log = LoggerFactory.getLogger(BuildManager.class);

	private final Map<String, String> hashLookup = new LinkedHashMap<String, String>();
	private final Map<String, List<String>> dependencyMap = new LinkedHashMap<String, List<String>>();
	private final Map<String, List<String>> childLinkMap = new LinkedHashMap<String, List<String>>();
	private final Map<String, Compactor> compactors;
	private final Settings settings;
	private final Stack<String> dependencyChain = new Stack<String>();

	/**
	 * @param settings path location settings
	 */
	public BuildManager(Settings settings) {
		this(settings,
			new MergeCompactor(
				new JSPlaceholderGenerator(),
				new CSSPlaceholderGenerator()),
			new NullCompactor(settings.getExtensions()),
			new CSSCompactor(),
			new JSCompactor());
	}

	/**
	 * @param settings path location settings
	 * @param compactors list of all active compactors
	 */
	public BuildManager(Settings settings, Compactor... compactors) {
		if (settings == null) {
			throw new NullPointerException("settings");
		}
		if (compactors == null) {
			throw new NullPointerException("compactors");
		}
		if (settings.getSourceDir() == null || !settings.getSourceDir().exists()) {
			throw new IllegalArgumentException("Missing source directory "+settings.getSourceDir());
		}

		this.settings = settings;

		this.compactors = new LinkedHashMap<String, Compactor>(compactors.length);
		for (Compactor compactor : compactors) {
			for (String ext : compactor.getSourceExtensions()) {
				this.compactors.put(ext, compactor);
			}
		}
	}

	/**
	 * Compiles merge files and processes resources
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void execute()
			throws IOException, NoSuchAlgorithmException {

		Map<File, String> inputFiles = findFiles();

		for (File source : inputFiles.keySet()) {
			processResource(
				inputFiles.get(source),
				source);
		}

		writeCompactionMap();
		writeChildLinksMap();
	}

	public boolean isProcessed(String path) {
		return hashLookup.containsKey(path);
	}

	public String getProcessedPath(String path) {
		return hashLookup.get(path);
	}

	public String getPlaceholderPath(String path) {
		String hashed = hashLookup.get(path);
		if (hashed == null || hashed.isEmpty()) {
			return path;
		}

		hashed = hashLookup.get(hashed);
		if (hashed == null || hashed.isEmpty()) {
			return path;
		}

		return hashed;
	}

	public void setProcessedPath(String path, String hashPath) {
		hashLookup.put(path, hashPath);
	}

	private void removeProcessedPath(String path) {
		hashLookup.remove(path);
	}

	public void ensureProcessed(String path) {

		if (isProcessed(path) && getTargetFile(path).exists()) {
			return;
		}

		try {
			processResource(path, settings.findSourceFile(path));

		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage(), e);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	private void processResource(String path, File source)
			throws IOException, NoSuchAlgorithmException {

		// keep track of currently compacting paths to prevent cycles
		if (dependencyChain.contains(path)) {
			log.error("Cyclical dependencies detected in: "+path);
			return;
		}
		dependencyChain.push(path);

		try {
			String sourceExt = getExtension( source.getCanonicalPath() );
			Compactor compactor = compactors.get(sourceExt);
			if (compactor == null) {
				log.error("No compactor registered for "+sourceExt);
				return;
			}

			File target;
			if (isProcessed(path)) {
				target = getTargetFile(path);

			} else {
				MessageDigest hash = MessageDigest.getInstance(HASH_ALGORITHM);
				if (source != null && source.exists()) {
					compactor.calcHash(this, hash, path, source);
				}
				String hashPath = encodeBytes(hash.digest());
				String targetExt = compactor.getTargetExtension(this, path);
				setProcessedPath(path, settings.getCDNRoot()+hashPath+targetExt);

				target = getTargetFile(path);
				if (source.exists()) {
					// ensure target path exists
					target.getParentFile().mkdirs();
		
					// ensure the file has been compacted
					compactor.compact(this, path, source, target);
				}
			}

			if (!target.exists()) {
				// file still missing, remove
				log.error(path+" failed to compact (output missing)");
				removeProcessedPath(path);
	
			} else if (target.length() < 1L) {
				if (source.length() < 1L) {
					// special case for files which compact to empty
					log.warn(path+" is an empty file");
	
					// remove from listings
					removeProcessedPath(path);
	
				} else {
					// special case for files which compact to empty
					log.warn(path+" compacted to an empty file (using original for merge)");
	
					// copy over original contents (as wasn't actually empty)
					new NullCompactor().compact(this, path, source, target);
				}
			}

		} finally {
			dependencyChain.pop();
		}
	}

	public void addChildLink(String path, String child) {
		List<String> children = childLinkMap.get(path);
		if (children == null) {
			children = new ArrayList<String>();
			childLinkMap.put(path, children);
		}
		if (!children.contains(child)) {
			children.add(child);
		}
	}

	public List<String> getChildLinks(String path) {
		List<String> children = childLinkMap.get(path);
		if (children == null) {
			children = Collections.emptyList();
		}
		return children;
	}

	public void addDependency(String path, String dependency) {
		List<String> dependencies = dependencyMap.get(path);
		if (dependencies == null) {
			dependencies = new ArrayList<String>();
			dependencyMap.put(path, dependencies);
		}
		if (!dependencies.contains(dependency)) {
			dependencies.add(dependency);
		}
	}

	public List<String> getDependencies(String path) {
		List<String> dependencies = dependencyMap.get(path);
		if (dependencies == null) {
			dependencies = Collections.emptyList();
		}
		return dependencies;
	}

	public static String getExtension(String path) {
		// query and hash
		int query = path.indexOf('?');
		if (query >= 0) {
			path = path.substring(0, query);
		}
		int hash = path.indexOf('#');
		if (hash >= 0) {
			path = path.substring(0, hash);
		}

		int dot = path.lastIndexOf('.');
		if (dot < 0) {
			return "";
		}

		return path.substring(dot).toLowerCase();
	}

	private static String encodeBytes(byte[] digest) {
		StringBuilder hex = new StringBuilder();
		for (int i=0; i<digest.length; i++) {
			int digit = 0xFF & digest[i];
			if (digit < 0x10) {
				hex.append('0');
			}
			hex.append(Integer.toHexString(digit));
		}
		return hex.toString();
	}

	public File getTargetFile(String path) {
		String outputPath = getProcessedPath(path);

		return settings.getTargetFile(outputPath);
	}

	private Map<File, String> findFiles()
			throws IOException {

		Set<String> extensions = getExtensions();
		String filterPath = settings.getCDNDir().getCanonicalPath();
		Queue<File> folders = new LinkedList<File>();
		Map<File, String> files = new LinkedHashMap<File, String>();

		for (File inputDir : new File[] { settings.getSourceDir(), settings.getTargetDir() }) {
			int rootPrefix = inputDir.getCanonicalPath().length();

			folders.add(inputDir);
			while (!folders.isEmpty()) {
				File file = folders.remove();

				if (file.getCanonicalPath().startsWith(filterPath)) {
					// filter any output files, e.g., if dirs overlap
					continue;
				}

				if (file.isDirectory()) {
					folders.addAll(Arrays.asList(file.listFiles()));
					continue;
				}

				String ext = BuildManager.getExtension(file.getCanonicalPath());
				if (extensions.contains(ext)) {
					files.put(file, file.getCanonicalPath().substring(rootPrefix));
				}
			}
		}

		return files;
	}

	private Set<String> getExtensions() {
		return this.compactors.keySet();
	}

	private void writeCompactionMap()
			throws IOException {

		File cdnMapFile = settings.getCDNMapFile();

		cdnMapFile.getParentFile().mkdirs();

		FileWriter writer = new FileWriter(cdnMapFile, false);
		try {
			writeCompactionMap(writer);

		} finally {
			writer.flush();
			writer.close();
		}
	}

	private void writeCompactionMap(Appendable output)
			throws IOException {

		// generate output
		for (String key : hashLookup.keySet()) {
			String value = hashLookup.get(key);
			value = escapePropertyValue(value);

			output
				.append(key)
				.append('=')
				.append(value)
				.append(NEWLINE);
		}
	}

	private void writeChildLinksMap()
			throws IOException {

		File cdnLinksFile = settings.getCDNLinksFile();
		cdnLinksFile.getParentFile().mkdirs();

		FileWriter writer = new FileWriter(cdnLinksFile, false);
		try {
			writeChildLinksMap(writer);

		} finally {
			writer.flush();
			writer.close();
		}
	}

	private void writeChildLinksMap(Appendable output)
			throws IOException {

		// propagate transitive children to dependents
		for (String path : this.dependencyMap.keySet()) {
			addTransitiveChildLinks(path);
		}

		// generate output
		for (String key : childLinkMap.keySet()) {
			List<String> children = childLinkMap.get(key);

			boolean needsDelim = false;
			output
				.append(key)
				.append('=');

			for (String child : children) {
				if (needsDelim) {
					output.append(PROPERTY_LIST_DELIM);
				} else {
					needsDelim = true;
				}
				output.append(escapePropertyValue(child));
			}

			output.append(NEWLINE);
		}
	}

	private void addTransitiveChildLinks(String path) {
		if (!dependencyMap.containsKey(path)) {
			// no dependencies so nothing to propagate
			return;
		}

		List<String> dependencies = dependencyMap.get(path);
		for (String dependency : dependencies) {
			// recursively ripple links up to dependent parents
			this.addTransitiveChildLinks(dependency);
			if (!childLinkMap.containsKey(dependency)) {
				// no child links so nothing to propagate
				continue;
			}

			// add as if was a direct child of dependent parent 
			List<String> children = childLinkMap.get(dependency);
			for (String child : children) {
				// propagate child link up to aggregate parent
				this.addChildLink(path, child);
				log.info("transitive child link: "+path+"=>"+child);
			}
		}
	}

	/**
	 * http://download.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)
	 * @param value
	 * @return
	 */
	private static String escapePropertyValue(String value) {
		if (value == null) {
			return "";
		}

		StringBuilder output = null;
		int start = 0,
			length = value.length();

		for (int i=start; i<length; i++) {
			char ch = value.charAt(i);

			// escape any illegal chars [:=#!\s]+
			switch (ch) {
				case ':':
				case '=':
				case '#':
				case '!':
				case '\t':
				case '\n':
				case '\r':
				case ' ':
					if (output == null) {
						output = new StringBuilder(length * 2);
					}

					if (i > start) {
						// emit any leading unescaped chunk
						output.append(value, start, i);
					}
					start = i+1;

					// emit escape
					output.append('\\').append(ch);
					continue;
			}
		}

		if (output == null) {
			// nothing to escape, can write entire string directly
			return value;
		}

		if (length > start) {
			// emit any trailing unescaped chunk
			output.append(value, start, length);
		}

		return output.toString();
	}
}
