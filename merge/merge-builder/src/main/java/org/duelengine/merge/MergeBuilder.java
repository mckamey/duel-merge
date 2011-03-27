package org.duelengine.merge;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.Logger;

public class MergeBuilder {

	private static final String HELP =
		"Usage:\n" +
		"\tjava -jar merge.jar <input-file|input-folder>\n" +
		"\tjava -jar merge.jar <input-file|input-folder> <output-folder>\n" +
		"\tjava -jar merge.jar <input-file|input-folder> <output-client-folder> <output-resource-file>\n\n"+
		"\tinput-file: path to the Merge input file (e.g. foo.merge)\n"+
		"\tinput-folder: path to the input folder containing Merge files\n"+
		"\toutput-folder: path to the output folder\n"+
		"\toutput-client-folder: path to the client-script folder\n"+
		"\toutput-resource-file: path to the generated resource file\n";

	private static final int BUFFER_SIZE = 4096;
	private static final String HASH_ALGORITHM = "SHA-1";
	private static final String CHAR_ENCODING = "utf-8";

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println(HELP);
			return;
		}

		MergeBuilder builder = new MergeBuilder();
		builder.setInputFolder(args[0]);

		if (args.length > 1) {
			builder.setOutputClientFolder(args[1]);

			if (args.length > 2) {
				builder.setOutputResourceFile(args[2]);
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
	private File inputFolder;
	private File outputClientFolder;
	private File outputResourceFile;

	public MergeBuilder() {
		this(Arrays.asList(new JSPlaceholderGenerator(), new CSSPlaceholderGenerator()),
			Arrays.asList(new JSCompactor(), new CSSCompactor()));
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

	public String getInputFolder() {
		return this.inputFolder.getAbsolutePath();
	}

	public void setInputFolder(String value) {
		this.inputFolder = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	public String getOutputClientFolder() {
		return this.outputClientFolder.getAbsolutePath();
	}

	public void setOutputClientFolder(String value) {
		this.outputClientFolder = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	public String getOutputResourceFile() {
		return this.outputResourceFile.getAbsolutePath();
	}

	public void setOutputResourceFile(String value) {
		this.outputResourceFile = (value != null) ? new File(value.replace('\\', '/')) : null;
	}

	private boolean ensureSettings() {
		if (this.inputFolder == null || !this.inputFolder.exists()) {
			throw new IllegalArgumentException("Error: no input files found in "+this.inputFolder);
		}

		if (this.outputClientFolder == null) {
			this.outputClientFolder = this.inputFolder.getParentFile();
		}

		if (this.outputResourceFile == null) {
			this.outputResourceFile = new File(this.inputFolder.getParentFile(), "cdn.properties");
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
			throw new IllegalArgumentException("Error: no input files found in "+this.inputFolder);
		}

		// calculate hash for all the merge files and determine dependencies
		Map<String, List<String>> dependencyMap = this.hashMergeFiles(hashLookup);

		for (String path : dependencyMap.keySet()) {
			List<String> children = dependencyMap.get(path);

			this.buildMerge(hashLookup, path, children);
			this.buildDevPlaceholders(hashLookup.get(path), children);
		}

		saveHashLookup(hashLookup);
	}

	private void buildMerge(final Map<String, String> hashLookup, String path, List<String> children)
		throws FileNotFoundException, IOException {

		log.info("Building "+path);
		String outputPath = hashLookup.get(path);

		File outputFile = new File(this.outputClientFolder, outputPath);
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
				File inputFile = new File(this.outputClientFolder, hashLookup.get(child));
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

	private void buildDevPlaceholders(String outputPath, List<String> children)
		throws FileNotFoundException, IOException {

		File outputFile = new File(this.outputClientFolder, "dev/"+outputPath);
		if (outputFile.exists()) {
			return;
		}

		PlaceholderGenerator generator = this.placeholders.get(this.getExtension(outputPath));
		if (generator == null) {
			log.warning("Cannot generate placeholder for "+outputPath);
			return;
		}

		generator.build(outputFile, children);
	}

	private Map<String, List<String>> hashMergeFiles(final Map<String, String> hashLookup)
			throws IOException, NoSuchAlgorithmException {

		final int rootPrefix = this.inputFolder.getCanonicalPath().length();
		final List<File> inputFiles = findFiles(this.inputFolder, ".merge", this.outputClientFolder);
		final Map<String, List<String>> dependencyMap = new LinkedHashMap<String, List<String>>(inputFiles.size());

		for (File inputFile : inputFiles) {
			List<String> children = new ArrayList<String>();

			String hashPath = calcMergeHash(inputFile, children, hashLookup);

			// merge file takes the first non-empty extension
			String ext = null;
			for (String child : children) {
				ext = this.getExtension(child);
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

		final int rootPrefix = this.inputFolder.getCanonicalPath().length();
		final Compactor compactor = compactors.get(ext);
		if (compactor == null) {
			throw new IllegalArgumentException("Error: no compactor registered for "+ext);
		}

		final List<File> inputFiles = findFiles(this.inputFolder, ext, this.outputClientFolder);

		for (File inputFile : inputFiles) {

			// calculate and store the hash
			String hashPath = this.calcFileHash(inputFile) + compactor.getTargetExtension();
			String path = inputFile.getCanonicalPath().substring(rootPrefix);
			hashLookup.put(path, hashPath);

			// ensure all the client files have been compacted
			File outputFile = new File(this.outputClientFolder, hashPath);
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

		this.outputResourceFile.getParentFile().mkdirs();

		final String newline = System.getProperty("line.separator");
		FileWriter writer = new FileWriter(this.outputResourceFile, false);
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

			return this.encodeBytes(sha1.digest());

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

			return this.encodeBytes(sha1.digest());

		} finally {
			stream.close();
		}
	}

	private String encodeBytes(byte[] digest) {
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

	private String getExtension(String path) {
		int dot = path.lastIndexOf('.');
		if (dot < 0) {
			return "";
		}

		return path.substring(dot);
	}

	private static List<File> findFiles(File inputFolder, String ext, File outputFolder)
			throws IOException {

		final String outputPath = outputFolder.getCanonicalPath();
		List<File> files = new ArrayList<File>();
		Queue<File> folders = new LinkedList<File>();
		folders.add(inputFolder);

		while (!folders.isEmpty()) {
			File file = folders.poll();
			if (file.getCanonicalPath().startsWith(outputPath)) {
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
