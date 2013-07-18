package edu.mit.media.openpds.client;

public class RegistryResponse {
	protected static final String LOG_TAG = "RegistryResponse";	
	private String mError;
	private String mErrorDescription;
	
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

}
