package edu.yu.cs.com1320.project.stage5.impl;

//import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1CFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


public class DocumentImpl implements Document{

    Map<String, Integer> words= new HashMap<>();
    private Long lastUsedTime;


    public DocumentImpl(URI uri, String txt, int txtHash)
    {
        this.uri=uri;
        this.txt=txt;
        this.txtHash=txtHash;
        HashMap<String, Integer> words = new HashMap<>();
        this.words=words;
        lastUsedTime=System.nanoTime();



        if(uri == null || txt== null)
        {
            throw new IllegalArgumentException("cant be null inputs");
        }


    }
    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes){
        this.uri=uri;
        this.txt=txt;
        this.txtHash=txtHash;
        this.pdfBytes=pdfBytes;
        HashMap<String, Integer> words = new HashMap<>();
        this.words=words;
        lastUsedTime=System.nanoTime();



        if(uri == null || txt== null)
        {
            throw new IllegalArgumentException("cant be null inputs");
        }

    }


    InputStream file = this.file;
    URI uri=this.uri;
    String txt=this.txt;
    int txtHash=this.txtHash;
    byte[] pdfBytes=this.pdfBytes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentImpl document = (DocumentImpl) o;

        if (uri != null ? !uri.equals(document.uri) : document.uri != null) return false;
        return txt != null ? txt.equals(document.txt) : document.txt == null;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int key=1;
        key=prime*key+((txt==null)?0:txt.hashCode());
        key=prime*key+uri.hashCode();
        return key;
    }
    public byte[] getDocumentAsPdf() {
        try
        {
            PDDocument document = new PDDocument();

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document,page);
            contentStream.beginText();

            contentStream.newLineAtOffset(20, 450);
            PDFont font = PDType1Font.HELVETICA_BOLD;
            contentStream.setFont(font,12 );



            this.txt=this.txt.replace("\n", "").replace("\r","");
            contentStream.showText(this.txt);
            contentStream.endText();

            contentStream.close();

            ByteArrayOutputStream array = new ByteArrayOutputStream();

            document.save(array);
            document.close();


            return array.toByteArray();
        }
        catch(IOException e)
        {
            System.out.println("your file needs to exist");
            return null;
        }
    }

    public String getDocumentAsTxt() {



        return this.txt;

    }

    public int getDocumentTextHashCode() {

        String text = this.getDocumentAsTxt();
        return text.hashCode();
    }

    public URI getKey() {
        return this.uri;
    }

    /**
     * how many times does the given word appear in the document?
     *
     * @param word
     * @return the number of times the given words appears in the document
     */

    public int wordCount(String word) {

        return words.get(word);
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */

    public long getLastUseTime() {
        return this.lastUsedTime;
    }


    public void setLastUseTime(long timeInMilliseconds) {
        this.lastUsedTime=timeInMilliseconds;

    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    public Map<String, Integer> getWordMap() {
        return this.words;
    }

    /**
     * This must set the word to count map during deserialization
     *
     * @param wordMap
     */
    public void setWordMap(Map<String, Integer> wordMap) {
        this.words=wordMap;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */


    @Override
    public int compareTo(Document o) {

        if(o==null) {
            return 0;
        }
        if(this.getLastUseTime()>o.getLastUseTime())
        {
            return 1;
        }
        if(this.getLastUseTime()<o.getLastUseTime())
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
}
