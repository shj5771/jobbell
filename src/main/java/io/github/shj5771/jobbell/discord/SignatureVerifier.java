package io.github.shj5771.jobbell.discord;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;

/**
 * 디스코드 요청 서명(Ed25519) 검증. 서명 대상은 {@code 타임스탬프 헤더 + 요청 본문 원문} 이다.
 *
 * <p>디스코드가 주는 공개키는 32바이트 원본이다. JDK 는 X.509 로 감싼 형태만 읽으므로 고정 접두어를 붙인다.
 */
public class SignatureVerifier {

    private static final String X509_ED25519_PREFIX = "302a300506032b6570032100";

    private final PublicKey publicKey;

    public SignatureVerifier(String publicKeyHex) {
        try {
            byte[] encoded = HexFormat.of().parseHex(X509_ED25519_PREFIX + publicKeyHex);
            this.publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("디스코드 공개키를 읽을 수 없다. hex 64자여야 한다.", e);
        }
    }

    public boolean verify(String signatureHex, String timestamp, byte[] body) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(timestamp.getBytes(StandardCharsets.UTF_8));
            signature.update(body);
            return signature.verify(HexFormat.of().parseHex(signatureHex));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
