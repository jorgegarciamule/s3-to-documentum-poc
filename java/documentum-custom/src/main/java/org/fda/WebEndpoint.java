package org.fda;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.documentum.fc.client.IDfSession;

@Path("/")
public class WebEndpoint {
	private Logger log = Logger.getAnonymousLogger();
	private Manager manager;

	public WebEndpoint(Properties p) {
		manager = new Manager(p.getProperty("dfc.globalregistry.repository"),
				p.getProperty("dfc.globalregistry.username"),
				p.getProperty("dfc.globalregistry.password"),
				p.getProperty("tmp.folder"));
	}

	@GET
	@Path("ping")
	public String ping() {
		return "pong";
	}

	@GET()
	@Path("file/merge")
	// http://localhost:8080/documentum-custom/rest/file/merge?filePath=%2FTemp%2Fsubmition_0001%2Fexample_file.txt
	public String mergeFile(@QueryParam("filePath") final String filePath) {
		String threadName = filePath.replaceAll("[^a-zA-Z0-9]", "");

		Thread t = getThread(threadName);

		if (t == null) {
			t = new Thread(threadName) {
				@Override
				public void run() {
					IDfSession session = null;
					try {
						session = manager.getSession();
						manager.mergeDocument(filePath, session);
						manager.updateThreadState(
								"Merge completed");
					} catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						manager.updateThreadState(
								"Error in merge: " + e.getMessage(), "2");
						throw new RuntimeException(e);
					} finally {
						if (session != null) {
							manager.releaseSession(session);
						}
					}
					manager.cleanStates();
				}
			};
			t.start();

		} else {
			return "1 - Operation canceled: There is one thread running for that file.";
		}

		return "0 - Merge process started. ThreadName: " + threadName;
	}

	@GET()
	@Path("file/merge/status")
	public String getMergeStatus(@QueryParam("filePath") final String filePath) {
		String threadName = filePath.replaceAll("[^a-zA-Z0-9]", "");
		Map<String, String> state = manager.getState(threadName);
		if(state == null){
			return "1 - Status not found";
		}
		return state.get(Manager.STATE_CODE) + " - " + state.get(Manager.STATE);
	}

	@GET()
	@Path("file/merge/stop")
	public String stopMerge(@QueryParam("filePath") final String filePath) {
		String threadName = filePath.replaceAll("[^a-zA-Z0-9]", "");

		Thread t = getThread(threadName);
		if (t == null) {
			return "1 - Thread not running.";
		}

		try {
			t.interrupt();
			t.stop();
		} catch (Exception e) {
			log.log(Level.WARNING, "Thread stoped", e);
		}
		return "0 - Thread " + threadName + " stopped";
	}
	
	@GET()
	@Path("status/clear")
	public String statusClear() {
		manager.clearStates();
		return "Status cleared";
	}

	private Thread getThread(String name) {
		Thread thread = null;
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if (t.getName().equals(name))
				thread = t;
			break;
		}
		return thread;
	}

	protected class CallMerge implements Callable<String> {
		@Override
		public String call() throws Exception {
			return null;
		}
	}

}
