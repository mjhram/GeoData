package com.mjhram.geodata.helper;

public interface Constants {

    /**
     * Base URL of the Demo Server (such as http://my_host:8080/gcm-demo)
     */
	String ver = "2016Sep26"; //versioning used to force update

	String SERVER_URL = "http://geo.ajerlitaxi.com";
	/**
     * Google API project id registered to use GCM.
     */
	String SENDER_ID = "368880841097";

	String SENDER_EMAIL 		= "senderEmail";
	String RECEIVER_EMAIL 		= "receiverEmail";
	String REG_ID 				= "regId";
	String MESSAGE 				= "message";
    String UPDATE_REG_ID        = "updateRegId";

    String KEY_IS_LOGGEDIN = "isLoggedIn";

	String KEY_CARBRAND = "carbrand";
	String KEY_CARMODEL = "carmodel";
	String KEY_CARMAKE = "carmake";
	String KEY_CARCOLOR = "carcolor";
	String KEY_CARPLATENO = "carplateno";
	String KEY_CAROTHER = "carother";

	String KEY_PHOTO = "userPhoto";
    String KEY_NAME = "name";
    String KEY_PHONE = "userPhone";
	String KEY_PHOTO_ID = "userPhotoId";
    String KEY_EMAIL = "key_email";
    String KEY_UID = "key_uid";
	String KEY_REGID = "key_RegId";
	String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
	String UPDATE_REQ = "updateRequest";
	String KEY_IS_ShowHelpOverlay = "isShowHelpOverlay";
	String PROFILE_PHONE = "profilePhone";
	String KEY_isPhoneVerified = "isPhoneVerified";

	String KEY_TRIPID = "tripId";

	String URL_ads = SERVER_URL+"/images/";

	String URL_CALC = SERVER_URL+"/calc.php";
	String URL_LOGIN = SERVER_URL+"/login.php";
	//public static String URL_REQUESTS = SERVER_URL+"/getRequests.php";
	String URL_updateUserInfo = SERVER_URL+"/updateUserInfo.php";
	String URL_uploadImage = SERVER_URL+"/uploadImage.php";
	String URL_UpdateTReq = SERVER_URL+"/updateTRequest.php";
	String URL_UpdateRegId = SERVER_URL+"/updateRegId.php";
	String URL_downloadUserPhoto = SERVER_URL+"/downloadImage.php?id=";
	String RequestsIdx = "idx";
	String RequestsTime = "time";
	String RequestsPassangerId = "passangerId";
	String RequestsPassangerName = "passangerName";
	String RequestsPassengerEmail = "passengerEmail";
	String RequestsPassengerPhone = "passengerPhone";
	String RequestsPassengerPhotoId = "passengerPhotoId";
	String RequestsFromLat = "fromLat";
	String RequestsFromLong = "fromLong";
	String RequestsToLat = "toLat";
	String RequestsToLong = "toLong";
	String RequestsFromDesc = "fromDesc";
	String RequestsToDesc = "toDesc";
	String RequestsDriverId = "driverId";
	String RequestsStatus = "status";
	String RequestsSuggestedFee = "suggestedFee";
	String RequestsNoOfPassangers = "noOfPassangers";
	String RequestsAdditionalNotes = "additionalNotes";

	String ProfileName = "name";
	String ProfileEmail = "email";
	String ProfilePhone = "phone";
	String ProfileImageId = "image_id";
	String ProfileLicenseState = "licenseState";
	//Car info
	String ProfileCarBrand = "brand";
	String ProfileCarModel = "model";
	String ProfileCarMake = "make";
	String ProfileCarColor = "color";
	String ProfileCarPlateType = "plate_type";
	String ProfileCarPlateNo = "plateno";
	String ProfileCarOther = "other";

	String TRequest_Assigned = "assigned";

	//public static final String ACTION_REGISTER = "com.mjhram.geodata.REGISTER";
	String EXTRA_STATUS = "status";
	int STATUS_SUCCESS = 1;
	int STATUS_FAILED = 0;
}
