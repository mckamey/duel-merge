package org.duelengine.merge.maven;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class MavenLoggerAdapterFactory implements ILoggerFactory {

	private static Log log;

	static void setMavenLogger(Log log) {
		MavenLoggerAdapterFactory.log = log;
	}

	@Override
	public Logger getLogger(String name) {
		return new MavenLoggerAdapter(name, log);
	}
}
