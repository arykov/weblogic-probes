package com.ryaltech.weblogic.probe;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect was developed to log when problem described in mybatis spring bug
 * 18 {@link https://github.com/mybatis/spring/issues/18} occurs
 * 
 * @author rykov
 * 
 */
@Aspect
public class ExecuteThreadMyBatisLeakDetectAspect {

	private static Logger logger = Logger
			.getLogger(ExecuteThreadMyBatisLeakDetectAspect.class);

	private static void log(String msg, Object... objects) {
		logger.info(String.format(msg, objects));

	}

	private static void error(String msg, Object... objects) {
		logger.error(String.format(msg, objects));

	}

	private static void debug(String msg, Object... objects) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(msg, objects));

		}

	}

	interface ThreadLocalFoundCallback {
		void threadLocalFound(ThreadLocal<?> threadLocal);
	}

	private void iterateCurrentThreadLocal(ThreadLocalFoundCallback callback) {
		try {
			// Get a reference to the thread locals table of the current thread
			Thread thread = Thread.currentThread();
			Field threadLocalsField = Thread.class
					.getDeclaredField("threadLocals");
			threadLocalsField.setAccessible(true);
			Object threadLocalTable = threadLocalsField.get(thread);

			// Get a reference to the array holding the thread local variables
			// inside the
			// ThreadLocalMap of the current thread
			Class<?> threadLocalMapClass = Class
					.forName("java.lang.ThreadLocal$ThreadLocalMap");
			Field tableField = threadLocalMapClass.getDeclaredField("table");
			tableField.setAccessible(true);
			if (threadLocalTable == null) {
				debug("threadLocalTable is null.");
				return;
			}
			Object table = tableField.get(threadLocalTable);

			// The key to the ThreadLocalMap is a WeakReference object. The
			// referent field of this object
			// is a reference to the actual ThreadLocal variable
			Field referentField = Reference.class.getDeclaredField("referent");
			referentField.setAccessible(true);

			for (int i = 0; i < Array.getLength(table); i++) {
				// Each entry in the table array of ThreadLocalMap is an Entry
				// object
				// representing the thread local reference and its value
				Object entry = Array.get(table, i);
				if (entry != null) {
					// Get a reference to the thread local object and remove it
					// from the table
					ThreadLocal<?> threadLocal = (ThreadLocal<?>) referentField
							.get(entry);
					callback.threadLocalFound(threadLocal);

				}
			}
		} catch (Exception e) {
			// We will tolerate an exception here and just log it
			throw new IllegalStateException(e);
		}
	}

	@After("execution(void weblogic.work.ExecuteThread.execute(..))")
	public void logAndCleanThreadLocal() throws Throwable {
		debug("logAndCleanThreadLocal started");
		iterateCurrentThreadLocal(new ThreadLocalFoundCallback() {
			@Override
			public void threadLocalFound(ThreadLocal<?> threadLocal) {
				if ((threadLocal != null)
						&& "Transactional resources".equals(threadLocal
								.toString())) {
					debug("Thread local %s", threadLocal);
					if (logger.isDebugEnabled()) {
						Map map = (Map) threadLocal.get();
						if (map != null) {
							for (Object value : map.values()) {
								if (value != null
										&& value.getClass()
												.toString()
												.contains(
														"org.mybatis.spring.SqlSessionHolder")) {
									error("TRANSACTIONAL RESOURCES WERE NOT CLEANED UP. CLASSLOADER: "
											+ value.getClass().getClassLoader()
													.toString());
								}
							}
						}
					}
					debug("Thread local %s removed", threadLocal);
					threadLocal.remove();
				}

			}
		});

		debug("logAndCleanThreadLocal finished");

	}
}
