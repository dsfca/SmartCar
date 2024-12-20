package org.example.manufacturer;

import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.time.DateUtils;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.logging.Logger;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/login")
public class Login {
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username, @FormParam("password") String password) {
        InteractWithDB it = null;
        try {
            it = new InteractWithDB("conn_mongo.ini", "content_db.json");
            if(it.validateUser(username, password)) {
                String jwt = issueToken(username);
                StringWriter sw = new StringWriter();
                JsonGenerator jsonGenerator = Json.createGenerator(sw);
                jsonGenerator.writeStartObject();
                jsonGenerator.write("authentication", "succeeded");
                jsonGenerator.write("user", username);
                jsonGenerator.writeEnd();
                jsonGenerator.close();
                return Response.ok().header(AUTHORIZATION, "Bearer " + jwt).entity(sw.toString()).build();
            }
            else {
                StringWriter sw = new StringWriter();
                JsonGenerator jsonGenerator = Json.createGenerator(sw);
                jsonGenerator.writeStartObject();
                jsonGenerator.write("authentication", "denied");
                jsonGenerator.writeEnd();
                jsonGenerator.close();
                return Response.status(UNAUTHORIZED).entity(sw.toString()).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            JsonGenerator jsonGenerator = Json.createGenerator(sw);
            jsonGenerator.writeStartObject();
            jsonGenerator.write("error", e.getMessage());
            jsonGenerator.writeEnd();
            jsonGenerator.close();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String issueToken(String username) {
        String jwToken = null;
        try {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.severe("Working Directory = " + System.getProperty("user.dir"));
            jwToken = Jwts.builder()
                    .setSubject(username)
                    .setIssuer("manufacturer")
                    .setIssuedAt(new Date())
                    .setExpiration(DateUtils.addMinutes(new Date(), 15))
                    .signWith(Cryptography.getPrivateKey("manufacturer_pkcs8.der"))
                    .compact();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jwToken;
    }
}
