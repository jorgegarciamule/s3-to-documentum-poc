package org.fda;

import com.documentum.fc.client.IDfSession;


public class MainTest {

	public static void main(String[] args) throws Exception {

		Manager m = new Manager("MyRepo","dmadmin","password","/tmp");
		
		IDfSession session = m.getSession();

		m.deleteFolder("/Submissions/Submission_67468/file_20GB.txt", session);
		
		// m.mergeDocument("/Temp/submition_0001/example_file.txt");
		
		//IDfId id = m.createDocument("000004");
		//m.append(id, "/Users/chorch/Desktop/borrar/prueba2");
		//m.print(id);
	}

}
