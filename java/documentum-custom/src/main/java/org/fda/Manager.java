package org.fda;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.documentum.operations.IDfDeleteOperation;

public class Manager {
	public static final String DM_DOCUMENT = "dm_document";
	public static final String F_BINARY = "binary";
	public static final String STATE = "state";
	public static final String STATE_CODE = "state_code";
	public static final String STATE_TIMESTAMP = "state_timestamp";
	public static final long STATE_TTL = 12 * 60 * 60 * 1000;

	private Logger log = Logger.getAnonymousLogger();
	private static final Map<String, Map<String, String>> threadState = new ConcurrentHashMap<String, Map<String, String>>();

	private IDfSessionManager m_sessionMgr;
	private String m_repository;
	private String m_userName;
	private String m_password;
	private String m_tmpFolder;

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
			throw new RuntimeException(e);
		}
	}

	public String mergeDocument(String folderPath, IDfSession session)
			throws Exception {
		updateThreadState("Merge started");
		IDfFolder folder = session.getFolderByPath(folderPath);
		IDfCollection fileList = folder.getContents(null);

		String documentName = folderPath
				.substring(folderPath.lastIndexOf('/') + 1);
		String documentFolder = folderPath.substring(0,
				folderPath.lastIndexOf('/'));

		Map<String, String> orderedObjectList = new TreeMap<String, String>();
		while (fileList.next()) {
			IDfTypedObject doc = (IDfTypedObject) fileList.getTypedObject();
			orderedObjectList.put(doc.getString("object_name"),
					doc.getString("r_object_id"));
		}

		File tmpFile = new File(m_tmpFolder, documentName);
		FileUtils.deleteQuietly(tmpFile);
		OutputStream output = new BufferedOutputStream(new FileOutputStream(
				tmpFile));

		try {
			int size = orderedObjectList.entrySet().size();
			int count = 0;
			for (Map.Entry<String, String> objId : orderedObjectList.entrySet()) {
				IDfSysObject obj = (IDfSysObject) session.getObject(new DfId(
						objId.getValue()));
				IOUtils.copy(obj.getContent(), output);
				if (size % 10 == 0) {
					updateThreadState(String.format("%d parts merged from %d",
							count, size));
				}
			}
			output.flush();
		} finally {
			IOUtils.closeQuietly(output);
		}
		try {
			updateThreadState("Importing new File");
			beginTrans(session);
			IDfId newObjId = createDocument(documentName, documentFolder,
					session);
			append(newObjId, tmpFile.getAbsolutePath(), session);
			deleteFolder(folderPath, session);
		} catch (Exception e) {
			abortTrans(session);
			updateThreadState("Error importing new File","1");
			throw new RuntimeException(e);
		} finally {
			FileUtils.deleteQuietly(tmpFile);
		}
		commitTrans(session);

		return "ok";
	}

	public IDfId createDocument(String documentName, String documentFolder,
			IDfSession session) throws Exception {
		IDfId id = null;
		try {
			beginTrans(session);
			IDfSysObject newDoc = (IDfSysObject) session.newObject(DM_DOCUMENT);
			newDoc.setObjectName(documentName);
			newDoc.setContentType(F_BINARY);
			newDoc.link(documentFolder);
			newDoc.save();

			id = newDoc.getObjectId();
			commitTrans(session);
		} catch (Exception e) {
			abortTrans(session);
			throw new RuntimeException(e);

		}
		return id;
	}

	public void append(IDfId id, String file, IDfSession session)
			throws Exception {
		try {
			beginTrans(session);
			IDfSysObject newDoc = (IDfSysObject) session.getObject(id);
			newDoc.setFile(file);
			newDoc.save();
			commitTrans(session);
		} catch (Exception e) {
			abortTrans(session);
			throw new RuntimeException(e);

		}
	}

	public void deleteFolder(String folderPath, IDfSession session) {
		IDfClientX clientx = new DfClientX();
		try {
			IDfDeleteOperation delop = clientx.getDeleteOperation();
			IDfFolder folder = session.getFolderByPath(folderPath);

			delop.add(folder);
			delop.setDeepFolders(true);

			if (!delop.execute()) {
				throw new RuntimeException("Folder could not be removed");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void print(IDfId id, IDfSession session) throws Exception {
		IDfSysObject newDoc = (IDfSysObject) session.getObject(id);
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

	private void beginTrans(IDfSession session) throws DfException {
		if (!session.isTransactionActive()) {
			session.beginTrans();
		}
	}

	private void abortTrans(IDfSession session) throws DfException {
		if (session.isTransactionActive()) {
			session.abortTrans();
		}
	}

	private void commitTrans(IDfSession session) throws DfException {
		if (session.isTransactionActive()) {
			session.flushCache(true);
			session.commitTrans();
		}
	}

	// Release an active connection to the repository for reuse.
	public void releaseSession(IDfSession session) {
		m_sessionMgr.release(session);
	}

	public void updateThreadState(String state) {
		updateThreadState(state, "0");
	}

	public void updateThreadState(String state, String code) {
		String threadName = Thread.currentThread().getName();
		Map<String, String> stateMap = threadState.get(threadName);
		if (stateMap == null) {
			stateMap = new HashMap<String, String>();
			threadState.put(threadName, stateMap);
		}
		stateMap.put(STATE, state);
		stateMap.put(STATE_CODE, code);
		stateMap.put(STATE_TIMESTAMP, "" + new Date().getTime());
	}

	public Map<String, String> getState(String threadName) {
		return threadState.get(threadName);
	}
	
	public void cleanStates(){
		for (Iterator<Map.Entry<String,Map<String,String>>> iterator = threadState.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String,Map<String,String>> state = iterator.next();
			try {
				if(Long.parseLong(state.getValue().get(STATE_TIMESTAMP)) + STATE_TTL < new Date().getTime()){
					iterator.remove();
				}
			} catch (Exception e) {
				log.log(Level.SEVERE,e.getMessage(),e);
			}
			
		}
	}

}
