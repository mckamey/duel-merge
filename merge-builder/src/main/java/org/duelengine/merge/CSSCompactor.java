package org.duelengine.merge;

import java.io.File;
import java.io.IOException;

import org.cssless.css.codegen.CodeGenSettings;
import org.cssless.css.compiler.CssCompiler;

class CSSCompactor extends NullCompactor {

	private final CssCompiler compiler = new CssCompiler();
	private final CodeGenSettings settings = new CodeGenSettings();

	public CSSCompactor() {
		super(".css", ".less");
	}

	@Override
	public String getTargetExtension(BuildManager manager, String path) {
		if (".less".equals(BuildManager.getExtension(path))) {
			return ".css";
		}

		return super.getTargetExtension(manager, path);
	}

	@Override
	public void compact(BuildManager manager, String path, File source, File target)
			throws IOException {

		this.compiler.process(
			source,
			target,
			this.settings,
			new LinkInterceptorCssFilter(manager, path));
	}
}
