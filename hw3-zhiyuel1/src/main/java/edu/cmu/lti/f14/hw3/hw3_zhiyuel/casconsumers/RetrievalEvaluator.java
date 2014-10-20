package edu.cmu.lti.f14.hw3.hw3_zhiyuel.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  public ArrayList<Integer> rankList;

  public List<Double> coslist;

  public ArrayList<Map<String, Integer>> queryVectorlist;

  public ArrayList<Map<String, Integer>> docVectorlist;

  private int lines = 0, gold = 0;

  private double goldcos = 0;
  private String text="";
  private int saveqid=0;

  private File out = null;

  private BufferedWriter bw = null;
  
  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();
    rankList = new ArrayList<Integer>();
    coslist = new ArrayList<Double>();
    queryVectorlist = new ArrayList<Map<String, Integer>>();
    docVectorlist = new ArrayList<Map<String, Integer>>();
    try {
      System.out.println("OUTPUT_FILE:" + (String) getConfigParameterValue("OUTPUT_FILE"));
      out = new File((String) getConfigParameterValue("OUTPUT_FILE"));
      bw = new BufferedWriter(new FileWriter(out));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   * calculate the first n-1 query's cosine similarity and its rank, and save the information of last query to be calculated in the complete function
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {
//    System.out.println("Retrival evaluator...");
    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();
      int queryid = doc.getQueryID();
      int rel = doc.getRelevanceValue();
      
      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      Map<String, Integer> queryVector = new HashMap<String, Integer>();
      Map<String, Integer> docVector = new HashMap<String, Integer>();
      if (rel == 99) {
        for (Token t : tokenList) {
          queryVector.put(t.getText(), t.getFrequency());
        }
        queryVectorlist.add(queryVector);
      } else {
        for (Token t : tokenList) {
          docVector.put(t.getText(), t.getFrequency());
        }

        // compute similarity with its query doc
        double cos = computeCosineSimilarity(queryVectorlist.get(queryid - 1), docVector);
        log(cos);
        coslist.add(cos);

        if (rel == 1) {
          gold = lines;
          goldcos = cos;
          text=doc.getText();
          saveqid=queryid;
//          System.out.println("goldcos" + goldcos);
        }
      }
      // when we encounter some item has different
      if (qIdList.size() >= 1 && qIdList.get(qIdList.size() - 1) != queryid) {
//        System.out.println("lines:"+lines);//record the gold answer line
        sortSimilarity(coslist,gold);
//        log(gold);
//        log(coslist);
        int rank = findRank(coslist, goldcos);
        
        try {
          writeIntoFile(String.format("%.4f", goldcos), rank, saveqid, 1, text);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        rankList.add(rank);
        coslist = new ArrayList<Double>();
      }

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      // Do something useful here
      lines++;
    }
  }
/**
 * print the intermediate results with the following methods
 * */
  private void log(List<Double> arr) {
    for (double d : arr) {
      System.out.println(d + "");
    }
  }

  private void log(int a) {
    System.out.println(a + "");
  }

  private void log(double a) {
    System.out.println(a + "");
  }

  private void log(String a) {
    System.out.println(a);
  }

  /**
   * 
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);
    // TODO :: compute the cosine similarity measure
    sortSimilarity(coslist,gold);
//    log(coslist);
 // TODO :: compute the rank of retrieved sentences
    int rank = findRank(coslist, goldcos);
    try {
      writeIntoFile(String.format("%.4f", goldcos), rank, saveqid, 1, text);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    rankList.add(rank);
    coslist = new ArrayList<Double>();
    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    try {
      writeFile(metric_mrr);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }
/**
 * @param cosine a format of similarity value
 * @param rank  its rank due to its cosine similarity
 * @param qid
 * @param rel
 * @param text
 * <p>write the information into the file with a specific format</p>
 * */
  public void writeIntoFile(String cosine, int rank, int qid, int rel, String text)
          throws Exception {

    String phrase = "cosine="+cosine + "\t" + "rank="+rank + "\t" +"qid="+qid+"\t"+"rel="+rel+ "\t" + text+" ";
    
    bw.write(phrase);
    bw.newLine();
    bw.flush();
  }
  public void writeFile(double mrr)
          throws Exception {
    
    String phrase ="MRR="+mrr;
    bw.write(phrase);
    bw.newLine();
    bw.flush();
  }
  /**
   * @param cos sorted similarity list
   * @param goldcos gold answer's cosine similarity
   * @return the rank of the gold answer according to its cosine similarity
   * */
  private int findRank(List<Double> cos, double goldcos) {
    for (int i = 0; i < cos.size(); i++) {
      if (goldcos == cos.get(i))
        return i + 1;
    }
    return -1;
  }
/**
 * @param cos unsorted list
 * @param k if we have tie, and k with more relevance ranks higher
 * @return sorted similarity list
 * */
  private List<Double> sortSimilarity(List<Double> cos, int k) {
    for (int i = 0; i < cos.size(); i++) {
      for (int j = i + 1; j < cos.size(); j++) {
        if (cos.get(i) < cos.get(j)) {
            swap(cos, i, j);
        }
        else if(cos.get(i)==cos.get(j)){
          log("same!!!!!!!!!");
          if(j==k){
            log(cos.get(j)+"|"+j);
            swap(cos,i,j);
          }
        }
      }
    }
    return cos;
  }

  private void swap(List<Double> cos, int i, int j) {
    double temp = cos.get(i);
    cos.set(i, cos.get(j));
    cos.set(j, temp);
  }

  /**
   * @param queryVector
   * @param docVector
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;

    // TODO :: compute cosine similarity between two sentences
    for (Map.Entry<String, Integer> qm : queryVector.entrySet()) {
      String key = qm.getKey();
      if (!docVector.containsKey(key)) {
        docVector.put(key, 0);
      }
    }
    for (Map.Entry<String, Integer> dm : docVector.entrySet()) {
      String key = dm.getKey();
      if (!queryVector.containsKey(key)) {
        queryVector.put(key, 0);
      }
    }
    // System.out.println(docVector.size()+"|"+queryVector.size());
    for (Map.Entry<String, Integer> dm : docVector.entrySet()) {
      for (Map.Entry<String, Integer> qm : queryVector.entrySet()) {
        if (dm.getKey().equals(qm.getKey()))
          cosine_similarity += dm.getValue() * qm.getValue();
      }
    }
    double lenq = compute_eucli(queryVector);
    double lend = compute_eucli(docVector);
    // System.out.println("cosine_similarity"+cosine_similarity+",lenq"+lenq+",lend"+lend);
    cosine_similarity = cosine_similarity / (lenq * lend);
    // System.out.println("cosine_similarity"+cosine_similarity+",lenq"+lenq+",lend"+lend);
    return cosine_similarity;
  }

/**
 * @param vector
 * @return  euclidean length
 * compute euclidean length to normalize the vector
 * */
  private double compute_eucli(Map<String, Integer> vector) {
    double len = 0;
    for (Map.Entry<String, Integer> v : vector.entrySet()) {
      int val = v.getValue();
      len += Math.pow(val, 2);
    }
    return Math.sqrt(len);
  }

  /**
   * compute the mrr value with its formula
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
    for(int i=0;i<rankList.size();i++){
      metric_mrr=metric_mrr+1.0/rankList.get(i);
    }
//    log(metric_mrr/rankList.size());
    return metric_mrr/rankList.size();
  }

}
