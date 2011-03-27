package org.duelengine.merge.maven;

import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
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
	 * The project currently being built.
	 * 
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Location of the client-side source files.
	 * 
	 * @parameter default-value="${project.build.sourceDirectory}"
	 */
	private String inputFolder;

	/**
	 * Location of the generated client-side files.
	 * 
	 * @parameter default-value="${project.build.sourceDirectory}/../webapp/cdn/"
	 */
	private String outputClientFolder;

	/**
	 * Location of the generated resources.
	 * 
	 * @parameter default-value="${project.build.sourceDirectory}/../resources/merge/cdn.properties"
	 */
	private String outputResourceFile;

    public void execute()
        throws MojoExecutionException {

	    Log log = this.getLog();
	    log.info("\tinputFolder="+this.inputFolder);
	    log.info("\toutputClientFolder="+this.outputClientFolder);
	    log.info("\toutputResourceFile="+this.outputResourceFile);

	    MergeBuilder merger = new MergeBuilder();
	    merger.setInputFolder(this.inputFolder);

	    if (this.outputClientFolder != null && !this.outputClientFolder.isEmpty()) {
		    merger.setOutputClientFolder(this.outputClientFolder);
	    }

		if (this.outputResourceFile != null && !this.outputResourceFile.isEmpty()) {
			merger.setOutputResourceFile(this.outputResourceFile);
		}

	    try {
		    merger.execute();

//		    this.project.addCompileSourceRoot(merger.getOutputResourceFile()); 

	    } catch (Exception e) {
		    log.error(e);
	    }
    }
}
