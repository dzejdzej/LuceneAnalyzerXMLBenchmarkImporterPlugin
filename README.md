# LuceneAnalyzerXMLBenchmarkImporterPlugin
Plugin for Lucene Analyzer that enables us to import Benchmark data from ZIP packages containing SCHEMA validated XML files



Here are 4 steps for how to package Lucene Benchmark Importer into jar:

1. Create Simple Maven Project (USE THE CORE lib containing the "CONTRACT" that your importer has to implemenet and Data model it has to return
See this project's pom.xml for more info (localRepo + core project jar available here:
https://github.com/dzejdzej/LuceneAnalyzerBenchmarkImporterCore

2. In default package add all classes that are used in class that implements BenchmarkImporter
3. Create a built target for jar
4. run maven install with dependencies 
5. Upload the jar file later on using the lucene analyzer tester UI


