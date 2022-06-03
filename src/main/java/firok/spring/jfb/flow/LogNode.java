package firok.spring.jfb.flow;

import lombok.Data;

import java.util.logging.Level;

@Data
class LogNode
{
	Level level;
	String message;
	long time;

	public String toString()
	{
		return time + "|" + level + ": " + message;
	}
}
