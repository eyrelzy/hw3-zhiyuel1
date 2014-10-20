package edu.cmu.lti.f14.hw3.hw3_zhiyuel.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_zhiyuel.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      // every line in meta data is a document
      // among them rel=99 is a query document
      createTermFreqVector(jcas, doc);

    }

  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   * 
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();

    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }

  /**
   *  construct a vector of tokens and update the tokenList in CAS using tokenize0 from above
   * @param jcas
   * @param doc
   * @param aToken
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
//    if (docText.endsWith("."))
//      docText = docText.substring(0, docText.length() - 1);
//    docText = StanfordLemmatizer.stemText(docText);
//    System.out.println("====" + docText);
    // TO DO: construct a vector of tokens and update the tokenList in CAS
    // TO DO: use tokenize0 from above
    List<String> arr = tokenize0(docText);

    // String[] spam = docText.split(" ");
    Map<String, Integer> freq = new HashMap<String, Integer>();
    for (int i = 0; i < arr.size(); i++) {

      /*
       * // change to lowercase for the first capital letter! String text=arr.get(i).toLowerCase();
       * //delete 's if(text.endsWith("'s")||text.endsWith("s'")||text.endsWith("--")){
       * text=text.substring(0, text.length()-2); // System.out.println(text); } //punctuation
       * if(text
       * .endsWith("\"")||text.endsWith(",")||text.endsWith(";")||text.endsWith(".")||text.endsWith
       * ("?")||text.endsWith("!")) { text=text.substring(0,text.length()-1); if(text.endsWith("."))
       * text=text.substring(0,text.length()-1); }
       * 
       * if(text.startsWith("\"")||text.startsWith(",")||text.startsWith(";")||text.startsWith("."))
       * text=text.substring(1); System.out.println(text); if (!freq.containsKey(text)) {
       * 
       * freq.put(text, 1); } else { for (Map.Entry<String, Integer> m : freq.entrySet()) { if
       * (arr.get(i).equals(m.getKey())) { freq.put(m.getKey(), (int) m.getValue() + 1); } } }
       */
      // original

      if (!freq.containsKey(arr.get(i))) {
        freq.put(arr.get(i), 1);
      } else {
        for (Map.Entry<String, Integer> m : freq.entrySet()) {
          if (arr.get(i).equals(m.getKey())) {
            freq.put(m.getKey(), (int) m.getValue() + 1);
          }
        }
      }

    }
    List<Token> aToken = new ArrayList<Token>();

    for (Map.Entry<String, Integer> m : freq.entrySet()) {
      // System.out.println(id+"|"+rel+"|"+m.getKey() + "," + m.getValue());
      Token t = new Token(jcas);
      t.setText(m.getKey());
      t.setFrequency(m.getValue());
      aToken.add(t);
      t.addToIndexes();
    }

    FSList fsTokenList = Utils.fromCollectionToFSList(jcas, aToken);
    // update the token list in cas??????
    doc.setTokenList(fsTokenList);

  }

}
