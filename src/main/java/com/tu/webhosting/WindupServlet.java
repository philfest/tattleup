package com.tu.webhosting;

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

@WebServlet(urlPatterns = "/fileUpload")
@MultipartConfig
public class WindupServlet extends HttpServlet {
    
    private static final String INPUT_BASE_DIR = "/Users/Phil/redhat/windup/input";
    
    private static final String OUTPUT_BASE_DIR = "/Users/Phil/redhat/windup/output";

    private static Logger logger = Logger.getLogger(WindupServlet.class);

    public WindupServlet() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
          throws ServletException, IOException {
        
        String time = Long.toString(System.currentTimeMillis());
           
        String fileName = null;
        
        String appName = null;
        
        byte[] archive;
        
        String inputPath = null;
            
        for (Part part : request.getParts()) {
          
            fileName = getFileName(part);
          
              if(fileName != null && !fileName.isEmpty()){
              
                  logger.info("File name : " + fileName);
              
                  logger.info("Packages : " + request.getParameter("javaPkgs"));
                  
                  appName = request.getParameter("appName");
                  
                  archive = getArchiveAsBytes(request, part.getName());
                  
                  logger.info("Packages : " + request.getParameter("javaPkgs"));
                  
                  inputPath = saveArchive(appName, fileName, archive, time);
                  
              }
          
        } 
        
        ServletContext servletContext = getServletContext();
        
        String outputBase = servletContext.getInitParameter("OUTPUT_BASE_DIR");
        
        String outputDir = appName + "-" + time;
        
        WindupEnvironment settings = processRequest(request);
          
        WindupReportEngine engine = new WindupReportEngine(settings);
  
        engine.generateReport(new File(inputPath), new File(outputBase + File.separator + outputDir));     
        
        String windupContext = servletContext.getInitParameter("WINDUP_CONTEXT");
        
        response.sendRedirect(outputDir);
  
  }

    public WindupEnvironment processRequest(HttpServletRequest request) {

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
            
            settings.setLogLevel("INFO");
            if (StringUtils.isNotBlank(request.getParameter("logLevel"))) {
                settings.setLogLevel(request.getParameter("logLevel"));
            }
                        
            boolean captureLog = false; 
            if (BooleanUtils.toBoolean(request.getParameter("captureLog"))) {
               captureLog = true; 
            }
            settings.setCaptureLog(captureLog);
 
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

    private String saveArchive(String appName, String fileName, byte[] b, String time) throws IOException {
        
        ServletContext servletContext = getServletContext();
        
        String archiveDir = servletContext.getInitParameter("INPUT_BASE_DIR") + File.separator + appName + "-" + time;
        
        // Create input subdirectory based on app name 
        new File(archiveDir).mkdir();
        
        // Build path to archive file
        String fullPath = archiveDir + File.separator + fileName; 
        
        // Create the archive file
        File archive = new File(fullPath);
        
        logger.info("Writing archive to " + fullPath);
        
        if(!archive.canWrite()) logger.error("!!!!!   Cannot save archive!   !!!!!!");
               
        FileOutputStream os = new FileOutputStream(archive);

        os.write(b);

        os.flush();
        
        os.close();
        
        return fullPath; 
        
    }
    
    private String getOutputPath(String appName){
        
        ServletContext servletContext = getServletContext();
        
        return servletContext.getInitParameter("OUTPUT_BASE_DIR") + File.separator + appName; 

    }
    
    private byte[] getArchiveAsBytes(HttpServletRequest request, String partName) throws IOException, ServletException{
        
        InputStream is = request.getPart(partName).getInputStream();
        
        return IOUtils.toByteArray(is);
        
    }
    

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        doGet(request, response);

    }

}
