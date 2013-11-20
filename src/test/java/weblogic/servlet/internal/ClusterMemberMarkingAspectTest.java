package weblogic.servlet.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import com.ryaltech.weblogic.probe.ClusterMemberMarkingAspect;

public class ClusterMemberMarkingAspectTest {

	static final String SERVER_NAME = "SERVER_NAME";
	HttpServletResponse httpResponse;
	ServletResponse response;
	@Before
	public void before(){
		System.setProperty("weblogic.Name", SERVER_NAME);
		httpResponse = createMock(HttpServletResponse.class);
		response = createMock(ServletResponse.class);
	}
	@Test
	public void testHappyPath() {
		httpResponse.setHeader(ClusterMemberMarkingAspect.HEADER_NAME, SERVER_NAME);
		replay(httpResponse);
		new ServletStubImpl().execute(null, httpResponse);;
		verify(httpResponse);
	}
	
	@Test
	public void testWrongParamTypes() {	
		
		replay(response);
		new ServletStubImpl().execute(null, response);;
		
		verify(response);
	}

	@Test
	public void testWrongMethod() {
		
		replay(httpResponse);
		new ServletStubImpl().execute(null, httpResponse, 10);;
		verify(httpResponse);
	}
	
}
