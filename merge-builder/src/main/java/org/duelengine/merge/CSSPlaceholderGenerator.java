package org.duelengine.merge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSSPlaceholderGenerator implements PlaceholderGenerator {

	@Override
	public String getTargetExtension() {
		return ".css";
	}

	@Override
	public void build(BuildManager manager, File target, List<String> children) throws IOException {
		String nocache = '?'+target.getName();
		int dot = target.getName().lastIndexOf('.');
		if (dot > 0) {
			nocache = nocache.substring(0, dot);
		}

		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);

		try {
			writer.append("/* simulate semantics of merged stylesheets but allow debugging of original files; append anti-caching suffix */\n");

			// concatenate references to children
			for (String child : children) {
				child = manager.getPlaceholderPath(child);

				// insert child files into outputFile
				writer
					.append("@import url(")
					.append(child)
					.append(nocache)
					.append(");\n");
			}

		} finally {
			writer.flush();
			writer.close();
		}
	}
}
