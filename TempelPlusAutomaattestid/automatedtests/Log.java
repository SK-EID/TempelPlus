package ee.sk.automatedtests;

/**  
* Log.java - a simple class for logging actions & throwing exceptions  
* @author  Erik Kaju
* @version 1.0
*/ 

public class Log {

	/**
	 * Throws exception with 
	 * @param message
	 * @throws Exception
	 */
	private static void throwError(String message) throws Exception{
		write(message, "ERROR");
		throw new Exception(message);
	}

	
	/**
	 * Writes in console [Info] Message
	 * 
	 * @param message
	 */
	public static void write(String message){
		write(message, "Info");
	}
	
	
	/**
	 * If isError is set to "true":
	 * Writes in console [textInBrackets] Message
	 * Throws exception: Message
	 * 
	 * @param message
	 * @param isError
	 * @throws Exception
	 */
	public static void write(String message, boolean isError) throws Exception{
		if (isError){
			throwError(message);
		}else{
			write(message);
		}
	}
	
    /**
     * Writes in console [textInBrackets] Message
     * @param message
     * @param textInBrackets
     */
	public static void write(String message, String textInBrackets) {
		System.out.println("[" + textInBrackets + "] " + message);
	}
	
	public static void nextLine(){
		System.out.println();
	}

}
