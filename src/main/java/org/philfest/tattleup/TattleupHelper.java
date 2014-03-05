package org.philfest.tattleup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.jboss.tattletale.Main;
import org.jboss.windup.WindupEnvironment;
import org.jboss.windup.WindupReportEngine;

public class TattleupHelper {
	
	private static final String TATTLETALE = "tattletale";
	
	protected static String saveArchive(String archiveDir, String fileName, byte[] b) throws IOException {
        
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
    

    protected static void createZip(String directoryPath, String zipPath) throws IOException {
	
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
    
	private static void addFileToZip(ZipArchiveOutputStream zOut, String path, String base) throws IOException {
		
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
	
    protected static void cleanup(String path) throws IOException{
    	
    	delete(new File(path));
    	
    }
    
    private static void delete(File file) throws IOException{
     
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
    
    public static void doWindup(WindupEnvironment settings, String inputPath, String output) 
    		throws IOException{
    	
    	//WindupEnvironment settings = processWindupRequest(request);
        
        // Initialize windup with settings  
        WindupReportEngine engine = new WindupReportEngine(settings);
  
        // Run windup 
        engine.generateReport(new File(inputPath), new File(output)); 
        
    }
    
    
    public static void doTattletale(String propsFile, String source, String baseDir, String propsDir) throws Exception {
    	
    	String destination = baseDir + TATTLETALE;
    	
    	//String propsFile = processTattletaleRequest(request, propsDir);
    	
    	Main main = new Main();
    	
    	main.setSource(source);
    	
    	main.setDestination(destination);
    	
    	main.setFailOnInfo(false);
    	
    	main.setFailOnWarn(false);
    	
    	main.setFailOnError(false);
    	
    	main.setConfiguration(propsFile);
    	
    	main.execute();
    	
    }
    
    protected static String nowToString(){
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("MMddyy-HHmmss");
    	
    	return sdf.format(new java.util.Date());
    }
    
    protected static String joinStrings(String[] strings, String token){
    	
    	if (token ==null) token = ",";
    	
    	StringBuffer sb = new StringBuffer("");
    	
    	for(int i = 0; i < strings.length; i++){
    		
    			sb.append(strings[i] + token);
    		
    	}
    	
    	sb.deleteCharAt(sb.lastIndexOf(token));
    	
    	return sb.toString();
    	
    }
    

}
