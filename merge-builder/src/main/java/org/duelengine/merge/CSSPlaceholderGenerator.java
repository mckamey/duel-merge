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
	public void build(File target, List<String> children) throws IOException {
		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);

		try {
			writer.append("/* simulate semantics of merged stylesheets but allow debugging of original files */\n");

			// concatenate references to children
			for (String child : children) {
				// insert child files into outputFile
				writer
					.append("@import url(")
					.append(child)
					.append(");\n");
			}

		} finally {
			writer.flush();
			writer.close();
		}
	}
}
