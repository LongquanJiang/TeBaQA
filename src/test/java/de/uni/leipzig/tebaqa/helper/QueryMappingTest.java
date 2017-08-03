package de.uni.leipzig.tebaqa.helper;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueryMappingTest {

    @Test
    public void testCreateQueryPattern() throws Exception {
        Query query = QueryFactory.create("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");
        String question = "In which country does the Nile start?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        QueryMapping queryMapping = new QueryMapping(question, posSequence, query);

        assertEquals("{ <^NNP2^> <^NN1^> ?uri }", queryMapping.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternUriMismatch() throws Exception {
        Query query = QueryFactory.create("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Niles dbo:city ?uri . }");
        String question = "In which country does the Nile start?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile", "NNP2");
        QueryMapping queryMapping = new QueryMapping(question, posSequence, query);

        assertEquals("{ <^NNP2^> <^NN1^> ?uri }", queryMapping.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternTestsForCompleteResourceString() throws Exception {
        Query query = QueryFactory.create("PREFIX dbo: <http://dbpedia.org/ontology/> " +
                "PREFIX res: <http://dbpedia.org/resource/> " +
                "SELECT DISTINCT ?uri WHERE { res:Nile dbo:city ?uri . }");
        String question = "In which country does the Nile_FooBar_FooBar start?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("start", "VB0");
        posSequence.put("city", "NN1");
        posSequence.put("Nile_FooBar_FooBar", "NNP2");
        QueryMapping queryMapping = new QueryMapping(question, posSequence, query);

        assertEquals("{ <http://dbpedia.org/resource/Nile> <^NN1^> ?uri }", queryMapping.getQueryPattern());
    }

    @Test
    public void testCreateQueryPatternWithEntityFromSpotlight() throws Exception {
        Query query = QueryFactory.create("SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/Yeti_Airlines>" +
                " <http://dbpedia.org/resource/Airport> ?uri . }");
        String question = "Which airport does Yeti Airlines serve?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("serve", "VB0");
        posSequence.put("airport", "NNS1");
        posSequence.put("Airlines", "NNP2");
        posSequence.put("Yeti", "NNP3");
        QueryMapping queryMapping = new QueryMapping(question, posSequence, query);

        assertEquals("{ <^NNP3_NNP2^> <^NNS1^> ?uri }", queryMapping.getQueryPattern());
    }


    @Test
    public void testCreateQueryPatternWithUnknownSpotlightEntity() throws Exception {
        Query query = QueryFactory.create("SELECT DISTINCT ?uri WHERE {  <http://dbpedia.org/resource/Yeti_Airlines>" +
                " <http://dbpedia.org/resource/Airport> ?uri . }");
        String question = "Which airports does Yeti Airlines serve?";
        Map<String, String> posSequence = new HashMap<>();
        posSequence.put("serve", "VB0");
        posSequence.put("airports", "NNP1");
        posSequence.put("Airlines", "NNP2");
        posSequence.put("Yeti", "NNP3");
        QueryMapping queryMapping = new QueryMapping(question, posSequence, query);

        assertEquals("{ <^NNP3_NNP2^> <^NNP1^> ?uri }", queryMapping.getQueryPattern());
    }
}