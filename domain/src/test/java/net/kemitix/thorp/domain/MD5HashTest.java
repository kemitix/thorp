package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MD5HashTest
        implements WithAssertions {

    @Test
    @DisplayName("recover base64 hash")
    public void recoverBase64Hash() {
        assertThat(MD5HashData.Root.hash.hash64())
                .isEqualTo(MD5HashData.Root.base64);
        assertThat(MD5HashData.Leaf.hash.hash64())
                .isEqualTo(MD5HashData.Leaf.base64);
    }

    @Test
    @DisplayName("hash() strips quotes")
    public void hashStripsQuotes() {
        //given
        String dQuote = "\"";
        MD5Hash md5Hash = MD5Hash.create(dQuote + MD5HashData.Root.hashString + dQuote);
        //when
        String result = md5Hash.hash();
        //then
        assertThat(result).isEqualTo(MD5HashData.Root.hashString);
    }
}
