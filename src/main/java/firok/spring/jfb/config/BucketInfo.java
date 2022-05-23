package firok.spring.jfb.config;

import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;

public record BucketInfo(String nameBucket, String domain, Region region,
                         boolean useHttps, int deadline, UploadManager uploadManager)
{ }
