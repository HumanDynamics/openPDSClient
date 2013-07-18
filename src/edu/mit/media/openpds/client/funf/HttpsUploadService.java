package edu.mit.media.openpds.client.funf;

import edu.mit.media.funf.storage.HttpUploadService;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.openpds.client.PersonalDataStore;

public class HttpsUploadService extends HttpUploadService {

	@Override
	protected RemoteFileArchive getRemoteArchive(String name) {
		FunfPDS pds = null;
		try {
			pds = new FunfPDS(this);
		} catch (Exception e) {
		}
		
		return new HttpsArchive(pds);
	}

}
