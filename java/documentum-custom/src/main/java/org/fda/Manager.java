package org.fda;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.operations.IDfFile;
import com.documentum.operations.IDfImportNode;
import com.documentum.operations.IDfImportOperation;
import com.rsa.crypto.ncm.key.o;
import com.rsa.cryptoj.o.my;

public class Manager {
	public static final String DM_DOCUMENT = "dm_document";
	public static final String F_BINARY = "binary";

	private IDfSessionManager m_sessionMgr;
	private String m_repository;
	private String m_userName;
	private String m_password;
	private String m_tmpFolder;

	byte[] b = "Hola Mundo".getBytes();

	public Manager(String rep, String user, String pword, String tmpFolder) {
		try {
			// Populate member variables.
			m_repository = rep;
			m_userName = user;
			m_password = pword;
			m_tmpFolder = tmpFolder;
			// Call the local createSessionManager method.
			m_sessionMgr = createSessionManager();
		} catch (Exception e) {
			System.out.println("An exception has been thrown: " + e);
		}
	}
	
	public String mergeDocument(String folderPath) throws Exception {
		IDfSession mySession = getSession();
		IDfFolder folder = mySession.getFolderByPath(folderPath);
		IDfCollection fileList = 	folder.getContents(null);

		String documentName = folderPath.substring(folderPath.lastIndexOf('/')+1);
		String documentFolder = folderPath.substring(0,folderPath.lastIndexOf('/'));

		Map<String, String> orderedObjectList = new TreeMap<String, String>();
		while (fileList.next()) {
			IDfTypedObject  doc = (IDfTypedObject ) fileList.getTypedObject();
			orderedObjectList.put(doc.getString("object_name"), doc.getString("r_object_id"));
		}
		
		File tmpFile = new File(m_tmpFolder, documentName);
		OutputStream output = new BufferedOutputStream(new FileOutputStream(tmpFile));
		
		for (Map.Entry<String, String> objId : orderedObjectList.entrySet()) {
			IDfSysObject obj = (IDfSysObject) mySession.getObject(new DfId(objId.getValue()));
			IOUtils.copy(obj.getContent(), output);
		}
		output.flush();
		output.close();
		
		IDfId newObjId = createDocument(documentName, documentFolder);
		append(newObjId, tmpFile.getAbsolutePath());
		
		FileUtils.deleteQuietly(tmpFile);
		
		return "ok";
	}
	
	public void name() throws Exception {
		 IDfClientX clientX = new DfClientX();
         
         IDfImportOperation impOper = clientX.getImportOperation();
         
         IDfFile localFile = clientX.getFile("");
         IDfImportNode impNode = (IDfImportNode) impOper.add(localFile);
         
         IDfSession mySession = getSession();
         impOper.setSession(mySession);
         
         IDfId destId = new DfId("/Temp");
         impOper.setDestinationFolderId(destId);
         
	}

	public IDfId createDocument(String documentName, String documentFolder) throws Exception {
		IDfId id = null;
		IDfSession mySession = getSession();
		try {
			mySession.beginTrans();
			IDfSysObject newDoc = (IDfSysObject) mySession
					.newObject(DM_DOCUMENT);
			newDoc.setObjectName(documentName);
			newDoc.setContentType(F_BINARY);
			newDoc.link(documentFolder);

			// for (int i = 0; i < 1; i++) {
			// ByteArrayOutputStream bytes = new MyOutputStream();
			// bytes.write(b);
			// bytes.write(i%20+65);
			// newDoc.appendContent(bytes);
			// }
			newDoc.save();

			id = newDoc.getObjectId();
			// newDoc.getContent();
			mySession.flushCache(true);
			mySession.commitTrans();
		} catch (Exception e) {
			mySession.abortTrans();
			throw new RuntimeException(e);

		}
		releaseSession(mySession);
		return id;
	}

	public void append(IDfId id, String file) throws Exception {
		IDfSession mySession = getSession();
		try {
			mySession.beginTrans();
			IDfSysObject newDoc = (IDfSysObject) mySession.getObject(id);
//			for (int i = 0; i < 20; i++) {
//				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//				bytes.write(i % 20 + 65);
//				newDoc.appendContent(bytes);
//			}
			newDoc.setFile(file);
			newDoc.save();
			mySession.flushCache(true);
			mySession.commitTrans();
		} catch (Exception e) {
			mySession.abortTrans();
			throw new RuntimeException(e);

		}
		releaseSession(mySession);
	}
	
	public void print(IDfId id) throws Exception  {
		IDfSession mySession = getSession();
		IDfSysObject newDoc = (IDfSysObject) mySession.getObject(id);
		for (int i = 0; i < newDoc.getPageCount(); i++) {
			InputStream input = newDoc.getContentEx("binary", i);
			org.apache.commons.io.IOUtils.copy(input, System.out);
		}
		
	}

	private IDfSessionManager createSessionManager() throws Exception {
		// The only class we instantiate directly is DfClientX.
		DfClientX clientx = new DfClientX();
		// Most objects are created using factory methods in interfaces.
		// Create a client based on the DfClientX object.
		IDfClient client = clientx.getLocalClient();
		// Create a session manager based on the local client.
		IDfSessionManager sMgr = client.newSessionManager();
		// Set the user information in the login information variable.
		IDfLoginInfo loginInfo = clientx.getLoginInfo();
		loginInfo.setUser(m_userName);
		loginInfo.setPassword(m_password);
		// Set the identity of the session manager object based on the
		// repository
		// name and login information.
		sMgr.setIdentity(m_repository, loginInfo);
		// Return the populated session manager to the calling class. The
		// session
		// manager object now has the required information to connect to the
		// repository, but is not actively connected.
		return sMgr;
	}

	// Request an active connection to the repository.
	public IDfSession getSession() {
		try {
			return m_sessionMgr.getSession(m_repository);
		} catch (DfException e) {
			throw new RuntimeException(e);
		}
	}

	// Release an active connection to the repository for reuse.
	public void releaseSession(IDfSession session) {
		m_sessionMgr.release(session);
	}

}
