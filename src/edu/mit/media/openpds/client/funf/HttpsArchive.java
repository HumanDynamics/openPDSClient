package edu.mit.media.openpds.client.funf;

import java.io.File;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.openpds.client.PersonalDataStore;

public class HttpsArchive implements RemoteFileArchive {

	private PersonalDataStore mPds;

	public HttpsArchive(PersonalDataStore pds) {
		mPds = pds;
	}

	public String getId() {
		return (mPds == null)? "" : mPds.buildAbsoluteUrl("");
	}

	public boolean add(File file) {
		return (mPds == null)? false : mPds.uploadFunfData(file);
	}
}
