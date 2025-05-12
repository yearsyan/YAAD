// IExtractorService.aidl
package io.github.yearsyan.yaad;

import io.github.yearsyan.yaad.model.MediaItem;
import io.github.yearsyan.yaad.model.MediaResult;

interface IExtractorService {
    MediaResult extractDownloadMedia(String url, in Map options);
}