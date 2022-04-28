package firok.spring.jfb.ioo.rdo;

import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public record CacheFolder(File folderBase, File folderSlice, File fileMerge, File folderTranscode) implements Closeable
{
	public File sliceOf(int sliceIndex)
	{
		return new File(folderSlice, "slice_" + sliceIndex + ".bin");
	}

	public File[] listFileSlice()
	{
		return Objects.requireNonNullElse(folderSlice.listFiles(), new File[0]);
	}
	public File[] listFileTranscode()
	{
		return Objects.requireNonNullElse(folderTranscode.listFiles(), new File[0]);
	}

	@Override
	public void close() throws IOException
	{
		FileUtils.forceDelete(folderBase);
	}
}
