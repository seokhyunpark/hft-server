package io.github.seokhyunpark.hft.exchange.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignatureUtilTest {
    private final SignatureUtil signatureUtil = new SignatureUtil();

    @Test
    @DisplayName("Ed25519 알고리즘으로 서명을 생성하고 검증할 수 있어야 한다.")
    void signatureAndVerify() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String data = "apiKey=test&timestamp=0123456789";
        String signatureBase64 = signatureUtil.generateSignature(data, keyPair.getPrivate());

        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(data.getBytes(StandardCharsets.UTF_8));

        boolean isVerified = verifier.verify(Base64.getDecoder().decode(signatureBase64));

        assertThat(isVerified).isTrue();
    }
}
