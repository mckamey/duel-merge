package org.duelengine.merge;

import java.io.File;
import java.io.IOException;

import org.duelengine.css.codegen.CodeGenSettings;
import org.duelengine.css.compiler.CssCompiler;

class CSSCompactor extends NullCompactor {
	public static final String CSS_EXT = ".css";
	public static final String LESS_EXT = ".less";

	private final CssCompiler compiler = new CssCompiler();
	private final CodeGenSettings settings = new CodeGenSettings();

	public CSSCompactor() {
		super(CSS_EXT, LESS_EXT);
	}

	@Override
	public String getTargetExtension(BuildManager manager, String path) {
		if (LESS_EXT.equalsIgnoreCase(BuildManager.getExtension(path))) {
			return CSS_EXT;
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
