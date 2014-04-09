package com.roadwatch.server.meta;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2014-04-09 15:08:32")
/** */
public final class UserMeta extends org.slim3.datastore.ModelMeta<com.roadwatch.server.model.User> {

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> androidVersion = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "androidVersion", "androidVersion");

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> appVersion = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "appVersion", "appVersion");

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, java.util.Date> createdAt = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, java.util.Date>(this, "createdAt", "createdAt", java.util.Date.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> email = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "email", "email");

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> encryptedPassword = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "encryptedPassword", "encryptedPassword");

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> licensePlate = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "licensePlate", "licensePlate");

    /** */
    public final org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>> ownGcmIds = new org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>>(this, "ownGcmIds", "ownGcmIds", java.util.List.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, java.lang.Integer> purchasedTrackedLicensePlatesSize = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, java.lang.Integer>(this, "purchasedTrackedLicensePlatesSize", "purchasedTrackedLicensePlatesSize", int.class);

    /** */
    public final org.slim3.datastore.CollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<com.google.appengine.api.datastore.Key>, com.google.appengine.api.datastore.Key> sharingUserKeys = new org.slim3.datastore.CollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<com.google.appengine.api.datastore.Key>, com.google.appengine.api.datastore.Key>(this, "sharingUserKeys", "sharingUserKeys", java.util.List.class);

    /** */
    public final org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>> trackedLicensePlates = new org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>>(this, "trackedLicensePlates", "trackedLicensePlates", java.util.List.class);

    /** */
    public final org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>> trackedNames = new org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>>(this, "trackedNames", "trackedNames", java.util.List.class);

    /** */
    public final org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>> trackingGcmIds = new org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.User, java.util.List<java.lang.String>>(this, "trackingGcmIds", "trackingGcmIds", java.util.List.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, com.roadwatch.server.model.User.UserType> type = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.User, com.roadwatch.server.model.User.UserType>(this, "type", "type", com.roadwatch.server.model.User.UserType.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> username = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "username", "username");

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User> uuid = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.User>(this, "uuid", "uuid");

    private static final org.slim3.datastore.CreationDate slim3_createdAtAttributeListener = new org.slim3.datastore.CreationDate();

    private static final UserMeta slim3_singleton = new UserMeta();

    /**
     * @return the singleton
     */
    public static UserMeta get() {
       return slim3_singleton;
    }

    /** */
    public UserMeta() {
        super("User", com.roadwatch.server.model.User.class);
    }

    @Override
    public com.roadwatch.server.model.User entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.roadwatch.server.model.User model = new com.roadwatch.server.model.User();
        model.setAndroidVersion((java.lang.String) entity.getProperty("androidVersion"));
        model.setAppVersion((java.lang.String) entity.getProperty("appVersion"));
        model.setCreatedAt((java.util.Date) entity.getProperty("createdAt"));
        model.setEmail((java.lang.String) entity.getProperty("email"));
        model.setEncryptedPassword((java.lang.String) entity.getProperty("encryptedPassword"));
        model.setKey(entity.getKey());
        model.setLicensePlate((java.lang.String) entity.getProperty("licensePlate"));
        model.setOwnGcmIds(toList(java.lang.String.class, entity.getProperty("ownGcmIds")));
        model.setPurchasedTrackedLicensePlatesSize(longToPrimitiveInt((java.lang.Long) entity.getProperty("purchasedTrackedLicensePlatesSize")));
        model.setSharingUserKeys(toList(com.google.appengine.api.datastore.Key.class, entity.getProperty("sharingUserKeys")));
        model.setTrackedLicensePlates(toList(java.lang.String.class, entity.getProperty("trackedLicensePlates")));
        model.setTrackedNames(toList(java.lang.String.class, entity.getProperty("trackedNames")));
        model.setTrackingGcmIds(toList(java.lang.String.class, entity.getProperty("trackingGcmIds")));
        model.setType(stringToEnum(com.roadwatch.server.model.User.UserType.class, (java.lang.String) entity.getProperty("type")));
        model.setUsername((java.lang.String) entity.getProperty("username"));
        model.setUuid((java.lang.String) entity.getProperty("uuid"));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.roadwatch.server.model.User m = (com.roadwatch.server.model.User) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("androidVersion", m.getAndroidVersion());
        entity.setProperty("appVersion", m.getAppVersion());
        entity.setProperty("createdAt", m.getCreatedAt());
        entity.setProperty("email", m.getEmail());
        entity.setProperty("encryptedPassword", m.getEncryptedPassword());
        entity.setProperty("licensePlate", m.getLicensePlate());
        entity.setProperty("ownGcmIds", m.getOwnGcmIds());
        entity.setProperty("purchasedTrackedLicensePlatesSize", m.getPurchasedTrackedLicensePlatesSize());
        entity.setProperty("sharingUserKeys", m.getSharingUserKeys());
        entity.setProperty("trackedLicensePlates", m.getTrackedLicensePlates());
        entity.setProperty("trackedNames", m.getTrackedNames());
        entity.setProperty("trackingGcmIds", m.getTrackingGcmIds());
        entity.setProperty("type", enumToString(m.getType()));
        entity.setProperty("username", m.getUsername());
        entity.setProperty("uuid", m.getUuid());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.roadwatch.server.model.User m = (com.roadwatch.server.model.User) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.roadwatch.server.model.User m = (com.roadwatch.server.model.User) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.roadwatch.server.model.User) is not defined.");
    }

    @Override
    protected void assignKeyToModelRefIfNecessary(com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
    }

    @Override
    protected void incrementVersion(Object model) {
    }

    @Override
    protected void prePut(Object model) {
        com.roadwatch.server.model.User m = (com.roadwatch.server.model.User) model;
        m.setCreatedAt(slim3_createdAtAttributeListener.prePut(m.getCreatedAt()));
    }

    @Override
    protected void postGet(Object model) {
    }

    @Override
    public String getSchemaVersionName() {
        return "slim3.schemaVersion";
    }

    @Override
    public String getClassHierarchyListName() {
        return "slim3.classHierarchyList";
    }

    @Override
    protected boolean isCipherProperty(String propertyName) {
        return false;
    }

    @Override
    protected void modelToJson(org.slim3.datastore.json.JsonWriter writer, java.lang.Object model, int maxDepth, int currentDepth) {
        com.roadwatch.server.model.User m = (com.roadwatch.server.model.User) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getAndroidVersion() != null){
            writer.setNextPropertyName("androidVersion");
            encoder0.encode(writer, m.getAndroidVersion());
        }
        if(m.getAppVersion() != null){
            writer.setNextPropertyName("appVersion");
            encoder0.encode(writer, m.getAppVersion());
        }
        if(m.getCreatedAt() != null){
            writer.setNextPropertyName("createdAt");
            encoder0.encode(writer, m.getCreatedAt());
        }
        if(m.getEmail() != null){
            writer.setNextPropertyName("email");
            encoder0.encode(writer, m.getEmail());
        }
        if(m.getEncryptedPassword() != null){
            writer.setNextPropertyName("encryptedPassword");
            encoder0.encode(writer, m.getEncryptedPassword());
        }
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        if(m.getLicensePlate() != null){
            writer.setNextPropertyName("licensePlate");
            encoder0.encode(writer, m.getLicensePlate());
        }
        if(m.getOwnGcmIds() != null){
            writer.setNextPropertyName("ownGcmIds");
            writer.beginArray();
            for(java.lang.String v : m.getOwnGcmIds()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getPassword() != null){
            writer.setNextPropertyName("password");
            encoder0.encode(writer, m.getPassword());
        }
        writer.setNextPropertyName("purchasedTrackedLicensePlatesSize");
        encoder0.encode(writer, m.getPurchasedTrackedLicensePlatesSize());
        if(m.getSharingUserKeys() != null){
            writer.setNextPropertyName("sharingUserKeys");
            writer.beginArray();
            for(com.google.appengine.api.datastore.Key v : m.getSharingUserKeys()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getTrackedLicensePlates() != null){
            writer.setNextPropertyName("trackedLicensePlates");
            writer.beginArray();
            for(java.lang.String v : m.getTrackedLicensePlates()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getTrackedNames() != null){
            writer.setNextPropertyName("trackedNames");
            writer.beginArray();
            for(java.lang.String v : m.getTrackedNames()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getTrackingGcmIds() != null){
            writer.setNextPropertyName("trackingGcmIds");
            writer.beginArray();
            for(java.lang.String v : m.getTrackingGcmIds()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getType() != null){
            writer.setNextPropertyName("type");
            encoder0.encode(writer, m.getType());
        }
        if(m.getUsername() != null){
            writer.setNextPropertyName("username");
            encoder0.encode(writer, m.getUsername());
        }
        if(m.getUuid() != null){
            writer.setNextPropertyName("uuid");
            encoder0.encode(writer, m.getUuid());
        }
        writer.endObject();
    }

    @Override
    protected com.roadwatch.server.model.User jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.roadwatch.server.model.User m = new com.roadwatch.server.model.User();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("androidVersion");
        m.setAndroidVersion(decoder0.decode(reader, m.getAndroidVersion()));
        reader = rootReader.newObjectReader("appVersion");
        m.setAppVersion(decoder0.decode(reader, m.getAppVersion()));
        reader = rootReader.newObjectReader("createdAt");
        m.setCreatedAt(decoder0.decode(reader, m.getCreatedAt()));
        reader = rootReader.newObjectReader("email");
        m.setEmail(decoder0.decode(reader, m.getEmail()));
        reader = rootReader.newObjectReader("encryptedPassword");
        m.setEncryptedPassword(decoder0.decode(reader, m.getEncryptedPassword()));
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("licensePlate");
        m.setLicensePlate(decoder0.decode(reader, m.getLicensePlate()));
        reader = rootReader.newObjectReader("ownGcmIds");
        {
            java.util.ArrayList<java.lang.String> elements = new java.util.ArrayList<java.lang.String>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("ownGcmIds");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    java.lang.String v = decoder0.decode(reader, (java.lang.String)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setOwnGcmIds(elements);
            }
        }
        reader = rootReader.newObjectReader("password");
        m.setPassword(decoder0.decode(reader, m.getPassword()));
        reader = rootReader.newObjectReader("purchasedTrackedLicensePlatesSize");
        m.setPurchasedTrackedLicensePlatesSize(decoder0.decode(reader, m.getPurchasedTrackedLicensePlatesSize()));
        reader = rootReader.newObjectReader("sharingUserKeys");
        {
            java.util.ArrayList<com.google.appengine.api.datastore.Key> elements = new java.util.ArrayList<com.google.appengine.api.datastore.Key>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("sharingUserKeys");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    com.google.appengine.api.datastore.Key v = decoder0.decode(reader, (com.google.appengine.api.datastore.Key)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setSharingUserKeys(elements);
            }
        }
        reader = rootReader.newObjectReader("trackedLicensePlates");
        {
            java.util.ArrayList<java.lang.String> elements = new java.util.ArrayList<java.lang.String>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("trackedLicensePlates");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    java.lang.String v = decoder0.decode(reader, (java.lang.String)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setTrackedLicensePlates(elements);
            }
        }
        reader = rootReader.newObjectReader("trackedNames");
        {
            java.util.ArrayList<java.lang.String> elements = new java.util.ArrayList<java.lang.String>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("trackedNames");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    java.lang.String v = decoder0.decode(reader, (java.lang.String)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setTrackedNames(elements);
            }
        }
        reader = rootReader.newObjectReader("trackingGcmIds");
        {
            java.util.ArrayList<java.lang.String> elements = new java.util.ArrayList<java.lang.String>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("trackingGcmIds");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    java.lang.String v = decoder0.decode(reader, (java.lang.String)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setTrackingGcmIds(elements);
            }
        }
        reader = rootReader.newObjectReader("type");
        m.setType(decoder0.decode(reader, m.getType(), com.roadwatch.server.model.User.UserType.class));
        reader = rootReader.newObjectReader("username");
        m.setUsername(decoder0.decode(reader, m.getUsername()));
        reader = rootReader.newObjectReader("uuid");
        m.setUuid(decoder0.decode(reader, m.getUuid()));
        return m;
    }
}