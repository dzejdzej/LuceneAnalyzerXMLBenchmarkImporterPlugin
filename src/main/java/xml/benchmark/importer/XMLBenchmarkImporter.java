package xml.benchmark.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import xml.benchmark.importer.model.Queries;

public class XMLBenchmarkImporter   {  /*} implements BenchmarkImporter */ 

	JAXBContext jaxbContext;
    Unmarshaller jaxbUnmarshaller;
	
	public Object parseViaJaxb(Class clazz, InputStreamReader inputStreamReader) {
        try {
            jaxbContext = JAXBContext.newInstance(clazz);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
           	return  jaxbUnmarshaller.unmarshal(inputStreamReader);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return null;
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, FileNotFoundException {
		
		Class<?> clazz = XMLBenchmarkImporter.class;
		XMLBenchmarkImporter importer = (XMLBenchmarkImporter) clazz.newInstance();
		
		File file = new File("test.xml");
		InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
		Queries q = (Queries) importer.parseViaJaxb(Queries.class, reader);
		q.toString();
	}

}
