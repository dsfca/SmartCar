package org.example.manufacturer;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Cryptography {
    public static byte nonce = 0;


    public static String sign(byte[] plainText, PrivateKey privateKey) throws InvalidKeyException {
        try {
            plainText = ArrayUtils.add(plainText, nonce++);
            Signature privateSignature = Signature.getInstance("SHA1withRSA");
            privateSignature.initSign(privateKey);
            privateSignature.update(plainText);

            byte[] signature = privateSignature.sign();

            return Base64.getEncoder().encodeToString(signature);
        } catch (NoSuchAlgorithmException | SignatureException e) {
            e.printStackTrace();
            return null;
        }

    }



    public static PrivateKey getPrivateKey(String filename) throws IOException {
        byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
        PKCS8EncodedKeySpec spec =
                new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        try {
            return kf.generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PublicKey getPublicKey(String filename) {

        byte[] keyBytes = new byte[0];
        try {
            keyBytes = Files.readAllBytes(Paths.get(filename));
            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
}
