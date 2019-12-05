import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import javax.imageio.ImageIO;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class Verify {
	public static void main(String[] args) throws IOException, ImageProcessingException, NoSuchAlgorithmException,
			InvalidKeySpecException, InvalidKeyException, SignatureException {
		// Get the image path
		System.out.println("Enter your image path");
		Scanner sc = new Scanner(System.in);
		String imagePath = sc.nextLine();

		// Get image file from image path
		File jpegFile = new File(imagePath);

		// Get ARGB pixel data (Integer Array)
		BufferedImage im = ImageIO.read(jpegFile);
		int[] argbPixels = new int[im.getWidth() * im.getHeight()]; // Could also let getRGB allocate this array, but
																	// doing it like this to make it more similar to
																	// Android
		im.getRGB(0, 0, im.getWidth(), im.getHeight(), argbPixels, 0, im.getWidth());
		// Convert ARGB Integer Array to Byte Array
		ByteBuffer byteBuffer = ByteBuffer.allocate(argbPixels.length * 4);
		IntBuffer intBuffer = byteBuffer.asIntBuffer();
		intBuffer.put(argbPixels);
		byte[] picBytes = byteBuffer.array();
		intBuffer.clear();
		byteBuffer.clear();

		// Initialize Signature Class for Verifying
		Signature ecdsa;
		ecdsa = Signature.getInstance("SHA256withECDSA");

		// Get X509 Encoded public key specification
		System.out.println("Enter your public key");
		String pubKeyString = sc.nextLine();
		byte publicKeyData[] = Base64.getDecoder().decode(pubKeyString);

		// Generate public key to verify
		X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
		KeyFactory kf = KeyFactory.getInstance("EC");
		PublicKey publicKey = kf.generatePublic(spec);

		// Initialize the verification with public key
		ecdsa.initVerify(publicKey);

		// Put the picture byte array in
		ecdsa.update(picBytes);

		// Extract EXIF tag from image
		String sig = "";
		// Get signature string from EXIF tag
		Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
		for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
			for (Tag tag : directory.getTags()) {
				if (tag.getTagName().equals("Image Description")) {
					sig = tag.getDescription();
				}
			}
		}
		byte signature[] = Base64.getDecoder().decode(sig);

		// Verify the picture
		boolean isAuthentic = ecdsa.verify(signature);
		if (isAuthentic) {
			System.out.println("Yayyy, the image is authentic.");
		} else {
			System.out.println("Oh no:( You have a fake image.");
		}

		// Close the scanner
		sc.close();
	}
}
