package co.hypertest.mongodb.controller;

import co.hypertest.commonutils.helper.ResponseGenerator;
import co.hypertest.mongodb.entity.MyResult;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/mongo")
public class MongoDriverSyncController {

    private final MongoCollection<Document> collection;
    private final ClientSession session;
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private static final String DATABASE_TO_DROP = "tempDatabaseForDropping";
    private static final String SAMPLE_EMAIL = "ht!@#*&#@ht.com";

    public MongoDriverSyncController(MongoDatabase database, MongoClient mongoClient, ClientSession session) {
        // Ensure the collection “mycollection” exists in “mydb”
        System.out.println("database---"+ database);
        System.out.println(database.listCollectionNames().into(new ArrayList<>()).contains("mycollection"));

        if (!database.listCollectionNames().into(new ArrayList<>()).contains("mycollection")) {
            database.createCollection("mycollection");
        }

        this.database = database;
        this.mongoClient = mongoClient;
        this.collection = database.getCollection("mycollection");
        this.session = session;


//            Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
//            if (collection.find(filter).first() == null) {
//                collection.insertMany(Arrays.asList(
//                        new Document("contact", new Document("email", SAMPLE_EMAIL)).append("initializedAt", System.currentTimeMillis()),
//                        new Document("name", "Alice").append("contact", "alice@example.com").append("age", 30),
//                        new Document("name", "Bob").append("age", 25).append("city", "New York"),
//                        new Document("name", "Charlie").append("contact", "123-456-7890").append("occupation", "Engineer"),
//                        new Document("name", "David").append("contact", 12345678).append("hobbies", Arrays.asList("reading", "hiking")),
//                        new Document("name", "Eve").append("contact", null).append("country", "Canada"),
//                        new Document("name", "Frank").append("details", new Document("phone", "555-1212")), // No top-level "contact"
//                        new Document("name", "Grace").append("contact", "grace@work.net"),
//                        new Document("name", "Henry") // No "contact" field at all
//                ));
//            }
    }

    /**
     * Checks for—and if missing, inserts—a document with our SAMPLE_EMAIL
     **/

    /**
     * GET /mongo/docs — return every document in the collection
     **/
    @GetMapping("/docs")
    public Object listAll() {
        List<Document> docs = new ArrayList<>();
        collection.find().into(docs);
        return ResponseGenerator.generate(docs);
    }

    /**
     * GET /mongo/docs/sample
     * — return a single document matching a hard‑coded filter
     * (no @PathVariable or @RequestBody; filter values supplied in code)
     **/
    @GetMapping("/docs/sample")
    public Object getSample() {
        // filter on contact.email = "johndoe@test.com", supplied here in code
        return ResponseGenerator.generate(collection
                .find(Filters.eq("contact.email", SAMPLE_EMAIL))
                .first());
    }

    // Helper: a "match anything" pipeline
    private List<Bson> matchAll() {
        return Collections.singletonList(Aggregates.match(Filters.exists("_id")));
    }


//    ------------------------------------ AggregateIterable Methods ------------------------------------    //


    @GetMapping("aggregate/toCollection")
    public Object toCollection() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id")),
                Aggregates.merge("outCollection")
        );
        collection.aggregate(pipeline).toCollection();
        return ResponseGenerator.generate("Merged into 'outCollection'");
    }

    @GetMapping("aggregate/allowDiskUse")
    public Object allowDiskUse() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .allowDiskUse(true)
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/batchSize")
    public Object batchSize() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .batchSize(5)
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/maxTime")
    public Object maxTime() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .maxTime(1, TimeUnit.SECONDS)
                .into(new java.util.ArrayList<>()));
    }
    @GetMapping("aggregate/maxAwaitTime")
    public Object maxAwaitTime() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .maxAwaitTime(500, TimeUnit.MILLISECONDS)
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/bypassDocumentValidation")
    public Object bypassDocumentValidation() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id")),
                Aggregates.merge("outBypassCollection")
        );
        collection.aggregate(pipeline)
                .bypassDocumentValidation(true)
                .toCollection();
        return ResponseGenerator.generate("Merged into 'outBypassCollection' bypassing validation");
    }

    @GetMapping("aggregate/collation")
    public Object collation() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .collation(Collation.builder().locale("en").build())
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/commentString")
    public Object commentString() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .comment("myComment")
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/commentValue")
    public Object commentValue() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .comment(new BsonString("typedComment"))
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/hint")
    public Object hint() {
        Document indexKey = new Document("contact", 1);
        collection.createIndex(indexKey);

        return ResponseGenerator.generate(collection.aggregate(List.of(new Document("$match", new Document()))) // Using matchAll equivalent
                .hint(new Document("contact", 1))
                .into(new ArrayList<>()));
    }

    @GetMapping("aggregate/hintString")
    public Object hintString() {
        Document indexKey = new Document("contact", 1);
        String indexName = collection.createIndex(indexKey); // This returns something like "contact_1"

        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .hintString(indexName)
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/let")
    public Object letVariables() {
        List<Bson> pipeline = Collections.singletonList(
                Aggregates.match(Filters.expr(
                        new Document("$eq", Arrays.asList("$contact.email", "$$emailVar"))
                ))
        );
        return ResponseGenerator.generate(collection.aggregate(pipeline)
                .let(new Document("emailVar", SAMPLE_EMAIL))
                .into(new java.util.ArrayList<>()));
    }

    @GetMapping("aggregate/explain")
    public Object explain() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .explain());
    }

    @GetMapping("aggregate/explainVerbosity")
    public Object explainVerbosity() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .explain(ExplainVerbosity.EXECUTION_STATS));
    }

    @GetMapping("aggregate/explainClass")
    public Object explainClass() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .explain(Document.class));
    }

    @GetMapping("aggregate/explainClassVerbosity")
    public Object explainClassVerbosity() {
        return ResponseGenerator.generate(collection.aggregate(matchAll())
                .explain(Document.class, ExplainVerbosity.QUERY_PLANNER));
    }

    //  --------------------------------- ChangeStreamIterable Methods ---------------------------------    //
    @GetMapping("changeStream/cursor")
    public Object cursor() {
        return ResponseGenerator.generate(collection.watch().cursor());
    }

    @GetMapping("changeStream/fullDocument")
    public Object fullDocument() {
        return ResponseGenerator.generate(collection.watch()
                .fullDocument(FullDocument.UPDATE_LOOKUP)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/fullDocumentBeforeChange")
    public Object fullDocumentBeforeChange() {
        return ResponseGenerator.generate(collection.watch()
                .fullDocumentBeforeChange(FullDocumentBeforeChange.WHEN_AVAILABLE)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/resumeAfter")
    public Object resumeAfter() {
        BsonDocument token = new BsonDocument("dummy", new BsonString("token"));
        return ResponseGenerator.generate(collection.watch()
                .resumeAfter(token)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/batchSize")
    public Object batchSizeStream() {
        return ResponseGenerator.generate(collection.watch()
                .batchSize(5)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/maxAwaitTime")
    public Object maxAwaitTimeStream() {
        return ResponseGenerator.generate(collection.watch()
                .maxAwaitTime(1, TimeUnit.SECONDS)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/collation")
    public Object collationStream() {
        return ResponseGenerator.generate(collection.watch()
                .collation(Collation.builder().locale("en").build())
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/withDocumentClass")
    public Object withDocumentClass() {
        return ResponseGenerator.generate(collection.watch()
                .withDocumentClass(Document.class)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/startAtOperationTime")
    public Object startAtOperationTime() {
        BsonTimestamp ts = new BsonTimestamp((int) (System.currentTimeMillis() / 1000), 1);
        return ResponseGenerator.generate(collection.watch()
                .startAtOperationTime(ts)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/startAfter")
    public Object startAfter() {
        BsonDocument token = new BsonDocument("dummy", new BsonString("token"));
        return ResponseGenerator.generate(collection.watch()
                .startAfter(token)
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/commentString")
    public Object commentStringStream() {
        return ResponseGenerator.generate(collection.watch()
                .comment("myComment")
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/commentValue")
    public Object commentValueStream() {
        return ResponseGenerator.generate(collection.watch()
                .comment(new BsonString("typedComment"))
                .into(new ArrayList<>()));
    }

    @GetMapping("changeStream/showExpandedEvents")
    public Object showExpandedEvents() {
        return ResponseGenerator.generate(collection.watch()
                .showExpandedEvents(true)
                .into(new ArrayList<>()));
    }


// ------------------------------------------- ClientSession Session -------------------------------------------    //


    @GetMapping("session/pinnedServerAddress")
    public Object getPinnedServerAddress() {
        return ResponseGenerator.generate(session.getPinnedServerAddress());
    }

    @GetMapping("session/hasActiveTransaction")
    public Object hasActiveTransaction() {
        return ResponseGenerator.generate(session.hasActiveTransaction());
    }

    @GetMapping("session/notifyMessageSent")
    public Object notifyMessageSent() {
        return ResponseGenerator.generate(session.notifyMessageSent());
    }

    @GetMapping("session/transactionOptions")
    public Object getTransactionOptions() {
        boolean hasStartedTransactionHere = false;
        if (!session.hasActiveTransaction()) {
            session.startTransaction(); // Start transaction first
            hasStartedTransactionHere = true;
        }
        String options = session.getTransactionOptions().toString();
        if (hasStartedTransactionHere) {
            session.abortTransaction(); // Cleanup
        }
        return ResponseGenerator.generate(options);
    }


    @GetMapping("session/startTransaction")
    public Object startTransaction() {
        session.startTransaction();
        return ResponseGenerator.generate("Transaction started");
    }

    @GetMapping("session/startTransactionWithOptions")
    public Object startTransactionWithOptions() {
        TransactionOptions opts = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.LOCAL)
                .writeConcern(WriteConcern.MAJORITY)
                .build();
        boolean hasEndedTransactionHere = false;
        if (session.hasActiveTransaction()) {
            session.abortTransaction();
            hasEndedTransactionHere = true;
        }
        session.startTransaction(opts);
        return ResponseGenerator.generate("Transaction started with custom options");
    }

    @GetMapping("session/commitTransaction")
    public Object commitTransaction() {
        if (!session.hasActiveTransaction()) {
            session.startTransaction();
        }
        session.commitTransaction();
        return ResponseGenerator.generate("Transaction committed");
    }

    @GetMapping("session/abortTransaction")
    public Object abortTransaction() {
        if (!session.hasActiveTransaction()) {
            session.startTransaction();
        }
        session.abortTransaction();
        return ResponseGenerator.generate("Transaction aborted");
    }

    @GetMapping("session/withTransaction")
    public Object withTransaction() {
        return ResponseGenerator.generate(session.withTransaction(() -> {
            // perform work here
            return "Result from transaction body";
        }));
    }

    @GetMapping("session/withTransactionWithOptions")
    public Object withTransactionWithOptions() {
        TransactionOptions opts = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.W1)
                .build();
        return ResponseGenerator.generate(session.withTransaction(() -> {
            // perform work here
            return "Result with options";
        }, opts));
    }


// ------------------------------------------- DistinctIterable Methods -------------------------------------------    //

    @GetMapping("distinct/filter")
    public Object distinctFilter() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .filter(Filters.eq("contact.email", SAMPLE_EMAIL))
                .into(new ArrayList<>()));
    }

    @GetMapping("distinct/maxTime")
    public Object distinctMaxTime() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .maxTime(1, TimeUnit.SECONDS)
                .into(new ArrayList<>()));
    }

    @GetMapping("distinct/batchSize")
    public Object distinctBatchSize() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .batchSize(5)
                .into(new ArrayList<>()));
    }

    @GetMapping("distinct/collation")
    public Object distinctCollation() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .collation(Collation.builder().locale("en").build())
                .into(new ArrayList<>()));
    }

    @GetMapping("distinct/commentString")
    public Object distinctCommentString() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .comment("myComment")
                .into(new ArrayList<>()));
    }

    @GetMapping("distinct/commentValue")
    public Object distinctCommentValue() {
        return ResponseGenerator.generate(collection
                .distinct("contact.email", String.class)
                .comment(new BsonString("typedComment"))
                .into(new ArrayList<>()));
    }

    // ------------------------------------------- FindIterable Methods -------------------------------------------    //
    @GetMapping("find")
    public Object find() {
        List<Document> docs = collection.find().into(new ArrayList<>());
        Document first = docs.isEmpty() ? null : docs.get(0);
        return ResponseGenerator.generate(first);
    }

    @GetMapping("find/first")
    public Object findFirst() {
        Document result = collection.find().first();
        return ResponseGenerator.generate(result);
    }


    @GetMapping("find/filter")
    public Object findFilter() {
        return ResponseGenerator.generate(collection.find(Filters.eq("contact.email", SAMPLE_EMAIL)).into(new ArrayList<>()));
    }

    @GetMapping("find/limit")
    public Object findLimit() {
        return ResponseGenerator.generate(collection.find().limit(2).into(new ArrayList<>()));
    }

    @GetMapping("find/skip")
    public Object findSkip() {
        return ResponseGenerator.generate(collection.find().skip(1).into(new ArrayList<>()));
    }

    @GetMapping("find/maxTime")
    public Object findMaxTime() {
        return ResponseGenerator.generate(collection.find().maxTime(1, TimeUnit.SECONDS).into(new ArrayList<>()));
    }

    @GetMapping("find/maxAwaitTime")
    public Object findMaxAwaitTime() {
        return ResponseGenerator.generate(collection.find().maxAwaitTime(500, TimeUnit.MILLISECONDS).into(new ArrayList<>()));
    }

    @GetMapping("find/projection")
    public Object findProjection() {
        return ResponseGenerator.generate(collection.find().projection(new Document("contact.email", 1)).into(new ArrayList<>()));
    }

    @GetMapping("find/sort")
    public Object findSort() {
        return ResponseGenerator.generate(collection.find().sort(new Document("initializedAt", -1)).into(new ArrayList<>()));
    }

    @GetMapping("find/noCursorTimeout")
    public Object findNoCursorTimeout() {
        return ResponseGenerator.generate(collection.find().noCursorTimeout(true).into(new ArrayList<>()));
    }

    @GetMapping("find/partial")
    public Object findPartial() {
        return ResponseGenerator.generate(collection.find().partial(true).into(new ArrayList<>()));
    }

    @GetMapping("find/cursorType")
    public Object findCursorType() {
        return ResponseGenerator.generate(collection.find().cursorType(com.mongodb.CursorType.NonTailable).into(new ArrayList<>()));
    }

    @GetMapping("find/batchSize")
    public Object findBatchSize() {
        return ResponseGenerator.generate(collection.find().batchSize(3).into(new ArrayList<>()));
    }

    @GetMapping("find/collation")
    public Object findCollation() {
        return ResponseGenerator.generate(collection.find().collation(Collation.builder().locale("en").build()).into(new ArrayList<>()));
    }

    @GetMapping("find/commentString")
    public Object findCommentString() {
        return ResponseGenerator.generate(collection.find().comment("findWithComment").into(new ArrayList<>()));
    }

    @GetMapping("find/commentValue")
    public Object findCommentValue() {
        return ResponseGenerator.generate(collection.find().comment(new BsonString("typedFindComment")).into(new ArrayList<>()));
    }

    @GetMapping("find/hint")
    public Object findHint() {
        Document indexKey = new Document("initializedAt", 1); // 1 for ascending
        collection.createIndex(indexKey);
        return ResponseGenerator.generate(collection.find().hint(new Document("initializedAt", 1)).into(new ArrayList<>()));
    }

    @GetMapping("find/hintString")
    public Object findHintString() {
        Document indexKey = new Document("contact", 1);
        String indexName = collection.createIndex(indexKey); // This returns something like "contact_1"

        return ResponseGenerator.generate(collection.find().hintString(indexName).into(new ArrayList<>()));
    }

    @GetMapping("find/let")
    public Object findLet() {
        Bson filter = Filters.expr(new Document("$eq", Arrays.asList("$contact.email", "$$emailVar")));
        return ResponseGenerator.generate(collection.find(filter).let(new Document("emailVar", SAMPLE_EMAIL)).into(new ArrayList<>()));
    }

    @GetMapping("find/max")
    public Object findMax() {
        collection.createIndex(Indexes.ascending("initializedAt"));
        Document indexKey = new Document("initializedAt", 1); // 1 for ascending
        collection.createIndex(indexKey); // You can also use Indexes.ascending("initializedAt")
        return ResponseGenerator.generate(collection.find()
                .max(new Document("initializedAt", 2))
                .hint(indexKey) // **Explicitly hint the index to use**
                .into(new ArrayList<>()));
    }

    @GetMapping("find/min")
    public Object findMin() {
        collection.createIndex(Indexes.ascending("initializedAt"));
        Document indexKey = new Document("initializedAt", 1); // 1 for ascending
        collection.createIndex(indexKey); // You can also use Indexes.ascending("initializedAt")
        return ResponseGenerator.generate(collection.find()
                .min(new Document("initializedAt", 0)) // Your lower bound
                .hint(indexKey) // **Explicitly hint the index to use**
                // Alternative for hint: .hint(Indexes.ascending("initializedAt"))
                .into(new ArrayList<>()));
    }

    @GetMapping("find/returnKey")
    public Object findReturnKey() {
        return ResponseGenerator.generate(collection.find().returnKey(true).into(new ArrayList<>()));
    }

    @GetMapping("find/showRecordId")
    public Object findShowRecordId() {
        return ResponseGenerator.generate(collection.find().showRecordId(true).into(new ArrayList<>()));
    }

    @GetMapping("find/allowDiskUse")
    public Object findAllowDiskUse() {
        return ResponseGenerator.generate(collection.find().allowDiskUse(true).into(new ArrayList<>()));
    }

    @GetMapping("find/explain")
    public Object findExplain() {
        return ResponseGenerator.generate(collection.find().explain());
    }

    @GetMapping("find/explainVerbosity")
    public Object findExplainVerbosity() {
        return ResponseGenerator.generate(collection.find().explain(ExplainVerbosity.EXECUTION_STATS));
    }

    @GetMapping("find/explainClass")
    public Object findExplainClass() {
        return ResponseGenerator.generate(collection.find().explain(Document.class));
    }

    @GetMapping("find/explainClassVerbosity")
    public Object findExplainClassVerbosity() {
        return ResponseGenerator.generate(collection.find().explain(Document.class, ExplainVerbosity.QUERY_PLANNER));
    }


// ------------------------------------------- ListCollectionsIterable Methods -------------------------------------------    //

    @GetMapping("listCollections/filter")
    public Object listCollectionsFilter() {
        return ResponseGenerator.generate(database
                .listCollections()
                .filter(Filters.eq("name", "mycollection"))
                .into(new ArrayList<>()));
    }

    @GetMapping("listCollections/maxTime")
    public Object listCollectionsMaxTime() {
        return ResponseGenerator.generate(database
                .listCollections()
                .maxTime(1, TimeUnit.SECONDS)
                .into(new ArrayList<>()));
    }

    @GetMapping("listCollections/batchSize")
    public Object listCollectionsBatchSize() {
        return ResponseGenerator.generate(database
                .listCollections()
                .batchSize(2)
                .into(new ArrayList<>()));
    }

    @GetMapping("listCollections/commentString")
    public Object listCollectionsCommentString() {
        return ResponseGenerator.generate(database
                .listCollections()
                .comment("listCollections comment")
                .into(new ArrayList<>()));
    }

    @GetMapping("listCollections/commentValue")
    public Object listCollectionsCommentValue() {
        return ResponseGenerator.generate(database
                .listCollections()
                .comment(new BsonString("typed listCollections comment"))
                .into(new ArrayList<>()));
    }


//    -------------------------------------------- ListDatabasesIterable Methods -------------------------------------------    //


    @GetMapping("listDatabases/filter")
    public Object listDatabasesFilter() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .filter(Filters.eq("name", "mydb"))
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/maxTime")
    public Object listDatabasesMaxTime() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .maxTime(1, TimeUnit.SECONDS)
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/batchSize")
    public Object listDatabasesBatchSize() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .batchSize(2)
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/nameOnly")
    public Object listDatabasesNameOnly() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .nameOnly(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/authorizedOnly")
    public Object listDatabasesAuthorizedOnly() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .authorizedDatabasesOnly(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/commentString")
    public Object listDatabasesCommentString() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .comment("listing dbs with comment")
                .into(new ArrayList<>()));
    }

    @GetMapping("listDatabases/commentValue")
    public Object listDatabasesCommentValue() {
        return ResponseGenerator.generate(mongoClient
                .listDatabases()
                .comment(new BsonString("typed listDatabases comment"))
                .into(new ArrayList<>()));
    }


//    ---------------------------------------------- ListIndexesIterable Methods -------------------------------------------    //


    @GetMapping("listIndexes/maxTime")
    public Object listIndexesMaxTime() {
        return ResponseGenerator.generate(collection.listIndexes()
                .maxTime(1, TimeUnit.SECONDS)
                .into(new ArrayList<>()));
    }

    @GetMapping("listIndexes/batchSize")
    public Object listIndexesBatchSize() {
        return ResponseGenerator.generate(collection.listIndexes()
                .batchSize(2)
                .into(new ArrayList<>()));
    }

    @GetMapping("listIndexes/commentString")
    public Object listIndexesCommentString() {
        return ResponseGenerator.generate(collection.listIndexes()
                .comment("listIndexes comment")
                .into(new ArrayList<>()));
    }

    @GetMapping("listIndexes/commentValue")
    public Object listIndexesCommentValue() {
        return ResponseGenerator.generate(collection.listIndexes()
                .comment(new BsonString("typed listIndexes comment"))
                .into(new ArrayList<>()));
    }


//    ------------------------------------------------ ListSearchIndexesIterable Methods -------------------------------------------    //


//    @GetMapping("listSearchIndexes/name")
//    public Object listSearchIndexesByName() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .name("default")); // replace with an actual index name if needed
//    }
//
//    @GetMapping("listSearchIndexes/allowDiskUse")
//    public Object listSearchIndexesAllowDiskUse() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .allowDiskUse(true));
//    }
//
//    @GetMapping("listSearchIndexes/batchSize")
//    public Object listSearchIndexesBatchSize() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .batchSize(2));
//    }
//
//    @GetMapping("listSearchIndexes/maxTime")
//    public Object listSearchIndexesMaxTime() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .maxTime(1, TimeUnit.SECONDS));
//    }
//
//    @GetMapping("listSearchIndexes/collation")
//    public Object listSearchIndexesCollation() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .collation(Collation.builder().locale("en").build()));
//    }
//
//    @GetMapping("listSearchIndexes/commentString")
//    public Object listSearchIndexesCommentString() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .comment("listSearchIndexes comment"));
//    }
//
//    @GetMapping("listSearchIndexes/commentValue")
//    public Object listSearchIndexesCommentValue() {
//        ListIndexesIterable<Document> iterable = collection.listIndexes()
//                .comment("Demo comment for listing regular indexes");
//        List<Document> indexes = new ArrayList<>();
//        iterable.into(indexes);
//        return ResponseGenerator.generate(indexes);
//    }
//
//    @GetMapping("listSearchIndexes/explain")
//    public Object listSearchIndexesExplain() {
//        return ResponseGenerator.generate(collection.listSearchIndexes().explain());
//    }
//
//    @GetMapping("listSearchIndexes/explainVerbosity")
//    public Object listSearchIndexesExplainVerbosity() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .explain(ExplainVerbosity.EXECUTION_STATS));
//    }
//
//    @GetMapping("listSearchIndexes/explainClass")
//    public Object listSearchIndexesExplainClass() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .explain(Document.class));
//    }
//
//    @GetMapping("listSearchIndexes/explainClassVerbosity")
//    public Object listSearchIndexesExplainClassVerbosity() {
//        return ResponseGenerator.generate(collection.listSearchIndexes()
//                .explain(Document.class, ExplainVerbosity.QUERY_PLANNER));
//    }

//    ---------------------------------------------- MongoChangeStreamCursor Methods -------------------------------------------    //
@GetMapping("changeStream/cursor/resumeToken")
public Object getChangeStreamResumeToken() {
    try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = collection.watch().cursor()) {
        if (cursor.hasNext()) {
            cursor.next(); // advance to fetch resume token
        }
        BsonDocument token = cursor.getResumeToken();
        return ResponseGenerator.generate(token != null ? token.toJson() : "No resume token available.");
    }
}

    @GetMapping("/mapReduce/toCollection")
    public Object mapReduceToCollection() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        collection.mapReduce(map, reduce).collectionName("outputCollection").toCollection();
        return ResponseGenerator.generate("MapReduce results written to 'outputCollection'");
    }

    @GetMapping("/mapReduce/collectionName")
    public Object mapReduceWithCollectionName() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("outputCollection")
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/finalizeFunction")
    public Object mapReduceWithFinalize() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        String finalizeFn = "function(key, reducedValue) { return reducedValue; }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .finalizeFunction(finalizeFn)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/scope")
    public Object mapReduceWithScope() {
        String map = "function() { emit(this.category, factor); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        Bson scope = new Document("factor", 2);
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .scope(scope)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/sort")
    public Object mapReduceWithSort() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        Bson sort = Sorts.ascending("category");
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .sort(sort)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/filter")
    public Object mapReduceWithFilter() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        Bson filter = Filters.eq("active", true);
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .filter(filter)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/limit")
    public Object mapReduceWithLimit() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .limit(5)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/jsMode")
    public Object mapReduceWithJsMode() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .jsMode(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/verbose")
    public Object mapReduceWithVerbose() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .verbose(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/maxTime")
    public Object mapReduceWithMaxTime() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .maxTime(1, TimeUnit.MINUTES)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/action")
    public Object mapReduceWithAction() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("outputActionCollection")
                .action(MapReduceAction.REPLACE)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/databaseName")
    public Object mapReduceWithDatabaseName() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("outputDBCollection")
                .databaseName("mydb")
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/sharded")
    public Object mapReduceWithSharded() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("shardedOutput")
                .sharded(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/nonAtomic")
    public Object mapReduceWithNonAtomic() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("nonAtomicOutput")
                .action(MapReduceAction.REPLACE)
                .nonAtomic(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/batchSize")
    public Object mapReduceWithBatchSize() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .batchSize(2)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/bypassValidation")
    public Object mapReduceWithBypassValidation() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collectionName("bypassOutput")
                .bypassDocumentValidation(true)
                .into(new ArrayList<>()));
    }

    @GetMapping("/mapReduce/collation")
    public Object mapReduceWithCollation() {
        String map = "function() { emit(this.category, 1); }";
        String reduce = "function(key, values) { return Array.sum(values); }";
        Collation collation = Collation.builder().locale("en").caseLevel(true).build();
        return ResponseGenerator.generate(collection.mapReduce(map, reduce)
                .collation(collation)
                .into(new ArrayList<>()));
    }

    // ---------------------------------------------- MongoClient Methods -------------------------------------------    
    @GetMapping("mongoClient/getDatabase")
    public Object getDatabase() {
        MongoDatabase db = mongoClient.getDatabase("mydb");
        return ResponseGenerator.generate("Database name: " + db.getName());
    }

    @GetMapping("mongoClient/listDatabaseNames")
    public Object listDatabaseNames() {
        return ResponseGenerator.generate(mongoClient.listDatabaseNames().into(new ArrayList<>()));
    }

    @GetMapping("mongoClient/listDatabases")
    public Object listDatabases() {
        return ResponseGenerator.generate(mongoClient.listDatabases().into(new ArrayList<>()));
    }

    @GetMapping("mongoClient/startSession")
    public Object startSession() {
        try (ClientSession session = mongoClient.startSession()) {
            return ResponseGenerator.generate("Session started with ID: " + session.getServerSession().getIdentifier().toJson());
        }
    }

    @GetMapping("mongoClient/clusterDescription")
    public Object getClusterDescription() {
        return ResponseGenerator.generate(mongoClient.getClusterDescription().getType().name());
    }

    @GetMapping("mongoClient/watch")
    public Object clientLevelWatch() {
        return ResponseGenerator.generate(mongoClient.watch().into(new ArrayList<>()));
    }

    @GetMapping("mongoClient/watchWithPipeline")
    public Object clientLevelWatchWithPipeline() {
        List<Bson> pipeline = Collections.singletonList(Aggregates.match(Filters.exists("_id")));
        return ResponseGenerator.generate(mongoClient.watch(pipeline).into(new ArrayList<>()));
    }

//    ------------------------------------------------- MongoClientFactory Methods ---------------------------------------

    @GetMapping("/collection/factoryClient")
    public Object createClientWithFactory() throws Exception {
        MongoClientFactory factory = new MongoClientFactory();

        Hashtable<String, String> environment = new Hashtable<>();
        environment.put("connectionString", "mongodb://localhost:27017"); // static value

        // getObjectInstance parameters: obj, name, nameCtx, environment
        MongoClient client = (MongoClient) factory.getObjectInstance(null, null, null, environment);

        // Just fetch and return the first database name as a proof of connection  
        String dbName = client.listDatabaseNames().first();
        client.close(); // cleanup

        return ResponseGenerator.generate("Connected. First DB name: " + dbName);
    }
//    --------------------------------------------------- MongoClients Methods -------------------------------------------    //

    @GetMapping("mongoClients/createDefault")
    public Object createDefaultClient() {
        try (MongoClient client = MongoClients.create()) {
            return ResponseGenerator.generate("Connected to: " + client.getClusterDescription().getType());
        }
    }

    @GetMapping("mongoClients/createFromString")
    public Object createFromString() {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            return ResponseGenerator.generate("Connected to: " + client.getClusterDescription().getType());
        }
    }

    @GetMapping("mongoClients/createFromConnectionString")
    public Object createFromConnectionString() {
        ConnectionString conn = new ConnectionString("mongodb://localhost:27017");
        try (MongoClient client = MongoClients.create(conn)) {
            return ResponseGenerator.generate("Connected to: " + client.getClusterDescription().getType());
        }
    }

    @GetMapping("mongoClients/createFromSettings")
    public Object createFromSettings() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                .build();
        try (MongoClient client = MongoClients.create(settings)) {
            return ResponseGenerator.generate("Connected to: " + client.getClusterDescription().getType());
        }
    }

    @GetMapping("mongoClients/createFromConnStrAndDriverInfo")
    public Object createWithDriverInfo() {
        ConnectionString conn = new ConnectionString("mongodb://localhost:27017");
        MongoDriverInformation info = MongoDriverInformation.builder()
                .driverName("my-custom-driver")
                .driverVersion("1.0")
                .build();

        try (MongoClient client = MongoClients.create(conn, info)) {
            return ResponseGenerator.generate("Connected with custom driver info: " + client.getClusterDescription().getType());
        }
    }

    @GetMapping("mongoClients/createFromSettingsAndDriverInfo")
    public Object createWithSettingsAndDriverInfo() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                .applicationName("MyApp")
                .build();

        MongoDriverInformation info = MongoDriverInformation.builder()
                .driverName("my-custom-driver")
                .driverVersion("1.0")
                .build();

        try (MongoClient client = MongoClients.create(settings, info)) {
            return ResponseGenerator.generate("Connected with settings + driver info: " + client.getClusterDescription().getType());
        }
    }


//    ------------------------------------------------ Collection Methods -------------------------------------------    //

    @GetMapping("collection/namespace")
    public Object getNamespace() {
        return ResponseGenerator.generate(collection.getNamespace().getFullName());
    }

    @GetMapping("collection/documentClass")
    public Object getDocumentClass() {
        return ResponseGenerator.generate(collection.getDocumentClass().getName());
    }

    @GetMapping("collection/codecRegistry")
    public Object getCodecRegistry() {
        return ResponseGenerator.generate(collection.getCodecRegistry().toString());
    }

    @GetMapping("collection/readPreference")
    public Object getReadPreference() {
        return ResponseGenerator.generate(collection.getReadPreference().toString());
    }

    @GetMapping("collection/writeConcern")
    public Object getWriteConcern() {
        return ResponseGenerator.generate(collection.getWriteConcern().toString());
    }

    @GetMapping("collection/readConcern")
    public Object getReadConcern() {
        return ResponseGenerator.generate(collection.getReadConcern().toString());
    }

    @GetMapping("collection/withDocumentClass")
    public Object collectionwithDocumentClass() {
        MongoCollection<Document> newCollection = collection.withDocumentClass(Document.class);
        return ResponseGenerator.generate("New collection with class: " + newCollection.getDocumentClass().getName());
    }

    @GetMapping("collection/withCodecRegistry")
    public Object withCodecRegistry() {
        MongoCollection<Document> newCollection = collection.withCodecRegistry(collection.getCodecRegistry());
        return ResponseGenerator.generate("New collection with codec registry: " + newCollection.getCodecRegistry().toString());
    }

    @GetMapping("collection/withReadPreference")
    public Object withReadPreference() {
        MongoCollection<Document> newCollection = collection.withReadPreference(collection.getReadPreference());
        return ResponseGenerator.generate("New collection with read preference: " + newCollection.getReadPreference().toString());
    }

    @GetMapping("collection/withWriteConcern")
    public Object withWriteConcern() {
        MongoCollection<Document> newCollection = collection.withWriteConcern(collection.getWriteConcern());
        return ResponseGenerator.generate("New collection with write concern: " + newCollection.getWriteConcern().toString());
    }

    @GetMapping("collection/withReadConcern")
    public Object withReadConcern() {
        MongoCollection<Document> newCollection = collection.withReadConcern(collection.getReadConcern());
        return ResponseGenerator.generate("New collection with read concern: " + newCollection.getReadConcern().toString());
    }

    @GetMapping("collection/countDocuments")
    public Object countDocuments() {
        return ResponseGenerator.generate(collection.countDocuments());
    }

    @GetMapping("collection/countDocuments/filter")
    public Object countDocumentsWithFilter() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        return ResponseGenerator.generate(collection.countDocuments(filter));
    }

    @GetMapping("collection/countDocuments/options")
    public Object countDocumentsWithOptions() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        CountOptions options = new CountOptions().limit(10);
        return ResponseGenerator.generate(collection.countDocuments(filter, options));
    }

    @GetMapping("collection/countDocuments/session")
    public Object countDocumentsWithSession() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        return ResponseGenerator.generate(collection.countDocuments(session, filter));
    }

    // countDocuments with filter, ClientSession, and options
    @GetMapping("collection/countDocumentsWithSession/filter/options")
    public Object countDocumentsWithSessionFilterOptions() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        CountOptions options = new CountOptions().limit(10);
        return ResponseGenerator.generate(collection.countDocuments(session, filter, options));
    }

    // estimatedDocumentCount
    @GetMapping("collection/estimatedDocumentCount")
    public Object estimatedDocumentCount() {
        return ResponseGenerator.generate(collection.estimatedDocumentCount());
    }

    // estimatedDocumentCount with options
    @GetMapping("collection/estimatedDocumentCount/options")
    public Object estimatedDocumentCountWithOptions() {
        EstimatedDocumentCountOptions options = new EstimatedDocumentCountOptions();
        return ResponseGenerator.generate(collection.estimatedDocumentCount(options));
    }

    // distinct fieldName
    @GetMapping("collection/distinct")
    public Object distinctField() {
        return ResponseGenerator.generate(collection.distinct("contact.email", String.class).into(new ArrayList<>()));
    }

    // distinct fieldName with filter
    @GetMapping("collection/distinct/filter")
    public Object distinctFieldWithFilter() {
        Bson filter = Filters.eq("contact.name", "John Doe");
        return ResponseGenerator.generate(collection.distinct("contact.email", filter, String.class).into(new ArrayList<>()));
    }

    // distinct with ClientSession and fieldName
    @GetMapping("collection/distinct/session")
    public Object distinctWithSession() {
        return ResponseGenerator.generate(collection.distinct(session, "contact.email", String.class).into(new ArrayList<>()));
    }

    // distinct with ClientSession, fieldName, and filter
    @GetMapping("collection/distinct/session/filter")
    public Object distinctWithSessionFilter() {
        Bson filter = Filters.eq("contact.name", "John Doe");
        return ResponseGenerator.generate(collection.distinct(session, "contact.email", filter, String.class).into(new ArrayList<>()));
    }

    // find all documents
    @GetMapping("collection/find")
    public Object collectionFind() {
        return ResponseGenerator.generate(collection.find().into(new ArrayList<>()));
    }

    // find with filter
    @GetMapping("collection/find/filter")
    public Object findWithFilter() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        return ResponseGenerator.generate(collection.find(filter).into(new ArrayList<>()));
    }

    // find with ClientSession
    @GetMapping("collection/find/session")
    public Object findWithSession() {
        return ResponseGenerator.generate(collection.find(session).into(new ArrayList<>()));
    }

    // find with filter and ClientSession
    @GetMapping("collection/find/filter/session")
    public Object findWithFilterAndSession() {
        Bson filter = Filters.eq("contact.email", SAMPLE_EMAIL);
        return ResponseGenerator.generate(collection.find(session, filter).into(new ArrayList<>()));
    }

    // aggregate
    @GetMapping("collection/aggregate")
    public Object aggregate() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id"))
        );
        return ResponseGenerator.generate(collection.aggregate(pipeline).into(new ArrayList<>()));
    }

    // aggregate with ClientSession 
    @GetMapping("collection/aggregate/session")
    public Object aggregateWithSession() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id"))
        );
        return ResponseGenerator.generate(collection.aggregate(session, pipeline).into(new ArrayList<>()));
    }

    // watch change stream
    @GetMapping("collection/watch")
    public Object watch() {
        return ResponseGenerator.generate(collection.watch().into(new ArrayList<>()));
    }

    // watch with ClientSession
    @GetMapping("collection/watch/session")
    public Object watchWithSession() {
        return ResponseGenerator.generate(collection.watch(session).into(new ArrayList<>()));
    }

    // watch with pipeline
    @GetMapping("collection/watch/pipeline")
    public Object watchWithPipeline() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id"))
        );
        return ResponseGenerator.generate(collection.watch(pipeline).into(new ArrayList<>()));
    }

    // watch with pipeline and ClientSession
    @GetMapping("collection/watch/pipeline/session")
    public Object watchWithPipelineAndSession() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.exists("_id"))
        );
        return ResponseGenerator.generate(collection.watch(session, pipeline).into(new ArrayList<>()));
    }

    // mapReduce (Deprecated)
    @GetMapping("collection/mapReduce")
    public Object mapReduce() {
        String mapFunction = "function() { emit(this._id, 1); }";
        String reduceFunction = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(mapFunction, reduceFunction).into(new ArrayList<>()));
    }

    // mapReduce with result class (Deprecated)
    @GetMapping("collection/mapReduce/resultClass")
    public Object mapReduceWithResultClass() {
        String mapFunction = "function() { emit(this._id, 1); }";
        String reduceFunction = "function(key, values) { return Array.sum(values); }";
        return ResponseGenerator.generate(collection.mapReduce(mapFunction, reduceFunction, MyResult.class).into(new ArrayList<>()));
    }

    // mapReduce with ClientSession (Deprecated)
    @GetMapping("collection/mapReduce/session")
    public Object mapReduceWithSession() {
        String mapFunction = "function() { emit(this._id, 1); }";
        String reduceFunction = "function(key, values) { return Array.sum(values); }";

        try (ClientSession session = mongoClient.startSession()) {
            return ResponseGenerator.generate(collection.mapReduce(session, mapFunction, reduceFunction).into(new ArrayList<>()));
        }
        // The try-with-resources ensures the session is closed after use.
    }

    // mapReduce with ClientSession and result class (Deprecated)
    @GetMapping("collection/mapReduce/session/resultClass")
    public Object mapReduceWithSessionAndResultClass() {
        String mapFunction = "function() { emit(this._id, 1); }";
        String reduceFunction = "function(key, values) { return Array.sum(values); }";
        try (ClientSession session = mongoClient.startSession()) {
            return ResponseGenerator.generate(collection.mapReduce(session, mapFunction, reduceFunction, MyResult.class).into(new ArrayList<>()));
        }
    }

//dropIndex
    @GetMapping("collection/dropIndex")
    public Object dropIndex() {
        // Drop index on "initializedAt" field if exists
        String indexName = "initializedAt_1";
        collection.dropIndex(indexName);
        return ResponseGenerator.generate("Dropped index: " + indexName);
    }

//    dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions)
    @GetMapping("collection/dropIndexSessionByKeysOptions")
    public Object dropIndexSessionByKeysOptions() {
        Bson keys = new Document("initializedAt", 1);
        DropIndexOptions options = new DropIndexOptions();
        collection.dropIndex(session, keys, options);
        return ResponseGenerator.generate("Dropped index by keys with session and options: " + keys);
    }

    // bulkWrite
    @GetMapping("collection/bulkWrite")
    public Object bulkWrite() {
        List<WriteModel<Document>> requests = Arrays.asList(
                new InsertOneModel<>(new Document("name", "Alice")),
                new DeleteOneModel<>(Filters.eq("name", "Bob"))
        );
        return ResponseGenerator.generate(collection.bulkWrite(requests));
    }

    // bulkWrite with options
    @GetMapping("collection/bulkWrite/options")
    public Object bulkWriteWithOptions() {
        List<WriteModel<Document>> requests = Arrays.asList(
                new InsertOneModel<>(new Document("name", "Alice")),
                new DeleteOneModel<>(Filters.eq("name", "Bob"))
        );
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        return ResponseGenerator.generate(collection.bulkWrite(requests, options));
    }

    // bulkWrite with ClientSession
    @GetMapping("collection/bulkWrite/session")
    public Object bulkWriteWithSession() {
        List<WriteModel<Document>> requests = Arrays.asList(
                new InsertOneModel<>(new Document("name", "Alice")),
                new DeleteOneModel<>(Filters.eq("name", "Bob"))
        );
        return ResponseGenerator.generate(collection.bulkWrite(session, requests));
    }

    // bulkWrite with ClientSession and options
    @GetMapping("collection/bulkWrite/session/options")
    public Object bulkWriteWithSessionAndOptions() {
        List<WriteModel<Document>> requests = Arrays.asList(
                new InsertOneModel<>(new Document("name", "Alice")),
                new DeleteOneModel<>(Filters.eq("name", "Bob"))
        );
        BulkWriteOptions options = new BulkWriteOptions().ordered(false);
        return ResponseGenerator.generate(collection.bulkWrite(session, requests, options));
    }

    // insertOne
    @GetMapping("collection/insertOne")
    public Object insertOne() {
        Document doc = new Document("name", "Alice");
       Object insertResult =  collection.insertOne(doc);
        mongoClient.close();
        return ResponseGenerator.generate(insertResult);
    }

    // insertOne with options
    @GetMapping("collection/insertOne/options")
    public Object insertOneWithOptions() {
        Document doc = new Document("name", "Alice");
        InsertOneOptions options = new InsertOneOptions().bypassDocumentValidation(true);
        return ResponseGenerator.generate(collection.insertOne(doc, options));
    }

    // insertOne with ClientSession
    @GetMapping("collection/insertOne/session")
    public Object insertOneWithSession() {
        Document doc = new Document("name", "Alice");
        return ResponseGenerator.generate(collection.insertOne(session, doc));
    }

    // insertOne with ClientSession and options
    @GetMapping("collection/insertOne/session/options")
    public Object insertOneWithSessionAndOptions() {
        Document doc = new Document("name", "Alice");
        InsertOneOptions options = new InsertOneOptions().bypassDocumentValidation(true);
        return ResponseGenerator.generate(collection.insertOne(session, doc, options));
    }

    // insertMany
    @GetMapping("collection/insertMany")
    public Object insertMany() {
        List<Document> docs = Arrays.asList(
                new Document("name", "Alice"),
                new Document("name", "Bob")
        );
        return ResponseGenerator.generate(collection.insertMany(docs));
    }

    // insertMany with options
    @GetMapping("collection/insertMany/options")
    public Object insertManyWithOptions() {
        List<Document> docs = Arrays.asList(
                new Document("name", "Alice"),
                new Document("name", "Bob")
        );
        InsertManyOptions options = new InsertManyOptions().ordered(false);
        return ResponseGenerator.generate(collection.insertMany(docs, options));
    }

    // insertMany with ClientSession
    @GetMapping("collection/insertMany/session")
    public Object insertManyWithSession() {
        List<Document> docs = Arrays.asList(
                new Document("name", "Alice"),
                new Document("name", "Bob")
        );
        return ResponseGenerator.generate(collection.insertMany(session, docs));
    }

    // insertMany with ClientSession and options
    @GetMapping("collection/insertMany/session/options")
    public Object insertManyWithSessionAndOptions() {
        List<Document> docs = Arrays.asList(
                new Document("name", "Alice"),
                new Document("name", "Bob")
        );
        InsertManyOptions options = new InsertManyOptions().ordered(false);
        return ResponseGenerator.generate(collection.insertMany(session, docs, options));
    }

    // deleteOne
    @GetMapping("collection/deleteOne")
    public Object deleteOne() {
        Bson filter = Filters.eq("name", "Alice");
        return ResponseGenerator.generate(collection.deleteOne(filter));
    }

    // deleteOne with options
    @GetMapping("collection/deleteOne/options")
    public Object deleteOneWithOptions() {
        Bson filter = Filters.eq("name", "Alice");
        DeleteOptions options = new DeleteOptions().collation(Collation.builder().locale("en").build());
        return ResponseGenerator.generate(collection.deleteOne(filter, options));
    }

    // deleteOne with ClientSession
    @GetMapping("collection/deleteOne/session")
    public Object deleteOneWithSession() {
        Bson filter = Filters.eq("name", "Alice");
        return ResponseGenerator.generate(collection.deleteOne(session, filter));
    }

    // deleteOne with ClientSession and options
    @GetMapping("collection/deleteOne/session/options")
    public Object deleteOneWithSessionAndOptions() {
        Bson filter = Filters.eq("name", "Alice");
        DeleteOptions options = new DeleteOptions().collation(Collation.builder().locale("en").build());
        return ResponseGenerator.generate(collection.deleteOne(session, filter, options));
    }

    // deleteMany
    @GetMapping("collection/deleteMany")
    public Object deleteMany() {
        List<Document> docs = Arrays.asList(
                new Document("name", "Alice"),
                new Document("name", "Bob")
        );
        Bson filter = Filters.and(
                Filters.eq("name", "Alice"),
                Filters.eq("city", "New York"),
                Filters.eq("active", true)
        );
        return ResponseGenerator.generate(collection.deleteMany(filter));
    }

    // deleteMany with options
    @GetMapping("collection/deleteMany/options")
    public Object deleteManyWithOptions() {
        Bson filter = Filters.eq("status", "inactive");
        DeleteOptions options = new DeleteOptions().collation(Collation.builder().locale("en").build());
        return ResponseGenerator.generate(collection.deleteMany(filter, options));
    }

    // deleteMany with ClientSession
    @GetMapping("collection/deleteMany/session")
    public Object deleteManyWithSession() {
        Bson filter = Filters.eq("status", "inactive");
        return ResponseGenerator.generate(collection.deleteMany(session, filter));
    }

    // deleteMany with ClientSession and options
    @GetMapping("collection/deleteMany/session/options")
    public Object deleteManyWithSessionAndOptions() {
        Bson filter = Filters.eq("status", "inactive");
        DeleteOptions options = new DeleteOptions().collation(Collation.builder().locale("en").build());
        return ResponseGenerator.generate(collection.deleteMany(session, filter, options));
    }

    // replaceOne
    @GetMapping("collection/replaceOne")
    public Object replaceOne() {
        Bson filter = Filters.eq("name", "Alice");
        Document replacement = new Document("name", "Alice Updated");
        return ResponseGenerator.generate(collection.replaceOne(filter, replacement));
    }

    // replaceOne with options
    @GetMapping("collection/replaceOne/options")
    public Object replaceOneWithOptions() {
        Bson filter = Filters.eq("name", "Alice Updated");
        Document replacement = new Document("name", "Alice");
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        return ResponseGenerator.generate(collection.replaceOne(filter, replacement, options));
    }

    // replaceOne with ClientSession
    @GetMapping("collection/replaceOne/session")
    public Object replaceOneWithSession() {
        Bson filter = Filters.eq("name", "Alice");
        Document replacement = new Document("name", "Alice Updated");
        return ResponseGenerator.generate(collection.replaceOne(session, filter, replacement));
    }

    // replaceOne with ClientSession and options
    @GetMapping("collection/replaceOne/session/options")
    public Object replaceOneWithSessionAndOptions() {
        Bson filter = Filters.eq("name", "Alice");
        Document replacement = new Document("name", "Alice Updated");
        ReplaceOptions options = new ReplaceOptions().upsert(true);
        return ResponseGenerator.generate(collection.replaceOne(session, filter, replacement, options));
    }

    // updateOne
    @GetMapping("collection/updateOne")
    public Object updateOne() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.updateOne(filter, update));
    }

    // updateOne with options
    @GetMapping("collection/updateOne/options")
    public Object updateOneWithOptions() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        UpdateOptions updateOptions = new UpdateOptions().upsert(true); // Static option
        return ResponseGenerator.generate(collection.updateOne(filter, update, updateOptions));
    }

    // updateOne with ClientSession
    @GetMapping("collection/updateOne/session")
    public Object updateOneWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.updateOne(clientSession, filter, update));
    }

    // updateOne with ClientSession and options
    @GetMapping("collection/updateOne/session/options")
    public Object updateOneWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        UpdateOptions updateOptions = new UpdateOptions().upsert(true); // Static option
        return ResponseGenerator.generate(collection.updateOne(clientSession, filter, update, updateOptions));
    }

    // updateMany
    @GetMapping("collection/updateMany")
    public Object updateMany() {
        Bson filter = Filters.eq("status", "active"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.updateMany(filter, update));
    }

    // updateMany with options
    @GetMapping("collection/updateMany/options")
    public Object updateManyWithOptions() {
        Bson filter = Filters.eq("status", "active"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        UpdateOptions updateOptions = new UpdateOptions().upsert(true); // Static option
        return ResponseGenerator.generate(collection.updateMany(filter, update, updateOptions));
    }

    // updateMany with ClientSession
    @GetMapping("collection/updateMany/session")
    public Object updateManyWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("status", "active"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.updateMany(clientSession, filter, update));
    }

    // updateMany with ClientSession and options
    @GetMapping("collection/updateMany/session/options")
    public Object updateManyWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("status", "active"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        UpdateOptions updateOptions = new UpdateOptions().upsert(true); // Static option
        return ResponseGenerator.generate(collection.updateMany(clientSession, filter, update, updateOptions));
    }

    // findOneAndDelete
    @GetMapping("collection/findOneAndDelete")
    public Object findOneAndDelete() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        return ResponseGenerator.generate(collection.findOneAndDelete(filter));
    }

    // findOneAndDelete with options
    @GetMapping("collection/findOneAndDelete/options")
    public Object findOneAndDeleteWithOptions() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        FindOneAndDeleteOptions options = new FindOneAndDeleteOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndDelete(filter, options));
    }

    // findOneAndDelete with ClientSession
    @GetMapping("collection/findOneAndDelete/session")
    public Object findOneAndDeleteWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        return ResponseGenerator.generate(collection.findOneAndDelete(clientSession, filter));
    }

    // findOneAndDelete with ClientSession and options
    @GetMapping("collection/findOneAndDelete/session/options")
    public Object findOneAndDeleteWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        FindOneAndDeleteOptions options = new FindOneAndDeleteOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndDelete(clientSession, filter, options));
    }

    // findOneAndReplace
    @GetMapping("collection/findOneAndReplace")
    public Object findOneAndReplace() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Document replacement = new Document("name", "Jane Doe").append("age", 30); // Example static replacement
        return ResponseGenerator.generate(collection.findOneAndReplace(filter, replacement));
    }

    // findOneAndReplace with options
    @GetMapping("collection/findOneAndReplace/options")
    public Object findOneAndReplaceWithOptions() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Document replacement = new Document("name", "Jane Doe").append("age", 30); // Example static replacement
        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndReplace(filter, replacement, options));
    }

    // findOneAndReplace with ClientSession
    @GetMapping("collection/findOneAndReplace/session")
    public Object findOneAndReplaceWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Document replacement = new Document("name", "Jane Doe").append("age", 30); // Example static replacement
        return ResponseGenerator.generate(collection.findOneAndReplace(clientSession, filter, replacement));
    }

    // findOneAndReplace with ClientSession and options
    @GetMapping("collection/findOneAndReplace/session/options")
    public Object findOneAndReplaceWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Document replacement = new Document("name", "Jane Doe").append("age", 30); // Example static replacement
        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndReplace(clientSession, filter, replacement, options));
    }

    // findOneAndUpdate
    @GetMapping("collection/findOneAndUpdate")
    public Object findOneAndUpdate() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.findOneAndUpdate(filter, update));
    }

    // findOneAndUpdate with options
    @GetMapping("collection/findOneAndUpdate/options")
    public Object findOneAndUpdateWithOptions() {
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndUpdate(filter, update, options));
    }

    // findOneAndUpdate with ClientSession
    @GetMapping("collection/findOneAndUpdate/session")
    public Object findOneAndUpdateWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        return ResponseGenerator.generate(collection.findOneAndUpdate(clientSession, filter, update));
    }

    // findOneAndUpdate with ClientSession and options
    @GetMapping("collection/findOneAndUpdate/session/options")
    public Object findOneAndUpdateWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        Bson filter = Filters.eq("name", "John Doe"); // Example static value
        Bson update = Updates.set("age", 30); // Example static update
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions(); // Example static options
        return ResponseGenerator.generate(collection.findOneAndUpdate(clientSession, filter, update, options));
    }

    // drop
    @GetMapping("collection/drop")
    public Object drop() {
        collection.drop();
        return ResponseGenerator.generate("Collection dropped successfully");
    }

    // drop with ClientSession
    @GetMapping("collection/drop/session")
    public Object dropWithSession() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        collection.drop(clientSession);
        return ResponseGenerator.generate("Collection dropped successfully with session");
    }

    // drop with options
    @GetMapping("collection/drop/options")
    public Object dropWithOptions() {
        DropCollectionOptions options = new DropCollectionOptions(); // Example static options
        collection.drop(options);
        return ResponseGenerator.generate("Collection dropped successfully with options");
    }

    // drop with ClientSession and options
    @GetMapping("collection/drop/session/options")
    public Object dropWithSessionAndOptions() {
        ClientSession clientSession = mongoClient.startSession(); // Example static session
        DropCollectionOptions options = new DropCollectionOptions(); // Example static options
        collection.drop(clientSession, options);
        return ResponseGenerator.generate("Collection dropped successfully with session and options");
    }

    //  --------------------------------------------------------------- MongoCursor Methods -------------------------------------------    //
    @GetMapping("cursor/hasNext")
    public Object cursorHasNext() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            return ResponseGenerator.generate(cursor.hasNext());
        }
    }

//    @GetMapping("cursor/hasNextRecursive")
//    public Object cursorHasNext() {
//Map
//        try (MongoCursor<Document> cursor = collection.find().cursor()) {
//            return ResponseGenerator.generate(cursor.hasNext());
//        }
//    }

    @GetMapping("cursor/hasNextBreak")
    public Object hasNextBreak() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            // Assume 'collection' is your MongoCollection object

            int count = 0;
            int stopAfter = 6; // The number of documents you want to process
                while (cursor.hasNext()) {
                    // Stop if we have processed enough documents
                    if (count >= stopAfter) {
                        System.out.println("\nStopping loop after processing " + stopAfter + " documents.");
                        break; // Exit the loop immediately
                    }

                    Document doc = cursor.next();
                    System.out.println("Processing document #" + (count + 1));
                    // ... do your work with the 'doc' ...

                    count++;
                }
            System.out.println("Loop finished. Total documents processed: " + count);
        }
        return ResponseGenerator.generate("test hasNextBreak");
    };

    @GetMapping("/cursor/testLeak")
    public Object testLeakCursor() {
        MongoCursor<Document> cursor = collection.find().cursor(); // no try-with-resources

        int count = 0;
        int stopAfter = 6; // The number of documents you want to process
        while (cursor.hasNext()) {
            // Stop if we have processed enough documents
            if (count >= stopAfter) {
               return ResponseGenerator.generate("\nStopping loop after processing " + stopAfter + " documents.");
            }

            Document doc = cursor.next();
            System.out.println("Processing document #" + (count + 1));

            count++;
        }

        return ResponseGenerator.generate("test leak");
    }

    @GetMapping("/cursor/hasNextBreak2")
    public Object hasNextBreak2() {
        List<String> results = new ArrayList<>();
        MongoCursor<Document> cursor = collection.find().cursor();
            int count = 0;
            while (cursor.hasNext()) {
                if (count == 4) {
                   return ResponseGenerator.generate("Simulated error at 4th document");
                }

                Document doc = cursor.next();
                results.add(doc.toJson());
                count++;
            }

        return ResponseGenerator.generate(Map.of(
                "status", "success",
                "documents", results
        ));
    }

    @GetMapping("cursor/next")
    public Object cursorNext() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            if (cursor.hasNext()) {
                return ResponseGenerator.generate(cursor.next().toJson());
            }
            return ResponseGenerator.generate("No documents found");
        }
    }

    @GetMapping("cursor/tryNext")
    public Object cursorTryNext() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            Document doc = cursor.tryNext();
            return ResponseGenerator.generate(doc != null ? doc.toJson() : "No document available yet");
        }
    }

    @GetMapping("cursor/check")
    public Object cursorCheck() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            return ResponseGenerator.generate("cursor checked successfully");
        }
    }

    @GetMapping("cursor/available")
    public Object cursorAvailable() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            return ResponseGenerator.generate(cursor.available());
        }
    }

    @GetMapping("cursor/serverCursor")
    public Object cursorServerCursor() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            cursor.tryNext(); // advance to create cursor
            ServerCursor serverCursor = cursor.getServerCursor();
            return ResponseGenerator.generate(serverCursor != null ? String.valueOf(serverCursor.getId()) : "No server cursor");
        }
    }

    @GetMapping("cursor/serverAddress")
    public Object cursorServerAddress() {
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            return ResponseGenerator.generate(cursor.getServerAddress().toString());
        }
    }

    @GetMapping("cursor/forEachRemaining")
    public Object cursorForEachRemaining() {
        List<String> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            cursor.forEachRemaining(doc -> results.add(doc.toJson()));
        }
        return ResponseGenerator.generate(results);
    }


//    ----------------------------------------------------- Database Methods -------------------------------------------


    @GetMapping("/database/get-name")
    public Object getName() {
        String name = database.getName();
        return ResponseGenerator.generate("Database Name: " + name);
    }

    @GetMapping("/database/get-codec-registry")
    public Object getDatabaseCodecRegistry() {
        CodecRegistry codecRegistry = database.getCodecRegistry();
        return ResponseGenerator.generate("CodecRegistry Fetched");
    }

    @GetMapping("/database/get-read-preference")
    public Object getDatabaseReadPreference() {
        ReadPreference readPreference = database.getReadPreference();
        return ResponseGenerator.generate("ReadPreference: " + readPreference.getName());
    }

    @GetMapping("/database/get-write-concern")
    public Object getDatabaseWriteConcern() {
        WriteConcern writeConcern = database.getWriteConcern();
        return ResponseGenerator.generate("WriteConcern: " + writeConcern.asDocument().toJson());
    }

    @GetMapping("/database/get-read-concern")
    public Object getDatabaseReadConcern() {
        ReadConcern readConcern = database.getReadConcern();
        return ResponseGenerator.generate("ReadConcern: " + readConcern.asDocument().toJson());
    }

    @GetMapping("/database/with-codec-registry")
    public Object withDatabaseCodecRegistry() {
        MongoDatabase newDb = database.withCodecRegistry(database.getCodecRegistry());
        return ResponseGenerator.generate("Created New DB with CodecRegistry");
    }

    @GetMapping("/database/with-read-preference")
    public Object withDatabaseReadPreference() {
        MongoDatabase newDb = database.withReadPreference(ReadPreference.primary());
        return ResponseGenerator.generate("Created New DB with ReadPreference");
    }

    @GetMapping("/database/with-write-concern")
    public Object withDatabaseWriteConcern() {
        MongoDatabase newDb = database.withWriteConcern(WriteConcern.ACKNOWLEDGED);
        return ResponseGenerator.generate("Created New DB with WriteConcern");
    }

    @GetMapping("/database/with-read-concern")
    public Object withDatabaseReadConcern() {
        MongoDatabase newDb = database.withReadConcern(ReadConcern.LOCAL);
        return ResponseGenerator.generate("Created New DB with ReadConcern");
    }

    @GetMapping("/database/get-collection")
    public Object getCollection() {
        MongoCollection<Document> collection = database.getCollection("sampleCollection");
        return ResponseGenerator.generate("Fetched Collection: " + collection.getNamespace().getCollectionName());
    }

    @GetMapping("/database/get-collection-with-class")
    public Object getCollectionWithClass() {
        MongoCollection<Document> collection = database.getCollection("sampleCollection", Document.class);
        return ResponseGenerator.generate("Fetched Collection with Class: " + collection.getNamespace().getCollectionName());
    }

    @GetMapping("/database/run-command")
    public Object runCommand() {
        Document result = database.runCommand(new Document("ping", 1));
        return ResponseGenerator.generate("Run Command Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-with-read-preference")
    public Object runCommandWithReadPreference() {
        Document result = database.runCommand(new Document("ping", 1), ReadPreference.primary());
        return ResponseGenerator.generate("Run Command with ReadPreference Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-with-result-class")
    public Object runCommandWithResultClass() {
        Document result = database.runCommand(new Document("ping", 1), Document.class);
        return ResponseGenerator.generate("Run Command with ResultClass Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-with-read-preference-and-result-class")
    public Object runCommandWithReadPreferenceAndResultClass() {
        Document result = database.runCommand(new Document("ping", 1), ReadPreference.primary(), Document.class);
        return ResponseGenerator.generate("Run Command with ReadPreference and ResultClass Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-client-session")
    public Object runCommandClientSession() {
        Document result = database.runCommand(session, new Document("ping", 1));
        return ResponseGenerator.generate("Run Command with ClientSession Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-client-session-with-read-preference")
    public Object runCommandClientSessionWithReadPreference() {
        Document result = database.runCommand(session, new Document("ping", 1), ReadPreference.primary());
        return ResponseGenerator.generate("Run Command with ClientSession and ReadPreference Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-client-session-with-result-class")
    public Object runCommandClientSessionWithResultClass() {
        Document result = database.runCommand(session, new Document("ping", 1), Document.class);
        return ResponseGenerator.generate("Run Command with ClientSession and ResultClass Result: " + result.toJson());
    }

    @GetMapping("/database/run-command-client-session-with-read-preference-and-result-class")
    public Object runCommandClientSessionWithReadPreferenceAndResultClass() {
        Document result = database.runCommand(session, new Document("ping", 1), ReadPreference.primary(), Document.class);
        return ResponseGenerator.generate("Run Command with ClientSession, ReadPreference and ResultClass Result: " + result.toJson());
    }

    @GetMapping("/database/drop")
    public Object dropDatabase() {
        MongoDatabase dbToDrop = mongoClient.getDatabase(DATABASE_TO_DROP);
        dbToDrop.drop();
        return ResponseGenerator.generate("Database dropped.");
    }

    @GetMapping("/database/drop-client-session")
    public Object dropDatabaseWithClientSession() {
        database.drop(session);
        return ResponseGenerator.generate("Database dropped with ClientSession.");
    }

    @GetMapping("/database/list-collection-names")
    public Object listCollectionNames() {
        MongoIterable<String> collections = database.listCollectionNames();
        return ResponseGenerator.generate("Collections: " + String.join(", ", collections));
    }

    @GetMapping("/database/list-collections")
    public Object listCollections() {
        boolean collections = database.listCollectionNames().into(new ArrayList<>()).contains("mycollection");
        return ResponseGenerator.generate("Listed collections.");
    }

    @GetMapping("/database/list-collections-with-class")
    public Object listCollectionsWithClass() {
        ListCollectionsIterable<Document> collections = database.listCollections(Document.class);
        return ResponseGenerator.generate("Listed collections with class.");
    }

    @GetMapping("/database/list-collection-names-client-session")
    public Object listCollectionNamesClientSession() {
        MongoIterable<String> collections = database.listCollectionNames(session);
        return ResponseGenerator.generate("Collections (ClientSession): " + String.join(", ", collections));
    }

    @GetMapping("/database/list-collections-client-session")
    public Object listCollectionsClientSession() {
        ListCollectionsIterable<Document> collections = database.listCollections(session);
        return ResponseGenerator.generate("Listed collections (ClientSession).");
    }

    @GetMapping("/database/list-collections-client-session-with-class")
    public Object listCollectionsClientSessionWithClass() {
        ListCollectionsIterable<Document> collections = database.listCollections(session, Document.class);
        return ResponseGenerator.generate("Listed collections (ClientSession, with class).");
    }

    @GetMapping("/database/create-collection")
    public Object createCollection() {
        boolean collectionExists = true;
//        for (String collectionName : database.listCollectionNames()) {
//            if (collectionName.equals("testCollection2")) {
//                collectionExists = true;
//                break;
//            }
//        }

        if (collectionExists) {
            database.createCollection("testCollection5");
            return ResponseGenerator.generate("Created collection 'testCollection'.");
        } else {
            return ResponseGenerator.generate("Collection 'testCollection' already exists.");
        }
    }

    @GetMapping("/database/create-collection-with-options")
    public Object createCollectionWithOptions() {
        boolean collectionExists = false;
        for (String collectionName : database.listCollectionNames()) {
            if (collectionName.equals("testCollectionOptions")) {
                collectionExists = true;
                break;
            }
        }

        if (!collectionExists) {
            database.createCollection("testCollectionOptions", new CreateCollectionOptions());
            return ResponseGenerator.generate("Created collection 'testCollectionOptions'.");
        } else {
            return ResponseGenerator.generate("Collection 'testCollectionOptions' already exists.");
        }
    }

    @GetMapping("/database/create-collection-client-session")
    public Object createCollectionClientSession() {
        boolean collectionExists = false;
        for (String collectionName : database.listCollectionNames()) {
            if (collectionName.equals("testCollectionSession")) {
                collectionExists = true;
                break;
            }
        }

        if (!collectionExists) {
            database.createCollection(session, "testCollectionSession");
            return ResponseGenerator.generate("Created collection 'testCollectionSession' with ClientSession.");
        } else {
            return ResponseGenerator.generate("Collection 'testCollection' already exists.");
        }
    }

    @GetMapping("/database/create-collection-client-session-with-options")
    public Object createCollectionClientSessionWithOptions() {
        boolean collectionExists = false;
        for (String collectionName : database.listCollectionNames()) {
            if (collectionName.equals("testCollectionSessionOptions")) {
                collectionExists = true;
                break;
            }
        }

        if (!collectionExists) {
            database.createCollection(session, "testCollectionSessionOptions", new CreateCollectionOptions());
            return ResponseGenerator.generate("Created collection 'testCollectionSessionOptions' with options.");
        } else {
            return ResponseGenerator.generate("Collection 'testCollectionSessionOptions' already exists.");
        }
    }

    @GetMapping("/database/create-view")
    public Object createView() {
        long epochMillis = System.currentTimeMillis();
        String viewName = "testView_" + epochMillis;

        database.createView(viewName, "testCollection", Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Created view '" + viewName + "'.");
    }

    @GetMapping("/database/create-view-with-options")
    public Object createViewWithOptions() {
        long epochMillis = System.currentTimeMillis();
        String viewName = "testView_" + epochMillis;
        database.createView(viewName, "testCollection", Arrays.asList(Aggregates.match(Filters.exists("_id"))), new CreateViewOptions());
        return ResponseGenerator.generate("Created view 'testViewOptions' with options.");
    }

    @GetMapping("/database/create-view-client-session")
    public Object createViewClientSession() {
        long epochMillis = System.currentTimeMillis();
        String viewName = "testViewSession" + epochMillis;
        database.createView(session, viewName, "testCollection", Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Created view 'testViewSession' with ClientSession.");
    }

    @GetMapping("/database/create-view-client-session-with-options")
    public Object createViewClientSessionWithOptions() {
        long epochMillis = System.currentTimeMillis();
        String viewName = "testViewSession" + epochMillis;
        database.createView(session, viewName, "testCollection", Arrays.asList(Aggregates.match(Filters.exists("_id"))), new CreateViewOptions());
        return ResponseGenerator.generate("Created view 'testViewSessionOptions' with options.");
    }

    @GetMapping("/database/watch")
    public Object watchDatabase() {
        ChangeStreamIterable<Document> stream = database.watch();
        return ResponseGenerator.generate("Watching database.");
    }

    @GetMapping("/database/watch-with-class")
    public Object watchWithDatabaseClass() {
        ChangeStreamIterable<Document> stream = database.watch(Document.class);
        return ResponseGenerator.generate("Watching database with class.");
    }

    @GetMapping("/database/watch-with-pipeline")
    public Object watchWithDatabasePipeline() {
        ChangeStreamIterable<Document> stream = database.watch(Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Watching database with pipeline.");
    }

    @GetMapping("/database/watch-with-pipeline-and-class")
    public Object watchWithPipelineAndClass() {
        ChangeStreamIterable<Document> stream = database.watch(Arrays.asList(Aggregates.match(Filters.exists("_id"))), Document.class);
        return ResponseGenerator.generate("Watching database with pipeline and class.");
    }

    @GetMapping("/database/watch-client-session")
    public Object watchClientSession() {
        ChangeStreamIterable<Document> stream = database.watch(session);
        return ResponseGenerator.generate("Watching database with ClientSession.");
    }

    @GetMapping("/database/watch-client-session-with-class")
    public Object watchClientSessionWithClass() {
        ChangeStreamIterable<Document> stream = database.watch(session, Document.class);
        return ResponseGenerator.generate("Watching database with ClientSession and class.");
    }

    @GetMapping("/database/watch-client-session-with-pipeline")
    public Object watchClientSessionWithPipeline() {
        ChangeStreamIterable<Document> stream = database.watch(session, Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Watching database with ClientSession and pipeline.");
    }

    @GetMapping("/database/watch-client-session-with-pipeline-and-class")
    public Object watchClientSessionWithPipelineAndClass() {
        ChangeStreamIterable<Document> stream = database.watch(session, Arrays.asList(Aggregates.match(Filters.exists("_id"))), Document.class);
        return ResponseGenerator.generate("Watching database with ClientSession, pipeline and class.");
    }

    @GetMapping("/database/aggregate")
    public Object aggregateDatabase() {
        AggregateIterable<Document> result = database.aggregate(Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Aggregation done.");
    }

    @GetMapping("/database/aggregate-with-class")
    public Object aggregateWithClass() {
        AggregateIterable<Document> result = database.aggregate(Arrays.asList(Aggregates.match(Filters.exists("_id"))), Document.class);
        return ResponseGenerator.generate("Aggregation with class done.");
    }

    @GetMapping("/database/aggregate-client-session")
    public Object aggregateClientSession() {
        AggregateIterable<Document> result = database.aggregate(session, Arrays.asList(Aggregates.match(Filters.exists("_id"))));
        return ResponseGenerator.generate("Aggregation with ClientSession done.");
    }

    @GetMapping("/database/aggregate-client-session-with-class")
    public Object aggregateClientSessionWithClass() {
        AggregateIterable<Document> result = database.aggregate(session, Arrays.asList(Aggregates.match(Filters.exists("_id"))), Document.class);
        return ResponseGenerator.generate("Aggregation with ClientSession and class done.");
    }



//    ------------------------------------------- MongoIterable Methods -------------------------------------------    //

    @GetMapping("mongoIterable/batchSize")
    public Object iterableBatchSize() {
        return ResponseGenerator.generate(collection.find()
                .batchSize(5)
                .into(new ArrayList<>()));
    }

    @GetMapping("mongoIterable/first")
    public Object iterableFirst() {
        return ResponseGenerator.generate(collection.find().first()); // returns the first document or null
    }

    @GetMapping("mongoIterable/into")
    public Object iterableInto() {
        List<Document> target = new ArrayList<>();
        return ResponseGenerator.generate(collection.find().into(target));
    }

    @GetMapping("mongoIterable/iterator")
    public Object iterableIterator() {
        List<String> docs = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                docs.add(cursor.next().toJson());
            }
        }
        return ResponseGenerator.generate(docs);
    }

    @GetMapping("mongoIterable/cursor")
    public Object iterableCursor() {
        List<String> result = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().cursor()) {
            while (cursor.hasNext()) {
                result.add(cursor.next().toJson());
            }
        }
        return ResponseGenerator.generate(result);
    }

    @GetMapping("mongoIterable/map")
    public Object iterableMap() {
       // Now perform the map operation
        return ResponseGenerator.generate(collection.find()
                .map(doc -> {
                    Object contactValue = doc.get("contact");
                    if (contactValue != null) {
                        return contactValue.toString(); // Use toString() for any non-null value
                    } else {
                        return "no-contact";
                    }
                })
                .into(new ArrayList<>()));
    }

//    ---------------------------------------------- TransactionBody Method -------------------------------------------    //

    @GetMapping("transactionBody/withTransaction")
    public Object runWithTransaction() {
        return ResponseGenerator.generate( session.withTransaction((TransactionBody<String>) () -> {
            Document doc = new Document("contact", new Document("email", "tx_user@ht.com"))
                    .append("createdInTransaction", true);
            collection.insertOne(session, doc);
            return "Inserted inside transaction";
        }));
    }
}
