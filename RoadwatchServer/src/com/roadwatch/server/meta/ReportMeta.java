package com.roadwatch.server.meta;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2014-04-09 15:08:32")
/** */
public final class ReportMeta extends org.slim3.datastore.ModelMeta<com.roadwatch.server.model.Report> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Double> latitude = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Double>(this, "latitude", "latitude", double.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.Report> licensePlate = new org.slim3.datastore.StringAttributeMeta<com.roadwatch.server.model.Report>(this, "licensePlate", "licensePlate");

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Double> longitude = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Double>(this, "longitude", "longitude", double.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Integer> reportCode = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Integer>(this, "reportCode", "reportCode", int.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Long> reportTime = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.lang.Long>(this, "reportTime", "reportTime", long.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.util.Date> reportTimeForDisplay = new org.slim3.datastore.CoreAttributeMeta<com.roadwatch.server.model.Report, java.util.Date>(this, "reportTimeForDisplay", "reportTimeForDisplay", java.util.Date.class);

    /** */
    public final org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.Report, java.util.List<java.lang.String>> reportedBy = new org.slim3.datastore.StringCollectionAttributeMeta<com.roadwatch.server.model.Report, java.util.List<java.lang.String>>(this, "reportedBy", "reportedBy", java.util.List.class);

    private static final ReportMeta slim3_singleton = new ReportMeta();

    /**
     * @return the singleton
     */
    public static ReportMeta get() {
       return slim3_singleton;
    }

    /** */
    public ReportMeta() {
        super("Report", com.roadwatch.server.model.Report.class);
    }

    @Override
    public com.roadwatch.server.model.Report entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.roadwatch.server.model.Report model = new com.roadwatch.server.model.Report();
        model.setKey(entity.getKey());
        model.setLatitude(doubleToPrimitiveDouble((java.lang.Double) entity.getProperty("latitude")));
        model.setLicensePlate((java.lang.String) entity.getProperty("licensePlate"));
        model.setLongitude(doubleToPrimitiveDouble((java.lang.Double) entity.getProperty("longitude")));
        model.setReportCode(longToPrimitiveInt((java.lang.Long) entity.getProperty("reportCode")));
        model.setReportTime(longToPrimitiveLong((java.lang.Long) entity.getProperty("reportTime")));
        model.setReportTimeForDisplay((java.util.Date) entity.getProperty("reportTimeForDisplay"));
        model.setReportedBy(toList(java.lang.String.class, entity.getProperty("reportedBy")));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.roadwatch.server.model.Report m = (com.roadwatch.server.model.Report) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("latitude", m.getLatitude());
        entity.setProperty("licensePlate", m.getLicensePlate());
        entity.setProperty("longitude", m.getLongitude());
        entity.setProperty("reportCode", m.getReportCode());
        entity.setProperty("reportTime", m.getReportTime());
        entity.setProperty("reportTimeForDisplay", m.getReportTimeForDisplay());
        entity.setProperty("reportedBy", m.getReportedBy());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.roadwatch.server.model.Report m = (com.roadwatch.server.model.Report) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.roadwatch.server.model.Report m = (com.roadwatch.server.model.Report) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.roadwatch.server.model.Report) is not defined.");
    }

    @Override
    protected void assignKeyToModelRefIfNecessary(com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
    }

    @Override
    protected void incrementVersion(Object model) {
    }

    @Override
    protected void prePut(Object model) {
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
        com.roadwatch.server.model.Report m = (com.roadwatch.server.model.Report) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        writer.setNextPropertyName("latitude");
        encoder0.encode(writer, m.getLatitude());
        if(m.getLicensePlate() != null){
            writer.setNextPropertyName("licensePlate");
            encoder0.encode(writer, m.getLicensePlate());
        }
        writer.setNextPropertyName("longitude");
        encoder0.encode(writer, m.getLongitude());
        writer.setNextPropertyName("reportCode");
        encoder0.encode(writer, m.getReportCode());
        writer.setNextPropertyName("reportTime");
        encoder0.encode(writer, m.getReportTime());
        if(m.getReportTimeForDisplay() != null){
            writer.setNextPropertyName("reportTimeForDisplay");
            encoder0.encode(writer, m.getReportTimeForDisplay());
        }
        if(m.getReportedBy() != null){
            writer.setNextPropertyName("reportedBy");
            writer.beginArray();
            for(java.lang.String v : m.getReportedBy()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    @Override
    protected com.roadwatch.server.model.Report jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.roadwatch.server.model.Report m = new com.roadwatch.server.model.Report();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("latitude");
        m.setLatitude(decoder0.decode(reader, m.getLatitude()));
        reader = rootReader.newObjectReader("licensePlate");
        m.setLicensePlate(decoder0.decode(reader, m.getLicensePlate()));
        reader = rootReader.newObjectReader("longitude");
        m.setLongitude(decoder0.decode(reader, m.getLongitude()));
        reader = rootReader.newObjectReader("reportCode");
        m.setReportCode(decoder0.decode(reader, m.getReportCode()));
        reader = rootReader.newObjectReader("reportTime");
        m.setReportTime(decoder0.decode(reader, m.getReportTime()));
        reader = rootReader.newObjectReader("reportTimeForDisplay");
        m.setReportTimeForDisplay(decoder0.decode(reader, m.getReportTimeForDisplay()));
        reader = rootReader.newObjectReader("reportedBy");
        {
            java.util.ArrayList<java.lang.String> elements = new java.util.ArrayList<java.lang.String>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("reportedBy");
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
                m.setReportedBy(elements);
            }
        }
        return m;
    }
}