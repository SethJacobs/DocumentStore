package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    public DocumentPersistenceManager(File baseDir){
        this.baseDir=baseDir;
    }
    File baseDir;


    @Override
    public void serialize(URI uri, Document val) throws IOException {

        //JsonObject json = new JsonObject();
        Gson gson = new Gson();

       // json.addProperty("txt", val.getDocumentAsTxt());
       // json.addProperty("hashcode", val.getDocumentTextHashCode());
       // String mapString = gson.toJson(val.getWordMap());
        //json.addProperty("wordMap", mapString);
        //json.addProperty("uri", uri.toString());

        FakeDoc doc = new FakeDoc(val);



        try {
            File file = buildDir(uri,baseDir);
            file.getParentFile().mkdirs();
            //Files.createDirectories(Paths.get(file.getPath()));

            //FileOutputStream fileOut = new FileOutputStream(file);
           // ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
           // objectOut.writeObject(json);
            Writer writer = new FileWriter(file +".json");
            gson.toJson(doc,writer);
            writer.close();

            //objectOut.close();
            //fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    @Override
    public Document deserialize(URI uri) throws IOException {

        File file = buildDir(uri,baseDir);
        //FileInputStream inputStream = new FileInputStream(file+".json");
       // ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
       // System.out.println(objectInputStream);

        try {


            JsonObject json = new JsonObject();




            Gson gson= new Gson();
            Reader reader = new FileReader(file+".json");

            BufferedReader BRead = new BufferedReader(reader);

            json=gson.fromJson(BRead, JsonObject.class);


           // DocumentImpl doc = gson.fromJson(String.valueOf(BRead),DocumentImpl.class);

            String txt = json.get("txt").getAsString();

            int hashCode = json.get("HashCode").getAsInt();
            String uriA = json.get("uri").getAsString();
            URI  realUri =URI.create(uriA);
            Map map = gson.fromJson(json.get("wordMap"),Map.class);
            DocumentImpl doc = new DocumentImpl(realUri,txt,hashCode);
            doc.setWordMap(map);



            reader.close();
            BRead.close();
            File again = new File(file.getPath() + ".json");
            deleteALL(again);
            return doc;
        } catch (Exception e) {
           // e.printStackTrace();
            return null;
        }



        //return null;
    }

    private File buildDir(URI uri, File basedir)
    {
        if(uri.getHost()!=null) {
            return new File(File.separator + basedir + File.separator + uri.getHost() + File.separator + uri.getPath());
        }
        else
        {
            return new File(File.separator + basedir + File.separator + uri.getPath());
        }

    }

    private void deleteALL(File path)
    {

        while(path.getParentFile().listFiles().length>=1)
        {
            File temp=path;
            path=path.getParentFile();
            temp.delete();
            if(path.getParentFile()==null){
                break;
        }


        }
         path.delete();



    }


    class FakeDoc
    {
        String txt;
        int HashCode;
        URI uri;
        Map<String, Integer> wordMap;
        public FakeDoc(Document doc)
        {
            this.uri=doc.getKey();
            this.txt=doc.getDocumentAsTxt();
            this.HashCode=doc.hashCode();
            this.wordMap=doc.getWordMap();
        }
    }



}
