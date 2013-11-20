package com.ryaltech.weblogic.probe;


import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;



@Aspect
public class ClusterMemberMarkingAspect {
	public static final String HEADER_NAME = "weblogic.Name";
	private String serverName = System.getProperty("weblogic.Name");

	private static Logger logger = Logger.getLogger(ClusterMemberMarkingAspect.class);
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

	
	@After("execution(void weblogic.servlet.internal.ServletStubImpl.execute(javax.servlet.ServletRequest, javax.servlet.ServletResponse))&&args(request, response)")
	public void addNodeToResponse(ServletRequest request, ServletResponse response)
			throws Throwable {
		if(response instanceof HttpServletResponse){
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			httpResponse.setHeader(HEADER_NAME, serverName);
			
		}

	}
}
