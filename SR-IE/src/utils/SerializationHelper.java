package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class SerializationHelper {

	public static void serialize(String fileName, Object obj){

		try{
			//use buffering


			OutputStream file = new FileOutputStream( fileName );
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutput output = new ObjectOutputStream( buffer );

			try{
				output.writeObject(obj);
			}
			finally{
				output.close();
			}
		}  
		catch(IOException ex){
			ex.printStackTrace();
		}

	}

	public static Object deserialize(String fileName){

		Object obj = null;

		try{
			//use buffering
			InputStream file = new FileInputStream( fileName );
			InputStream buffer = new BufferedInputStream( file );
			ObjectInput input = new ObjectInputStream ( buffer );

			try{

				obj = input.readObject();


			}
			finally{
				input.close();
			}


		}
		catch(ClassNotFoundException ex){
			ex.printStackTrace();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}		

		return obj;

	}

}
