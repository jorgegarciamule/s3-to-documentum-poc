package com.mule.poc.documentum;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.transport.PropertyScope;

import com.mulesoft.module.batch.api.BatchManager;
import com.mulesoft.module.batch.record.Record;

public class DocumentumCall implements Callable {

	private String metadataJson = "{\"name\":null,\"type\":null,\"definition\":null,\"properties\":{\"object_name\":\"{OBJECT_NAME}\"},\"links\":null,\"propertiesType\":null}";

	String username = "dmadmin";
	String password = "password";
	String url = "http://{HOST}:{PORT}/dctm-rest/repositories/MyRepo/folders/{FOLDER_ID}/documents";
	protected HttpClient httpClient = new HttpClient();
	
	@Override
	public Object onCall(MuleEventContext event) throws Exception {
		MuleRegistry registry = event.getMuleContext().getRegistry();
		
		
		HashMap<String,Object> fileData = (HashMap<String,Object>)(( Record)event.getMessage().getProperty("BATCH_RECORD", PropertyScope.INVOCATION)).getVariable("fileData");
				
		url = url.replace("{FOLDER_ID}", (String)fileData.get("ParentFolderId")).replace("{HOST}", (String)registry.lookupObject("documentum.rest.host")).replace("{PORT}", (String)registry.lookupObject("documentum.rest.port"));
		username = (String)registry.lookupObject("documentum.username");
		password = (String)registry.lookupObject("documentum.password");
		metadataJson = metadataJson.replace("{OBJECT_NAME}", (String)fileData.get("FileName"));
		
		Part[] parts = new Part[2];
		PartSource metadata = new ByteArrayPartSource("metadata", metadataJson.getBytes("UTF-8"));
		PartSource binary = new ByteArrayPartSource("binary", event.getMessage().getPayloadAsBytes());
		parts[0] = new FilePart("metadata", metadata, "application/vnd.emc.documentum+json", "UTF-8");
		parts[1] = new FilePart("binary", binary, "application/octet-stream", "UTF-8");

		PostMethod uploadPost = new PostMethod(url);
        String usernameAndPassword = username + ":" + password;
		uploadPost.addRequestHeader("Authorization", String.format("Basic %s", new String(Base64.encodeBase64(usernameAndPassword.getBytes()))));
		uploadPost.addRequestHeader("Accept", "application/vnd.emc.documentum+json");
		MultipartRequestEntity entity = new MultipartRequestEntity(parts, uploadPost.getParams());
		uploadPost.setRequestEntity(entity);

		int responseCode = httpClient.executeMethod(uploadPost);
		String response = uploadPost.getResponseBodyAsString();
		System.out.println(response);
		
//		Map<String, BatchManager> batchManagerMap = event.getMuleContext().getRegistry().lookupByType(BatchManager.class);
//		
//		BatchManager batchManager = batchManagerMap.values().iterator().next();
//		
//		batchManager.getJobInstances("file-batch-processing").next()
//		batchManager.getJobInstance(arg0, arg1)
//		batchManager.getJobInstance("08c4e857-8087-11e4-842a-685b35cd0642", "08c4e857-8087-11e4-842a-685b35cd0642").
//		Thread.sleep(5000);
		return event.getMessage();
	}

}
