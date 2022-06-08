package firok.spring.jfb.config;

import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;

/**
 * 七牛云桶配置信息
 */
public record QiniuBucketInfo(String nameBucket, String domain, Region region,
                              boolean useHttps, int deadline, UploadManager uploadManager)
{ }
