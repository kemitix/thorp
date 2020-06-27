package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;

public class HashesTest
        implements WithAssertions {

    @Test
    @DisplayName("mergeAll()")
    public void mergeAll() {
        //given
        HashType key1 = HashType.MD5;
        HashType key2 = HashType.DUMMY;
        MD5Hash value1 = MD5Hash.create("1");
        MD5Hash value2 = MD5Hash.create("2");
        Hashes hashes1 = Hashes.create(key1, value1);
        Hashes hashes2 = Hashes.create(key2, value2);
        //when
        Hashes result = Hashes.mergeAll(Arrays.asList(hashes1, hashes2));
        //then
        assertThat(result.keys()).containsExactlyInAnyOrder(key1, key2);
        assertThat(result.values()).containsExactlyInAnyOrder(value1, value2);
    }

}