package cn.ingenic.glasssync.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CompatibilityTools {
	
	public static class CompatibilityException extends Exception {

		private static final long serialVersionUID = -6324132535599338117L;

		public CompatibilityException(String detailMessage) {
			super(detailMessage);
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T, E> E invokeWithReflect(Class<T> c, T t, String methodName,
			Class[] argsType, Object[] argsValue) throws CompatibilityException {
			try {
				Method m = c.getMethod(methodName, argsType);
				return (E) m.invoke(t, argsValue);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
			
			throw new CompatibilityException("not success, see stack trace");
	}
}
