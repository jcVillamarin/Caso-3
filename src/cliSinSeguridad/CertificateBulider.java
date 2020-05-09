package cliSinSeguridad;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateBulider {

	
	public static X509Certificate gc(KeyPair keyPair) throws CertificateException, OperatorCreationException {
		BouncyCastleProvider bc=new BouncyCastleProvider();
		Calendar endCalendar=Calendar.getInstance();
		endCalendar.add(Calendar.YEAR, 10);
		X509v3CertificateBuilder certificateBuilder=new X509v3CertificateBuilder(new X500Name("CN=localhost"), BigInteger.valueOf(1),Calendar.getInstance().getTime(),endCalendar.getTime(),new X500Name("CN=localhost"),SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
		org.bouncycastle.operator.ContentSigner contSig=new JcaContentSignerBuilder("SHA1withRSA").build(keyPair.getPrivate());
		X509CertificateHolder x509CertificateHolder=certificateBuilder.build(contSig);
		
		return new JcaX509CertificateConverter().setProvider(bc).getCertificate(x509CertificateHolder);
	}
	
	public static Key validar(byte[] infoCert) throws CertificateException {
		CertificateFactory cerFactory=CertificateFactory.getInstance("X.509");
		InputStream inStream=new ByteArrayInputStream(infoCert);
		X509Certificate cer=(X509Certificate) cerFactory.generateCertificate(inStream);
		try {
			cer.verify(cer.getPublicKey());
			return cer.getPublicKey();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
