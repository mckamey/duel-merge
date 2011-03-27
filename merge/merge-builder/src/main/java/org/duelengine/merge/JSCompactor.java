package org.duelengine.merge;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

class JSCompactor implements Compactor {

	@Override
	public String[] getSourceExtensions() {
		return new String[] { ".js" };
	}

	@Override
	public String getTargetExtension() {
		return ".js";
	}

	@Override
	public void compact(File source, File target) throws IOException {

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

		// compiler is responsible for generating the compiled code
		// it is not accessible via the Result
		String result = compiler.toSource();

		target.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(target, false);
		try {
			writer.append(result);
		} finally {
			writer.flush();
			writer.close();
		}
	}
}
