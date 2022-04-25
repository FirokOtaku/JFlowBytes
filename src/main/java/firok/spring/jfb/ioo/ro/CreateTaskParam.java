package firok.spring.jfb.ioo.ro;

import firok.spring.jfb.constant.FileTaskTypeEnum;

public record CreateTaskParam(String fileName, long fileSize, long sliceSize, FileTaskTypeEnum type)
{
}
