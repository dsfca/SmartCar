package org.example.manufacturer;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bson.Document;
import org.ini4j.Ini;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;


public class InteractWithDB {
	
	//Mongo
	static Ini ini;
	static MongoDatabase mongo_db;
	static MongoClient mongo_client;
	static MongoCollection<Document> mongo_collection;
	
	//File names
	//private String mongo_json;
	
	//KDF
	private int seed_bytes;
	private int hash_bytes;
	private int iterations;
	private byte[] salt;

	
	
	public InteractWithDB(String mongo_ini, String mongo_json) throws FileNotFoundException, IOException {
		//this.mongo_json = mongo_json;
		InteractWithDB.ini = new Ini(new File(mongo_ini));
		connectToMongo();
		getKdfVariablesFromIni();
	}
	
	public String getHost() {
		return ini.get("Mongo","mongo_host", String.class);
	}
	
	public String getDatabase() {
		return ini.get("Mongo","mongo_database", String.class);
	}
	
	public String getCollection() {
		return ini.get("Mongo","mongo_collection", String.class);
	}
	
	public void connectToMongo() throws FileNotFoundException, IOException {
		mongo_client = new MongoClient(new MongoClientURI(getHost()));
		mongo_db = mongo_client.getDatabase(getDatabase());
		mongo_collection = mongo_db.getCollection(getCollection());
	}
	
	public void getKdfVariablesFromIni() throws IOException {
		seed_bytes = ini.get("Kdf","seed_bytes", Integer.class);
		hash_bytes = ini.get("Kdf","hash_bytes", Integer.class);
		iterations = ini.get("Kdf","iterations", Integer.class);
		salt = (new BigInteger(ini.get("Kdf","salt", String.class))).toByteArray();
		//System.out.println("ByteArray to Binary = "+Arrays.toString(salt));
	}
	
	/**
	 * If salt is not generated yet (in .ini file) use this function
	 * @throws IOException
	 */
	public void generateSalt() throws IOException {
		SecureRandom rng = new SecureRandom();
		byte [] salt = rng.generateSeed(seed_bytes);
	    ini.put("Kdf", "salt", new BigInteger(salt));
		ini.store();
	}
	
	public BigInteger hashPasswordKDF(String password) throws UnsupportedEncodingException {
		PKCS5S2ParametersGenerator kdf = new PKCS5S2ParametersGenerator();
		kdf.init(password.getBytes("UTF-8"), salt, iterations);
		byte[] hashed_password = ((KeyParameter) kdf.generateDerivedMacParameters(8*hash_bytes)).getKey();
		
		return new BigInteger(hashed_password);
	}

	/**
	 * Check username in database
	 * @param username
	 * @return
	 */
	public boolean existsUsername(String username) {
		MongoCursor<Document> cursor = mongo_collection.find().iterator();

		while (cursor.hasNext()) {
			if(cursor.next().get("username").equals(username)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get password from database given the username
	 * @param username
	 * @return
	 */
	public BigInteger getPassword(String username) {
		MongoCursor<Document> cursor = mongo_collection.find().iterator();
		while (cursor.hasNext()) {
			Document user = cursor.next();
			if (user.get("username").equals(username)) {
				return new BigInteger(user.get("password").toString());
			}
		}
		return null;
	}
	
	/**
	 * Check if login credentials are valid
	 * @param username
	 * @param password
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public boolean validateUser(String username, String password) throws UnsupportedEncodingException {
		MongoCursor<Document> cursor = mongo_collection.find().iterator();
		BigInteger hashed_password = hashPasswordKDF(password);
		
		while (cursor.hasNext()) {
			Document user = cursor.next();
			if (user.get("username").equals(username) && user.get("password").equals(hashed_password.toString())) {
				System.out.println("Login: Successful");
				return true;
			}
		}
		System.out.println("Login: Invalid credentials");
		return false;
	}
	
	public void addUserToDatabase(String username, String password) throws UnsupportedEncodingException {
		 Document document = new Document();
		 document.append("username", username);
		 String a = hashPasswordKDF(password).toString();
		System.out.println(a);
		 document.append("password", a);
		 mongo_collection.insertOne(document);
	}
	
	public void cleanDatabase() {
		mongo_collection.drop();
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		InteractWithDB it = new InteractWithDB("conn_mongo.ini", "content_db.json");
		it.addUserToDatabase("jose", "jose");
	}

}
