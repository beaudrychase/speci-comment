package com.beaudry.middles.finder;


import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphUtils;
import me.tongfei.progressbar.ProgressBar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author beaudry
 */
public class MiddleUtils {
    //pipeline shared by all methods of MiddleUtils

    private boolean verbose = false;

    public StanfordCoreNLP pipeline;
    private static HashSet<String> temporalAdverbs = new HashSet<>(Arrays.asList("soon", "recently", "now", "yesterday", "today", "later", "yet", "back", "forward",
            "away", "towards", "toward", "also", "below", "too", "before", "after", "once", "anymore", "in", "out", "up",

            "instead", "here", "again", "together", "second", "downstream", "low", "along", "back", "ago", "therefore",
            "only", "home", "elsewhere", "ahead", "alike", "then", "upon", "back", "around", "however", "down", "close",
            "aboard", "alongside", "forth", "anyway", "rather", "thereby", "anywhere", "far", "as", "alone", "all",
            "enough", "late", "plain", "if", "forever", "thus", "first", "there", "above", "early", "otherwise",
            "somewhat", "near", "so", "behind", "therein", "maybe", "somewhere", "abroad", "inside", "backward", "nevertheless",
            "someday", "high", "twice", "indeed", "afterward", "adrift", "and", "still", "ever", "near", "about", "straight",
            "off", "even", "though", "straight", "overnight", "though", "moreover", "apart", "namely", "often"
    ));

    public MiddleUtils() {
        initializePipeline();
    }

    public MiddleUtils(boolean verbose) {
        initializePipeline();
        this.verbose = verbose;
    }
    /* IN:
    String pathName: the name of a text file that you want to find middles in.
    OUT: will write the sentences it thinks it has middles to pathName + ".out"
    Goes through each sentence in the text file and determines whether or not it may have a middle.
    If it thinks that a sentence does have a middle it writes it to the output file.
    * */
    String findAllMiddles(String text, String inputFileName) {
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline
        StanfordCoreNLP sentencePipeline = new StanfordCoreNLP(props);

        if(pipeline == null) {
            // set up pipeline properties
            initializePipeline();
        }

        // create a document object
        CoreDocument document = new CoreDocument(text);
        // annnotate the document
        sentencePipeline.annotate(document);
        // examples
        JsonObject result = initializeResultJson(inputFileName);
        int sentencesFound = 0;
        int sentencesTotal = document.sentences().size();
        ProgressBar pb = new ProgressBar(inputFileName, sentencesTotal);
        for (CoreSentence sentence : document.sentences()) {
            pb.step();
            CoreDocument s = new CoreDocument(sentence.text());
            pipeline.annotate(s);
            TransitivePair r = hasMiddle(s.sentences().get(0));
            if (r.getFirst()) {
                ++sentencesFound;
                ((JsonArray) result.get("results")).add(constructResultEntry(s.sentences().get(0), r, sentencesTotal));

            }
        }
        pb.close();
        result.put("sentence_number", sentencesFound);
        return result.toJson();
    }

    private JsonObject constructResultEntry(CoreSentence sentence, TransitivePair r, int sentencePosition) {
        JsonObject resultEntry = new JsonObject();
        resultEntry.put("sentence", sentence.text());
        resultEntry.put("position", sentencePosition);
        resultEntry.put("source_of_transitivity", r.getSecond());
        TreeMap<String, IndexedWord> middleComp = getMiddleComponents(sentence.dependencyParse());
        JsonArray middles = new JsonArray();
        JsonObject middle = new JsonObject();
        middle.put("note", "");
        for (String str : middleComp.keySet()) {
            JsonObject word = new JsonObject();
            word.put("index", middleComp.get(str).index() - 1); //fixes ob1 error
            word.put("word", middleComp.get(str).word());
            middle.put(str, word);
        }
        middles.add(middle);
        resultEntry.put("middles", middles);

        return resultEntry;
    }

    private JsonObject initializeResultJson(String inputFileName) {
        JsonObject result = new JsonObject();
        result.put("input_file", inputFileName);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        result.put("time_created", dateFormat.format(date));
        System.out.println("hello");
        result.put("results", new JsonArray());
        System.out.println(result.toJson());
        return result;
    }

//    private void handleOutput(CoreSentence sentence, TransitivePair result, BufferedWriter outputFile) throws IOException {
//        TreeMap<String, IndexedWord> middleComp = getMiddleComponents(sentence.dependencyParse());
//
//        for (String str : middleComp.keySet()) {
//            outputFile.write(str);
//            outputFile.write(" : " + middleComp.get(str).word() + "\n");
//            }
//        outputFile.write(sentence.text() + "(Transitivity from: " + result.getSecond() + ")\n");
//        outputFile.write("\n");
//    }

    private TreeMap<String, IndexedWord> getMiddleComponents(SemanticGraph depParse) {
        TreeMap<String, IndexedWord> result = new TreeMap<>();
        for (IndexedWord w : depParse.vertexListSorted()) {
            if (w.get(MiddlesAnnotation.class) != null) {
                result.put(w.get(MiddlesAnnotation.class), w);
            }
        }
        return result;
    }

    /*
     * Given a sentence as a string this method will determine if it has a middle*/
    public TransitivePair sentenceHasMiddle(String sentence) {
        if (pipeline == null) {
            initializePipeline();
        }
        //turn the sentence into a core document.
        CoreDocument document = new CoreDocument(sentence);
        pipeline.annotate(document);
        if (verbose)
            System.out.println(document.sentences().get(0).dependencyParse().toString(SemanticGraph.OutputFormat.READABLE) + "\n\n");

        return hasMiddle(document.sentences().get(0));
    }


    /*
     * The core of this class
     * This contains all of the machinery to determine if a dependency parsed sentence has a middle
     *
     * */
    private TransitivePair hasMiddle(CoreSentence sentence) {
        if(sentence == null) {
            throw new IllegalArgumentException("sentence can't be null");
        }
        if (verbose) System.out.println(sentence.toString());
        SemanticGraph depParse = sentence.dependencyParse();

        //get the present tense verbs
        List<edu.stanford.nlp.ling.IndexedWord> presentVerbs =
                depParse.getAllNodesByPartOfSpeechPattern("^VB([ZPD])?$");
        TransitivePair r = new TransitivePair(false, "NO SOURCE");
        if (presentVerbs != null) {
            for (IndexedWord word : presentVerbs) {
                if (word.lemma().equals("be")) {
                    r = specialCase(word, depParse, "VBG");
                    if (r.getFirst()) return r;
                } else if (word.lemma().equals("have")) {
                    r = specialCase(word, depParse, "VBN");
                    if (r.getFirst()) {
                        return r;
                    }
                } else if (word.lemma().equals("do")) {
                    r = specialCase(word, depParse, "VB");
                    if (r.getFirst()) {
                        return r;
                    }
                } else if ((word.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("VBZ") ||
                        word.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("VBP") ||
                        word.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("VBD") ||
                        word.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("VB"))) {
                    r = middleStandardCase(word, depParse);
                    if (r.getFirst()) {
                        return r;
                    }
                }
            }
        }
        List<edu.stanford.nlp.ling.IndexedWord> modifiers =
                (depParse.getAllNodesByPartOfSpeechPattern("MD"));
        for (IndexedWord word : modifiers) {
            r = specialCase(word, depParse, "VB");
            if (r.getFirst()) {
                return r;
            }
        }
        return r;
    }

    /*
     * A helper method for hasMiddle. A verb that forms a middle must be in the present tense, unless there is a
     * auxiliary verb on it that changes the tense.
     * IN:
     *   IndexedWord auxiliary : the auxiliary verb that may have a verb of the right form to possibly create a middle
     *   SemanticGraph depParse: The dependency parse for the sentence
     *   String dependentVerbType: The POS tag that the possible middle would have to have to be a middle given this
     *   auxiliary. */
    private TransitivePair specialCase(IndexedWord auxiliary, SemanticGraph depParse, String dependentVerbType) {
        if (verbose) System.out.println("IN SPECIAL CASE: " + auxiliary.word());
        Collection<IndexedWord> blanket = SemanticGraphUtils.getDependencyBlanket(depParse, Arrays.asList(auxiliary));
        for (IndexedWord dependentWord : blanket) {
            if (verbose) System.out.println(dependentWord.get(CoreAnnotations.PartOfSpeechAnnotation.class));
            if (dependentWord.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals(dependentVerbType)) {
                if (verbose) System.out.println(dependentWord.word());
                return middleStandardCase(dependentWord, depParse);
            }
        }
        return new TransitivePair(false, "NO SOURCE");
    }

    /*
     * checks if a candidate verb has a middle construction.
     * IN:
     * IndexedWord word: the candidate verb that may be a middle construction
     * SemanticGraph depParse: the dependency parse for the sentence
     * OUT:
     * boolean: whether or not this sentence is a middle*/
    private TransitivePair middleStandardCase(IndexedWord verb, SemanticGraph depParse) {
        if (verbose) System.out.println("\nmiddle standard case:");
        TransitivePair result = new TransitivePair(false, "NO SOURCE");
        //CoreAnnotations.PartOfSpeechAnnotation x;
        if (verbose) {
            System.out.println(verb.word());
            System.out.println(verb.lemma());
            System.out.println(verb.get(CoreAnnotations.PartOfSpeechAnnotation.class));
            System.out.println(verb.get(CoreAnnotations.SentenceIndexAnnotation.class));
        }


        /*
         * A middle must have a nsub and an advmod. It cannot have a dobj nor a auxpass. */
        List<IndexedWord> nsubjList = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("nsubj"));
        List<IndexedWord> advmodList = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("advmod"));
        List<IndexedWord> dobjList = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("dobj"));
        List<IndexedWord> auxpass = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("auxpass"));
        List<IndexedWord> aux = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("aux"));

        IndexedWord nsub = null;
        IndexedWord advmod = null;

        for(IndexedWord x: nsubjList){
            nsub = x;
            if (verbose) {
                System.out.println("Found a nsubj");
                System.out.println(x.toString());
            }
        }
        for (IndexedWord x : advmodList) {
            if (x.index() > verb.index() && (
                    x.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("RB") ||
                            x.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("RBR") ||
                            x.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("RBS")
            ) && !temporalAdverbs.contains(x.word().toLowerCase())) {
                advmod = x;
                if (verbose) {
                    System.out.println("Found a advmod");
                    System.out.println(x.toString());
                }

            }

        }
        if (!verbose && (nsub == null || advmod == null)) {
            return result;
        }


        for(IndexedWord x: dobjList){
            if (verbose) {
                System.out.println("Found a dobj (not middle)");
                System.out.println(x.toString());
            } else {
                return result;
            }

        }

        boolean passiveConstruction = false;
        for (IndexedWord x : auxpass) {
            if (verbose) {
                System.out.println("Found a auxpass (not middle)");
                System.out.println(x.toString());
                passiveConstruction = true;
            } else {

                return result;
            }

        }

        for (IndexedWord x : aux) {
            if (verbose) {
                System.out.println("Found a aux");
                System.out.println(x.toString());
            }
            if (x.lemma().equals("be") && verb.get(CoreAnnotations.PartOfSpeechAnnotation.class).equals("VBG")) {
                if (verbose) {
                    System.out.println("(not middle) Has be aux and gerund, this looks like a passive.");
                    passiveConstruction = true;
                } else {
                    return result;
                }
            }
        }

        //the nsub must have an index less than the verb,
        //and the advmod must have an index greater than the verb.
        //if they exist and this is a middle.
        boolean correctRelativePositions = advmod != null && advmod.index() > verb.index();
        if (verbose && !correctRelativePositions && advmod != null)
            System.out.println("advmod in WRONG postion");

        if (dobjList.size() == 0 && !passiveConstruction && correctRelativePositions) {
            //if all of the other conditions are met, check if this verb is transitive.
            result = TransitiveDeterminer.isTransitive(constructVerb(verb, depParse));
            if (verbose) System.out.println("Transitive = " + result);
        }


        if (verbose)
            System.out.println(dobjList.size() == 0 && !passiveConstruction && correctRelativePositions && result.getFirst());

        //return dobjList.size() == 0 && !passiveConstruction && correctRelativePositions && transitive.getFirst();

        /*
         * Annotate the components of this middle construction.*/
        if (result.getFirst()) {
            verb.set(MiddlesAnnotation.class, "verb");
            assert advmod != null;
            advmod.set(MiddlesAnnotation.class, "adverb");
            assert nsub != null;
            nsub.set(MiddlesAnnotation.class, "subject");
        }
        return result;
    }

    static String constructVerb(IndexedWord verb, SemanticGraph depParse) {
        if (verb == null) {
            return "";
        }
        String result = verb.lemma();

        List<IndexedWord> compoundParts = SemanticGraphUtils.getChildrenWithRelnPrefix(depParse, verb, Arrays.asList("compound:prt"));

        if (compoundParts != null) {
            for (IndexedWord part : compoundParts) {
                result = result + "_" + part.word();
            }

        }

        return result;
    }

    /*
     * Reads in an entire file as a string.*/
    private static String readFile(String path, Charset encoding)
            throws IOException {
        if(path == null || encoding == null) {
            throw new IllegalArgumentException("invalid null input value");
        }
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private void initializePipeline() {
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma, depparse");
        // set a property for an annotator, in this case the coref annotator is being set to use the neural algorithm
        props.setProperty("coref.algorithm", "neural");
        // build pipeline
        pipeline = new StanfordCoreNLP(props);
        System.out.println(StanfordCoreNLP.getExistingAnnotator("depparse"));


    }

}

class MiddlesAnnotation implements CoreAnnotation<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }
}
