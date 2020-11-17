package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;


public class DocumentIOImpl extends DocumentIO implements com.google.gson.JsonSerializer<DocumentImpl>, com.google.gson.JsonDeserializer<DocumentImpl> {

    protected File baseDir;
    public DocumentIOImpl(File baseDir)
    {
        this.baseDir = baseDir;
    }


    @Override
    public DocumentImpl deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {


        Gson gson= new Gson();
        String txt = jsonElement.getAsJsonObject().get("txt").getAsString();
        int hashCode = jsonElement.getAsJsonObject().get("hashCode").getAsInt();
        String uri = jsonElement.getAsJsonObject().get("uri").getAsString();
        URI realUri =URI.create(uri);
        Map wordMap = (Map) jsonElement.getAsJsonObject().get("wordMap").getAsNumber();
        DocumentImpl doc = new DocumentImpl(realUri,txt,hashCode);
        doc.setWordMap(wordMap);

        return doc;
    }

    @Override
    public JsonElement serialize(DocumentImpl document, Type type, JsonSerializationContext jsonSerializationContext) {

       JsonObject json = new JsonObject();
       json.addProperty("txt", document.getDocumentAsTxt());
       json.addProperty("hashcode", document.getDocumentTextHashCode());
       json.addProperty("wordMap", (Number) document.getWordMap());
       json.addProperty("uri", document.getKey().toString());


        return json;
    }

    @Override
    public File serialize(Document doc) {

        JsonObject json = new JsonObject();
        json.addProperty("txt", doc.getDocumentAsTxt());
        json.addProperty("hashcode", doc.getDocumentTextHashCode());
        json.addProperty("wordMap", (Number) doc.getWordMap());
        json.addProperty("uri", doc.getKey().toString());

        try {
            FileOutputStream fileOut = new FileOutputStream(baseDir);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(json);
            objectOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get rid of http or anything given
        //add json at end
        return baseDir;
    }


    public Document deserialize(URI uri)
    {

        return null;
    }

}
