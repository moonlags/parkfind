import org.mindrot.jbcrypt.BCrypt;

public class Security {
	public static String hashPassword(String raw) {
		return BCrypt.hashpw(raw, BCrypt.gensalt());
	}

	public static boolean checkPassword(String raw, String hash) {
		return BCrypt.checkpw(raw, hash);
	}
}
