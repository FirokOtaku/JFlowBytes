package firok.spring.jfb.hash;

public record UUIDQuadrupleHash(
		UUIDQuadrupleMapper mapper,
		String p1,
		String p2,
		String p3,
		String p4,
		String full
) implements IMappedHash {

	@Override
	public IHashMapper<? extends IMappedHash> getMapper()
	{
		return mapper;
	}

	@Override
	public String getHashString()
	{
		return p1 + '/' +
				p2 + '/' +
				p3 + '/' +
				p4 + '/' +
				full;
	}
}
