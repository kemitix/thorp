package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SizeTranslationTest
        implements WithAssertions {

    @Nested
    @DisplayName("sizeInEnglish()")
    public class SizeInEnglish {
        @Test
        @DisplayName("when size is less the 1Kb")
        public void sizeLessThan1Kb() {
            //should be in bytes
            assertThat(SizeTranslation.sizeInEnglish(512))
                    .isEqualTo("512b");
        }

        @Test
        @DisplayName("when size is a less than 10Kb")
        public void sizeLessThan10Kb() {
            //should still be in bytes
            assertThat(SizeTranslation.sizeInEnglish(2000))
                    .isEqualTo("2000b");
        }

        @Test
        @DisplayName("when size is over 10Kb and less than 10Mb")
        public void sizeBetween10KbAnd10Mb() {
            //should be in Kb with zero decimal places
            assertThat(SizeTranslation.sizeInEnglish(5599232))
                    .isEqualTo("5468Kb");
        }

        @Test
        @DisplayName("when size is over 10Mb and less than 10Gb")
        public void sizeBetween10Mb10Gb() {
            //should be in Mb with two decimal place
            assertThat(SizeTranslation.sizeInEnglish(5733789833L))
                    .isEqualTo("5468.17Mb");
        }
        @Test@DisplayName("when size is over 10Gb")
        public void sizeOver10Gb() {
            //should be in Gb with three decimal place
            assertThat(SizeTranslation.sizeInEnglish(5871400857278L))
                    .isEqualTo("5468.168Gb");
        }
    }

}
