package io.github.shj5771.jobbell.support;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HexFormat;

/** 테스트 전용 Ed25519 키 쌍. 디스코드가 하듯 요청에 서명해 서명 검증 경로를 실제로 태운다. */
public final class TestDiscordKeys {

    public static final String PUBLIC_KEY_HEX = "f52975d966e41bdb7b05055c5705745e7e5c8b8f31d4b37f3aa0be99c841151a";

    private static final String PRIVATE_KEY_PKCS8_HEX =
            "302e020100300506032b657004220420c71b434ed9f5a356a70a551423797e882d4dfa57e91b1f5bbba36cce477f2e79";

    public static String sign(String timestamp, String body) {
        try {
            PrivateKey key = KeyFactory.getInstance("Ed25519")
                    .generatePrivate(new PKCS8EncodedKeySpec(HexFormat.of().parseHex(PRIVATE_KEY_PKCS8_HEX)));
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update((timestamp + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private TestDiscordKeys() {
    }
}
