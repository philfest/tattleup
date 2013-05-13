package org.philfest.windup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.windup.WindupEnvironment;
import org.jboss.windup.WindupReportEngine;

@WebServlet(urlPatterns = "/WindupInput")
@MultipartConfig
public class WindupServlet extends HttpServlet {

    private static Logger logger = Logger.getLogger(WindupServlet.class);

    public WindupServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
          throws ServletException, IOException {
           
        String fileName = null;
        
        String appName = null;
        
        String appDir = null;
        
        byte[] archive = null;
        
        String inputPath = null;
            
        for (Part part : request.getParts()) {
          
            fileName = getFileName(part);
          
              if(fileName != null && !fileName.isEmpty()){
                  
                  // Get the application name from the form.
                  // Required field that is used to derive output dir
                  appName = request.getParameter("appName");
                  
                  // Add timestamp to ensure uniquenes of dir name
                  appDir = appName + "-" + Long.toString(System.currentTimeMillis());
                   
                  // Get the upoaded archive as byte[]
                  archive = getArchiveAsBytes(request, part.getName());
                  
                  // Write the byte[] to file
                  inputPath = saveArchive(appDir, fileName, archive);
                  
              }
          
        } 
        
        if(archive.length > 0){
            
            String outputBase = getOutputPath();
    
            WindupEnvironment settings = processRequest(request);
            
            // Initialize windup with settings  
            WindupReportEngine engine = new WindupReportEngine(settings);
      
            // Run windup 
            engine.generateReport(new File(inputPath), new File(outputBase + File.separator + appDir));   
           
            response.sendRedirect("/windup-output/" + appDir);
        
        }
  
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

    
    private String saveArchive(String appDir, String fileName, byte[] b) throws IOException {
        
        ServletContext servletContext = getServletContext();

        // Get full path to this context's input dir
        String archiveDir = servletContext.getRealPath("/input") + File.separator + appDir;
        
        // Create input subdirectory based on app name 
        new File(archiveDir).mkdir();
        
        // Build path to archive file
        String fullPath = archiveDir + File.separator + fileName; 
        
        // Create the archive file
        File archive = new File(fullPath);
        
        logger.info("Writing archive to " + fullPath);
               
        FileOutputStream os = new FileOutputStream(archive);

        os.write(b);

        os.flush();
        
        os.close();
        
        return fullPath; 
        
    }
    
    
    private String getOutputPath(){
        
        ServletContext servletContext = getServletContext();
        
        StringBuffer input = new StringBuffer(servletContext.getRealPath("/"));
        
        return input.substring(0, input.lastIndexOf(File.separator)) + "-output";
        
    }
    

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doGet(request, response);

    }

}
