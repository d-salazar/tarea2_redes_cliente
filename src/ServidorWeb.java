import java.awt.Desktop;
import java.net.*;

public class ServidorWeb {
	public static boolean servidor_http_status = false;
	public static ServerSocket servidor_http;
	
	public static void main(String argv[]) throws Exception {
		servidor_http = new ServerSocket(0);
		servidor_http.setReuseAddress(true);
		servidor_http_status = true;
		
		if( Desktop.isDesktopSupported() ){
			Desktop.getDesktop().browse(new URI("http://localhost:"+servidor_http.getLocalPort()));
		}
		
		while ( servidor_http_status ) {
			PeticionHTTP request = new PeticionHTTP(servidor_http.accept());
			request.start(); 
		}
		
		if( !servidor_http.isClosed() ){
			System.out.println("Cerrando servidor HTTP"+servidor_http.getLocalSocketAddress());
			servidor_http.close();
		}
	}
}