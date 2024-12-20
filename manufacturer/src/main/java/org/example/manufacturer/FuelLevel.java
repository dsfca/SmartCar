package org.example.manufacturer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Path("/fuel-level")
@Produces(MediaType.APPLICATION_JSON)
public class FuelLevel {
    @GET
    @JWTTokenNeeded
    public String fuel() {
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject();
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage("get-fuel".getBytes(StandardCharsets.UTF_8));
        String res = sss.receiveMessage();
        jsonGenerator.write("fuel-level", res);
        jsonGenerator.writeEnd();
        jsonGenerator.close();
        return sw.toString();
    }
}
