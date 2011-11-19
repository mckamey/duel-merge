package org.duelengine.merge.maven;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.slf4j.helpers.MarkerIgnoringBase;

@SuppressWarnings("serial")
public class MavenLoggerAdapter extends MarkerIgnoringBase {

	private final Log log;

	public MavenLoggerAdapter(String name, Log log) {
		this.name = name;
		this.log = (log != null) ? log : new SystemStreamLog();
	}

	protected Log getLog() {
		return log;
	}
	
	@Override
	public void debug(String msg) {
		getLog().debug(msg);
	}

	@Override
	public void debug(String format, Object arg1) {
		debug(String.format(format, arg1));
	}

	@Override
	public void debug(String format, Object[] arg1) {
		debug(String.format(format, arg1));
	}

	@Override
	public void debug(String msg, Throwable t) {
		getLog().debug(msg, t);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		debug(String.format(format, arg1, arg2));
	}

	@Override
	public void error(String msg) {
		getLog().error(msg);
	}

	@Override
	public void error(String format, Object arg1) {
		error(String.format(format, arg1));
	}

	@Override
	public void error(String format, Object[] arg1) {
		error(String.format(format, arg1));
	}

	@Override
	public void error(String msg, Throwable t) {
		getLog().error(msg, t);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		error(String.format(format, arg1, arg2));
	}

	@Override
	public void info(String msg) {
		getLog().info(msg);
	}

	@Override
	public void info(String format, Object arg1) {
		info(String.format(format, arg1));
	}

	@Override
	public void info(String format, Object[] arg1) {
		info(String.format(format, arg1));
	}

	@Override
	public void info(String msg, Throwable t) {
		getLog().info(msg, t);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		info(String.format(format, arg1, arg2));
	}

	@Override
	public boolean isDebugEnabled() {
		return getLog().isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return getLog().isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return getLog().isInfoEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		return getLog().isWarnEnabled();
	}

	@Override
	public void trace(String msg) {
		// NOOP
	}

	@Override
	public void trace(String msg, Object arg1) {
		// NOOP
	}

	@Override
	public void trace(String msg, Object[] arg1) {
		// NOOP
	}

	@Override
	public void trace(String msg, Throwable t) {
		// NOOP
	}

	@Override
	public void trace(String msg, Object arg1, Object arg2) {
		// NOOP
	}

	@Override
	public void warn(String msg) {
		getLog().warn(msg);
	}

	@Override
	public void warn(String format, Object arg1) {
		warn(String.format(format, arg1));
	}

	@Override
	public void warn(String format, Object[] arg1) {
		warn(String.format(format, arg1));
	}

	@Override
	public void warn(String msg, Throwable t) {
		getLog().warn(msg, t);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		warn(String.format(format, arg1, arg2));
	}
}
