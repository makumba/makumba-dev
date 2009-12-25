package org.makumba.controller.http;
import javax.servlet.*;
import javax.servlet.http.*;
import org.makumba.*;
import java.net.URL;
import java.util.Hashtable;
import java.util.Date;

/** The filter that controls each makumba HTTP access. Performs login, form response, exception handling. */
public class ControllerFilter implements Filter
{
  public static final String ORIGINAL_REQUEST="org.makumba.originalRequest";

  static FilterConfig conf;
  public void init(FilterConfig c) { conf=c; }
  
  /** the filtering method, basically puts some wrappers around the request and the response */
  public void doFilter(ServletRequest req, ServletResponse resp,
		       FilterChain chain)
        throws ServletException, java.io.IOException
  {
    boolean filter= shouldFilter((HttpServletRequest)req);
      if(filter){
	try{
	  RequestAttributes.getAttributes((HttpServletRequest)req);
	}
	catch(Throwable e)
	  { 
	    treatException(e, (HttpServletRequest)req, (HttpServletResponse)resp);
	    return; 
	  }
	
	Responder.response((HttpServletRequest)req, (HttpServletResponse)resp);
	if(wasException((HttpServletRequest)req))
	  return;
      }
      try{
	  chain.doFilter(req, resp);
      }catch(AllowedException e)
	  { }
  }

  /** decide if we filter or not */
  public boolean shouldFilter(HttpServletRequest req)
  {
    String uri= req.getRequestURI();   
    if(uri.startsWith("/dataDefinitions")|| uri.startsWith("/logic") || uri.startsWith("/classes"))
	return false;
    String file=null;
    try{
      file= new URL(req.getRequestURL().toString()).getFile();
    }catch(java.net.MalformedURLException e) { } // can't be

    // JSP and HTML are always filtered
    if(file.endsWith(".jsp") || file.endsWith(".html"))
	return true;

    // JSPX is never filtered
    if(file.endsWith(".jspx"))
	return false;

    // we compute the file that corresponds to the indicated path
    java.io.File f= new java.io.File(conf.getServletContext().getRealPath(req.getRequestURI()));    
    
    // if it's a directory, there will most probably be a redirection, we filter anyway
    if(f.isDirectory())
	return true;
    
    // if the file does not exist on disk, it means that it's produced dynamically, so we filter
    // it it exists, it's probably an image or a CSS, we don't filter
    return !f.exists();
  }

  public void destroy(){}
  

  //------------- treating exceptions ------------------
  /** treat an exception that occured during the request */
  static public void treatException(Throwable t, HttpServletRequest req, HttpServletResponse resp) 
  {
    resp.setContentType("text/html");
    req.setAttribute(javax.servlet.jsp.PageContext.EXCEPTION, t);
    if(req.getAttribute("org.makumba.exceptionTreated")==null && !((t instanceof UnauthorizedException) && login(req, resp)))
      {
	try{
	  req.getRequestDispatcher("/servlet/org.makumba.devel.TagExceptionServlet").forward(req, resp); 
	}catch(Throwable q){ q.printStackTrace(); throw new MakumbaError(q); }
      }
    setWasException(req);
    req.setAttribute("org.makumba.exceptionTreated", "yes");
  }

  /** signal that there was an exception during the request, so some operations can be skipped */
  public static void setWasException(HttpServletRequest req)
  {
    req.setAttribute("org.makumba.wasException", "yes");
  }

  /** test if there was an exception during the request */
  public static boolean wasException(HttpServletRequest req)
  {
    return "yes".equals(req.getAttribute("org.makumba.wasException"));
  }

  //---------------- login ---------------
  /** compute the login page from a servletPath */
  public static String getLoginPage(String servletPath)
  {
    String root= conf.getServletContext().getRealPath("/");    
    String virtualRoot="/";
    String login="/login.jsp";
    
    java.util.StringTokenizer st= new java.util.StringTokenizer(servletPath, "/");
    while(st.hasMoreElements())
      {
        if(new java.io.File(root+"login.jsp").exists())
	    login=virtualRoot+"login.jsp"; 
	String s=st.nextToken()+"/";
	root+=s;
	virtualRoot+=s;
      }
    if(new java.io.File(root+"login.jsp").exists())
	login=virtualRoot+"login.jsp"; 
    return login;
  }

  /** find the closest login.jsp and forward to it */
  protected static boolean login(HttpServletRequest req, HttpServletResponse resp)
  {
    // the request path may be modified by the filter, we take it as is
    String login= getLoginPage(req.getServletPath());

    if(login==null)
      return false;

    // we will forward to the login page using the original request
    while(req instanceof HttpServletRequestWrapper)
      req=(HttpServletRequest)((HttpServletRequestWrapper) req).getRequest();

    req.setAttribute(ORIGINAL_REQUEST, req);

    try{
      req.getRequestDispatcher(login).forward(req, resp);
    }catch(Throwable q){ q.printStackTrace(); return false; }
    return true;
  }
}