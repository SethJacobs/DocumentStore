package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.*;

//import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.text.PDFTextStripper;



import java.io.*;
import java.net.URI;
import java.io.InputStream;

import java.util.*;
import java.util.function.Function;



public class DocumentStoreImpl implements DocumentStore {


   // HashTableImpl<URI, DocumentImpl> hashTable = new HashTableImpl();
    StackImpl<Undoable> commandStack = new StackImpl<>();
    TrieImpl<URI> trieImpl = new TrieImpl<URI>();
    MinHeapImpl<HeapNode> minHeap = new MinHeapImpl<>();
    BTreeImpl<URI, DocumentImpl> bTree = new BTreeImpl<>();
    HashMap<URI, HeapNode> heapContents = new HashMap<>();




    int maxDocBytes;
    int maxDocCount=heapContents.size();
    int maxDocCountLimit=Integer.MAX_VALUE;
    int maxDocBytesLimit=Integer.MAX_VALUE;
    File baseDir;

    public DocumentStoreImpl()
    {
        this.baseDir= new File(System.getProperty("user.dir"));
        PersistenceManager persistenceManager = new DocumentPersistenceManager(baseDir);
        bTree.setPersistenceManager(persistenceManager);
        String emptyString ="";
        URI sentinel = URI.create(emptyString);
        bTree.put(sentinel,null);
    }

    public DocumentStoreImpl(File baseDir)
    {
        this.baseDir= baseDir;
        PersistenceManager persistenceManager = new DocumentPersistenceManager(baseDir);
        bTree.setPersistenceManager(persistenceManager);
        String emptyString ="";
        URI sentinel = URI.create(emptyString);
        bTree.put(sentinel,null);

    }





    /**
     * @param input  the document being put
     * @param uri    unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return the hashcode of the String version of the documents
     */
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        int oldHash = 0;
        int hi;


        if (uri == null || format == null) {
            throw new IllegalArgumentException("cant have null format or uri");
        }

        if (bTree.get(uri) != null) {
            oldHash = bTree.get(uri).getDocumentAsTxt().hashCode();
            bTree.get(uri).setLastUseTime(System.nanoTime());

        }
        if (input == null) {

            if (deleteDocument(uri)) {
                String txt = "txt";
                DocumentImpl document = new DocumentImpl(uri, txt, txt.hashCode());

                // oldHash=hashTable.get(uri).hashCode();
                return oldHash;
            } else {
                return 0;
            }
        }
       // if (deleteDocument(uri)) {

        //    putDocument(input, uri, format);
        //    return oldHash;
      //  } else {
      //      commandStack.pop();
     //   }
        byte[] bytes = new byte[0];
        try {
            bytes = new byte[input.available()];
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            input.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (format == DocumentFormat.TXT) {
            String array = new String(bytes);
            DocumentImpl document = new DocumentImpl(uri, array, array.hashCode());

            if (bTree.get(uri) != null) {
                DocumentImpl doc = bTree.get(uri);
                Function<URI, Boolean> function = uriB -> {
                    bTree.put(uri, null);
                    for (String word : doc.getWordMap().keySet()) {
                        trieImpl.delete(word, document.uri);
                    }
                    bTree.put(uri, doc);
                    putWordMap(bTree.get(uri));
                    doc.setLastUseTime(System.nanoTime());
                    updateCountRemove(document);
                    updateCountAdd(doc);
                    document.setLastUseTime(Long.MIN_VALUE);
                    minHeap.insert(heapContentsAdd(doc.uri,doc));



                    minHeap.removeMin();
                    maxDocCheck();
                    return true;
                };
                GenericCommand command = new GenericCommand(uri, function);
                commandStack.push(command);
                oldHash = bTree.get(uri).txt.hashCode();
                bTree.put(uri, null);
                bTree.put(uri, document);
                putWordMap(bTree.get(uri));
                document.setLastUseTime(System.nanoTime());
                minHeap.insert(heapContentsAdd(document.uri, document));
                updateCountAdd(document);
                maxDocCheck();

                return oldHash;
            }

            bTree.put(uri, document);
            putWordMap(bTree.get(uri));
            document.setLastUseTime(System.nanoTime());

            minHeap.insert(heapContentsAdd(document.uri, document));
            updateCountAdd(document);
            maxDocCheck();
            Function<URI, Boolean> function = uriB -> {
                document.setLastUseTime(Long.MIN_VALUE);
                minHeap.reHeapify(heapContentsRemove(document.uri));
                minHeap.removeMin();

                updateCountRemove(document);
                bTree.put(uri, null);
                for (String word : document.words.keySet()) {
                    trieImpl.delete(word, document.uri);
                }

                return true;
            };
            GenericCommand command = new GenericCommand(uri, function);
            commandStack.push(command);
        }
        if (format == DocumentFormat.PDF) {
            PDDocument documentA = null;

            try {
                documentA = PDDocument.load(bytes);
                PDFTextStripper PDFString = new PDFTextStripper();
                String text = PDFString.getText(documentA);
                documentA.close();
                DocumentImpl document = new DocumentImpl(uri, text.replace("\n", ""), text.hashCode(), bytes);
                document.setLastUseTime(System.nanoTime());
                putWordMap(document);

                if (bTree.get(uri) != null) {
                    DocumentImpl doc = bTree.get(uri);
                    Function<URI, Boolean> function = uriB -> {

                        bTree.put(uri, null);
                        for (String word : document.words.keySet()) {
                            trieImpl.delete(word,document.getKey());
                        }
                        bTree.put(uri, doc);
                        putWordMap(bTree.get(uri));
                        doc.setLastUseTime(System.nanoTime());


                        minHeap.insert(heapContentsAdd(doc.uri, doc));

                        document.setLastUseTime(System.nanoTime());

                        minHeap.reHeapify(heapContentsRemove(document.uri));
                        minHeap.removeMin();

                        updateCountRemove(document);
                        updateCountAdd(doc);
                        maxDocCheck();
                        return true;
                    };
                    GenericCommand command = new GenericCommand(uri, function);
                    commandStack.push(command);
                    oldHash = bTree.get(uri).txt.hashCode();
                    bTree.put(uri, null);
                    bTree.put(uri, document);
                    putWordMap(bTree.get(uri));
                    document.setLastUseTime(System.nanoTime());

                    minHeap.insert(heapContentsAdd(uri, document));
                    updateCountAdd(document);
                    maxDocCheck();

                    return oldHash;
                }
                bTree.put(uri,document);
                minHeap.insert(heapContentsAdd(document.uri, document));
                updateCountAdd(document);

                maxDocCheck();
                Function<URI, Boolean> function = uriB -> {
                    bTree.put(uri, null);
                    for (String word : document.words.keySet()) {
                        trieImpl.delete(word, document.getKey());
                    }
                    document.setLastUseTime(Long.MIN_VALUE);

                    minHeap.reHeapify(heapContents.get(document.uri));
                    minHeap.removeMin();
                    updateCountRemove(document);
                    return true;
                };
                GenericCommand command = new GenericCommand(uri, function);
                commandStack.push(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return 0;
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as a PDF, or null if no document exists with that URI
     */
    public byte[] getDocumentAsPdf(URI uri) {


        if (bTree.get(uri) == null) {
            return null;
        } else {
            DocumentImpl document = new DocumentImpl(uri, bTree.get(uri).txt, bTree.get(uri).txt.hashCode());
            if(heapContents.get(document.getKey())==null)
            {
                minHeap.insert(heapContentsAdd(document.getKey(),document));
            }
            document.txt = bTree.get(uri).txt;
            bTree.get(uri).setLastUseTime(System.nanoTime());
            HeapNode heapNode = heapContents.get(uri);

               heapNode.setLastUsedTime(System.nanoTime());


            minHeap.reHeapify(heapNode);

            return document.getDocumentAsPdf();
        }

    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as TXT, i.e. a String, or null if no document exists with that URI
     */
    public String getDocumentAsTxt(URI uri) {

        if (bTree.get(uri) == null) {
            return null;
        } else {
            DocumentImpl doc = bTree.get(uri);
            if(heapContents.get(doc.getKey())==null)
            {
                minHeap.insert(heapContentsAdd(doc.getKey(),doc));
            }
            doc.setLastUseTime(System.nanoTime());
            HeapNode heapNode = heapContents.get(uri);
            if (heapContents.get(uri)!=null) {
                 heapNode.setLastUsedTime(System.nanoTime());
            }
            minHeap.reHeapify(heapNode);
            return bTree.get(uri).txt.trim();
        }
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean deleteDocument(URI uri) {

        if (bTree.get(uri) == null) {
            Function<URI, Boolean> function = uriB -> {
                return true;
            };
            GenericCommand command = new GenericCommand(uri, function);
            commandStack.push(command);
            return false;
        } else {
            DocumentImpl document = bTree.get(uri);
            for (String word : document.words.keySet()) {
                trieImpl.delete(word, document.getKey());
            }
            HeapNode heapNode = heapContents.get(document.getKey());
            heapNode.setLastUsedTime(Long.MIN_VALUE);
            minHeap.reHeapify(heapNode);
            minHeap.removeMin();
            heapContentsRemove(document.uri);
            updateCountRemove(document);
            bTree.put(uri, null);
            document.setLastUseTime(Long.MIN_VALUE);


            Function<URI, Boolean> function = uriB -> {
                bTree.put(uri, document);
                putWordMap(bTree.get(uri));

                minHeap.insert(heapContentsAdd(document.uri, document));
                document.setLastUseTime(System.nanoTime());
                updateCountAdd(document);
                document.setLastUseTime(System.nanoTime());
                minHeap.reHeapify(heapContents.get(document.getKey()));
                maxDocCheck();

                return true;
            };
            GenericCommand<Undoable> command = new GenericCommand(uri, function);
            commandStack.push(command);
        }
        return true;
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    public void undo() throws IllegalStateException {
        if (commandStack.peek() == null) {
            throw new IllegalStateException("command Stack was empty");
        }
        if (commandStack.peek() instanceof GenericCommand) {
            commandStack.peek().undo();
            commandStack.pop();
        }

        if (commandStack.peek() instanceof CommandSet) {
            for (GenericCommand command : (CommandSet<GenericCommand>) commandStack.peek()) {
                command.undo();
            }
            commandStack.pop();
        }
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     *
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    public void undo(URI uri) throws IllegalStateException {

        if(commandStack.peek()==null)
        {
            throw new IllegalStateException();
        }
        StackImpl<Undoable> temp = new StackImpl<>();
        {
            while (commandStack.peek()!=null)
            {
                if(commandStack.peek() instanceof GenericCommand)
                {
                    if(((GenericCommand) commandStack.peek()).getTarget()==uri)
                    {
                        commandStack.peek().undo();
                        commandStack.pop();
                        break;
                    }
                    else
                    {
                        temp.push(commandStack.peek());
                        commandStack.pop();
                    }

                }
                if(commandStack.peek() instanceof CommandSet)
                {
                    if(((CommandSet)commandStack.peek()).containsTarget(uri)){
                        ((CommandSet) commandStack.peek()).undo(uri);
                        break;
                    }
                    if(commandStack.peek()==null)
                    {
                        commandStack.pop();
                        break;
                    }
                    else
                    {
                        temp.push(commandStack.peek());
                        commandStack.pop();
                    }

                }
            }
        }
        commandStack=temp;


    }


    private HashMap<String, Integer> putWordMap(DocumentImpl document) //adds a set of words to times appeared for each word and adds words to trie with their documents as value
    {

        String[] split = document.txt.replaceAll("[^a-zA-Z0-9\\s\n]", "").toUpperCase().split(" ");
        for (int i = 0; i < split.length; i++) {
            if (document.words.containsKey(split[i])) {
                int value = (int) document.words.get(split[i]) + 1;
                document.words.put(split[i], value);
                trieImpl.put(split[i], document.getKey());
            } else {
                document.words.put(split[i], 1);
                trieImpl.put(split[i], document.getKey());
            }

        }

        return (HashMap<String, Integer>) document.words;
    }


    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<String> search(String keyword) {

        Comparator<URI> comparator = (o1, o2) -> {
            if(bTree.get(o1).words!=null && bTree.get(o2).words!=null) {
                if (bTree.get(o1).words.get(keyword) == bTree.get(o2).words.get(keyword)) {
                    return 0;
                } else if (bTree.get(o1).wordCount(keyword) > bTree.get(o2).wordCount(keyword)) {
                    return -1;
                } else
                    return 1;
            }
            return 0;
        };

        Comparator<DocumentImpl> compare =(o1,o2)-> {
            if (o1.wordCount(keyword.toUpperCase()) == o2.wordCount(keyword.toUpperCase())) {
                return 0;
            } else if (o1.wordCount(keyword.toUpperCase()) > o2.wordCount(keyword.toUpperCase())) {
                return 1;
            } else {
                return -1;
            }
        };

        List<Set<URI>> list = (List) trieImpl.getAllSorted(keyword, comparator);
        List<String> stringList = new ArrayList<>();
        Long time = System.nanoTime();
        List<DocumentImpl> fun = new ArrayList<>();
        for (Set<URI> set : list) {
            List<URI> order = new ArrayList<>(set);
            Collections.sort(order, comparator);
            set = new HashSet<URI>(order);



            for (URI uri : set) {
                fun.add(bTree.get(uri));
            }
            Collections.sort(fun, compare);
        }


            /*for (URI uri : set) {
                System.out.println(bTree.get(uri).getDocumentAsTxt());
                stringList.add(bTree.get(uri).getDocumentAsTxt());
                bTree.get(uri).setLastUseTime(time);
                HeapNode heapNode = heapContents.get(uri);
                if (heapContents.get(uri)!=null) {
                    heapNode.setLastUsedTime(time);
                }

                minHeap.reHeapify(heapNode);

            }

        }

             */
            for(DocumentImpl doc :fun)
            {
                stringList.add(doc.getDocumentAsTxt());
                doc.setLastUseTime(time);
                HeapNode heapNode = heapContents.get(doc.uri);
                if(heapContents.get(doc.getKey())==null)
                {
                    minHeap.insert(heapContentsAdd(doc.getKey(),doc));
                    heapNode=heapContents.get(doc.getKey());
                    heapNode.setLastUsedTime(time);
                    updateCountAdd(doc);
                    maxDocCheck();
                }
                heapNode.setLastUsedTime(time);
                minHeap.reHeapify(heapNode);
            }

        return stringList;


    }


    /**
     * same logic as search, but returns the docs as PDFs instead of as Strings
     *
     * @param keyword
     */
    public List<byte[]> searchPDFs(String keyword) {


        Comparator<URI> comparator = new Comparator<URI>() {
            @Override
            public int compare(URI o1, URI o2) {
                if (bTree.get(o1).wordCount(keyword.toUpperCase()) == bTree.get(o2).wordCount(keyword.toUpperCase())) {
                    return 0;
                } else if (bTree.get(o1).wordCount(keyword.toUpperCase()) > bTree.get(o2).wordCount(keyword.toUpperCase())) {
                    return -1;
                } else
                    return 1;
            }
        };

        Comparator<DocumentImpl> compare =(o1,o2)-> {
            if (o1.wordCount(keyword.toUpperCase()) == o2.wordCount(keyword.toUpperCase())) {
                return 0;
            } else if (o1.wordCount(keyword.toUpperCase()) > o2.wordCount(keyword.toUpperCase())) {
                return 1;
            } else {
                return -1;
            }
        };

        List<Set<URI>> list = (List) trieImpl.getAllSorted(keyword, comparator);
        List<byte[]> byteList = new ArrayList<>();
        Long time = System.nanoTime();
        List<DocumentImpl> fun = new ArrayList<>();
        for (Set<URI> set : list) {
            List<URI> order = new ArrayList<>(set);
            Collections.sort(order, comparator);
            set = new HashSet<URI>(order);

            /*
            for (URI uri : set) {
                byteList.add(bTree.get(uri).getDocumentAsPdf());
                bTree.get(uri).setLastUseTime(time);
                HeapNode heapNode = heapContents.get(uri);
                heapNode.setLastUsedTime(time);
                updateLastUsedTime(bTree.get(uri),time);
                minHeap.reHeapify(heapNode);
            }

        }
        */
            for (URI uri : set) {
                fun.add(bTree.get(uri));
            }
            Collections.sort(fun, compare);
            for (DocumentImpl doc : fun) {
                byteList.add(doc.getDocumentAsPdf());
                doc.setLastUseTime(time);
                HeapNode heapNode = heapContents.get(doc.uri);
                heapNode.setLastUsedTime(time);
                minHeap.reHeapify(heapNode);
            }

        }

        return byteList;
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<String> searchByPrefix(String prefix) {
        HashMap<DocumentImpl, Integer> prefixCount = new HashMap<>();
        Comparator<URI> fake = new Comparator<URI>() {
            @Override
            public int compare(URI o1, URI o2) {
                return 0;
            }
        };
        // List<Set<DocumentImpl>> original = (List) trieImpl.getAllWithPrefixSorted(prefix, fake);
        //  for(Set<DocumentImpl> set: original) {
        //    for (DocumentImpl doc : set) {
        //       if (prefixCount.containsKey(doc)) {
        //          prefixCount.put(doc, prefixCount.get(doc) + 1);
        //      } else {
        //         prefixCount.put(doc, 1);
        //     }
        // }
        // }
        List<URI> first = trieImpl.getAllWithPrefixSorted(prefix, fake);
        for (URI uri : first) {
            if (prefixCount.containsKey(bTree.get(uri))) {
                prefixCount.put(bTree.get(uri), prefixCount.get(bTree.get(uri)) + 1);
            } else {
                prefixCount.put(bTree.get(uri), 1);
            }
        }
        Comparator<URI> real = new Comparator<URI>() {
            @Override
            public int compare(URI o1, URI o2) {
                if (prefixCount.get(bTree.get(o1)) == prefixCount.get(bTree.get(o2))) {
                    return 0;
                } else if (prefixCount.get(bTree.get(o1)) > prefixCount.get(bTree.get(o2))) {
                    return -1;
                } else {
                    return 1;
                }

            }
        };

        Comparator<DocumentImpl> docCompare = new Comparator<DocumentImpl>(){

            @Override
            public int compare(DocumentImpl doc1, DocumentImpl doc2)
            {
                if(prefixCount.get(doc1)==prefixCount.get(doc2))
                {
                    return 0;
                }
                else if(prefixCount.get(doc1)>prefixCount.get(doc2))
                {
                    return -1;
                }
                else
                    return 1;
            }
        };

        List<URI> list = trieImpl.getAllWithPrefixSorted(prefix, real);
        List<String> stringList = new ArrayList<>();
        Long time = System.nanoTime();
        list.sort(real);
        List<DocumentImpl> again = new ArrayList<>();
        for (URI uri : list) {
            again.add(bTree.get(uri));
            stringList.add(bTree.get(uri).getDocumentAsTxt());
            bTree.get(uri).setLastUseTime(time);
            HeapNode heapNode = new HeapNode(uri, bTree.get(uri).getLastUseTime());
            updateLastUsedTime(bTree.get(uri),time);
            minHeap.reHeapify(heapNode);
        }
        again.sort(docCompare);
        stringList=new ArrayList<>();
        for(DocumentImpl doc: again)
        {
            stringList.add(doc.getDocumentAsTxt());
        }

        return stringList;
    }

    /**
     * same logic as searchByPrefix, but returns the docs as PDFs instead of as Strings
     *
     * @param prefix
     */
    public List<byte[]> searchPDFsByPrefix(String prefix) {
        HashMap<DocumentImpl, Integer> prefixCount = new HashMap<>();
        Comparator<URI> fake = new Comparator<URI>() {
            @Override
            public int compare(URI o1, URI o2) {
                return 0;
            }
        };
        List<URI> original = trieImpl.getAllWithPrefixSorted(prefix, fake);
        for (URI uri : original) {
            if (prefixCount.containsKey(bTree.get(uri))) {
                prefixCount.put(bTree.get(uri), prefixCount.get(bTree.get(uri)) + 1);
            } else {
                prefixCount.put(bTree.get(uri), 1);
            }
        }
        Comparator<URI> real = new Comparator<URI>() {
            @Override
            public int compare(URI o1, URI o2) {
                if (prefixCount.get(o1) == prefixCount.get(o2)) {
                    return 0;
                } else if (prefixCount.get(o1) > prefixCount.get(o2)) {
                    return -1;
                } else {
                    return 1;
                }

            }
        };


        List<URI> list = trieImpl.getAllWithPrefixSorted(prefix, real);
        List<byte[]> byteList = new ArrayList<>();
        Long time = System.nanoTime();
        for (URI uri : list) {
            byteList.add(getDocumentAsPdf(bTree.get(uri).uri));
            bTree.get(uri).setLastUseTime(time);
            HeapNode heapNode = new HeapNode(uri, bTree.get(uri).getLastUseTime());
            updateLastUsedTime(bTree.get(uri),time);
            minHeap.reHeapify(heapNode);
        }
        return byteList;
    }

    /**
     * delete ALL exact matches for the given key
     *
     * @param key
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAll(String key) {

        Set<URI> set = trieImpl.deleteAll(key);
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        Long time = System.nanoTime();
        Set<URI> uriSet = new HashSet<>();
        for (URI uri : set) {
            DocumentImpl doc = bTree.get(uri);
            Function<URI, Boolean> function = uriB -> {
                bTree.put(uri, doc);
                putWordMap(doc);
                minHeap.insert(heapContentsAdd(uri, doc));
                bTree.get(uri).setLastUseTime(time);
                maxDocCheck();
                return true;
            };
            GenericCommand command = new GenericCommand(uri, function);
            commandSet.addCommand(command);
            uriSet.add(uri);
            doc.setLastUseTime(Long.MIN_VALUE);
            HeapNode heapNode = new HeapNode(uri, doc.getLastUseTime());
            updateLastUsedTime(doc,time);
            minHeap.reHeapify(heapNode);
            bTree.put(uri,null);
        }
        commandStack.push(commandSet);

        return uriSet;
    }

    /**
     * Delete all matches that contain a String with the given prefix.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String prefix) {

        Set<URI> set = trieImpl.deleteAllWithPrefix(prefix);
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        Long time = System.nanoTime();
        for (URI uri : set) {
            DocumentImpl document = bTree.get(uri);
            Function<URI, Boolean> function = uriB -> {
                bTree.put(uri, document);
                putWordMap(document);

                return true;
            };
            GenericCommand command = new GenericCommand(uri, function);
            commandSet.addCommand(command);
        }
        commandStack.push(commandSet);

        Set<URI> uriSet = new HashSet<>();

        for (URI uri : set) {
            uriSet.add(uri);
            bTree.get(uri).setLastUseTime(Long.MIN_VALUE);
            HeapNode heapNode = new HeapNode(uri, bTree.get(uri).getLastUseTime());
            updateLastUsedTime(bTree.get(uri),time);
            minHeap.reHeapify(heapNode);
            bTree.put(uri,null);
        }
        return uriSet;
    }


    public void setMaxDocumentCount(int limit) {
        this.maxDocCountLimit=limit;
        maxDocCheck();
    }


    public void setMaxDocumentBytes(int limit) {
        this.maxDocBytesLimit=limit;
        maxDocCheck();
    }

    private void updateCountAdd(DocumentImpl doc)
    {
        this.maxDocCount=this.heapContents.size();
        this.maxDocBytes=this.maxDocBytes+((doc.txt.getBytes().length)+doc.getDocumentAsPdf().length);
    }

    private void updateCountRemove(DocumentImpl doc)
    {
        this.maxDocCount=heapContents.size();
        this.maxDocBytes=this.maxDocBytes-((doc.txt.getBytes().length)+doc.getDocumentAsPdf().length);
    }


    private void maxDocCheck()
    {


        while(this.maxDocCount>this.maxDocCountLimit || this.maxDocBytes>this.maxDocBytesLimit)
        {
            HeapNode heapNode = minHeap.removeMin();
            DocumentImpl doc= bTree.get(heapNode.uri);
            minHeap.insert(heapNode);
//            bTree.get(heapNode.getUri()).setLastUseTime(Long.MIN_VALUE);
            minHeap.reHeapify(heapNode);
            minHeap.removeMin();
            heapContents.remove(heapNode.getUri());
            //deleteDocument(heapNode.getUri());
            updateCountRemove(doc);
            try {
                bTree.moveToDisk(doc.getKey());
            } catch (Exception e) {
                e.printStackTrace();
           }


            /*StackImpl<Undoable> temp = new StackImpl<>();
            while (commandStack.peek()!=null)
            {
                if (commandStack.peek() instanceof GenericCommand)
                    if (((GenericCommand) commandStack.peek()).getTarget()==heapNode.getUri())
                    {
                        commandStack.pop();
                    }
                    else
                    {
                        temp.push(commandStack.peek());
                        commandStack.pop();
                    }
                if ((commandStack.pop() instanceof CommandSet))
                {
                    Iterator iterator = ((CommandSet) commandStack.peek()).iterator();
                    while (iterator.hasNext())
                    {
                        if(((GenericCommand)iterator.next()).getTarget()==heapNode.getUri())
                        {
                            iterator.remove();
                        }
                    }
                    if (commandStack.peek()!=null)
                    {
                        temp.push(commandStack.peek());
                    }
                    commandStack.pop();
                }
            }
            commandStack=temp;

             */
        }
    }








    private HeapNode heapContentsAdd(URI uri, DocumentImpl doc)
    {
        HeapNode heapNode = new HeapNode(uri, doc.getLastUseTime());
        heapContents.put(uri, heapNode);
        heapNode.setLastUsedTime(System.nanoTime());
        minHeap.reHeapify(heapNode);
        return heapNode;
    }
    private HeapNode heapContentsRemove(URI uri)
    {
        if (bTree.get(uri)!=null) {
            HeapNode heapNode = new HeapNode(uri, bTree.get(uri).getLastUseTime());
            heapNode.setLastUsedTime(Long.MIN_VALUE);
            minHeap.reHeapify(heapNode);
            heapContents.remove(uri);
            return heapNode;
        }
        return null;
    }

    //private HeapNode addHeapNode(URI uri, DocumentImpl doc )
   // {
   //     HeapNode heapNode = new HeapNode(uri, doc.getLastUseTime());
    //    heapContents.put(uri, heapNode);
    //    return heapNode;
    //}

    private Long updateLastUsedTime(DocumentImpl doc, Long lastUsedTime)
    {
        doc.setLastUseTime(lastUsedTime);

        heapContents.get(doc.uri).setLastUsedTime(lastUsedTime);

        return lastUsedTime;

    }





    protected Document getDocument(URI uri) {
        if (bTree.get(uri) == null) {
            return null;
        } else {
            if (heapContents.get(uri)==null)
            {
                return null;
            }

            return bTree.get(uri);
        }
    }


    class HeapNode implements Comparable<HeapNode>{

        URI uri;
        Long lastUsedTime;
        public HeapNode(URI uri, Long lastUsedTime)
        {
            this.uri=uri;
            this.lastUsedTime=lastUsedTime;
        }


        public Long getLastUseTime() {
            return lastUsedTime;
        }

        public void setLastUsedTime(Long lastUsedTime) {
            this.lastUsedTime = lastUsedTime;
        }

        public URI getUri()
        {
            return this.uri;
        }

        @Override
        public int compareTo(HeapNode o) {

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



}