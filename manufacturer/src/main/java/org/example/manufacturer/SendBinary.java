package org.example.manufacturer;

import org.apache.commons.io.FileUtils;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.logging.Logger;

@Path("/binary")
@Produces(MediaType.APPLICATION_JSON)
public class SendBinary {
    @POST
    @JWTTokenNeeded
    public String binary() {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage(("binary").getBytes(StandardCharsets.UTF_8));
        try {
            byte[] file = FileUtils.readFileToByteArray(new File("binary"));
            sss.sendMessage(file);
            sss.sendMessage(Cryptography.sign(file, Cryptography.getPrivateKey("manufacturer_pkcs8.der")).getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        String res = sss.receiveMessage();
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject();
        jsonGenerator.write("success", res);
        jsonGenerator.writeEnd();
        jsonGenerator.close();
        return sw.toString();
    }

}
