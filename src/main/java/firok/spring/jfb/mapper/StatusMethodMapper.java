package firok.spring.jfb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatusMethodMapper
{
	@Select("${sql}")
	List<Map<String,Object>> executeRawSQL(@Param("sql") String sql);
}
