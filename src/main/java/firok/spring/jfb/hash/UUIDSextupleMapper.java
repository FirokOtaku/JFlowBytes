package firok.spring.jfb.hash;

/**
 * 将 UUID 字符串提取 6 部分作为哈希值
 *
 * 例:
 * 8d6b5f5a-dc3b-4513-b504-e4de2d594751
 * 提取后:
 * 8d/6b/5f/5a/dc/3b/8d6b5f5a-dc3b-4513-b504-e4de2d594751
 */
public class UUIDSextupleMapper implements IHashMapper<UUIDSextupleHash>
{
	@Override
	public String getMapperName()
	{
		return "uuid-sextuple";
	}

	@Override
	public UUIDSextupleHash mapHash(String str)
	{
		if(str.length() < 13)
			throw new IllegalArgumentException("参数错误");

		var chars = str.toCharArray();
		var p1 = new String(chars, 0, 2);
		var p2 = new String(chars, 2, 2);
		var p3 = new String(chars, 4, 2);
		var p4 = new String(chars, 6, 2);
		var p5 = new String(chars, 9, 2);
		var p6 = new String(chars, 11, 2);
		return new UUIDSextupleHash(this, p1, p2, p3, p4, p5, p6, str);
	}

	@Override
	public String mapOrigin(UUIDSextupleHash hash)
	{
		return hash.full();
	}

}
