package firok.spring.jfb.ioo.rdo;

import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public record CacheFolder(File folderBase, File folderSlice, File fileMerge, File folderTranscode) implements Closeable
{
	public File sliceOf(int sliceIndex)
	{
		return new File(folderSlice, "slice_" + sliceIndex + ".bin");
	}
	@Override
	public void close() throws IOException
	{
		FileUtils.forceDelete(folderBase);
	}
}
