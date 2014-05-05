package edu.mit.media.openpds.client.funf;

import java.io.File;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.openpds.client.PersonalDataStore;

public class HttpsArchive implements RemoteFileArchive {

	private FunfPDS mPds;

	public HttpsArchive(FunfPDS pds) {
		mPds = pds;
	}

	public String getId() {
		return (mPds == null)? "" : mPds.getFunfUploadUrl();
	}

	public boolean add(File file) {
		return (mPds == null)? false : mPds.uploadFunfData(file);
	}
}
