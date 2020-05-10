package Cliente;

import java.io.BufferedReader;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * 
 * @author Juan Camilo Villamarin
 *
 */
public class Cliente {

	private int id;

	private  int PUERTO=3001;

	private static final String ALGS="AES";

	private static final String ALGA="RSA";

	private static final String ALGD="HMACSHA1";

	private Key puServerKey;

	private String HOST="localhost";

	private Socket sc;

	private PrintWriter escritor;

	private BufferedReader lector;

	private SecretKeySpec KeySimetrica;

	public boolean conectar() throws InterruptedException {
		synchronized (this) {
			int cont=0;
			boolean estado=false;
			while (!estado) {
				try {
					//System.setProperty("socksProxyHost", "157.253.236.45");
					//System.setProperty("socksProxyPort", "80");
					sc=new Socket(HOST, PUERTO);
					escritor=new PrintWriter(sc.getOutputStream(),true);
					lector=new BufferedReader(new InputStreamReader(sc.getInputStream()));
					estado= true;
					return true;
				} catch (Exception e) {
					System.out.println("Error en conexion con el servidor");
					e.printStackTrace();
					System.out.println("Intentando conexion con el servidor nuevamente");
					wait(2000);
					estado= false;
					cont++;
					if(cont==10) {
						break;
					}
				}
			}
			return false;
		}	
	}

	public void desconectar() {
		try {
			escritor.close();
			lector.close();
			sc.close();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}

	public Cliente(String host,int puerto,int id) {
		HOST=host;
		PUERTO=puerto;
		this.id=id;
	}

	public void protocolo() throws Exception  {
		String respuestas="";
		String textoCifrado="";
		String textoClaro="";
		String bas64str="";
		byte[] base64bytes=null;
		
		
		//Saludo al servidor
		enviarMs("HOLA");
		respuestas=lector.readLine();
		System.out.println("Respuesta del servidor: "+respuestas);
		if(!comprobarRes(respuestas)) {
			throw new Exception("Error en el mensaje de saludo enviado al servidor");
		}

		//Envio de algoritmos 
		enviarMs("ALGORITMOS:"+ALGS+":"+ALGA+":"+ALGD);
		respuestas=lector.readLine();
		if(!comprobarRes(respuestas)) {
			throw new Exception("Error en el mensaje con los algoritmos enviado al servidor");
		}
		
		//Instanciacion par de llaves asimetricas
		KeyPairGenerator generator=KeyPairGenerator.getInstance(ALGA);
		generator.initialize(1024);
		KeyPair keyPair= generator.genKeyPair();

		//Generando certificado
		System.out.println("Generando Certificado");
		X509Certificate certificado= CertificateBulider.gc(keyPair);
		byte[] cerInByte=certificado.getEncoded();
		bas64str=baseBite64Code(cerInByte);
		
		//Envio certificado
		enviarMs(bas64str);
		respuestas=lector.readLine();
		System.out.println("Respuesta del servidor: "+respuestas);
		if(!comprobarRes(respuestas)) {
			throw new Exception("Error en el flujo de bytes enviado al servidor");
		}

		//Lectura certificado servidor
		respuestas=lector.readLine();
		System.out.println("Certificado recivido del servidor: "+respuestas);
		base64bytes=baseBite64Decode(respuestas);
		System.out.println("Validando certificado");
		puServerKey=CertificateBulider.validar(base64bytes);
		if(puServerKey!=null) {
			enviarMs("OK");
		}else {
			enviarMs("ERROR");
		}
		
		//Desencriptacion llave simetrica enviada por el servidor
		respuestas=lector.readLine();
		System.out.println("Desencriptando llave simetrica");
		base64bytes=baseBite64Decode(respuestas);
		byte[] llaveSimetrica=Asimetrico.descifrar(keyPair.getPrivate(), ALGA,base64bytes);
		SecretKeySpec symmetricKey = new SecretKeySpec(llaveSimetrica, ALGS);
		KeySimetrica = symmetricKey;
		System.out.println("Llave simetrica desencriptada: "+KeySimetrica);

		//Realizando comprobacion llave simetrica
		respuestas=lector.readLine();
		System.out.println("Respuesta del servidor: "+respuestas);
		System.out.println("Desencriptando reto con llave simetrica");
		base64bytes=baseBite64Decode(respuestas);
		byte[] bytesTextoClaro=Simetrico.descifrarSimetrico(KeySimetrica,base64bytes);
		textoClaro=new String(bytesTextoClaro);
		System.out.println("Mensaje des encriptado: "+textoClaro);
		System.out.println("Enviando respuesta al sevidor");
		bas64str=baseBite64Code(Asimetrico.cifrar(puServerKey, ALGA, textoClaro));
		enviarMs(bas64str);
		respuestas=lector.readLine();
		System.out.println("Respuesta del servidor: "+respuestas);
		if(!comprobarRes(respuestas)) {
			throw new Exception("Error en el reto del servidor");
		}

		//Envio info de usuario
		System.out.println("Enviando id de usuario");
		textoCifrado=baseBite64Code(Simetrico.cifrarSimetrico(KeySimetrica, Integer.toString(id)));
		enviarMs(textoCifrado);

		respuestas=lector.readLine();
		System.out.println("Respuesta del servidor: "+respuestas);
		enviarMs("OK");


	}

	private String baseBite64Code(byte[] bytes) {
		String strCode=DatatypeConverter.printBase64Binary(bytes);
		return strCode;
	}

	private byte[] baseBite64Decode(String strCode) {
		return DatatypeConverter.parseBase64Binary(strCode);

	}

	private boolean comprobarRes(String ms) {
		if(ms.equals("OK")) {
			return true;
		}
		return false;
	}

	private void enviarMs(String ms) {
		System.out.println("Enviando: "+ms);
		escritor.println(ms);
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Iniciando servicio");

		//Lectura datos de comunicacion del cliente
		System.out.println("Ingrese host");
		Scanner reader=new Scanner(System.in);
		String host=reader.nextLine();
		System.out.println("Ingrese puerto");
		int puerto=Integer.parseInt(reader.nextLine());
		System.out.println("Ingrese numero de identificacion de usuario");
		int id=Integer.parseInt(reader.nextLine());
		reader.close();

		Cliente cl=new Cliente(host, puerto,id);
		System.out.println("Iniciando conexion con el servidor");

		if(cl.conectar()) {
			try {
				cl.protocolo();}
			catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
		}else {
			System.out.println("No se pudo establecer la conexion con el servidor tiempo de espera agotado");
		}

		cl.desconectar();
	}
}