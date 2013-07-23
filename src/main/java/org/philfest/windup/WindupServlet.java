package org.philfest.windup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.jboss.tattletale.Main;
import org.jboss.windup.WindupEnvironment;
import org.jboss.windup.WindupReportEngine;

@WebServlet(urlPatterns = "/WindupInput")
@MultipartConfig
public class WindupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(WindupServlet.class);
    
    private static final String INPUT = "input";
    
    private static final String OUTPUT = "output";
    
    private static final String REPORT = "report";
    
    private static final String TATTLETALE = "tattletale";
    
    private static final String WINDUP = "windup";

    public WindupServlet() {
    	
        super();
        
    }  

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
          throws ServletException, IOException {
    	
    	String responseMesssage = null;
         
    	String realPath =  this.getServletContext().getRealPath("/");
           
    	String fileName = null;
        
        String appName = null;
        
        String inputPath = null;
        
        try{
            
	        for (Part part : request.getParts()) {
	          
	            fileName = getFileName(part);
	          
	              if(fileName != null && !fileName.isEmpty()){
	                  
	            	  // Get app name request parm. Append timestamp to it for uniqueness
	                  appName = request.getParameter("appName") + "-" + nowToString();               
	                  
	                  // Convert uploaded archive to byte[]. Write it to file and save the path.
	                  inputPath = saveArchive(realPath + INPUT + File.separator + appName, fileName, getArchiveAsBytes(request, part.getName()));
	                  
	              }
	          
	        } 
                                   	
        	String outputBaseDir = realPath + OUTPUT + File.separator + appName + File.separator;
        
        	doWindup(request, inputPath, outputBaseDir + WINDUP);
        
        	doTattletale(request, inputPath, outputBaseDir, realPath + INPUT + File.separator + appName);
        	
        	zipOutput(realPath, appName);
        	
        	cleanupAll(outputBaseDir, realPath + INPUT + File.separator + appName);
        	
        	//cleanup(outputBaseDir);  
        	
        	//cleanup(realPath + INPUT + File.separator + appName);
        	
        	responseMesssage = "Done! Please download your <a href='" + REPORT + "/" + appName + ".zip'>results here</a>.";

                      
        } catch (Exception e){
        	
        	logger.error("Could not process.", e);
        	
        	responseMesssage = "Sorry. Your request could not be processed: " + e.getMessage();
        	
        }
        
    	writeResponse(response, responseMesssage);
                    
    } 
    
    
    public void doTattletale(HttpServletRequest request, String source, String baseDir, String propsDir) throws Exception {
    	
    	String destination = baseDir + TATTLETALE;
    	
    	String propsFile = processTattletaleRequest(request, propsDir);
    	
    	Main main = new Main();
    	
    	main.setSource(source);
    	
    	main.setDestination(destination);
    	
    	main.setFailOnInfo(false);
    	
    	main.setFailOnWarn(false);
    	
    	main.setFailOnError(false);
    	
    	main.setDeleteOutputDirectory(true);
    	
    	main.setConfiguration(propsFile);
    	
    	main.execute();
    	
    }
    
    
    public void doWindup(HttpServletRequest request, String inputPath, String output) 
    		throws IOException{
    	
    	WindupEnvironment settings = processWindupRequest(request);
        
        // Initialize windup with settings  
        WindupReportEngine engine = new WindupReportEngine(settings);
  
        // Run windup 
        engine.generateReport(new File(inputPath), new File(output)); 
        
    }
    
    
    private void zipOutput(String realPath, String appDir) throws Exception{
    	
    	createZip(realPath + OUTPUT + File.separator + appDir, realPath + REPORT + File.separator + appDir + ".zip");
    	
    }
    
    
    private void cleanup(String path) throws IOException{
    	
    	delete(new File(path));
    	
    }
    
    public void delete(File file) throws IOException{
     
        	if(file.isDirectory()){
     
        		//directory is empty, then delete it
        		if(file.list().length==0){
     
        		   file.delete();
     
        		}else{
     
        		   //list all the directory contents
            	   String files[] = file.list();
     
            	   for (String temp : files) {
            	      //construct the file structure
            	      File fileDelete = new File(file, temp);
     
            	      //recursive delete
            	     delete(fileDelete);
            	   }
     
            	   //check the directory again, if empty then delete it
            	   if(file.list().length==0){
               	     file.delete();
            	   }
        		}
     
        	}else{
        		//if file, then delete it
        		file.delete();
        	}
        }
    
    
	public void createZip(String directoryPath, String zipPath) throws IOException {
		
		FileOutputStream fOut = null;
		
		BufferedOutputStream bOut = null;
		
		ZipArchiveOutputStream tOut = null;
		
		try {
		
			fOut = new FileOutputStream(new File(zipPath));
			
			bOut = new BufferedOutputStream(fOut);
			
			tOut = new ZipArchiveOutputStream(bOut);
			
			addFileToZip(tOut, directoryPath, "");
		
		} finally {
			
			tOut.finish();
			
			tOut.close();
			
			bOut.close();
			
			fOut.close();
			
		}
	}
	
	
	private void addFileToZip(ZipArchiveOutputStream zOut, String path, String base) throws IOException {
		
		File f = new File(path);
		
		String entryName = base + f.getName();
		
		ZipArchiveEntry zipEntry = new ZipArchiveEntry(f, entryName);
		
		zOut.putArchiveEntry(zipEntry);
		
		if (f.isFile()) {
		
			FileInputStream fInputStream = null;
			
			try {
			
				fInputStream = new FileInputStream(f);
				
				IOUtils.copy(fInputStream, zOut);
				
				zOut.closeArchiveEntry();
			
			} finally {
			
				fInputStream.close();
			
			}
		
		} else {
			
			zOut.closeArchiveEntry();
			
			File[] children = f.listFiles();
			
			if (children != null) {
			
				for (File child : children) {
				
					addFileToZip(zOut, child.getAbsolutePath(), entryName + "/");
				
				}
			
			}
		
		}
	}
   
    
    public void writeResponse(HttpServletResponse response, String message) throws IOException{
    	
    	PrintWriter writer = response.getWriter();
    	
    	response.setContentType("text/html");
    	
    	writer.write(message);
    	
    	writer.flush();
    	
    	writer.close();
    	
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
    		
    		String profiles = joinStrings(request.getParameterValues("profiles"), null);
    		
    		props.setProperty("profiles", profiles);
    		
        }
    	
    	if (!ArrayUtils.isEmpty(request.getParameterValues("reports"))) {
    		
    		String reports = joinStrings(request.getParameterValues("reports"), null);
    		
    		props.setProperty("reports", reports);
    		
        }
    	
    	
    	if (!ArrayUtils.isEmpty(request.getParameterValues("scans"))) {
    		
    		String scans = joinStrings(request.getParameterValues("scans"), null);
    		
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
    
    private String joinStrings(String[] strings, String token){
    	
    	if (token ==null) token = ",";
    	
    	StringBuffer sb = new StringBuffer("");
    	
    	for(int i = 0; i < strings.length; i++){
    		
    			sb.append(strings[i] + token);
    		
    	}
    	
    	sb.deleteCharAt(sb.lastIndexOf(token));
    	
    	return sb.toString();
    	
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

    
    private String saveArchive(String archiveDir, String fileName, byte[] b) throws IOException {
        
        // Create input subdirectory based on app name 
        new File(archiveDir).mkdir();
        
        // Build path to archive file
        String fullPath = archiveDir + File.separator + fileName; 
        
        // Create the archive file
        File archive = new File(fullPath);
               
        FileOutputStream os = new FileOutputStream(archive);

        os.write(b);

        os.flush();
        
        os.close();
        
        return fullPath; 
        
    }
    
    private String nowToString(){
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("MMddyy-HHmm");
    	
    	return sdf.format(new java.util.Date());
    }
    
    private void cleanupAll(String outputDir, String inputDir) throws IOException{
    	
    	cleanup(outputDir);  
    	
    	cleanup(inputDir);
    	
    }
    

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doGet(request, response);

    }

}
