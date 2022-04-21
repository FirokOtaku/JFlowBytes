package firok.spring.jfb.bean;

import lombok.Data;

@Data
@SuppressWarnings("unused")
public class Ret<TypeData>
{
	TypeData data;

	boolean success;

	String msg;

	public static <TypeData> Ret<TypeData> success(TypeData data)
	{
		Ret<TypeData> ret = new Ret<>();
		ret.data = data;
		ret.success = true;
		return ret;
	}
	public static <TypeData> Ret<TypeData> success()
	{
		return success(null);
	}
	public static <TypeData> Ret<TypeData> fail(String msg)
	{
		Ret<TypeData> ret = new Ret<>();
		ret.success = false;
		ret.msg = msg;
		return ret;
	}
	public static <TypeData> Ret<TypeData> fail()
	{
		return fail(null);
	}
}
