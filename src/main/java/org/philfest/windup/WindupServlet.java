package org.philfest.windup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;

import javax.servlet.ServletContext;
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
        
        String appDir = null;
        
        byte[] archive = null;
        
        String inputPath = null;
        
        try{
            
	        for (Part part : request.getParts()) {
	          
	            fileName = getFileName(part);
	          
	              if(fileName != null && !fileName.isEmpty()){
	                  
	                  // Get the application name from the form.
	                  // Required field that is used to derive output dir
	            	  String appName = request.getParameter("appName");
	                  
	                  // Add timestamp to ensure uniquenes of dir name
	                  appDir = appName + "-" + Long.toString(System.currentTimeMillis());
	                   
	                  // Get the upoaded archive as byte[]
	                  archive = getArchiveAsBytes(request, part.getName());                  
	                  
	                  // Write the byte[] to file
	                  inputPath = saveArchive(realPath + INPUT + File.separator + appDir, fileName, archive);
	                  
	              }
	          
	        } 
        
        	if(archive.length > 0){
                                   	
            	String outputBaseDir = realPath + OUTPUT + File.separator + appDir + File.separator;
            
            	doWindup(request, inputPath, outputBaseDir + WINDUP);
            
            	doTattletale(request, inputPath, outputBaseDir + TATTLETALE);
            	
            	zipOutput(realPath, appDir);
            	
            	cleanup(outputBaseDir);  
            	
            	cleanup(realPath + INPUT + File.separator + appDir);
            	
            	responseMesssage = "Done! Please download your <a href='" + REPORT + "/" + appDir + ".zip'>results here</a>.";
            	
            }
                      
        } catch (Exception e){
        	
        	logger.error("Could not process.", e);
        	
        	responseMesssage = "Sorry. Your request could not be processed: " + e.getMessage();
        	
        }
        
    	writeResponse(response, responseMesssage);
                    
    }
    
    
    public void doTattletale(HttpServletRequest request, String source, String destination) throws Exception {
    	
    	Main main = new Main();
    	
    	main.setSource(source);
    	
    	main.setDestination(destination);
    	
    	main.setFailOnInfo(false);
    	
    	main.setFailOnWarn(false);
    	
    	main.setFailOnError(false);
    	
    	main.setDeleteOutputDirectory(true);
    	
    	main.execute();
    	
    }
    
    
    public void doWindup(HttpServletRequest request, String inputPath, String output) 
    		throws IOException{
    	
    	WindupEnvironment settings = processRequest(request);
        
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

    
    public WindupEnvironment processRequest(HttpServletRequest request) {
        
            // Grab settings from  request. Some of these may or may not be 
            // text fields w/in the form so do sanity check first
            WindupEnvironment settings = new WindupEnvironment();

            if (StringUtils.isNotBlank(request.getParameter("javaPkgs"))) {
                settings.setPackageSignature(request.getParameter("javaPkgs"));
            } 

            if (StringUtils.isNotBlank(request.getParameter("excludePkgs"))) {
                settings.setExcludeSignature(request.getParameter("excludePkgs"));
            }

            if (StringUtils.isNotBlank(request.getParameter("targetPlatform"))) {
                settings.setTargetPlatform(request.getParameter("targetPlatform"));
            }

            if (StringUtils.isNotBlank(request.getParameter("fetchRemote")) ) {
                settings.setFetchRemote(request.getParameter("fetchRemote"));
            }
            
            settings.setLogLevel("info");
            if (StringUtils.isNotBlank(request.getParameter("logLevel"))) {
                settings.setLogLevel(request.getParameter("logLevel"));
            }
                        
            boolean captureLog = false; 
            if (BooleanUtils.toBoolean(request.getParameter("captureLog"))) {
               captureLog = true; 
            }
            settings.setCaptureLog(captureLog);
            
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
        
        ServletContext servletContext = getServletContext();

        // Get full path to this context's input dir
        //String archiveDir = servletContext.getRealPath("/" + INPUT) + File.separator + appDir;
        
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
    

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doGet(request, response);

    }

}
