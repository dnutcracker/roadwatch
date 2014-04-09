package com.roadwatch.server.model.gson;

/**
 * Create model objects using the Gson library.
 * Used to decouple the model from a specific protocol or library.
 * @author Nati
 *
 */
public class GSONModelFactory
{
	//	public static final Report createReportObject(Reader reader)
	//	{
	//		// Parse the reader into a JSON object. 
	//		Gson gson = new Gson();
	//		JsonObject jsonObject = (JsonObject) new JsonParser().parse(reader);
	//
	//		// Construct the object field by field
	//		Report report = new Report();
	//		report.setLicensePlate(gson.fromJson(jsonObject.get("licensePlate"), String.class));
	//		report.setLatitude(gson.fromJson(jsonObject.get("latitude"), double.class).doubleValue());
	//		report.setLongitude(gson.fromJson(jsonObject.get("longitude"), double.class).doubleValue());
	//		report.setReportCode(gson.fromJson(jsonObject.get("reportCode"), int.class).intValue());
	//		report.setReportTime(gson.fromJson(jsonObject.get("reportTime"), Long.class).longValue());
	//		List<String> reportedBy = new ArrayList<String>(1);
	//		reportedBy.add(gson.fromJson(jsonObject.get("reportedBy"), String.class));
	//		report.setReportedBy(reportedBy);
	//
	//		return report;
	//	}

	//	public static final User createUserObject(Reader reader)
	//	{
	//		// Parse the reader into a JSON object. 
	//		Gson gson = new Gson();
	//		JsonObject jsonObject = (JsonObject) new JsonParser().parse(reader);
	//		User user = new User();
	//
	//		user.setUuid(gson.fromJson(jsonObject.get("uuid"), String.class));
	//		user.setAppVersion(gson.fromJson(jsonObject.get("appVersion"), String.class));
	//		user.setAndroidVersion(gson.fromJson(jsonObject.get("androidVersion"), String.class));
	//		user.setType(gson.fromJson(jsonObject.get("type"), String.class));
	//		user.setLicensePlate(gson.fromJson(jsonObject.get("licensePlate"), String.class));
	//		user.setUsername(gson.fromJson(jsonObject.get("username"), String.class));
	//		user.setEmail(gson.fromJson(jsonObject.get("email"), String.class));
	//		user.setPassword(gson.fromJson(jsonObject.get("password"), String.class));
	//		String[] gcmRegIDsArray = gson.fromJson(jsonObject.get("gcmRegistrationIDs"), String[].class);
	//		user.setGcmRegistrationIDs(Arrays.asList(gcmRegIDsArray));
	//
	//		return user;
	//	}
}
