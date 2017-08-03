package de.uni.leipzig.tebaqa.helper;

import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance;

/**
 * Creates a Mapping between a part-of-speech tag sequence and a SPARQL query.
 * Algorithm:
 * <br>Input: Question, wordPosMap(mapping between the words of the question and their part-of-speech tag),
 * QueryPattern from <i>sparqlQuery</i></br>
 * <ol>
 * <li>Get Named Entities of the <i>question</i> from DBpedia Spotlight.</li>
 * <li>Replace every named entity from step 1 in the <i><QueryPattern</i> with its part-of-speech tag.
 * If there is no exactly same entity to replace in step go to step 3.</li>
 * <li>Find possible matches based on string similarities:
 * <ol>
 * <li type="1">Create a List with all possible co-occurrences from the words in <i>Question</i>. Calculate the
 * levenshtein distance between every co-occurrence permutation and the entity from Spotlight</li>
 * <li type="1">If the distance of the likeliest group of co-occurrences is lower than 0.4 and the ratio between
 * the 2 likeliest group of words is smaller than 0.6, replace the resource in the <i>QueryPattern</i> with the
 * part-of-speech tags of the word group</li>
 * </ol>
 * </li>
 * <li>For every resource in the <i>QueryPattern</i> which isn't detected in the steps above, search for a
 * similar(based on levenshtein distance, see step 3) string in the question.</li>
 * <p>
 * </ol>
 */
public class QueryMapping {

    private String queryPattern = "";

    /**
     * Default constructor.
     *
     * @param wordPosMap A map which contains every relevant word as key and its part-of-speech tag as value.
     *                   Like this: "Airlines" -> "NNP"
     * @param sparqlQuery The SPARQL query with a query pattern. The latter is used to replace resources with their
     *                    part-of-speech tag.
     */
    QueryMapping(String question, Map<String, String> wordPosMap, Query sparqlQuery) {
        this.queryPattern = sparqlQuery.toString();

        List<String> permutations = getCoOccurrencePermutations(question.split(" "));

        ElementGroup queryPattern = (ElementGroup) sparqlQuery.getQueryPattern();
        String queryPatternString = queryPattern.toString();


        Spotlight spotlight = Utilities.createCustomSpotlightInstance("http://model.dbpedia-spotlight.org/en/annotate");
        //spotlight.setConfidence(0.5);
        spotlight.setSupport("10");
        Map<String, List<Entity>> spotlightEntities = spotlight.getEntities(question);

        //get (multi-word) named entities from DBpedia's Spotlight
        final String[] tmpQueryPatternString = {queryPatternString};
        if (spotlightEntities.size() > 0) {
            spotlightEntities.get("en").forEach((Entity entity) -> {
                        String label = entity.getLabel();
                        List<Resource> uris = entity.getUris();
                        String[] words = label.split("\\s");
                        //Replace every named entity with its part-of-speech tag
                        //e.g.: <http://dbpedia.org/resource/Computer_science> => computer science => NN0 NN1 => ^NN0_NN1^
                        for (Resource uri : uris) {
                            List<String> wordPos = new ArrayList<>();
                            for (String word : words) {
                                wordPos.add(wordPosMap.get(word));
                            }
                            String replace;
                            if (tmpQueryPatternString[0].toLowerCase().contains("<" + uri.toString().toLowerCase() + ">")) {
                                replace = tmpQueryPatternString[0].replace(uri.toString(),
                                        "^" + join("_", wordPos) + "^");
                            } else {
                                //get the most similar word
                                TreeMap<Double, String> distances = getLevenshteinDistances(permutations, uri.getLocalName());
                                replace = conditionallyReplaceResourceWithPOSTag(wordPosMap, tmpQueryPatternString[0],
                                        uri.toString(), distances);

                            }
                            tmpQueryPatternString[0] = replace;
                        }
                    }
            );
        }

        queryPatternString = tmpQueryPatternString[0];

        Pattern pattern = Pattern.compile("<(.*?)>");
        Matcher matcher = pattern.matcher(queryPatternString);

        //Step 4: If there is a resource which isn't detected by Spotlight, search for a similar string in the question.
        //Find every resource between <>
        while (matcher.find()) {
            String resource = matcher.group(1);
            if (!resource.startsWith("<^") && !resource.startsWith("^")) {
                String[] split = resource.split("/");
                String entity = split[split.length - 1];
                if (wordPosMap.containsKey(entity)) {
                    queryPatternString = queryPatternString.replace(resource,
                            "^" + wordPosMap.get(entity) + "^");
                } else {
                    //Calculate levenshtein distance
                    TreeMap<Double, String> distances = getLevenshteinDistances(permutations, entity);
                    queryPatternString = conditionallyReplaceResourceWithPOSTag(wordPosMap, tmpQueryPatternString[0],
                            resource, distances);
                }
            }
        }

        this.queryPattern = queryPatternString
                .replaceAll("\n", " ")
                .replaceAll("\\s+", " ");
    }

    private String conditionallyReplaceResourceWithPOSTag(Map<String, String> wordPosMap, String stringWithResources,
                                                          String uriToReplace, TreeMap<Double, String> distances) {
        String newString = stringWithResources;

        //Check if the difference between the two shortest distances is big enough
        if (distances.size() > 1) {
            Object[] keys = distances.keySet().toArray();
            //The thresholds 0.4 and 0.6 are based on testing and might be suboptimal.
            if ((double) keys[0] < 0.4 && (double) keys[0] / (double) keys[1] < 0.6) {
                List<String> posList = new ArrayList<>();
                String[] split = distances.firstEntry().getValue().split(" ");
                for (String aSplit : split) {
                    posList.add(wordPosMap.get(aSplit));
                }
                if (newString.contains("<" + uriToReplace + ">")) {
                    newString = newString.replace(uriToReplace, "^" + join("_", posList) + "^");
                }
            }
        }
        return newString;
    }

    @NotNull
    private TreeMap<Double, String> getLevenshteinDistances(List<String> permutations, String string) {
        TreeMap<Double, String> distances = new TreeMap<>();
        permutations.forEach((word) -> {
            int lfd = getLevenshteinDistance(string, word);
            double ratio = ((double) lfd) / (Math.max(string.length(), word.length()));
            distances.put(ratio, word);
        });
        return distances;
    }

    private List<String> getCoOccurrencePermutations(String[] s) {
        List<String> permutations = new ArrayList<>();
        for (int i = 0; i <= s.length; i++) {
            for (int y = 1; y <= s.length - i; y++) {
                permutations.add(join(" ", Arrays.asList(s).subList(i, i + y)));
            }
        }
        return permutations;
    }

    /**
     * Creates a SPARQL Query Pattern like this: SELECT DISTINCT ?uri WHERE { ^NNP_0 ^VBZ_0 ?uri . }
     * Every entity which is recognized with the DBPedia Spotlight API is replaced by it's part-of-speech Tag.
     *
     * @return A string with part-of-speech tag placeholders.
     */
    public String getQueryPattern() {
        return queryPattern;
    }
}