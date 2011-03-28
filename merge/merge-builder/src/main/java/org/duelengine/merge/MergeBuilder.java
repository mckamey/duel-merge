package org.duelengine.merge;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.Logger;

public class MergeBuilder {

	private static final String HELP =
		"Usage:\n" +
		"\tjava -jar merge.jar <webapp-dir>\n" +
		"\tjava -jar merge.jar <webapp-dir> <cdn-output-path>\n" +
		"\tjava -jar merge.jar <webapp-dir> <cdn-output-path> <cdn-map-file>\n\n"+
		"\twebapp-dir: file path to the root of the webapp\n"+
		"\tcdn-output-path: webapp-relative path for the cdn output\n"+
		"\tcdn-map-file: path to the generated resource file\n";

	private static final int BUFFER_SIZE = 4096;
	private static final String HASH_ALGORITHM = "SHA-1";
	private static final String CHAR_ENCODING = "utf-8";

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println(HELP);
			return;
		}

		MergeBuilder builder = new MergeBuilder();
		builder.setWebAppDir(args[0]);

		if (args.length > 1) {
			builder.setCDNRoot(args[1]);

			if (args.length > 2) {
				builder.setCDNMapFile(args[2]);
			}
		}

		try {
			builder.execute();

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private final Logger log = Logger.getLogger(MergeBuilder.class.getCanonicalName());
	private final Map<String, Compactor> compactors;
	private final Map<String, PlaceholderGenerator> placeholders;
	private File webappDir;
	private String cdnRoot;
	private File cdnMapFile;

	public MergeBuilder(String... cdnExtensions) {
		this(Arrays.asList(
				new JSPlaceholderGenerator(),
				new CSSPlaceholderGenerator()),
			Arrays.asList(
				new NullCompactor(cdnExtensions),
				new CSSCompactor(),
				new JSCompactor()));
	}

	public MergeBuilder(List<PlaceholderGenerator> placeholders, List<Compactor> compactors) {
		if (placeholders == null) {
			throw new NullPointerException("placeholders");
		}
		if (compactors == null) {
			throw new NullPointerException("compactors");
		}

		this.placeholders = new HashMap<String, PlaceholderGenerator>(placeholders.size());
		if (placeholders != null) {
			for (PlaceholderGenerator placeholder : placeholders) {
				this.placeholders.put(placeholder.getTargetExtension(), placeholder);
			}
		}

		this.compactors = new HashMap<String, Compactor>(compactors.size());
		for (Compactor compactor : compactors) {
			for (String ext : compactor.getSourceExtensions()) {
				this.compactors.put(ext, compactor);
			}
		}
	}

	public String getWebAppDir() {
		return this.webappDir.getAbsolutePath();
	}

	public void setWebAppDir(String value) {
		this.webappDir = (value != null) ? new File(value.replace('\\', '/')) : null;
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

	public String getCDNMapFile() {
		return this.cdnMapFile.getAbsolutePath();
	}

	public void setCDNMapFile(String value) {
		this.cdnMapFile = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	private boolean ensureSettings() {
		if (this.webappDir == null || !this.webappDir.exists()) {
			throw new IllegalArgumentException("Error: missing webapp "+this.webappDir);
		}

		if (this.cdnRoot == null) {
			this.cdnRoot = "/cdn/";
		}

		if (this.cdnMapFile == null) {
			this.cdnMapFile = new File(this.webappDir.getParentFile(), "resources/cdn.properties");
		}

		return true;
	}

	/**
	 * Compiles merge files
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void execute() throws IOException, NoSuchAlgorithmException {
		if (!this.ensureSettings()) {
			return;
		}

		final Map<String, String> hashLookup = new LinkedHashMap<String, String>();

		// calculate hash and compact all the source files
		for (String ext : this.compactors.keySet()) {
			hashClientFiles(hashLookup, ext);
		}

		if (hashLookup.size() < 1) {
			throw new IllegalArgumentException("Error: no input files found in "+this.webappDir);
		}

		// calculate hash for all the merge files and determine dependencies
		Map<String, List<String>> dependencyMap = this.hashMergeFiles(hashLookup);

		for (String path : dependencyMap.keySet()) {
			List<String> children = dependencyMap.get(path);

			this.buildMerge(hashLookup, path, children);
			this.buildDevPlaceholders(hashLookup, path, children);
		}

		saveHashLookup(hashLookup);
	}

	private void buildMerge(final Map<String, String> hashLookup, String path, List<String> children)
		throws FileNotFoundException, IOException {

		log.info("Building "+path);
		String outputPath = hashLookup.get(path);

		File outputFile = new File(this.webappDir, outputPath);
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
				File inputFile = new File(this.webappDir, hashLookup.get(child));
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

	private void buildDevPlaceholders(final Map<String, String> hashLookup, String path, List<String> children)
		throws FileNotFoundException, IOException {

		String hashPath = hashLookup.get(path);
		int slash = hashPath.lastIndexOf('/');

		// insert dev dir
		String devPath = hashPath.substring(0, slash)+"/dev"+hashPath.substring(slash);
		hashLookup.put(hashPath, devPath);

		File outputFile = new File(this.webappDir, devPath);
		if (outputFile.exists()) {
			return;
		}

		PlaceholderGenerator generator = this.placeholders.get(getExtension(hashPath));
		if (generator == null) {
			log.warning("Cannot generate placeholder for "+hashPath);
			return;
		}

		generator.build(outputFile, children);
	}

	private Map<String, List<String>> hashMergeFiles(final Map<String, String> hashLookup)
			throws IOException, NoSuchAlgorithmException {

		final int rootPrefix = this.webappDir.getCanonicalPath().length();
		final List<File> inputFiles = findFiles(this.webappDir, ".merge", this.cdnRoot);
		final Map<String, List<String>> dependencyMap = new LinkedHashMap<String, List<String>>(inputFiles.size());

		for (File inputFile : inputFiles) {
			List<String> children = new ArrayList<String>();

			String hashPath = this.cdnRoot+calcMergeHash(inputFile, children, hashLookup);

			// merge file takes the first non-empty extension
			String ext = null;
			for (String child : children) {
				ext = getExtension(child);
				if (ext != null) {
					break;
				}
			}
			if (ext == null) {
				ext = ".merge";
			}
			hashPath += ext;

			String path = inputFile.getCanonicalPath().substring(rootPrefix);
			hashLookup.put(path, hashPath);
			dependencyMap.put(path, children);
		}

		return dependencyMap;
	}

	private void hashClientFiles(final Map<String, String> hashLookup, String ext)
			throws IOException, NoSuchAlgorithmException {

		final int rootPrefix = this.webappDir.getCanonicalPath().length();
		final Compactor compactor = compactors.get(ext);
		if (compactor == null) {
			throw new IllegalArgumentException("Error: no compactor registered for "+ext);
		}
		String targetExt = compactor.getTargetExtension();
		if (targetExt == null || targetExt.indexOf('.') < 0) {
			targetExt = ext;
		}

		final List<File> inputFiles = findFiles(this.webappDir, ext, this.cdnRoot);

		for (File inputFile : inputFiles) {

			// calculate and store the hash
			String hashPath = this.cdnRoot + this.calcFileHash(inputFile) + targetExt;
			String path = inputFile.getCanonicalPath().substring(rootPrefix);
			hashLookup.put(path, hashPath);

			// ensure all the client files have been compacted
			File outputFile = new File(this.webappDir, hashPath);
			if (!outputFile.exists()) {
				// ensure compacted target path exists
				compactor.compact(inputFile, outputFile);
			}

			if (!outputFile.exists()) {
				// file still missing, remove
				log.severe(path+" failed to compact");
				hashLookup.remove(path);

			} else if (outputFile.length() < 1L) {
				// special case for files which compact to empty
				log.warning(path+" compacted to an empty file");
				hashLookup.remove(path);
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
				// http://download.oracle.com/javase/6/docs/api/java/util/Properties.html#load(java.io.Reader)
				// TODO: escape any illegal chars [:=#!\s]+
				writer.append(key).append('=').append(hashLookup.get(key)).append(newline);
			}

		} finally {
			writer.flush();
			writer.close();
		}
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
					log.warning("Missing merge reference: "+line);

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

		return path.substring(dot);
	}

	private static List<File> findFiles(File webappDir, String ext, String cdnFolder)
			throws IOException {

		final String outputPath = webappDir.getCanonicalPath()+cdnFolder;
		List<File> files = new ArrayList<File>();
		Queue<File> folders = new LinkedList<File>();
		folders.add(webappDir);

		while (!folders.isEmpty()) {
			File file = folders.poll();
			if (file.getCanonicalPath().startsWith(outputPath)) {
				// filter the output
				continue;
			}
			if (file.isDirectory()) {
				folders.addAll(Arrays.asList(file.listFiles()));
			} else if (file.getName().toLowerCase().endsWith(ext)) {
				files.add(file);
			}
		}

		return files;
	}
}
