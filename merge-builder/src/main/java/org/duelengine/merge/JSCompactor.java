package org.duelengine.merge;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

class JSCompactor extends NullCompactor {

	public JSCompactor() {
		super(".js");
	}

	@Override
	public void compact(BuildManager manager, String path, File source, File target)
			throws IOException {

		// adapted from http://blog.bolinfest.com/2009/11/calling-closure-compiler-from-java.html
		CompilerOptions options = new CompilerOptions();

		// Simple mode is used here, but additional options could be set, too.
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

		// only log warnings
		Compiler.setLoggingLevel(Level.WARNING);

		Compiler compiler = new Compiler();

		List<JSSourceFile> externs = CommandLineRunner.getDefaultExterns();
		List<JSSourceFile> inputs = Collections.singletonList(JSSourceFile.fromFile(source));

		// compile() returns a Result, but it is not needed here.
		compiler.compile(externs, inputs, options);

		FileWriter writer = new FileWriter(target, false);
		try {
			// compiler is responsible for generating the compiled code
			// it is not accessible via the Result
			String result = compiler.toSource();
			writer.append(result);

		} finally {
			writer.flush();
			writer.close();
		}
	}
}
