package org.duelengine.merge;

import java.io.*;

import org.cssless.css.compiler.*;

class CSSCompactor implements Compactor {

	private final CssCompiler compiler = new CssCompiler();

	@Override
	public String[] getSourceExtensions() {
		return new String[] { ".css", ".less" };
	}

	@Override
	public String getTargetExtension() {
		return ".css";
	}

	@Override
	public void compact(File source, File target) throws IOException {
		target.getParentFile().mkdirs();
		this.compiler.process(source, target);
	}
}
