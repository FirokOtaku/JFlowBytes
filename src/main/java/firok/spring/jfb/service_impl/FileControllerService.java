package firok.spring.jfb.service_impl;

import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.jfb.bean.FileInfoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiredFieldsWarningInspection"})
@Service
public class FileControllerService
{
	@Autowired
	IService<FileInfoBean> serviceFileInfo;


}
