package firok.spring.jfb.hash;

public class NoHashMapper implements IHashMapper<NoHash>
{
	@Override
	public String getMapperName()
	{
		return "no-hash";
	}

	@Override
	public NoHash mapHash(String str)
	{
		return new NoHash(this, str);
	}

	@Override
	public String mapOrigin(NoHash hash)
	{
		return hash.full();
	}
}
