package org.example.manufacturer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Path("/open-door")
@Produces(MediaType.APPLICATION_JSON)
public class OpenDoor {
    @GET
    @JWTTokenNeeded
    public String openDoor() {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage("open-door".getBytes(StandardCharsets.UTF_8));
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