package edu.mit.media.openpds.client;

public abstract class RegistryResponse {
	protected static final String LOG_TAG = "RegistryResponse";	
	private String mError;
	private String mErrorDescription;
	private String mUuid;
	private String mPdsLocation;
	
	public boolean success() {
		return mError == null;
	}	
	
	public String getError() {
		return mError;
	}

	protected void setErrorInfo(String error, String errorDescription) {
		mError = error;
		mErrorDescription = errorDescription;
	}

	public String getErrorDescription() {
		return mErrorDescription;
	}

	public String getUUID() {
		return mUuid;
	}

	protected void setUUID(String uuid) {
		this.mUuid = uuid;
	}

	protected void setPDSLocation(String pdsLocation) {
		this.mPdsLocation = pdsLocation;
	}
	
	public String getPDSLocation(){
		return mPdsLocation;
	}
}
