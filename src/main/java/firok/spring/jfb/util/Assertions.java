package firok.spring.jfb.util;

public class Assertions
{
	public static void assertStrLenRange(String str, int minLen, int maxLen, String msg)
	{
		var len = str != null ? str.length() : 0;
		if(str == null || len >= minLen && len <= maxLen) return;
		throw new IllegalArgumentException(msg);
	}
//	public static void assertStrLen(String str, int len)
//	{
//		assertStrLen(str, 0, len, "字符串长度不符合要求");
//	}

	public static void assertNumberRange(Number num, Number min, Number max, String msg)
	{
		if(num == null) return;
		var dv = num.doubleValue();
		if(min != null && dv < min.doubleValue()) throw new IllegalArgumentException(msg);
		if(max != null && dv > max.doubleValue()) throw new IllegalArgumentException(msg);
	}
}
