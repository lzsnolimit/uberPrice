package UberPriceQuery;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Mongodb {

	private static MongoClient mongoClient;
	private static MongoDatabase currentDb;
	private static MongoCollection<Document> collection;

	/*
	 * Connect database
	 */
	@SuppressWarnings("deprecation")
	public static void connect() {
		mongoClient = new MongoClient("localhost", 27017);
	}

	/*
	 * Set database name
	 */
	public static void setDb(String dbName) {
		currentDb = mongoClient.getDatabase(dbName);
	}

	/*
	 * set collection
	 */
	public static void setCollection(String collectionName) {
		collection = currentDb.getCollection(collectionName);
	}

	/*
	 * insert a collection
	 */
	public static void insertCollection(String collectionName) {
		currentDb.createCollection(collectionName);
		setCollection(collectionName);
	}

	/*
	 * close the connection
	 */
	public static void close() {
		mongoClient.close();
	}
}