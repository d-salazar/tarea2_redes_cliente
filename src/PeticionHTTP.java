import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

/* Clase ThreadSocket de la tarea-1, con su propio archivo para mejor comprension. */
class PeticionHTTP extends Thread { 

	final private static String archivo_contactos = "usuarios.txt";
	private final static int servidor_tcp_puerto = 6000;
	private Socket socket;

	public PeticionHTTP(Socket insocket){
		this.socket = insocket;
	}
	
	@Override
	public void run() {
		
		try{
			BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter salida = new PrintWriter(socket.getOutputStream());
			
			//error socket closed??
			String request = entrada.readLine();
			System.out.println("["+this.getName()+"][HTTP-HEADER]: "+request); 
	
			if( request != null ){
				String[] req = request.split("\\s+");
				String metodo = req[0];
				String archivo_requerido = req[1];
				String instruccion = "";
				int post_data_index = -1;
				while( (instruccion = entrada.readLine()) != null && (instruccion.length() != 0)){
					System.out.println("["+this.getName()+"][HTTP-HEADER]: "+instruccion);
					
					if( instruccion.indexOf("Content-Length:") > -1){
						post_data_index = new Integer(instruccion.substring(instruccion.indexOf("Content-Length:") + 16,instruccion.length())).intValue();
					}
				}

				if( metodo.equals("POST")){
					/* revisar elementos enviados a traves del metodo POST, y guardar variables en post_data */
					char[] charArray = new char[post_data_index];
					entrada.read(charArray,0,post_data_index);
					String post_data = new String(charArray);

					/* cerrar_conexion */
					if( post_data.equals("cerrar=true") ){ 
						ServidorWeb.servidor_http_status = false;
						entrada.close();
						salida.close();
						this.interrupt();
						return;
					}
					/* agregar_contacto */
					if( archivo_requerido.equals("/lista_contacto.html")){
						try {
							agregar_contacto(post_data);
						} catch (FileNotFoundException e) {
							System.err.println( e.getMessage() );
							e.printStackTrace();
						}
					}
					/* mensaje_nuevo */
					if( archivo_requerido.equals("/mensajes.html") ){
						enviar_mensaje(post_data);
					}
				}
				else if(metodo.equals("GET")){		
					if( archivo_requerido.equals("/") || archivo_requerido.equals("") ){
						archivo_requerido = "/index.html";
					}
				}
				retorna_direccion(archivo_requerido,salida);
				salida.close();
				entrada.close();
				return;
			}
		}catch ( Exception e){
			System.out.println(e.getCause());
			System.err.println( "error lectura:"+e.getMessage() );
			e.printStackTrace();
		}
		return;
	}		

	private static void retorna_direccion(String direccion_archivo, PrintWriter salida){
		if( direccion_archivo.startsWith("/") ){
			direccion_archivo = direccion_archivo.substring(1);
		}
		try{
			if( Files.exists(Paths.get(direccion_archivo)) ){
				System.out.println("- REQUERIDO: "+direccion_archivo);
				/* HEADERS */
				salida.println("HTTP/1.0 200 OK");
				salida.println("DATE: "+(new Date().toString()));
				salida.println("SERVER: localhost");
				salida.println("HOST: localhost:"+ServidorWeb.servidor_http.getLocalPort());
				salida.println("Content-Type: "+Files.probeContentType(Paths.get(direccion_archivo)));
				salida.println("");
				/* ARCHIVO */
				BufferedReader fl = new BufferedReader(new FileReader(direccion_archivo));
				String linea = fl.readLine();
				int contador_linea = 0;
				while (linea != null ){
					/* filtrar para mensajes.html y listar_contactos.html */
					if( direccion_archivo.toLowerCase().contains("lista_contacto.html".toLowerCase()) && contador_linea==41){
						listar_contactos(salida);
					}else if( direccion_archivo.toLowerCase().contains("mensajes.html".toLowerCase()) && contador_linea==40){
						listar_mensajes(salida);
					}
					salida.println(linea);
					linea = fl.readLine();
					contador_linea++;
				}
				fl.close();
				System.out.println("- ENVIADO: "+direccion_archivo);
			}else{
				salida.println("HTTP/1.0 404 NOT FOUND");
				salida.println("<h1>ERROR 404 - NOT FOUND</h1>");
				salida.println("<p>Oops! No se encuentra la direccion solicitada.</p>");
				salida.println("<p>No se ha podido presentar la direccion: "+direccion_archivo+"</p>");
			}
		}catch( Exception e){
			System.err.println( e.getMessage() );
			e.printStackTrace();
		}
		return;
	}
	
	private static void listar_contactos(PrintWriter salida) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(archivo_contactos));
		
		String linea = br.readLine();
		String[] data;
		while( linea!= null ){
			data = linea.split("\\|",3);
			
			salida.println("<tr>");
	        salida.println("	<td>"+data[0]+"</td>");
	        salida.println("	<td>"+data[1]+"</td>");
	        salida.println("	<td>"+data[2]+"</td>");
	        salida.println("	<td><button type=\"button\" class=\"btn btn-link\" data-toggle=\"modal\" data-target=\"#modal_nuevo_mensaje\">Envia un mensaje!</button></td>");
	        salida.println("</tr>");
	        
	        linea = br.readLine();
		}
		br.close();
	}
		
    private static void agregar_contacto(String data) throws FileNotFoundException{
    	try(PrintWriter archivo = new PrintWriter(new BufferedWriter(new FileWriter(archivo_contactos, true)))) {
    		String[] s = data.split("&",3);
    		archivo.println(s[0].split("=")[1].replaceAll("[\f\n\r\t\'\"\\\\]", " ")+"|"+s[1].split("=")[1]+"|"+s[2].split("=")[1]);
    		archivo.close();
    	}catch (IOException e) {
    	    System.err.println( e.getMessage() );
    	    e.printStackTrace();
    	}
    }
    
	private static void listar_mensajes(PrintWriter salida) throws IOException{	
    	Socket cliente = new Socket(InetAddress.getByName("localhost"),servidor_tcp_puerto);
    	DataOutputStream outCliente = new DataOutputStream(cliente.getOutputStream());
    	DataInputStream inCliente = new DataInputStream(cliente.getInputStream());
    	
    	outCliente.writeUTF("L");
    	while( inCliente.available() > 0 ){
    		String inputData = inCliente.readUTF();
	    	if( !inputData.equals("NOTHING") ){
				String mensaje[] = inputData.split("\\|");
				
				salida.println("<tr>");
			    salida.println("	<td>"+mensaje[0]+"</td>");
			    salida.println("	<td>"+mensaje[1]+"</td>");
			    salida.println("	<td>"+URLDecoder.decode(mensaje[2],"UTF-8")+"</td>");
			    salida.println("</tr>");
	    	}else{
	    		break;
	    	}
    	}
		inCliente.close();
    	outCliente.close();
    	cliente.close();
	}
	
    private static void enviar_mensaje(String data) throws UnknownHostException, IOException{
		data = data.replace("emisor=","").replace("&destinatario=", "|").replace("&msj=", "|");
		/* Enviar a servidor TCP. */
    	Socket cliente = new Socket(InetAddress.getByName("localhost"),servidor_tcp_puerto);
    	DataOutputStream outCliente = new DataOutputStream(cliente.getOutputStream());
    	
    	outCliente.writeUTF("G|"+data);
    	outCliente.flush();
    	outCliente.close();
    	cliente.close();
    }
}
