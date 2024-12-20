package org.example.manufacturer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Path("/ac-control")
@Produces(MediaType.APPLICATION_JSON)
public class AC {
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @JWTTokenNeeded
    public String acControl(@FormParam("temperature") String temperature) {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage(("set-ac " + temperature).getBytes(StandardCharsets.UTF_8));
        String res = sss.receiveMessage();
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject();
        jsonGenerator.write("success", res);
        jsonGenerator.writeEnd();
        jsonGenerator.close();
        return sw.toString();
    }

    @GET
    @JWTTokenNeeded
    public String acControl() {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage("get-ac".getBytes(StandardCharsets.UTF_8));
        String res = sss.receiveMessage();
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject();
        jsonGenerator.write("temperature", res);
        jsonGenerator.writeEnd();
        jsonGenerator.close();
        return sw.toString();
    }

}
