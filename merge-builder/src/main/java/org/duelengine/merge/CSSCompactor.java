package org.duelengine.merge;

import java.io.*;
import java.util.Map;

import org.cssless.css.codegen.CodeGenSettings;
import org.cssless.css.compiler.*;

class CSSCompactor implements Compactor {

	private final CssCompiler compiler = new CssCompiler();
	private final CodeGenSettings settings = new CodeGenSettings();

	@Override
	public String[] getSourceExtensions() {
		return new String[] { ".css", ".less" };
	}

	@Override
	public String getTargetExtension() {
		return ".css";
	}

	@Override
	public void compact(Map<String, String> fileHashes, File source, File target) throws IOException {
		target.getParentFile().mkdirs();
		this.compiler.process(
			source,
			target,
			this.settings,
			new LinkInterceptorCssFilter(fileHashes));
	}
}
