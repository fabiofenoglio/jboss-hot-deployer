package com.ff.magicHotDeployer.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ff.magicHotDeployer.logging.Logger;

public class UpdateChecker implements Runnable {

	private final static String ACTUAL_VERSION = "0.4.2";
	
	public UpdateChecker() {
    }

    public void run() {
    	Logger.info("checking for updates ...");
		try {
			attemptCheckUpdates();
		}
		catch (Exception e) {
			Logger.warn("error checking for updates", e);
		}
    }
    
	public static void checkUpdates() {
		UpdateChecker instance = new UpdateChecker();
		Thread t = new Thread(instance);
        t.start();
	}
	
	private static void attemptCheckUpdates() throws IOException {
		String url = "http://www.fabiofenoglio.it/public-services/app/versioning/mhd.json";
		String downloadUrl = "http://static.fabiofenoglio.it/apps.mhd";

        String result = getUrlAsString(url);
        // decode
        ObjectMapper mapper = new ObjectMapper();
        UpdateFeedInfo info = mapper.readValue(result, UpdateFeedInfo.class);
        
        if (info.getEnable() != null && info.getEnable() < 1) {
        	Logger.warn("aborting version check (server discontinued). please check manually");
        	return;
        }
        
        Boolean abort = false;
        
        if (!info.getVersion().equals(ACTUAL_VERSION)) {
        	System.out.println("*******************************************");
        	System.out.println("NEW VERSION AVAILABLE : " + info.getVersion());
        	if (info.getMessage() != null) {
        		System.out.println("version update message : \"" + info.getMessage() + "\"");
        	}
        	System.out.println("download new version from " + downloadUrl);
        	
        	if (info.getMandatory() != null && info.getMandatory() > 0) {
        		System.out.println("MANDATORY FLAG ON - obsolete versions (like this) won't run. Please update.");
        		abort = true;
        	}
        	
        	System.out.println("*******************************************");
        	
        	try {
    			Thread.sleep(4000);
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
        }
        else {
        	Logger.debug("version is up to date (" + ACTUAL_VERSION + ")");
        }
        
        if (abort) {
        	Logger.debug("aborting execution from update check. bye");
        	System.exit(0);
        }
	}
	
	public static String getUrlAsString(String url)
	{
	    try
	    {
	        URL urlObj = new URL(url);
	        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
	        con.addRequestProperty("User-Agent", "Mozilla/4.76"); 

	        con.setDoOutput(true); // we want the response 
	        con.connect();

	        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

	        StringBuilder response = new StringBuilder();
	        String inputLine;

	        String newLine = System.getProperty("line.separator");
	        while ((inputLine = in.readLine()) != null)
	        {
	            response.append(inputLine + newLine);
	        }

	        in.close();

	        return response.toString();
	    }
	    catch (Exception e)
	    {
	        throw new RuntimeException(e);
	    }
	}
}
