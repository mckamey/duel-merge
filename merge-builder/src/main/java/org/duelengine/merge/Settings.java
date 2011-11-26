package org.duelengine.merge;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Settings {

	private static final String[] EMPTY = new String[0];
	private static final String DEFAULT_CDN_ROOT = "/cdn/";
	private static final String DEFAULT_MAP_FILE = "cdn.properties";

	private File cdnMapFile;
	private String cdnRoot = DEFAULT_CDN_ROOT;
	private File targetDir;
	private File sourceDir;
	private String[] extensions;

	public File getCDNDir() {
		return new File(getTargetDir(), this.cdnRoot);
	}

	public File getCDNMapFile() {
		if (this.cdnMapFile == null) {
			return new File(this.getTargetDir(), DEFAULT_MAP_FILE);
		}

		return this.cdnMapFile;
	}

	public void setCDNMapFile(String value) {
		if (value == null || value.isEmpty()) {
			this.cdnMapFile = null;
			return;
		}

		value = value.replace('\\', '/');
		if (!value.startsWith("/")) {
			value = '/'+value;
		}
		this.cdnMapFile = new File(value);
	}

	public String getCDNRoot() {
		return this.cdnRoot;
	}

	public void setCDNRoot(String value) {
		if (value == null || value.isEmpty()) {
			this.cdnRoot = DEFAULT_CDN_ROOT;
			return;
		}

		value = value.replace('\\', '/');
		if (!value.startsWith("/")) {
			value = '/'+value;
		}
		if (!value.endsWith("/")) {
			value += '/';
		}

		this.cdnRoot = value;
	}

	public String[] getExtensions() {
		return this.extensions;
	}

	public void setExtensions(String... value) {
		if (value == null) {
			this.extensions = EMPTY;
			return;
		}

		this.extensions = value;
	}

	public void setExtensionList(String value) {
		if (value == null || value.isEmpty()) {
			this.setExtensions(EMPTY);
			return;
		}

		this.setExtensions(value.split("[|,\\s]+"));
	}
	
	public File getSourceDir() {
		return this.sourceDir;
	}

	public void setSourceDir(String value) {
		if (value == null || value.isEmpty()) {
			this.sourceDir = null;
			return;
		}

		this.sourceDir = new File(value.replace('\\', '/'));
	}

	public File getTargetDir() {
		if (this.targetDir == null) {
			return this.getSourceDir();
		}

		return this.targetDir;
	}

	public void setTargetDir(String value) {
		if (value == null || value.isEmpty()) {
			this.targetDir = null;
			return;
		}

		this.targetDir = new File(value.replace('\\', '/'));
	}

	//----------------

	File getTargetFile(String targetPath) {
		return new File(getTargetDir(), targetPath);
	}

	File findSourceFile(String path) {
		File source = new File(getTargetDir()+path);
		if (source.exists()) {
			return source;
		}

		return new File(getSourceDir()+path);
	}

	Map<File, String> findFiles(Set<String> extensions)
		throws IOException {

		Map<File, String> files = new LinkedHashMap<File, String>();

		findFiles(files, extensions, getSourceDir());
		findFiles(files, extensions, getTargetDir());

		return files;
	}

	private void findFiles(Map<File, String> files, Set<String> extensions, File searchDir)
			throws IOException {

		final String cdnPath = getCDNDir().getCanonicalPath();
		final int rootPrefix = searchDir.getCanonicalPath().length();
		Queue<File> folders = new LinkedList<File>();

		folders.add(searchDir);
		while (!folders.isEmpty()) {
			File file = folders.remove();

			if (file.getCanonicalPath().startsWith(cdnPath)) {
				// filter CDN output files
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
}
