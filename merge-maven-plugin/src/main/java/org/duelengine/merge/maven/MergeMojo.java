package org.duelengine.merge.maven;

import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.duelengine.merge.Settings;
import org.duelengine.merge.BuildManager;

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

		Settings settings = new Settings();
		settings.setSourceDir(this.webappDir);
		settings.setTargetDir(this.outputDir);
		settings.setCDNMapFile(this.resourcesDir+this.cdnMapFile);
		settings.setCDNRoot(this.cdnRoot);
		settings.setExtensionList(this.cdnFiles);

		Log log = this.getLog();

		log.info("\tsourceDir="+settings.getSourceDir());
		log.info("\ttargetDir="+settings.getTargetDir());
		log.info("\tcdnMapFile="+settings.getCDNMapFile());
		log.info("\tcdnRoot="+settings.getCDNRoot());
		log.info("\tcdnFiles="+Arrays.toString(settings.getExtensions()));

		try {
			new BuildManager(settings).execute();

		} catch (Exception ex) {
			log.error(ex);
		}
	}
}
