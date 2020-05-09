package Cliente;

import javax.crypto.*;

public class Simetrico{

    private final static String PADDING ="AES/ECB/PKCS5Padding";

    public static byte[] cifrarSimetrico(SecretKey llave, String texto) {
        byte [] textoCifrado;

        try{
            Cipher cifrador= Cipher.getInstance(PADDING);
            byte[] textoClaro=texto.getBytes();

            cifrador.init(Cipher.ENCRYPT_MODE, llave);
            textoCifrado= cifrador.doFinal(textoClaro);

            return textoCifrado;
        }catch(Exception e){
            System.out.println("Exception: "+ e.getMessage());
            return null;
        }
    }


    public static byte[] descifrarSimetrico(SecretKey llave,byte[] texto){
        byte[] textoClaro;
        try{
            Cipher cifrador=Cipher.getInstance(PADDING);
            cifrador.init(Cipher.DECRYPT_MODE, llave);
            textoClaro=cifrador.doFinal(texto);

        }
        catch (Exception e){
            System.out.println("Exception: "+e.getMessage());
            return null;
        }
        return textoClaro;
    }
}