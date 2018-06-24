package xml.benchmark.importer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import benchmark.importer.core.contract.BenchmarkImporter;
import benchmark.importer.core.model.ImportedBenchmarkModel;
import benchmark.importer.core.model.ImportedQuery;
import benchmark.importer.core.model.ImportedRelevantDocument;
import xml.benchmark.importer.model.Documents.Document;
import xml.benchmark.importer.model.Matrix.MatrixQuery;
import xml.benchmark.importer.model.Matrix.MatrixQuery.MatrixDocument;
import xml.benchmark.importer.model.Queries.Query;

// xml.benchmark.importer.XMLBenchmarkImporter
public class XMLBenchmarkImporter implements BenchmarkImporter {

	JAXBContext jaxbContext;
	Unmarshaller jaxbUnmarshaller;

	public static final List<String> REQUIRED_XML_FILES = new ArrayList<>();
	static {
		REQUIRED_XML_FILES.add("Queries.xml");
		REQUIRED_XML_FILES.add("Documents.xml");
		REQUIRED_XML_FILES.add("Matrix.xml");
	}

	public Object parseViaJaxb(Class clazz, InputStreamReader inputStreamReader) {
		try {
			jaxbContext = JAXBContext.newInstance(clazz);
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			return jaxbUnmarshaller.unmarshal(inputStreamReader);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException {

		Class<?> clazz = XMLBenchmarkImporter.class;
		XMLBenchmarkImporter importer = (XMLBenchmarkImporter) clazz.newInstance();

		// File file = new File("test.xml");
		// InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
		// Queries q = (Queries) importer.parseViaJaxb(Queries.class, reader);
		// q.toString();
		//
		// List<String> fileNames = ZipReader.getZippedFileNames(new
		// FileInputStream("test.zip"));
		//
		// // System.out.println("Values:" + String.join(",", fileNames));
		//
		// List<String> filePaths = ZipReader.readZipFile(new
		// FileInputStream("test.zip"), "output");
		// // System.out.println("Values:" + String.join(",", filePaths));

		boolean valid = importer.canImport(new FileInputStream("test2.zip"));
		// System.out.println("Is this a vali zip? " + valid);
		if (!valid) {
			// 400 bad request.
		}
		String dirPath = "output";
		ImportedBenchmarkModel model = importer.importBenchmark(dirPath, new FileInputStream("lightweight.zip"));
		// System.out.println(model);
	}

	@Override
	public boolean canImport(InputStream zipWithAssetsForImport) {
		try {
			List<String> fileNames = ZipReader.getZippedFileNames(zipWithAssetsForImport);
			return fileNames.containsAll(REQUIRED_XML_FILES);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public ImportedBenchmarkModel importBenchmark(String documentDirPath, InputStream zipWithAssetsForImport) {
		List<String> xmlFiles = unpackZip(zipWithAssetsForImport);
		Collections.sort(xmlFiles);
		String documentsXML = xmlFiles.get(0);
		String matrixXML = xmlFiles.get(1);
		String queriesXML = xmlFiles.get(2);

		// extract Text from each Document
		// store its path on disk,
		// use its ID as key
		Map<Long, ImportedRelevantDocument> documentMap = processDocuments(documentsXML, documentDirPath);
		Map<Long, ImportedQuery> queryMap = processQueries(queriesXML, documentDirPath);
		List<ImportedQuery> processedQueries = connectQueriesWithDocuments(documentMap, queryMap, matrixXML);
				
		ImportedBenchmarkModel model = new ImportedBenchmarkModel();
		List<String> pathOfAllDocuments = getAllDocumentsPath(documentMap);
		
		model.setAllDocumentsPath(pathOfAllDocuments);
		model.setQueryAndRelevantDocumentsList(processedQueries);
		
		return model;

	}


	private List<ImportedQuery> connectQueriesWithDocuments(Map<Long, ImportedRelevantDocument> documentMap,
			Map<Long, ImportedQuery> queryMap, String matrixXML) {
		
		List<ImportedQuery> connectedQueries = new ArrayList<>();
		FileInputStream inputStream = null;
		// due to LARGE xml files, process XML file in chunks, 1 Document at a time
		try {
			inputStream = new FileInputStream(matrixXML);
			PartialUnmarshaller partialUnmarshaller = new PartialUnmarshaller<>(inputStream, MatrixQuery.class);
			while(partialUnmarshaller.hasNext()) {
				Object tag =  partialUnmarshaller.next();
				if(! (tag instanceof MatrixQuery)) {
					continue;
				}
				//// System.out.println("le document:" + ((MatrixQuery)tag).getId());
				// for each document, extract and store its Text into a file with the following format
				// ID-TIMESTAMP.txt
				ImportedQuery connected = processMatrixQuery((MatrixQuery)tag, queryMap, documentMap);
				connectedQueries.add(connected);
			}
		}catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		
		
		return connectedQueries;
	}


	private ImportedQuery processMatrixQuery(MatrixQuery tag, Map<Long, ImportedQuery> queryMap,
			Map<Long, ImportedRelevantDocument> documentMap) {
		if(!queryMap.containsKey(tag.getId())) {
			throw new IllegalStateException("Invalid <matrix-query encountered, no query for ID:" + tag.getId());
		}
		
		ImportedQuery query = queryMap.get(tag.getId());
		for(MatrixDocument doc : tag.getMatrixDocument() ) {
			ImportedRelevantDocument relevantDoc = documentMap.get(doc.getId());
			query.getRelevantDocumentsMeta().add(relevantDoc);
			query.getRelevantDocumentsPath().add(relevantDoc.getPath());
		}
		
		return query;
		
	}

	private List<String> getAllDocumentsPath(Map<Long, ImportedRelevantDocument> documentMap) {
		List<String> paths = new ArrayList<>();
		
		for(ImportedRelevantDocument document : documentMap.values()) {
			paths.add(document.getPath());
		}
		return paths;
	}
	
	private Map<Long, ImportedQuery> processQueries(String queriesXML, String documentDirPath) {
		Map<Long, ImportedQuery> queryMap = new HashMap<Long, ImportedQuery>();
		FileInputStream inputStream = null;
		// due to LARGE xml files, process XML file in chunks, 1 Document at a time
		try {
			 inputStream = new FileInputStream(queriesXML);
			PartialUnmarshaller partialUnmarshaller = new PartialUnmarshaller<>(inputStream, Query.class);
			while(partialUnmarshaller.hasNext()) {
				Object tag =  partialUnmarshaller.next();
				if(! (tag instanceof Query)) {
					continue;
				}
				// System.out.println("le document:" + ((Query)tag).getId());
				// for each document, extract and store its Text into a file with the following format
				// ID-TIMESTAMP.txt
				ImportedQuery query = processSingleQuery((Query)tag, documentDirPath);
				queryMap.put(query.getTempId(), query);
			}
		}catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return queryMap;
		
	}

	private ImportedQuery processSingleQuery(Query tag, String documentDirPath) {
		ImportedQuery query = new ImportedQuery();
		query.setRelevantDocumentsMeta(new ArrayList<>());
		query.setRelevantDocumentsPath(new ArrayList<String>());
		query.setSearchType("regular");
		query.setText(tag.getValue());
		query.setTempId(tag.getId());
		
		return query;
		
	}

	private Map<Long, ImportedRelevantDocument> processDocuments(String documentsXML, String documentDirPath) {
		Map<Long, ImportedRelevantDocument> documentMap = new HashMap<Long, ImportedRelevantDocument>();
		FileInputStream inputStream = null;
		// due to LARGE xml files, process XML file in chunks, 1 Document at a time
		try {
			 inputStream = new FileInputStream(documentsXML);
			PartialUnmarshaller partialUnmarshaller = new PartialUnmarshaller<>(inputStream, Document.class);
			while(partialUnmarshaller.hasNext()) {
				Object tag =  partialUnmarshaller.next();
				if(! (tag instanceof Document)) {
					continue;
				}
				// System.out.println("le document:" + ((Document)tag).getId());
				// for each document, extract and store its Text into a file with the following format
				// ID-TIMESTAMP.txt
				ImportedRelevantDocument document = processSingleDocument((Document)tag, documentDirPath);
				documentMap.put(document.getId(), document);
			}
		}catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return documentMap;
	}

	private void convertFileToPDF(String content, String filePath) throws DocumentException, IOException {
		InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
		FileOutputStream fos = new FileOutputStream(filePath);
		com.itextpdf.text.Document document = new com.itextpdf.text.Document();
		PdfWriter writer = PdfWriter.getInstance(document, fos);
		document.open();
		PdfContentByte cb = writer.getDirectContent();
		document.newPage();
		Paragraph p;
        Font normal = new Font(FontFamily.TIMES_ROMAN, 12);
        p = new Paragraph(content, normal);
        
        document.add(p);
        
		fos.flush();
		document.close();
		fos.close();
	}
	
	private ImportedRelevantDocument processSingleDocument(Document tag, String documentDirPath)
			throws DocumentException, IOException {
		// for each document, extract and store its Text into a file with the following
		// format
		// ID-TIMESTAMP.txta
		ImportedRelevantDocument importedDocument = new ImportedRelevantDocument();

		String fileName = "imported-document-" + tag.getId() + "-" + System.currentTimeMillis() + ".pdf";
		String filePath = documentDirPath + File.separator + fileName;
		String txtContent = TextExtractor.extractText(tag);
		
		/////////////////////////////////////////////
		//  FI L E S HAVE TO BVE CONVERTED TO PDF DUE TO
	 	convertFileToPDF(txtContent, filePath);
		
		
		// 
//		PrintWriter printer = new PrintWriter(filePath);
//		printer.print(txtContent);
//		printer.close();

		importedDocument.setPath(filePath);
		importedDocument.setId(tag.getId());
		importedDocument.setName(fileName);

		return importedDocument;

	}

	private List<String> unpackZip(InputStream zipWithAssetsForImport) {
		List<String> filePaths = new ArrayList<>();

		try {
			filePaths = ZipReader.readZipFile(zipWithAssetsForImport, System.getProperty("java.io.tmpdir"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return filePaths;

	}


	public void testXMLFileStreaming(InputStream zipWithAssetsForImport, String documentDirPath) {
		List<String> filePaths = null;
		try {
			filePaths = ZipReader.readZipFile(zipWithAssetsForImport, documentDirPath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("Values:" + String.join(",", filePaths));
		// System.out.println("Attempt to import each one?");
		try {
			FileInputStream inputStream = new FileInputStream("F:\\UDD\\xml.benchmark.importer\\output\\Matrix.xml");
			PartialUnmarshaller partialUnmarshaller = new PartialUnmarshaller<>(inputStream, MatrixQuery.class);
			while (partialUnmarshaller.hasNext()) {
				Object tag = partialUnmarshaller.next();
				if (tag instanceof MatrixQuery) {
					// System.out.println("le document:" + ((MatrixQuery) tag).getId());
				}

			}

			// Documents example

			// PartialUnmarshaller partialUnmarshaller = new
			// PartialUnmarshaller<>(inputStream, Document.class);
			// while(partialUnmarshaller.hasNext()) {
			// Object tag = partialUnmarshaller.next();
			// if(tag instanceof Document) {
			// // System.out.println("le document:" + ((Document)tag).getId());
			// }
			//
			// }

			// Queries example

			// PartialUnmarshaller partialUnmarshaller = new
			// PartialUnmarshaller<>(inputStream, Query.class);
			// while(partialUnmarshaller.hasNext()) {
			// Object tag = partialUnmarshaller.next();
			// if(tag instanceof Query) {
			// // System.out.println("le document:" + ((Query)tag).getId());
			// // System.out.println("query");
			// }
			//
			// }
			//
			// Documents docs = (Documents) parseViaJaxb(Documents.class, new
			// InputStreamReader(new
			// FileInputStream("F:\\UDD\\xml.benchmark.importer\\output\\Documents.xml")));
			// for(Document doc : docs.getDocument()) {
			// // System.out.println("doc" + doc.getId());

			// }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return;
	}

}
