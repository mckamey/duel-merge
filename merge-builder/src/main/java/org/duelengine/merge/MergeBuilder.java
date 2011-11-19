package org.duelengine.merge;

import java.io.*;
import java.security.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeBuilder {

	private static final int BUFFER_SIZE = 4096;
	private static final String HASH_ALGORITHM = "SHA-1";
	private static final String CHAR_ENCODING = "utf-8";

	private final Logger log = LoggerFactory.getLogger(MergeBuilder.class);
	private final Map<String, Compactor> compactors;
	private final Map<String, PlaceholderGenerator> placeholders;
	private File webappDir;
	private File outputDir;
	private File cdnMapFile;
	private String cdnRoot;

	/**
	 * @param cdnExtensions extensions which will be directly copied without processing
	 */
	public MergeBuilder(String... cdnExtensions) {
		this(Arrays.asList(
				new JSPlaceholderGenerator(),
				new CSSPlaceholderGenerator()),
			Arrays.asList(
				new NullCompactor(cdnExtensions),
				new CSSCompactor(),
				new JSCompactor()));
	}

	/**
	 * @param placeholders list of debug build placeholder generators
	 * @param compactors list of all active compactors
	 */
	public MergeBuilder(List<PlaceholderGenerator> placeholders, List<Compactor> compactors) {
		if (placeholders == null) {
			throw new NullPointerException("placeholders");
		}
		if (compactors == null) {
			throw new NullPointerException("compactors");
		}

		this.placeholders = new LinkedHashMap<String, PlaceholderGenerator>(placeholders.size());
		for (PlaceholderGenerator placeholder : placeholders) {
			this.placeholders.put(placeholder.getTargetExtension(), placeholder);
		}

		this.compactors = new LinkedHashMap<String, Compactor>(compactors.size());
		for (Compactor compactor : compactors) {
			for (String ext : compactor.getSourceExtensions()) {
				this.compactors.put(ext, compactor);
			}
		}
	}

	public String getWebAppDir() {
		try {
			return (this.webappDir != null) ? this.webappDir.getCanonicalPath() : null;
		} catch (IOException e) {
			return (this.webappDir != null) ? this.webappDir.getAbsolutePath() : null;
		}
	}

	public void setWebAppDir(String value) {
		this.webappDir = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	public String getOutputDir() {
		try {
			return (this.outputDir != null) ? this.outputDir.getCanonicalPath() : null;
		} catch (IOException e) {
			return (this.outputDir != null) ? this.outputDir.getAbsolutePath() : null;
		}
	}

	public void setOutputDir(String value) {
		this.outputDir = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	public String getCDNMapFile() {
		try {
			return (this.cdnMapFile != null) ? this.cdnMapFile.getCanonicalPath() : null;
		} catch (IOException e) {
			return (this.cdnMapFile != null) ? this.cdnMapFile.getAbsolutePath() : null;
		}
	}

	public void setCDNMapFile(String value) {
		this.cdnMapFile = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	public String getCDNRoot() {
		return this.cdnRoot;
	}

	public void setCDNRoot(String value) {
		if (value != null) {
			value = value.replace('\\', '/');
			if (!value.startsWith("/")) {
				value = '/'+value;
			}
			if (!value.endsWith("/")) {
				value += '/';
			}
		}
		this.cdnRoot = value;
	}

	private File getCDNDir() {
		return new File(getOutputDir(), this.cdnRoot);
	}

	private void ensureSettings() {
		if (this.webappDir == null || !this.webappDir.exists()) {
			throw new IllegalArgumentException("ERROR: missing webappDir "+this.webappDir);
		}
		if (this.cdnMapFile == null) {
			throw new IllegalArgumentException("ERROR: missing cdnMapFile");
		}

		if (this.cdnRoot == null) {
			this.cdnRoot = "/cdn/";
		}
		if (this.outputDir == null) {
			this.outputDir = this.webappDir;
		}
	}

	/**
	 * Compiles merge files and processes resources
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void execute() throws IOException, NoSuchAlgorithmException {
		this.ensureSettings();

		final Map<String, String> hashLookup = new LinkedHashMap<String, String>();

		// calculate hash and compact all the source files
		hashClientFiles(hashLookup, this.compactors.keySet());

		if (hashLookup.size() < 1) {
			throw new IllegalArgumentException("ERROR: no input files found in "+this.webappDir);
		}

		// calculate hash for all the merge files and determine dependencies
		Map<String, List<String>> dependencyMap = this.hashMergeFiles(hashLookup);

		for (String path : dependencyMap.keySet()) {
			List<String> children = dependencyMap.get(path);

			this.buildMerge(hashLookup, path, children);
			this.buildDebugPlaceholders(hashLookup, path, children);
		}

		saveHashLookup(hashLookup);
	}

	private void buildMerge(final Map<String, String> hashLookup, String path, List<String> children)
		throws FileNotFoundException, IOException {

		log.info("Building "+path);
		String outputPath = hashLookup.get(path);

		File outputFile = new File(this.outputDir, outputPath);
		if (outputFile.exists()) {
			log.info("- exists: "+outputPath);
			return;
		}
		log.info("- writing to "+outputPath);

		outputFile.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(outputFile, false);

		try {
			// concatenate children
			final char[] buffer = new char[BUFFER_SIZE];
			for (String child : children) {
				// insert child files into outputFile
				log.info("- adding "+child);
				File inputFile = new File(this.outputDir, hashLookup.get(child));
				FileReader reader = new FileReader(inputFile);
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

	private void buildDebugPlaceholders(final Map<String, String> hashLookup, String path, List<String> children)
		throws FileNotFoundException, IOException {

		String hashPath = hashLookup.get(path);
		if (children.size() == 1) {
			// if only one child then the source file is the debugPath
			hashLookup.put(hashPath, children.get(0));
			return;
		}

		// splice in the debug directory
		int slash = hashPath.lastIndexOf('/');
		String debugPath = hashPath.substring(0, slash)+"/debug"+hashPath.substring(slash);
		hashLookup.put(hashPath, debugPath);

		File outputFile = new File(this.outputDir, debugPath);
		if (outputFile.exists()) {
			return;
		}

		PlaceholderGenerator generator = this.placeholders.get(getExtension(hashPath));
		if (generator == null) {
			log.warn("Cannot generate debug placeholder for "+debugPath);
			return;
		}

		generator.build(outputFile, children);
	}

	private Map<String, List<String>> hashMergeFiles(Map<String, String> hashLookup)
			throws IOException, NoSuchAlgorithmException {

		Map<File, String> inputFiles = findFiles(Collections.singleton(".merge"), this.getCDNDir(), this.webappDir, this.outputDir);
		Map<String, List<String>> dependencyMap = new LinkedHashMap<String, List<String>>(inputFiles.size());

		for (File inputFile : inputFiles.keySet()) {
			List<String> children = new ArrayList<String>();

			String path = inputFiles.get(inputFile);
			if (hashLookup.containsKey(path)) {
				// duplicate from output
				continue;
			}
			String hashPath = this.cdnRoot+calcMergeHash(inputFile, children, hashLookup);

			// merge file assumes the first non-empty extension
			String ext = null;
			for (String child : children) {
				ext = getExtension(child);
				if (ext != null) {
					break;
				}
			}

			hashPath += (ext == null) ? ".merge" : ext;

			hashLookup.put(path, hashPath);
			dependencyMap.put(path, children);
		}

		return dependencyMap;
	}

	private void hashClientFiles(Map<String, String> hashLookup, Set<String> extensions)
			throws IOException, NoSuchAlgorithmException {

		Map<File, String> inputFiles = findFiles(extensions, this.getCDNDir(), this.webappDir, this.outputDir);

		for (File inputFile : inputFiles.keySet()) {

			// calculate and store the hash
			String path = inputFiles.get(inputFile);
			if (hashLookup.containsKey(path)) {
				// already processed
				continue;
			}

			hashClientFile(hashLookup, inputFile, path);
		}
	}

	private void hashClientFile(Map<String, String> hashLookup, File inputFile, String path)
			throws IOException, NoSuchAlgorithmException {

		String ext = getExtension( inputFile.getCanonicalPath() );
		Compactor compactor = compactors.get(ext);
		if (compactor == null) {
			throw new IllegalArgumentException("Error: no compactor registered for "+ext);
		}
		String targetExt = compactor.getTargetExtension();
		if (targetExt == null || targetExt.indexOf('.') < 0) {
			targetExt = ext;
		}

		// ensure the file has been compacted
		String hashPath = this.cdnRoot + this.calcFileHash(inputFile) + targetExt;
		File outputFile = new File(this.outputDir, hashPath);
		if (!outputFile.exists()) {
			// ensure compacted target path exists
			compactor.compact(hashLookup, inputFile, outputFile, path);
		}
		hashLookup.put(path, hashPath);

		if (!outputFile.exists()) {
			// file still missing, remove
			log.error(path+" failed to compact (output missing)");
			hashLookup.remove(path);

		} else if (outputFile.length() < 1L) {
			if (inputFile.length() < 1L) {
				// special case for files which compact to empty
				log.warn(path+" is an empty file");

				// remove from listings
				hashLookup.remove(path);
			} else {
				// special case for files which compact to empty
				log.warn(path+" compacted to an empty file (using original)");

				// copy over original contents (as wasn't really empty)
				new NullCompactor().compact(hashLookup, inputFile, outputFile, path);
			}
		}
	}

	private void saveHashLookup(final Map<String, String> hashLookup)
			throws IOException {

		this.cdnMapFile.getParentFile().mkdirs();

		final String newline = System.getProperty("line.separator");
		FileWriter writer = new FileWriter(this.cdnMapFile, false);
		try {
			// generate output
			for (String key : hashLookup.keySet()) {
				writer.append(key).append('=').append(escapePropertyValue(hashLookup.get(key))).append(newline);
			}

		} finally {
			writer.flush();
			writer.close();
		}
	}

	/**
	 * http://download.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)
	 * @param value
	 * @return
	 */
	private CharSequence escapePropertyValue(String value) {

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

	private String calcMergeHash(File inputFile, List<String> children, final Map<String, String> hashLookup)
			throws NoSuchAlgorithmException, FileNotFoundException, IOException, UnsupportedEncodingException {

		FileReader reader = new FileReader(inputFile);
		try {
			BufferedReader lineReader = new BufferedReader(reader);

			// calculate the hash for the merge file
			// will be a hash of the child hashes
			final MessageDigest sha1 = MessageDigest.getInstance(HASH_ALGORITHM);

			String line;
			while ((line = lineReader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					// skip empty lines and comments
					continue;
				}

				String childPath = hashLookup.get(line);
				if (childPath == null) {
					// TODO: allow chaining of .merge files by ordering by dependency
					log.warn("Missing merge reference: "+line);

					// skip missing resources (will be reflected in hash)
					continue;
				}

				children.add(line);
				sha1.update(childPath.getBytes(CHAR_ENCODING));
			}

			return encodeBytes(sha1.digest());

		} finally {
			reader.close();
		}
	}

	private String calcFileHash(File inputFile)
			throws IOException, NoSuchAlgorithmException {

		FileInputStream stream = new FileInputStream(inputFile);
		try {
			final MessageDigest sha1 = MessageDigest.getInstance(HASH_ALGORITHM);
			final byte[] buffer = new byte[BUFFER_SIZE];

			int count;
			while ((count = stream.read(buffer)) > 0) {
				sha1.update(buffer, 0, count);
			}

			return encodeBytes(sha1.digest());

		} finally {
			stream.close();
		}
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

	private static String getExtension(String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0) {
			return "";
		}

		return path.substring(dot).toLowerCase();
	}

	private static Map<File, String> findFiles(Set<String> extensions, File filterDir, File... inputDirs)
		throws IOException {

		String filterPath = filterDir.getCanonicalPath();
		Queue<File> folders = new LinkedList<File>();
		Map<File, String> files = new LinkedHashMap<File, String>();

		for (File inputDir : inputDirs) {
			int rootPrefix = inputDir.getCanonicalPath().length();

			folders.add(inputDir);
			while (!folders.isEmpty()) {
				File file = folders.remove();

				if (file.getCanonicalPath().startsWith(filterPath)) {
					// filter any output files if dirs overlap
					continue;
				}

				if (file.isDirectory()) {
					folders.addAll(Arrays.asList(file.listFiles()));
					continue;
				}

				String ext = getExtension(file.getCanonicalPath());
				if (extensions.contains(ext)) {
					files.put(file, file.getCanonicalPath().substring(rootPrefix));
				}
			}
		}

		return files;
	}
}
