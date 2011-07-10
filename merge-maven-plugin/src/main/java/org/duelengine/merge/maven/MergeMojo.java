package org.duelengine.merge.maven;

import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.duelengine.merge.*;

/**
 * Generates client-side and server-side sources
 *
 * @goal merge
 * @phase process-sources
 */
public class MergeMojo extends AbstractMojo {

	// http://maven.apache.org/ref/3.0.2/maven-model/maven.html#class_build

	/**
	 * Location of the webapp.
	 * 
	 * @parameter default-value="${project.basedir}/src/main/webapp/"
	 */
	private String webappDir;

	/**
	 * Location of the generated CDN files.
	 * 
	 * @parameter default-value="/cdn/"
	 */
	private String cdnRoot;

	/**
	 * Location of the generated resources.
	 * 
	 * @parameter default-value="${project.basedir}/src/main/resources/cdn.properties"
	 */
	private String cdnMapFile;

	/**
	 * List of additional file extensions to hash and copy directly into CDN.
	 * 
	 * @parameter default-value=""
	 */
	private String cdnFiles;

	public void execute()
		throws MojoExecutionException {

		Log log = this.getLog();
		log.info("\twebappDir="+this.webappDir);
		log.info("\tcdnRoot="+this.cdnRoot);
		log.info("\tcdnMapFile="+this.cdnMapFile);

		String[] exts;
		if (this.cdnFiles != null) {
			exts = this.cdnFiles.split("[|,\\s]+");
		} else {
			exts = new String[0];
		}

		log.info("\tcdnFiles="+Arrays.toString(exts));

		MergeBuilder merger = new MergeBuilder(exts);
		merger.setWebAppDir(this.webappDir);

		if (this.cdnRoot != null && !this.cdnRoot.isEmpty()) {
			merger.setCDNRoot(this.cdnRoot);
		}

		if (this.cdnMapFile != null && !this.cdnMapFile.isEmpty()) {
			merger.setCDNMapFile(this.cdnMapFile);
		}

		try {
			merger.execute();

		} catch (Exception e) {
			log.error(e);
		}
	}
}
