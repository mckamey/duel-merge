package org.duelengine.merge.maven;

import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.duelengine.merge.MergeBuilder;

/**
 * Generates client-side and server-side sources
 *
 * @goal merge
 * @phase generate-sources
 */
public class MergeMojo extends AbstractMojo {

	// http://maven.apache.org/ref/3.0.3/maven-model/maven.html#class_build

	/**
	 * Directory containing the webapp source.
	 * 
	 * @parameter default-value="${project.basedir}/src/main/webapp/"
	 * @readonly
	 * @required
	 */
	private String webappDir;

	/**
	 * Directory where webapp is output..
	 * 
	 * @parameter default-value="${project.build.directory}/${project.build.finalName}/"
	 * @readonly
	 * @required
	 */
	private String outputDir;

	/**
	 * File path of the generated resource map.
	 * 
	 * @parameter default-value="${project.build.outputDirectory}"
	 * @readonly
	 * @required
	 */
	private String resourcesDir;

	/**
	 * URL root path of CDN output.
	 * 
	 * @parameter default-value="/cdn/"
	 */
	private String cdnRoot;

	/**
	 * File name of the generated resource map.
	 * 
	 * @parameter default-value="cdn.properties"
	 */
	private String cdnMapFile;

	/**
	 * List of additional file extensions to hash and copy directly into CDN.
	 * 
	 * @parameter default-value=""
	 */
	private String cdnFiles;

	@Override
	public void setLog(Log log) {
		super.setLog(log);

		MavenLoggerAdapterFactory.setMavenLogger(log);
	};
	
	public void execute()
		throws MojoExecutionException {
		
		Log log = this.getLog();

		log.info("\twebappDir="+this.webappDir);
		log.info("\toutputDir="+this.outputDir);

		if (this.cdnMapFile == null || this.cdnMapFile.isEmpty()) {
			this.cdnMapFile = "/cdn.properties";
		} else if (!this.cdnMapFile.startsWith("/")) {
			this.cdnMapFile = '/'+this.cdnMapFile;
		}
		log.info("\tcdnMapFile="+this.resourcesDir+this.cdnMapFile);
		log.info("\tcdnRoot="+this.cdnRoot);

		String[] exts;
		if (this.cdnFiles != null) {
			exts = this.cdnFiles.split("[|,\\s]+");
		} else {
			exts = new String[0];
		}

		log.info("\tcdnFiles="+Arrays.toString(exts));

		MergeBuilder merger = new MergeBuilder(exts);
		merger.setWebAppDir(this.webappDir);
		merger.setOutputDir(this.outputDir);

		if (this.cdnRoot != null && !this.cdnRoot.isEmpty()) {
			merger.setCDNRoot(this.cdnRoot);
		}

		merger.setCDNMapFile(this.resourcesDir+this.cdnMapFile);

		try {
			merger.execute();

		} catch (Exception e) {
			log.error(e);
		}
	}
}
