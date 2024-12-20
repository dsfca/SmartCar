package org.example.manufacturer;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.annotation.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Logger;

@Provider
@JWTTokenNeeded
@Priority(Priorities.AUTHENTICATION)
public class JWTTokenNeededFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Logger logger = Logger.getLogger(getClass().getName());
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if(authorizationHeader == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        String sub = authorizationHeader.substring("Bearer".length());
        if (sub == null || sub.length() <= 0) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        String token = sub.trim();
        PublicKey pk = Cryptography.getPublicKey("manufacturer_pub.der");
        //openssl rsa -in manufacturer.pem -pubout -outform DER -out manufacturer_pub.der
        try {
            Jwts.parserBuilder().setSigningKey(pk).build().parseClaimsJws(token);
            logger.info("VALID TOKEN: " + token);
        }
        catch(JwtException e) {
            logger.severe("INVALID TOKEN " + token);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
