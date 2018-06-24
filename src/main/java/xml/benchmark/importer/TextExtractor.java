package xml.benchmark.importer;

import xml.benchmark.importer.model.Documents.Document;
import xml.benchmark.importer.model.Documents.Document.Sentence;

public class TextExtractor {
	
	public static String extractText(Document document) {
		StringBuilder builder = new StringBuilder();
		for(Sentence sentance : document.getSentence()) {
			builder.append(sentance.getValue());
		}
		return builder.toString();
	}
}
