package edu.mit.media.openpds.client;

public class RegistryConfig {
	private String mClientKey;
	private String mClientSecret;
	private String mScopes;
	private String mBasicAuth;
	private String mRegistryUrl;

	public RegistryConfig(String url, String clientKey, String clientSecret, String scopes, String basicAuth) {
		assert(url != null && clientKey != null && clientSecret != null && scopes != null && basicAuth != null);
		mRegistryUrl = url;;
		mClientKey = clientKey;
		mClientSecret = clientSecret;
		mScopes = scopes;
		mBasicAuth = basicAuth;
	}
	
	public String getRegistryUrl() {
		return mRegistryUrl;
	}

	public String getClientKey() {
		return mClientKey;
	}

	public String getClientSecret() {
		return mClientSecret;
	}

	public String getScopes() {
		return mScopes;
	}

	public String getBasicAuth() {
		return mBasicAuth;
	}
}
