package org.example.manufacturer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Path("/tire-pressure")
@Produces(MediaType.APPLICATION_JSON)
public class TirePressure {
    @GET
    @JWTTokenNeeded
    public String hello() {
        SSLSocketClientWithClientAuth sss = new SSLSocketClientWithClientAuth("host.docker.internal", 65431);
        sss.sendMessage("get-tires".getBytes(StandardCharsets.UTF_8));
        Logger logger = Logger.getLogger(getClass().getName());
        logger.severe("VOU RECEBER2222");
        String res = sss.receiveMessage();

        String[] tires = res.split(",");

        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject();
        jsonGenerator.write("lb", Double.parseDouble(tires[0]));
        jsonGenerator.write("rb", Double.parseDouble(tires[1]));
        jsonGenerator.write("lf", Double.parseDouble(tires[2]));
        jsonGenerator.write("rf", Double.parseDouble(tires[3]));
        //jsonGenerator.write("directory", System.getProperty("user.dir"));
        jsonGenerator.writeEnd();
        jsonGenerator.close();
        return sw.toString();
    }
}