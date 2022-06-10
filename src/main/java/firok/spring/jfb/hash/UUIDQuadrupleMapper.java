package firok.spring.jfb.hash;

/**
 * 将 UUID 字符串提取 4 部分作为哈希值
 *
 * 例:
 * 8d6b5f5a-dc3b-4513-b504-e4de2d594751
 * 提取后:
 * 8d/6b/5f/5a/8d6b5f5a-dc3b-4513-b504-e4de2d594751
 */
public class UUIDQuadrupleMapper implements IHashMapper<UUIDQuadrupleHash>
{
	@Override
	public String getMapperName()
	{
		return "uuid-quadruple";
	}

	@Override
	public UUIDQuadrupleHash mapHash(String str)
	{
		if(str.length() < 8)
			throw new IllegalArgumentException("参数错误");

		var chars = str.toCharArray();
		var p1 = new String(chars, 0, 2);
		var p2 = new String(chars, 2, 2);
		var p3 = new String(chars, 4, 2);
		var p4 = new String(chars, 6, 2);
		return new UUIDQuadrupleHash(this, p1, p2, p3, p4, str);
	}

	@Override
	public String mapOrigin(UUIDQuadrupleHash hash)
	{
		return hash.full();
	}
}
