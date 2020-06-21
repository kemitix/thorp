package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TerminalTest
        implements WithAssertions {

    @Nested
    @DisplayName("progressBar()")
    public class ProgressBar {
        @Test
        @DisplayName("width 10 - 0%") 
        public void width10at0() {
            String bar = Terminal.progressBar(0d, 10d, 12);
            assertThat(bar).isEqualTo("[          ]");
        }
        @Test
        @DisplayName("width 10 - 10%")
        public void width10at10()  {
            String bar = Terminal.progressBar(1d, 10d, 12);
            assertThat(bar).isEqualTo("[█         ]");
        }
        @Test
        @DisplayName("width 10 - 50%")
        public void width10at50()  {
            String bar = Terminal.progressBar(5d, 10d, 12);
            assertThat(bar).isEqualTo("[█████     ]");
        }
        @Test
        @DisplayName("width 1 - 8/8th")
        public void width8of8() {
            String bar = Terminal.progressBar(8d, 8d, 3);
            assertThat(bar).isEqualTo("[█]");
        }
        @Test
        @DisplayName("width 1 - 7/8th")
        public void width7of8() {
            String bar = Terminal.progressBar(7d, 8d, 3);
            assertThat(bar).isEqualTo("[▉]");
        }
        @Test
        @DisplayName("width 1 - 6/8th")
        public void width6of8() {
            String bar = Terminal.progressBar(6d, 8d, 3);
            assertThat(bar).isEqualTo("[▊]");
        }
        @Test
        @DisplayName("width 1 - 5/8th")
        public void width5of8() {
            String bar = Terminal.progressBar(5d, 8d, 3);
            assertThat(bar).isEqualTo("[▋]");
        }
        @Test
        @DisplayName("width 1 - 4/8th")
        public void width4of8() {
            String bar = Terminal.progressBar(4d, 8d, 3);
            assertThat(bar).isEqualTo("[▌]");
        }
        @Test
        @DisplayName("width 1 - 3/8th")
        public void width3of8() {
            String bar = Terminal.progressBar(3d, 8d, 3);
            assertThat(bar).isEqualTo("[▍]");
        }
        @Test
        @DisplayName("width 1 - 2/8th")
        public void width2of8() {
            String bar = Terminal.progressBar(2d, 8d, 3);
            assertThat(bar).isEqualTo("[▎]");
        }
        @Test
        @DisplayName("width 1 - 1/8th")
        public void width1of8() {
            String bar = Terminal.progressBar(1d, 8d, 3);
            assertThat(bar).isEqualTo("[▏]");
        }
    }
}
