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
import org.mule.api.transport.PropertyScope;

import com.mulesoft.module.batch.api.BatchManager;

public class DocumentumCall implements Callable {

	private String metadataJson = "{\"name\":null,\"type\":null,\"definition\":null,\"properties\":{\"object_name\":\"{OBJECT_NAME}\"},\"links\":null,\"propertiesType\":null}";

	String username = "dmadmin";
	String password = "password";
	String url = "http://172.16.57.128:8080/dctm-rest/repositories/MyRepo/folders/{FOLDER_ID}/documents";
	protected HttpClient httpClient = new HttpClient();
	
	@Override
	public Object onCall(MuleEventContext event) throws Exception {
		
		HashMap<String,Object> fileData = (HashMap<String,Object>)event.getMessage().getProperty("fileData", PropertyScope.INVOCATION);
		url = url.replace("{FOLDER_ID}", (String)fileData.get("documentumFolderId"));
		
		metadataJson = metadataJson.replace("{OBJECT_NAME}", (String)fileData.get("fileName"));
		
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
//		batchManager.getJobInstance("", null).
		return event.getMessage();
	}

}
