package edu.mit.media.openpds.client.funf;

import edu.mit.media.funf.storage.HttpUploadService;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.openpds.client.PersonalDataStore;

public class HttpsUploadService extends HttpUploadService {

	@Override
	protected RemoteFileArchive getRemoteArchive(String name) {
		PersonalDataStore pds = null;
		try {
			pds = new PersonalDataStore(this);
		} catch (Exception e) {
		}
		
		return new HttpsArchive(pds);
	}

}
