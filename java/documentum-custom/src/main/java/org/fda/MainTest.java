package org.fda;

import com.documentum.fc.common.IDfId;

public class MainTest {

	public static void main(String[] args) throws Exception {

		Manager m = new Manager("MyRepo","dmadmin","password","/tmp");
		
		m.mergeDocument("/Temp/submition_0001/example_file.txt");
		
		//IDfId id = m.createDocument("000004");
		//m.append(id, "/Users/chorch/Desktop/borrar/prueba2");
		//m.print(id);
	}

}
