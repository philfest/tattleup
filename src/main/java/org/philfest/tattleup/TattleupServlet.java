package org.philfest.tattleup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.windup.WindupEnvironment;

@WebServlet(urlPatterns = "/WindupInput")
@MultipartConfig
public class TattleupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(TattleupServlet.class);
    
    private static final String INPUT_DIR_NAME = "input";
    
    private static final String OUTPUT_DIR_NAME = "output";
    
    private static final String REPORT_DIR_NAME = "report";
    
    private static final String TATTLETALE_DIR_NAME = "tattletale";
    
    private static final String WINDUP_DIR_NAME = "windup";

    public TattleupServlet() {
    	
        super();
        
    }  

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
          throws ServletException, IOException {
    	
    	String responseMesssage = null;
         
    	//String realPath =  this.getServletContext().getRealPath("/");
    	
    	String fsBaseDir =  System.getenv("OPENSHIFT_DATA_DIR");
           
    	if (StringUtils.isBlank(fsBaseDir)) {
    		
    			logger.warn("OpenShift environment variable OPENSHIFT_DATA_DIR not found. Using ServletContext to get path.");
    			
    			fsBaseDir = this.getServletContext().getRealPath("/");
    			
    	}
    	
    	logger.info("realPath="+fsBaseDir);
    	
    	String fileName = null;
        
        String appName = null;
        
        String archive = null;
        
        try{
            
	        for (Part part : request.getParts()) {
	          
	            fileName = getFileName(part);
	          
	              if(fileName != null && !fileName.isEmpty()){
	                  
	            	  // Get app name request parm. Append timestamp to it for uniqueness
	                  appName = request.getParameter("appName") + "-" + TattleupHelper.nowToString();               
	                  
	                  // Convert uploaded archive to byte[]. Write it to file and save the path.
	                  //inputPath = saveArchive(realPath + INPUT + File.separator + appName, fileName, getArchiveAsBytes(request, part.getName()));
	                  archive = TattleupHelper.saveArchive(fsBaseDir + INPUT_DIR_NAME + File.separator + appName, fileName, getArchiveAsBytes(request, part.getName()));

	                  
	              }
	          
	        } 
                                   	
        	String outputBaseDir = fsBaseDir + OUTPUT_DIR_NAME + File.separator + appName + File.separator;
        	
        	boolean doWindup = BooleanUtils.toBoolean(request.getParameter("doWindup"));
        	
        	boolean doTattletale = BooleanUtils.toBoolean(request.getParameter("doTattletale"));
        	
        	if (doWindup) doWindup(request, archive, outputBaseDir + WINDUP_DIR_NAME);
        
        	if (doTattletale) doTattletale(request, archive, outputBaseDir, fsBaseDir + INPUT_DIR_NAME + File.separator + appName);
        	
        	responseMesssage = zipOutput(fsBaseDir, appName);
        	
        	cleanupAll(outputBaseDir, fsBaseDir + INPUT_DIR_NAME + File.separator + appName);
        	
        	//responseMesssage = "Done! Please download your <a href='" + REPORT + "/" + appName + ".zip'>results here</a>.";

                      
        } catch (Exception e){
        	
        	logger.error("Could not process.", e);
        	
        	responseMesssage = "Sorry. Your request could not be processed: " + e.getMessage();
        	
        }
        
    	writeResponse(response, responseMesssage, appName);
                    
    } 
    
    
    public void doTattletale(HttpServletRequest request, String source, String baseDir, String propsDir) throws Exception {
    	
    	String propsFile = processTattletaleRequest(request, propsDir);
    	
    	TattleupHelper.doTattletale(propsFile, source, baseDir, propsDir);

    	
    }
    
    
    public void doWindup(HttpServletRequest request, String inputPath, String output) 
    		throws IOException{
    	
    	WindupEnvironment settings = processWindupRequest(request);
    	
    	TattleupHelper.doWindup(settings, inputPath, output);
        
    }
    
    
    private String zipOutput(String realPath, String appName) throws Exception{
    	
    	String zipfile = realPath + REPORT_DIR_NAME + File.separator + appName + ".zip";
    	
    	TattleupHelper.createZip(realPath + OUTPUT_DIR_NAME + File.separator + appName, zipfile);
    	
    	return zipfile;
    	
    }
    
   
   
    
    public void writeResponse(HttpServletResponse response, String message, String appName) throws IOException{
    	
    	appName += ".zip";
    	
     	response.setContentType("application/zip");  
       	 
       	response.setHeader("Content-Disposition","attachment;filename=\"" + appName + "\"");
    	
       	File f = new File(message);  
        
       	byte[] arBytes = new byte[(int)f.length()];  
        
       	response.setContentLength(arBytes.length);
        
       	FileInputStream is = new FileInputStream(f);  
        
       	is.read(arBytes);  
        
       	ServletOutputStream op = response.getOutputStream();  
        
       	op.write(arBytes);  
        
       	op.flush(); 
    	
    }
    
    
    public String processTattletaleRequest(HttpServletRequest request, String outputBaseDir) throws Exception{
    	
    	Properties props = new Properties();
    	
    	String file = outputBaseDir + File.separator + "jboss-tattletale.properties";
    	
    	/*
    	if (StringUtils.isNotBlank(request.getParameter("classloader"))) {
    		
    		props.setProperty("classloader", request.getParameter("classloader"));
    		
        }
        */
    	
    	if (!ArrayUtils.isEmpty(request.getParameterValues("profiles"))) {
    		
    		String profiles = TattleupHelper.joinStrings(request.getParameterValues("profiles"), null);
    		
    		props.setProperty("profiles", profiles);
    		
        }
    	
    	if (!ArrayUtils.isEmpty(request.getParameterValues("reports"))) {
    		
    		String reports = TattleupHelper.joinStrings(request.getParameterValues("reports"), null);
    		
    		props.setProperty("reports", reports);
    		
        }
    	
    	
    	if (!ArrayUtils.isEmpty(request.getParameterValues("scans"))) {
    		
    		String scans = TattleupHelper.joinStrings(request.getParameterValues("scans"), null);
    		
    		props.setProperty("scan", scans);
    		
        }
    	
    	
    	/*
    	if (StringUtils.isNotBlank(request.getParameter("excludes"))) {
    		
    		props.setProperty("excludes", request.getParameter("excludes"));
    		
        }
        */
    	
    	/*
    	if (StringUtils.isNotBlank(request.getParameter("blacklisted"))) {
    		
    		props.setProperty("blacklisted", request.getParameter("blacklisted"));
    		
        }
        */

    	
    	/*
    	if (StringUtils.isNotBlank(request.getParameter("enableDot"))) {
    		
    		props.setProperty("enableDot", request.getParameter("enableDot"));
    		
        }
    	
    	if (StringUtils.isNotBlank(request.getParameter("graphvizDot"))) {
    		
    		props.setProperty("graphvizDot", request.getParameter("graphvizDot"));
    		
        }  	
        */
    	
    	logger.info("TATTLETALE PROPS: " + props.toString());
    	
    	props.store(new FileWriter(file), null);  	
    	
    	return file;
    	 	
    }

    
    public WindupEnvironment processWindupRequest(HttpServletRequest request) {
        
            // Grab settings from  request. Some of these may or may not be 
            // text fields w/in the form so do sanity check first
            WindupEnvironment settings = new WindupEnvironment();

            if (StringUtils.isNotBlank(request.getParameter("javaPkgs"))) {
                settings.setPackageSignature(request.getParameter("javaPkgs"));
            } 

            if (StringUtils.isNotBlank(request.getParameter("fetchRemote")) ) {
                settings.setFetchRemote(request.getParameter("fetchRemote"));
            }
            
            /*
            settings.setLogLevel("info");
            if (StringUtils.isNotBlank(request.getParameter("logLevel"))) {
                settings.setLogLevel(request.getParameter("logLevel"));
            }
                        
            boolean captureLog = false; 
            if (BooleanUtils.toBoolean(request.getParameter("captureLog"))) {
               captureLog = true; 
            }
            settings.setCaptureLog(captureLog);
            
            */
            
            logger.info(settings.toString());
 
            return settings;
    }

    
    private String getFileName(Part part) {

        for (String cd : part.getHeader("content-disposition").split(";")) {

            if (cd.trim().startsWith("filename")) {

                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");

            }

        }

        return null;

    }
    
    
    private byte[] getArchiveAsBytes(HttpServletRequest request, String partName) throws IOException, ServletException{
        
        InputStream is = request.getPart(partName).getInputStream();
        
        return IOUtils.toByteArray(is);
        
    }

    
    
    private void cleanupAll(String outputDir, String inputDir) throws IOException{
    	
    	TattleupHelper.cleanup(outputDir);  
    	
    	TattleupHelper.cleanup(inputDir);
    	
    }
    

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doPost(request, response);

    }

}
